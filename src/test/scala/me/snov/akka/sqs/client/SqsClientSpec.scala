package me.snov.akka.sqs.client

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSAsync}
import com.amazonaws.services.sqs.model.{ReceiveMessageRequest, ReceiveMessageResult}
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.{FlatSpec, Matchers}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._

class SqsClientSpec extends FlatSpec with Matchers {

  it should "call AWS client" in {

    val awsClient = mock[AmazonSQSAsync]

    val sqsClientSettings = SqsSettings(
      awsCredentialsProvider = Some(mock[AWSCredentialsProvider]),
      awsClientConfiguration = Some(mock[ClientConfiguration]),
      awsClient = Some(awsClient),
      queueUrl = ""
    )
    val sqsClient = SqsClient(sqsClientSettings)
    val receiveMessageResult = mock[ReceiveMessageResult]

    when(awsClient.receiveMessage(any[ReceiveMessageRequest])).thenReturn(receiveMessageResult)

    sqsClient.receiveMessages()

    verify(receiveMessageResult).getMessages
  }

  it should "pass parameters with ReceiveMessageRequest" in {

    val awsClient = mock[AmazonSQSAsync]

    val sqsClientSettings = SqsSettings(
      queueUrl = "",
      maxNumberOfMessages = 9,
      waitTimeSeconds = 7,
      awsCredentialsProvider = Some(mock[AWSCredentialsProvider]),
      awsClientConfiguration = Some(mock[ClientConfiguration]),
      awsClient = Some(awsClient),
      visibilityTimeout = Some(75)
    )
    val sqsClient = SqsClient(sqsClientSettings)
    val receiveMessageResult = mock[ReceiveMessageResult]

    val receiveMessageRequest = new ReceiveMessageRequest()
        .withQueueUrl("")
        .withMaxNumberOfMessages(9)
        .withVisibilityTimeout(75)
        .withWaitTimeSeconds(7)

    when(awsClient.receiveMessage(receiveMessageRequest)).thenReturn(receiveMessageResult)

    sqsClient.receiveMessages()

    verify(receiveMessageResult).getMessages
  }
}
