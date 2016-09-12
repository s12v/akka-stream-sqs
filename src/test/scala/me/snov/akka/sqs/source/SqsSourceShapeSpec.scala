package me.snov.akka.sqs.source

import java.util

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import me.snov.akka.sqs._
import me.snov.akka.sqs.client.SqsClient
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.{FlatSpec, Matchers}

class SqsSourceShapeSpec extends FlatSpec with Matchers {

  implicit val system = ActorSystem("test-system")
  implicit val materializer = ActorMaterializer()


  it should "pull messages from the client" in {

    val message = new SqsMessage().withMessageId("foo")

    val sqsClient = mock[SqsClient]
    when(sqsClient.receiveMessages()).thenReturn(util.Arrays.asList(message))


    val sourceUnderTest = Source.fromGraph(SqsSourceShape(sqsClient))
    val actual = sourceUnderTest
      .runWith(TestSink.probe[SqsMessage])
      .requestNext()

    verify(sqsClient).receiveMessages()
    actual.getMessageId shouldBe "foo"
  }

  it should "use internal buffer" in {

    val message1 = new SqsMessage().withMessageId("foo")
    val message2 = new SqsMessage().withMessageId("bar")

    val sqsClient = mock[SqsClient]
    when(sqsClient.receiveMessages()).thenReturn(util.Arrays.asList(message1, message2))

    val sourceUnderTest = Source.fromGraph(SqsSourceShape(sqsClient))
    val actual = sourceUnderTest
      .runWith(TestSink.probe[SqsMessage])
      .request(2)
      .expectNext()

    verify(sqsClient, times(1)).receiveMessages()
    actual.getMessageId shouldBe "foo"
  }
}
