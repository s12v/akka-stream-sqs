package me.snov.akka.sqs.client

import java.util

import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model.{DeleteMessageRequest, ReceiveMessageRequest, SendMessageRequest}
import me.snov.akka.sqs._

object SqsClient {
  def apply(settings: SqsClientSettings): SqsClient = new SqsClient(settings)
}

class SqsClient(settings: SqsClientSettings) {

  private val amazonSQSClient = settings.awsClient.getOrElse(
    new AmazonSQSClient(
      settings.awsCredentialsProvider,
      settings.awsClientConfiguration
    )
  )

  // Set optional client params
  settings.endpoint.foreach(amazonSQSClient.setEndpoint)

  private def receiveMessageRequest: ReceiveMessageRequest = {
    val receiveMessageRequest = new ReceiveMessageRequest(settings.queueUrl)

    // Set optional request params
    settings.maxNumberOfMessages.foreach(receiveMessageRequest.setMaxNumberOfMessages(_))
    settings.visibilityTimeout.foreach(receiveMessageRequest.setVisibilityTimeout(_))
    settings.waitTimeSeconds.foreach(receiveMessageRequest.setWaitTimeSeconds(_))

    receiveMessageRequest
  }

  def receiveMessages(): util.List[SqsMessage] = {
    println("*** receiveMessages() called ***")

    amazonSQSClient.receiveMessage(receiveMessageRequest).getMessages
  }

  def deleteMessage(message: SqsMessage) = {
    amazonSQSClient.deleteMessage(
      new DeleteMessageRequest(settings.queueUrl, message.getReceiptHandle)
    )
  }

  def requeueWithDelay(sqsMessage: SqsMessage, delaySeconds: Int): Unit =
    sendWithDelay(sqsMessage.getBody, delaySeconds)

  def send(body: String): Unit = {
    amazonSQSClient.sendMessage(
      new SendMessageRequest(settings.queueUrl, body)
    )
  }

  def sendWithDelay(body: String, delaySeconds: Int): Unit = {
    amazonSQSClient.sendMessage(
      new SendMessageRequest(settings.queueUrl, body)
        .withDelaySeconds(delaySeconds)
    )
  }
}
