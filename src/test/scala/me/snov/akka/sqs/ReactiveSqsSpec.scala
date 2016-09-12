package me.snov.akka.sqs

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import me.snov.akka.sqs.client.{SqsClient, SqsClientSettings}
import me.snov.akka.sqs.source.SqsSourceShape
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Properties

class ReactiveSqsSpec extends FlatSpec with Matchers {

  implicit val system = ActorSystem("test-system")
  implicit val materializer = ActorMaterializer()

  val endpoint = Properties.envOrNone("SQS_ENDPOINT")
  val queueUrl = Properties.envOrElse("SQS_QUEUE_URL", "http://localhost:9324/queue/queue1")

  it should "pull a message" in {
    val settings = SqsClientSettings(queueUrl = queueUrl, endpoint = endpoint)
    val sqsClient = SqsClient(settings)

    sqsClient.send("foo")

    val sourceUnderTest = Source.fromGraph(SqsSourceShape(sqsClient))
    val actual = sourceUnderTest
      .runWith(TestSink.probe[SqsMessage])
      .requestNext()

    actual.getBody shouldBe "foo"
  }

}
