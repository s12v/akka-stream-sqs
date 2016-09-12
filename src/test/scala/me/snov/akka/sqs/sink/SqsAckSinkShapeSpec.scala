package me.snov.akka.sqs.sink

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.testkit.scaladsl.TestSource
import me.snov.akka.sqs._
import me.snov.akka.sqs.client.SqsClient
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class SqsAckSinkShapeSpec extends FlatSpec with Matchers {

  implicit val system = ActorSystem("test-system")
  implicit val materializer = ActorMaterializer()

  it should "delete messages on Ack" in {

    val sqsClient = mock[SqsClient]

    val message = new SqsMessage().withMessageId("foo")
    val pair = (message, Ack())

    val sinkUnderTest = Sink.fromGraph(SqsAckSinkShape(sqsClient))
    val (testSource, future) = TestSource.probe[SqsMessageWithAction]
      .toMat(sinkUnderTest)(Keep.both)
      .run()

    testSource.sendNext(pair)

    Await.ready(future, 1.second)
    verify(sqsClient, times(1)).deleteMessage(message)
  }

  it should "requeue messages on RequeueWithDelay" in {

    val sqsClient = mock[SqsClient]

    val message = new SqsMessage().withMessageId("foo")
    val pair = (message, RequeueWithDelay(9))

    val sinkUnderTest = Sink.fromGraph(SqsAckSinkShape(sqsClient))
    val (testSource, future) = TestSource.probe[SqsMessageWithAction]
      .toMat(sinkUnderTest)(Keep.both)
      .run()

    testSource.sendNext(pair)

    Await.ready(future, 1.second)
    verify(sqsClient, times(1)).requeueWithDelay(message, 9)
  }
}
