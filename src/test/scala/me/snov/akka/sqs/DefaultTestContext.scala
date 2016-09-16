package me.snov.akka.sqs

import akka.actor.ActorSystem
import akka.stream._
import me.snov.akka.sqs.client.SqsSettings
import org.scalatest.Tag

import scala.util.Properties

trait DefaultTestContext {

  implicit val system = ActorSystem("test-system")
  implicit val materializer = ActorMaterializer()

  object Integration extends Tag("me.snov.akka.sqs.Integration")

  val defaultSettings = SqsSettings(
    queueUrl = Properties.envOrElse("SQS_QUEUE_URL", "http://localhost:9324/queue/queue1"),
    endpoint = Properties.envOrNone("SQS_ENDPOINT"),
    waitTimeSeconds = 1
  )
}
