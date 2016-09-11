package me.snov.akka.sqs.client

import java.util

import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import me.snov.akka.sqs._

object SqsClient {
  def apply(settings: SqsClientSettings): SqsClient = new SqsClient(settings)
}

class SqsClient(settings: SqsClientSettings) {
  val amazonSQSClient = new AmazonSQSClient(
    settings.awsCredentialsProvider,
    settings.awsClientConfiguration
  )

  // Set optional values
  settings.endpoint.foreach(amazonSQSClient.setEndpoint)

  def receiveMessages(): util.List[SqsMessage] = {

    println("*** receiveMessages() called ***")

    val receiveMessageRequest = new ReceiveMessageRequest()

    // Set optional request params
    settings.queueUrl.foreach(receiveMessageRequest.setQueueUrl)
    settings.maxNumberOfMessages.foreach(receiveMessageRequest.setMaxNumberOfMessages(_))
    settings.visibilityTimeout.foreach(receiveMessageRequest.setVisibilityTimeout(_))
    settings.waitTimeSeconds.foreach(receiveMessageRequest.setWaitTimeSeconds(_))

    // Execute request
    amazonSQSClient.receiveMessage(receiveMessageRequest).getMessages
  }
}
