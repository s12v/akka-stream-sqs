package me.snov.akka.sqs.sink

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.testkit.scaladsl.TestSource
import me.snov.akka.sqs._
import me.snov.akka.sqs.client.SqsClient
import org.mockito.Mockito._
import org.scalactic.source
import org.scalactic.source.Position
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class SqsAckSinkShapeSpec extends FlatSpec with Matchers {
  it should "delete messages on Ack" in {

    implicit val system = ActorSystem("test-system")
    implicit val materializer = ActorMaterializer()
    val sqsClient = mock[SqsClient]

    val message = new SqsMessage().withMessageId("foo")
    val sinkUnderTest = Sink.fromGraph(SqsAckSinkShape(sqsClient))
    TestSource.probe[SqsMessageWithAction]
      .to(sinkUnderTest)
      .run()
      .sendNext((message, Ack()))


    Await.ready(system.terminate(), 1.second)

    verify(sqsClient, times(1)).deleteMessage(message)
  }

  it should "requeue messages on RequeueWithDelay" in {

    implicit val system = ActorSystem("test-system")
    implicit val materializer = ActorMaterializer()
    val sqsClient = mock[SqsClient]

    val message = new SqsMessage().withMessageId("foo")
    val sinkUnderTest = Sink.fromGraph(SqsAckSinkShape(sqsClient))
    TestSource.probe[SqsMessageWithAction]
      .toMat(sinkUnderTest)(Keep.left)
      .run()
      .sendNext((message, RequeueWithDelay(9)))

    Await.ready(system.terminate(), 1.second)

    verify(sqsClient, times(1)).requeueWithDelay(message, 9)
  }

}
