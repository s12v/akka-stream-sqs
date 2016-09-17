package me.snov.akka.sqs.client

import java.util

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClient}
import com.amazonaws.services.sqs.model._
import me.snov.akka.sqs._

object SqsClient {
  def apply(settings: SqsSettings): SqsClient = new SqsClient(settings)
}

private[sqs] class SqsClient(settings: SqsSettings) {
  private val amazonSQSClient: AmazonSQSAsync = settings.awsClient.getOrElse(
    new AmazonSQSAsyncClient(settings.awsCredentialsProvider, settings.awsClientConfiguration)
  )

  // Set optional client params
  settings.endpoint.foreach(amazonSQSClient.setEndpoint)

  private val receiveMessageRequest = new ReceiveMessageRequest(settings.queueUrl)
    .withMaxNumberOfMessages(settings.maxNumberOfMessages)
    .withWaitTimeSeconds(settings.waitTimeSeconds)

  // Set optional request params
  settings.visibilityTimeout.foreach(receiveMessageRequest.setVisibilityTimeout(_))

  def receiveMessages(): util.List[SqsMessage] = {
    println("*** receiveMessages() called ***")

    amazonSQSClient.receiveMessage(receiveMessageRequest).getMessages
  }

  def receiveMessagesAsync(handler: AsyncHandler[ReceiveMessageRequest, ReceiveMessageResult]): Unit = {
    println("*** receiveMessagesAsync() called ***")

    amazonSQSClient.receiveMessageAsync(receiveMessageRequest, handler)
  }

  def deleteAsync(message: SqsMessage) =
    amazonSQSClient.deleteMessageAsync( new DeleteMessageRequest(settings.queueUrl, message.getReceiptHandle))

  def delete(message: SqsMessage) =
    amazonSQSClient.deleteMessage(new DeleteMessageRequest(settings.queueUrl, message.getReceiptHandle))

  def requeueWithDelayAsync(sqsMessage: SqsMessage, delaySeconds: Int): Unit =
    sendWithDelayAsync(sqsMessage.getBody, delaySeconds)

  private def sendWithDelayAsync(body: String, delaySeconds: Int): Unit =
    amazonSQSClient.sendMessageAsync(new SendMessageRequest(settings.queueUrl, body).withDelaySeconds(delaySeconds))

  def send(body: String): SendMessageResult =
    amazonSQSClient.sendMessage(new SendMessageRequest(settings.queueUrl, body))
}
