package me.snov.akka.sqs

import akka.stream.{ActorMaterializer, KillSwitches}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.testkit.scaladsl.TestSink
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.amazonaws.services.sqs.model.{DeleteMessageRequest, SendMessageRequest}
import me.snov.akka.sqs.client.{SqsClient, SqsSettings}
import me.snov.akka.sqs.sink.SqsAckSinkShape
import me.snov.akka.sqs.source.SqsSourceShape
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}


class ReactiveSqsSpec extends FlatSpec with Matchers with DefaultTestContext with BeforeAndAfter {

  def clean(): Unit = {
    import scala.collection.JavaConversions._

    // purgeQueue doesn't work well with elasticmq
    val sqsClient = SqsClient(defaultSettings)
    for (m <- sqsClient.receiveMessages()) {
      sqsClient.deleteMessage(m)
    }
  }

  before {
    clean()
  }

  it should "pull a message" in {
    val sqsClient = SqsClient(defaultSettings)

    sqsClient.send("foo")
    val sourceUnderTest = Source.fromGraph(SqsSourceShape(defaultSettings))
    val probe = sourceUnderTest.runWith(TestSink.probe[SqsMessage])
    val actual = probe.requestNext()

    probe.cancel()

    sqsClient.deleteMessage(actual)

    actual.getBody shouldBe "foo"
  }

  it should "pull multiple messages" in {

    val sqsClient = SqsClient(defaultSettings)

    sqsClient.send("foo")
    val sourceUnderTest = Source.fromGraph(SqsSourceShape(defaultSettings))
    val probe = sourceUnderTest.runWith(TestSink.probe[SqsMessage])

    val message1 = probe.requestNext()
    message1.getBody shouldBe "foo"

    Thread.sleep(50)

    sqsClient.send("bar")
    val message2 = probe.requestNext()
    message2.getBody shouldBe "bar"

    probe.cancel()

    sqsClient.deleteMessage(message1)
    sqsClient.deleteMessage(message2)
  }

  it should "pull a message, then delete the message" in {

    val awsClientSpy = spy(new AmazonSQSAsyncClient())
    val settings = SqsSettings(
      queueUrl = defaultSettings.queueUrl,
      endpoint = defaultSettings.endpoint,
      waitTimeSeconds = defaultSettings.waitTimeSeconds,
      awsClient = Some(awsClientSpy)
    )

    awsClientSpy.sendMessage(settings.queueUrl, "foo")

    val killSwitch = Source.fromGraph(SqsSourceShape(settings))
      .viaMat(KillSwitches.single)(Keep.right)
      .map({ message: SqsMessage => (message, Ack()) })
      .to(Sink.fromGraph(SqsAckSinkShape(settings)))
      .run()

    Thread.sleep(100)
    killSwitch.shutdown()

    verify(awsClientSpy).deleteMessage(any[DeleteMessageRequest])
  }

  it should "pull a message, then requeue the message" in {

    val awsClientSpy = spy(new AmazonSQSAsyncClient())
    val settings = SqsSettings(
      queueUrl = defaultSettings.queueUrl,
      endpoint = defaultSettings.endpoint,
      waitTimeSeconds = defaultSettings.waitTimeSeconds,
      awsClient = Some(awsClientSpy)
    )

    awsClientSpy.sendMessage(settings.queueUrl, "foo")

    val killSwitch = Source.fromGraph(SqsSourceShape(settings))
      .viaMat(KillSwitches.single)(Keep.right)
      .map({ message: SqsMessage => (message, RequeueWithDelay(31)) })
      .to(Sink.fromGraph(SqsAckSinkShape(settings)))
      .run()

    Thread.sleep(100)
    killSwitch.shutdown()

    verify(awsClientSpy, times(2)).sendMessage(any[SendMessageRequest])
  }

}
