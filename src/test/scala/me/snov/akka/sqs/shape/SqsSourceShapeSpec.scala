package me.snov.akka.sqs.shape

import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sqs.model.{Message, ReceiveMessageRequest, ReceiveMessageResult}
import me.snov.akka.sqs._
import me.snov.akka.sqs.client.SqsClient
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.{FlatSpec, Matchers}

class SqsSourceShapeSpec extends FlatSpec with Matchers with DefaultTestContext {

  it should "pull messages from the client" in {

    val message = new Message().withMessageId("foo")
    val sqsClient = mock[SqsClient]

    when(sqsClient.receiveMessageAsync(any())).thenAnswer(
      new Answer[Object] {
        override def answer(invocation: InvocationOnMock): Object = {
          invocation
            .getArgument[AsyncHandler[ReceiveMessageRequest, ReceiveMessageResult]](0)
            .onSuccess(
              new ReceiveMessageRequest(),
              new ReceiveMessageResult().withMessages(message, message)
            )
          None
        }
      }
    )

    Source.fromGraph(SqsSourceShape(sqsClient))
      .runWith(TestSink.probe[Message])
      .requestNext(message)

    verify(sqsClient, times(1)).receiveMessageAsync(any())
  }

  it should "use internal buffer" in {

    val message1 = new Message().withMessageId("foo")
    val message2 = new Message().withMessageId("bar")
    val message3 = new Message().withMessageId("baz")

    val sqsClient = mock[SqsClient]

    when(sqsClient.receiveMessageAsync(any())).thenAnswer(
      new Answer[Object] {
        override def answer(invocation: InvocationOnMock): Object = {
          invocation
            .getArgument[AsyncHandler[ReceiveMessageRequest, ReceiveMessageResult]](0)
            .onSuccess(
              new ReceiveMessageRequest(),
              new ReceiveMessageResult().withMessages(message1, message2, message3)
            )
          None
        }
      }
    )

    Source.fromGraph(SqsSourceShape(sqsClient))
      .runWith(TestSink.probe[Message])
      .requestNext(message1)
      .requestNext(message2)

    verify(sqsClient, times(1)).receiveMessageAsync(any())
  }

  it should "use internal buffer and load messages when it's empty" in {

    val message1 = new Message().withMessageId("foo")
    val message2 = new Message().withMessageId("bar")

    val sqsClient = mock[SqsClient]

    when(sqsClient.receiveMessageAsync(any())).thenAnswer(
      new Answer[Object] {
        override def answer(invocation: InvocationOnMock): Object = {
          invocation
            .getArgument[AsyncHandler[ReceiveMessageRequest, ReceiveMessageResult]](0)
            .onSuccess(
              new ReceiveMessageRequest(),
              new ReceiveMessageResult().withMessages(message1, message2)
            )
          None
        }
      }
    )

    Source.fromGraph(SqsSourceShape(sqsClient))
      .runWith(TestSink.probe[Message])
      .requestNext(message1)
      .requestNext(message2)

    verify(sqsClient, times(2)).receiveMessageAsync(any())
  }
}
