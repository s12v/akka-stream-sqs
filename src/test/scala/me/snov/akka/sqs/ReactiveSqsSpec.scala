package me.snov.akka.sqs

import akka.stream.KillSwitches
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.testkit.scaladsl.TestSink
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.amazonaws.services.sqs.model.{DeleteMessageRequest, SendMessageRequest}
import me.snov.akka.sqs.client.{SqsClient, SqsSettings}
import me.snov.akka.sqs.stage.{SqsAckSinkShape, SqsSourceShape}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.concurrent.duration._

class ReactiveSqsSpec extends FlatSpec with Matchers with DefaultTestContext with BeforeAndAfter {

  def clean(): Unit = {
    import scala.collection.JavaConversions._

    // purgeQueue doesn't work well with elasticmq
    val sqsClient = SqsClient(defaultSettings)
    for (m <- sqsClient.receiveMessages()) {
      sqsClient.delete(m)
    }
  }

  before {
    clean()
  }

  it should "pull a message" taggedAs Integration in {
    val sqsClient = SqsClient(defaultSettings)

    sqsClient.send("foo")
    val sourceUnderTest = Source.fromGraph(SqsSourceShape(defaultSettings))
    val probe = sourceUnderTest.runWith(TestSink.probe[SqsMessage])
    val actual = probe.requestNext()

    probe.cancel()

    sqsClient.deleteAsync(actual)

    actual.getBody shouldBe "foo"
  }

  it should "pull multiple messages" taggedAs Integration in {

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

    sqsClient.deleteAsync(message1)
    sqsClient.deleteAsync(message2)
  }

  it should "pull a message, then delete the message" taggedAs Integration in {

    val awsClientSpy = spy(new AmazonSQSAsyncClient())
    val settings = SqsSettings(
      queueUrl = defaultSettings.queueUrl,
      waitTimeSeconds = defaultSettings.waitTimeSeconds,
      awsClient = Some(awsClientSpy)
    )

    awsClientSpy.sendMessage(settings.queueUrl, "foo")

    val killSwitch = Source.fromGraph(SqsSourceShape(settings))
        .log("test-stream")
      .viaMat(KillSwitches.single)(Keep.right)
      .map({ message: SqsMessage => (message, Ack()) })
      .to(Sink.fromGraph(SqsAckSinkShape(settings)))
      .run()

    Thread.sleep(100)
    killSwitch.shutdown()

    verify(awsClientSpy).deleteMessage(any[DeleteMessageRequest])
  }

  it should "pull a message, then requeue the message" taggedAs Integration in {

    val awsClientSpy = spy(new AmazonSQSAsyncClient())
    val settings = SqsSettings(
      queueUrl = defaultSettings.queueUrl,
      waitTimeSeconds = defaultSettings.waitTimeSeconds,
      awsClient = Some(awsClientSpy)
    )

    awsClientSpy.sendMessage(settings.queueUrl, "t72310000")

    val killSwitch = Source.fromGraph(SqsSourceShape(settings))
      .log("test-1")
      .viaMat(KillSwitches.single)(Keep.right)
      .map({ message: SqsMessage => (message, RequeueWithDelay(3000)) })
      .to(Sink.fromGraph(SqsAckSinkShape(settings)))
      .run()

    Thread.sleep(100)
    killSwitch.shutdown()

    verify(awsClientSpy, times(2)).sendMessage(any[SendMessageRequest])
  }

  it should "reconnect to SQS" taggedAs Integration in {

    val httpProxy = new TestHttpProxy(port = 22701)

    // Send directly to SQS
    val sqsClientWithDirectAccess = SqsClient(defaultSettings)
    sqsClientWithDirectAccess.send("t36264")

    // Start stream via proxy
    val settingsUsingProxy = SqsSettings(
      queueUrl = "http://localhost:22701/queue/queue1",
      waitTimeSeconds = 1
    )
    val probe = Source.fromGraph(SqsSourceShape(settingsUsingProxy))
      .log("test-4")
      .runWith(TestSink.probe[SqsMessage])

    httpProxy.asyncStartAfter(3.second)
    probe.requestNext(10.seconds).getBody shouldBe "t36264"

    httpProxy.stop()
    probe.cancel()
  }

  it should "try again on network failure" taggedAs Integration in {

    val httpProxy = new TestHttpProxy(port = 22700)
    httpProxy.start()

    // Send directly to SQS
    val sqsClientWithDirectAccess = SqsClient(defaultSettings)
    sqsClientWithDirectAccess.send("t1371000")

    // Start stream via proxy
    val settingsUsingProxy = SqsSettings(
      queueUrl = "http://localhost:22700/queue/queue1",
      waitTimeSeconds = 1
    )
    val probe = Source.fromGraph(SqsSourceShape(settingsUsingProxy))
      .log("test-3")
      .runWith(TestSink.probe[SqsMessage])

    // Test the source
    probe.requestNext().getBody shouldBe "t1371000"

    // Interrupt
    httpProxy.stop()

    // Verify it's reconnected
    sqsClientWithDirectAccess.send("t1371111")
    httpProxy.asyncStartAfter(3.seconds)
    probe.requestNext(10.seconds).getBody shouldBe "t1371111"

    httpProxy.stop()
    probe.cancel()
  }
}
