package me.snov.akka.sqs

import akka.stream.KillSwitches
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.testkit.scaladsl.TestSink
import com.amazonaws.services.sqs.{AmazonSQSAsyncClient, AmazonSQSClient}
import com.amazonaws.services.sqs.model.{DeleteMessageRequest, SendMessageRequest}
import me.snov.akka.sqs.client.{SqsClient, SqsSettings}
import me.snov.akka.sqs.stage.{SqsAckSinkShape, SqsSourceShape}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.concurrent.duration._

class ReactiveSqsSpec extends FlatSpec with Matchers with DefaultTestContext with BeforeAndAfter {

  var tempQueueName: String = _
  var tempQueueUrl: String = _
  var testClient: AmazonSQSClient = _

  before {
    testClient = new AmazonSQSClient().withEndpoint(endpoint)
    tempQueueName = "qqq%d".format(System.currentTimeMillis())
    tempQueueUrl = testClient.createQueue(tempQueueName).getQueueUrl
    system.log.info(s"Create queue $tempQueueUrl")
  }

  after {
    testClient.deleteQueue(tempQueueUrl)
  }

  private def defaultSettings(queueUrl: String) = SqsSettings(queueUrl = queueUrl, waitTimeSeconds = 1)

  it should "pull a message" taggedAs Integration in {
    val sqsClient = SqsClient(defaultSettings(tempQueueUrl))

    sqsClient.send("t4263924")
    val sourceUnderTest = Source.fromGraph(SqsSourceShape(defaultSettings(tempQueueUrl)))
    val probe = sourceUnderTest.runWith(TestSink.probe[SqsMessage])

    probe.requestNext().getBody shouldBe "t4263924"

    probe.cancel()
  }

  it should "pull multiple messages" taggedAs Integration in {

    val sqsClient = SqsClient(defaultSettings(tempQueueUrl))
    sqsClient.send("t63684823")

    val sourceUnderTest = Source.fromGraph(SqsSourceShape(defaultSettings(tempQueueUrl)))
    val probe = sourceUnderTest.runWith(TestSink.probe[SqsMessage])

    probe.requestNext().getBody shouldBe "t63684823"

    Thread.sleep(50)

    sqsClient.send("t26145284")
    probe.requestNext().getBody shouldBe "t26145284"

    probe.cancel()
  }

  it should "pull a message, then delete the message" taggedAs Integration in {

    val awsClientSpy = spy(new AmazonSQSAsyncClient())
    val settings = SqsSettings(queueUrl = tempQueueUrl, waitTimeSeconds = 1, awsClient = Some(awsClientSpy))
    awsClientSpy.sendMessage(settings.queueUrl, "t88292222")

    val killSwitch = Source.fromGraph(SqsSourceShape(settings))
      .log("test-stream")
      .viaMat(KillSwitches.single)(Keep.right)
      .map({ message: SqsMessage => (message, Ack()) })
      .to(Sink.fromGraph(SqsAckSinkShape(settings)))
      .run()

    Thread.sleep(100)
    killSwitch.shutdown()

    verify(awsClientSpy).deleteMessageAsync(any[DeleteMessageRequest])
  }

  it should "pull a message, then requeue the message" taggedAs Integration in {

    val awsClientSpy = spy(new AmazonSQSAsyncClient())
    val settings = SqsSettings(queueUrl = tempQueueUrl, waitTimeSeconds = 1, awsClient = Some(awsClientSpy))
    awsClientSpy.sendMessage(settings.queueUrl, "t72310000")

    val killSwitch = Source.fromGraph(SqsSourceShape(settings))
      .log("test-1")
      .viaMat(KillSwitches.single)(Keep.right)
      .map({ message: SqsMessage => (message, RequeueWithDelay(31)) })
      .to(Sink.fromGraph(SqsAckSinkShape(settings)))
      .run()

    Thread.sleep(100)
    killSwitch.shutdown()

    verify(awsClientSpy, times(2)).sendMessage(any[SendMessageRequest])
  }

  it should "reconnect to SQS" taggedAs Integration in {

    val httpProxy = new TestHttpProxy(port = 22700)

    // Send directly to SQS
    val sqsClientWithDirectAccess = SqsClient(defaultSettings(tempQueueUrl))
    sqsClientWithDirectAccess.send("t36264")

    // Start stream via proxy
    val settingsUsingProxy = SqsSettings(
      queueUrl = "http://localhost:22700/queue/%s".format(tempQueueName),
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

    val httpProxy = new TestHttpProxy(port = 22701)
    httpProxy.start()

    // Send directly to SQS
    val sqsClientWithDirectAccess = SqsClient(defaultSettings(tempQueueUrl))
    sqsClientWithDirectAccess.send("t1371000")

    // Start stream via proxy
    val settingsUsingProxy = SqsSettings(
      queueUrl = "http://localhost:22701/queue/%s".format(tempQueueName),
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
