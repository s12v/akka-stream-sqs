package me.snov.akka.sqs

import akka.actor.ActorSystem
import akka.stream._
import org.scalatest.Tag

trait DefaultTestContext {

  implicit val system = ActorSystem("test-system")
  implicit val materializer = ActorMaterializer()
  implicit val ex = materializer.executionContext

  object Integration extends Tag("me.snov.akka.sqs.Integration")

  val endpoint = "http://localhost:9324/"
}
