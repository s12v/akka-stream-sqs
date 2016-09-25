package me.snov.akka.sqs.shape

import akka.Done
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.testkit.scaladsl.TestSource
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sqs.model.{SendMessageRequest, SendMessageResult}
import me.snov.akka.sqs._
import me.snov.akka.sqs.client.SqsClient
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class SqsPublishSinkShapeSpec extends FlatSpec with Matchers with DefaultTestContext {

  it should "send a message" in {

    val sqsClient = mock[SqsClient]

    when(sqsClient.sendMessageAsync(any(), any())).thenAnswer(
      new Answer[Object] {
        override def answer(invocation: InvocationOnMock): Object = {
          val sendMessageRequest = invocation.getArgument[SendMessageRequest](0)
          invocation
            .getArgument[AsyncHandler[SendMessageRequest, SendMessageResult]](1)
            .onSuccess(
              sendMessageRequest,
              new SendMessageResult().withMessageId(sendMessageRequest.getMessageBody)
            )
          None
        }
      }
    )

    val (probe, future) =
      TestSource.probe[SendMessageRequest]
        .toMat(Sink.fromGraph(SqsPublishSinkShape(sqsClient)))(Keep.both)
        .run()
    probe
      .sendNext(new SendMessageRequest().withMessageBody("test-1"))
      .sendComplete()

    Await.result(future, 1.second) shouldBe Done
    verify(sqsClient, times(1)).sendMessageAsync(any(), any())
  }

  it should "should fail stage on failure and failure the promise" in {

    val sqsClient = mock[SqsClient]
    when(sqsClient.sendMessageAsync(any(), any())).thenAnswer(
      new Answer[Object] {
        override def answer(invocation: InvocationOnMock): Object = {
          invocation
            .getArgument[AsyncHandler[SendMessageRequest, SendMessageResult]](1)
            .onError(new RuntimeException("Test error"))
          None
        }
      }
    )

    val (probe, future) =
      TestSource.probe[SendMessageRequest]
        .toMat(Sink.fromGraph(SqsPublishSinkShape(sqsClient)))(Keep.both)
        .run()
    probe
      .sendNext(new SendMessageRequest())
      .expectCancellation()

    a [RuntimeException] should be thrownBy {
      Await.result(future, 1.second)
    }

    verify(sqsClient, times(1)).sendMessageAsync(any(), any())
  }

  it should "complete promise after all messages sent" in {

    val sqsClient = mock[SqsClient]
    when(sqsClient.sendMessageAsync(any(), any())).thenAnswer(
      new Answer[Object] {
        override def answer(invocation: InvocationOnMock): Object = {
          Future {
            Thread.sleep(100)
            val sendMessageRequest = invocation.getArgument[SendMessageRequest](0)
            invocation
              .getArgument[AsyncHandler[SendMessageRequest, SendMessageResult]](1)
              .onSuccess(
                sendMessageRequest,
                new SendMessageResult().withMessageId(sendMessageRequest.getMessageBody)
              )
            None
          }
        }
      }
    )

    val (probe, future) =
      TestSource.probe[SendMessageRequest]
        .toMat(Sink.fromGraph(SqsPublishSinkShape(sqsClient)))(Keep.both)
        .run()

    probe
      .sendNext(new SendMessageRequest().withMessageBody("test-10"))
      .sendNext(new SendMessageRequest().withMessageBody("test-11"))
      .sendNext(new SendMessageRequest().withMessageBody("test-12"))
      .sendNext(new SendMessageRequest().withMessageBody("test-13"))
      .sendNext(new SendMessageRequest().withMessageBody("test-14"))
      .sendNext(new SendMessageRequest().withMessageBody("test-15"))
      .sendNext(new SendMessageRequest().withMessageBody("test-16"))
      .sendNext(new SendMessageRequest().withMessageBody("test-17"))
      .sendNext(new SendMessageRequest().withMessageBody("test-18"))
      .sendNext(new SendMessageRequest().withMessageBody("test-19"))
      .sendNext(new SendMessageRequest().withMessageBody("test-20"))
      .sendNext(new SendMessageRequest().withMessageBody("test-21"))
      .sendComplete()

    Await.result(future, 2.second) shouldBe Done
    verify(sqsClient, times(12)).sendMessageAsync(any(), any())
  }
}
