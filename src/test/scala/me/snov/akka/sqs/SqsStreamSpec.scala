package me.snov.akka.sqs

import akka.Done
import akka.stream.KillSwitches
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.testkit.scaladsl.TestSink
import com.amazonaws.auth.{AWSCredentials, AWSStaticCredentialsProvider, EnvironmentVariableCredentialsProvider}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSAsyncClient, AmazonSQSClient, AmazonSQSClientBuilder}
import com.amazonaws.services.sqs.model.{DeleteMessageRequest, Message, SendMessageRequest}
import me.snov.akka.sqs.client.{SqsClient, SqsSettings}
import me.snov.akka.sqs.shape.{SqsAckSinkShape, SqsPublishSinkShape, SqsSourceShape}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class SqsStreamSpec extends FlatSpec with Matchers with DefaultTestContext with BeforeAndAfter {

  var tempQueueName: String = _
  var tempQueueUrl: String = _
  var testClient: AmazonSQS = _

  before {
    testClient = AmazonSQSClientBuilder.standard().withEndpointConfiguration(new EndpointConfiguration(endpoint, "elasticmq")).build() //new AmazonSQSClient().withEndpoint(endpoint)
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

    sqsClient.sendMessage("t4263924")
    val sourceUnderTest = Source.fromGraph(SqsSourceShape(defaultSettings(tempQueueUrl)))
    val probe = sourceUnderTest.runWith(TestSink.probe[Message])

    probe.requestNext().getBody shouldBe "t4263924"

    probe.cancel()
  }

  it should "pull multiple messages" taggedAs Integration in {
    val sqsClient = SqsClient(defaultSettings(tempQueueUrl))
    sqsClient.sendMessage("t63684823")

    val sourceUnderTest = Source.fromGraph(SqsSourceShape(defaultSettings(tempQueueUrl)))
    val probe = sourceUnderTest.runWith(TestSink.probe[Message])

    probe.requestNext().getBody shouldBe "t63684823"

    sqsClient.sendMessage("t26145284")
    probe.requestNext().getBody shouldBe "t26145284"

    probe.cancel()
  }

  it should "pull a message, then delete the message" taggedAs Integration in {
    val awsClientSpy = spy(new AmazonSQSAsyncClient())
    val settings = SqsSettings(queueUrl = tempQueueUrl, waitTimeSeconds = 1, awsClient = Some(awsClientSpy))
    awsClientSpy.sendMessage(settings.queueUrl, "t88292222")

    val future = Source.fromGraph(SqsSourceShape(settings))
      .log("test-stream")
      .take(1)
      .map({ message: Message => (message, Ack()) })
      .toMat(Sink.fromGraph(SqsAckSinkShape(settings)))(Keep.right)
      .run()

    Await.result(future, 1.second) shouldBe Done
    verify(awsClientSpy).deleteMessageAsync(any[DeleteMessageRequest], any())
  }

  it should "pull a message, then requeue the message" taggedAs Integration in {
    val awsClientSpy = spy(new AmazonSQSAsyncClient())
    val settings = SqsSettings(queueUrl = tempQueueUrl, waitTimeSeconds = 1, awsClient = Some(awsClientSpy))
    awsClientSpy.sendMessage(settings.queueUrl, "t72310000")

    val future = Source.fromGraph(SqsSourceShape(settings))
      .log("test-1")
      .take(1)
      .viaMat(KillSwitches.single)(Keep.right)
      .map({ message: Message => (message, RequeueWithDelay(31)) })
      .toMat(Sink.fromGraph(SqsAckSinkShape(settings)))(Keep.right)
      .run()

    Await.result(future, 1.second) shouldBe Done
    verify(awsClientSpy, times(1)).sendMessageAsync(any[SendMessageRequest], any())
  }

  it should "reconnect on network failure" taggedAs Integration in {
    val httpProxy = new TestHttpProxy(port = 22701)
    httpProxy.start()

    // Send directly to SQS
    val sqsClientWithDirectAccess = SqsClient(defaultSettings(tempQueueUrl))
    sqsClientWithDirectAccess.sendMessage("t1371000")

    // Start stream via proxy
    val settingsUsingProxy = SqsSettings(
      queueUrl = "http://localhost:22701/queue/%s".format(tempQueueName),
      waitTimeSeconds = 1
    )
    val probe = Source.fromGraph(SqsSourceShape(settingsUsingProxy))
      .log("test-1")
      .runWith(TestSink.probe[Message])

    // Test the source
    probe.requestNext().getBody shouldBe "t1371000"

    // Interrupt
    httpProxy.stop()

    // Verify it's reconnected
    sqsClientWithDirectAccess.sendMessage("t1371111")
    httpProxy.asyncStartAfter(3.seconds)
    probe.requestNext(10.seconds).getBody shouldBe "t1371111"

    httpProxy.stop()
    probe.cancel()
  }

  it should "publish and pull a message" taggedAs Integration in {
    val sendMessageRequest = new SendMessageRequest().withMessageBody("236823645")
    val pubSink = Sink.fromGraph(SqsPublishSinkShape(defaultSettings(tempQueueUrl)))
    val consumerSource = Source.fromGraph(SqsSourceShape(defaultSettings(tempQueueUrl)))
    val probe = consumerSource.runWith(TestSink.probe[Message])

    val future = Source.single(sendMessageRequest).runWith(pubSink)
    Await.ready(future, 1.second)

    probe.requestNext().getBody shouldBe "236823645"
    probe.cancel()
  }
}
