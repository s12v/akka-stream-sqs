package me.snov.akka.sqs

import akka.actor.ActorSystem
import akka.stream._
import org.scalatest.Tag

trait DefaultTestContext {

  implicit val system = ActorSystem("test-system")
  implicit val materializer = ActorMaterializer()

  object Integration extends Tag("me.snov.akka.sqs.Integration")

  val endpoint = "http://localhost:9324/"
}
