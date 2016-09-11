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

  val sqsClient = mock[SqsClient]

  it should "pull messages from the client" in {

    val message = new SqsMessage().withMessageId("foo")
    val sourceUnderTest = Source.fromGraph(SqsSourceShape(sqsClient))

    when(sqsClient.receiveMessages()).thenReturn(util.Arrays.asList(message))

    val actual = sourceUnderTest
      .runWith(TestSink.probe[SqsMessage])
      .requestNext()

    verify(sqsClient).receiveMessages()
    actual.getMessageId shouldBe "foo"
  }
}
