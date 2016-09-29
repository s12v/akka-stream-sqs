package me.snov.akka.sqs.shape

import akka.Done
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.testkit.scaladsl.TestSource
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sqs.model._
import me.snov.akka.sqs._
import me.snov.akka.sqs.client.SqsClient
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class SqsAckSinkShapeSpec extends FlatSpec with Matchers with DefaultTestContext {
  it should "delete messages on Ack" in {

    val sqsClient = mock[SqsClient]
    when(sqsClient.deleteAsync(any(), any())).thenAnswer(
      new Answer[Object] {
        override def answer(invocation: InvocationOnMock): Object = {
          val receiptHandle = invocation.getArgument[String](0)
          val callback = invocation.getArgument[AsyncHandler[DeleteMessageRequest, DeleteMessageResult]](1)
          callback.onSuccess(
            new DeleteMessageRequest().withReceiptHandle(receiptHandle),
            new DeleteMessageResult
          )
          None
        }
      }
    )

    val (probe, future) = TestSource.probe[MessageActionPair]
      .toMat(Sink.fromGraph(SqsAckSinkShape(sqsClient)))(Keep.both)
      .run()

    probe
      .sendNext((new Message().withReceiptHandle("123"), Ack()))
      .sendComplete()

    Await.result(future, 1.second) shouldBe Done
    verify(sqsClient, times(1)).deleteAsync(any(), any())
  }

  it should "requeue messages on RequeueWithDelay" in {

    val sqsClient = mock[SqsClient]
    when(sqsClient.sendWithDelayAsync(any[String], any[Int], any())).thenAnswer(
      new Answer[Object] {
        override def answer(invocation: InvocationOnMock): Object = {
          val body = invocation.getArgument[String](0)
          val delay = invocation.getArgument[Int](1)
          val callback = invocation.getArgument[AsyncHandler[SendMessageRequest, SendMessageResult]](2)
          callback.onSuccess(
            new SendMessageRequest().withMessageBody(body).withDelaySeconds(delay),
            new SendMessageResult().withMessageId("12345")
          )
          None
        }
      }
    )

    val (probe, future) = TestSource.probe[MessageActionPair]
      .toMat(Sink.fromGraph(SqsAckSinkShape(sqsClient)))(Keep.both)
      .run()

    probe
      .sendNext((new Message().withBody("foo"), RequeueWithDelay(9)))
      .sendComplete()

    Await.result(future, 100.second) shouldBe Done
    verify(sqsClient, times(1)).sendWithDelayAsync(any(), any(), any())
  }
}
