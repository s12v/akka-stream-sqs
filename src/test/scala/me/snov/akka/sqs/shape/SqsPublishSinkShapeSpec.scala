package me.snov.akka.sqs.shape

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

import scala.concurrent.Await
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
    probe.sendNext(new SendMessageRequest().withMessageBody("test-1"))

    Await.result(future, 1.second).getMessageId shouldBe "test-1"
    verify(sqsClient, times(1)).sendMessageAsync(any(), any())
  }

  it should "report failure" in {

    val sqsClient = mock[SqsClient]
    when(sqsClient.sendMessageAsync(any(), any())).thenAnswer(
      new Answer[Object] {
        override def answer(invocation: InvocationOnMock): Object = {
          invocation
            .getArgument[AsyncHandler[SendMessageRequest, SendMessageResult]](1)
            .onError(new RuntimeException())
          None
        }
      }
    )

    val (probe, future) =
      TestSource.probe[SendMessageRequest]
        .toMat(Sink.fromGraph(SqsPublishSinkShape(sqsClient)))(Keep.both)
        .run()
    probe.sendNext(new SendMessageRequest())

    a [RuntimeException] should be thrownBy {
      Await.result(future, 1.second)
    }

    verify(sqsClient, times(1)).sendMessageAsync(any(), any())
  }

  it should "compete future only once" in {

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
    probe.sendNext(new SendMessageRequest().withMessageBody("test-3"))
    Await.result(future, 1.second).getMessageId shouldBe "test-3"

    probe.sendNext(new SendMessageRequest().withMessageBody("test-4"))
    Await.result(future, 1.second).getMessageId shouldBe "test-3" // Future is already completed
  }
}
