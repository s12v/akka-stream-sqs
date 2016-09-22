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

  def receiveMessage(): util.List[Message] =
    amazonSQSClient.receiveMessage(receiveMessageRequest).getMessages

  def receiveMessageAsync(handler: AsyncHandler[ReceiveMessageRequest, ReceiveMessageResult]): Unit =
    amazonSQSClient.receiveMessageAsync(receiveMessageRequest, handler)

  def deleteAsync(message: Message) =
    amazonSQSClient.deleteMessageAsync( new DeleteMessageRequest(settings.queueUrl, message.getReceiptHandle))

  def requeueWithDelayAsync(sqsMessage: Message, delaySeconds: Int): Unit =
    sendWithDelayAsync(sqsMessage.getBody, delaySeconds)

  private def sendWithDelayAsync(body: String, delaySeconds: Int): Unit =
    amazonSQSClient.sendMessageAsync(new SendMessageRequest(settings.queueUrl, body).withDelaySeconds(delaySeconds))

  def sendMessage(body: String): SendMessageResult =
    sendMessage(new SendMessageRequest().withMessageBody(body))

  def sendMessage(sendMessageRequest: SendMessageRequest): SendMessageResult =
    amazonSQSClient.sendMessage(sendMessageRequest.withQueueUrl(settings.queueUrl))

  def sendMessageAsync(sendMessageRequest: SendMessageRequest,
                       handler: AsyncHandler[SendMessageRequest, SendMessageResult]): Unit =
    amazonSQSClient.sendMessageAsync(sendMessageRequest.withQueueUrl(settings.queueUrl), handler)
}
