package me.snov.akka.sqs.client

import java.util

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sqs.model._
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClientBuilder}
import collection.JavaConverters._

object SqsClient {
  def apply(settings: SqsSettings): SqsClient = new SqsClient(settings)
}

private[sqs] class SqsClient(settings: SqsSettings) {
  private val amazonSQSClient: AmazonSQSAsync = settings.awsClient.getOrElse {
    val clientBuilder = AmazonSQSAsyncClientBuilder
      .standard()
      .withCredentials(settings.awsCredentialsProvider)

    settings.endpoint foreach clientBuilder.setEndpointConfiguration

    clientBuilder.build()
  }

  private val receiveMessageRequest =
    new ReceiveMessageRequest(settings.queueUrl)
      .withMaxNumberOfMessages(settings.maxNumberOfMessages)
      .withWaitTimeSeconds(settings.waitTimeSeconds)
      .withMessageAttributeNames(settings.messageAttributes.asJava)

  // Set optional request params
  settings.visibilityTimeout.foreach(receiveMessageRequest.setVisibilityTimeout(_))

  def receiveMessage(): util.List[Message] =
    amazonSQSClient.receiveMessage(receiveMessageRequest).getMessages

  def receiveMessageAsync(handler: AsyncHandler[ReceiveMessageRequest, ReceiveMessageResult]): Unit =
    amazonSQSClient.receiveMessageAsync(receiveMessageRequest, handler)

  def deleteAsync(receiptHandle: String, handler: AsyncHandler[DeleteMessageRequest, DeleteMessageResult]): Unit =
    amazonSQSClient.deleteMessageAsync(new DeleteMessageRequest(settings.queueUrl, receiptHandle), handler)

  def sendWithDelayAsync(body: String, delaySeconds: Int,
                         handler: AsyncHandler[SendMessageRequest, SendMessageResult]): Unit =
    amazonSQSClient.sendMessageAsync(
      new SendMessageRequest(settings.queueUrl, body).withDelaySeconds(delaySeconds),
      handler
    )

  def sendMessage(body: String): SendMessageResult =
    sendMessage(new SendMessageRequest().withMessageBody(body))

  def sendMessage(sendMessageRequest: SendMessageRequest): SendMessageResult =
    amazonSQSClient.sendMessage(sendMessageRequest.withQueueUrl(settings.queueUrl))

  def sendMessageAsync(sendMessageRequest: SendMessageRequest,
                       handler: AsyncHandler[SendMessageRequest, SendMessageResult]): Unit =
    amazonSQSClient.sendMessageAsync(sendMessageRequest.withQueueUrl(settings.queueUrl), handler)
}
