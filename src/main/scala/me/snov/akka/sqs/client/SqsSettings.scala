package me.snov.akka.sqs.client

import akka.actor.ActorSystem
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.{AWSCredentialsProvider, DefaultAWSCredentialsProviderChain}
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.typesafe.config.Config

object SqsSettings {
  private val defaultAWSCredentialsProvider = new DefaultAWSCredentialsProviderChain()
  private val defaultAWSClientConfiguration = new ClientConfiguration()
  private val defaultMaxNumberOfMessages = 10
  private val defaultWaitTimeSeconds = 10
  private val configurationRoot = "akka-stream-sqs"

  def apply(
             queueUrl: String,
             maxNumberOfMessages: Int = defaultMaxNumberOfMessages,
             waitTimeSeconds: Int = defaultWaitTimeSeconds,
             awsCredentialsProvider: Option[AWSCredentialsProvider] = None,
             awsClientConfiguration: Option[ClientConfiguration] = None,
             awsClient: Option[AmazonSQSAsync] = None,
             endpoint: Option[String] = None,
             visibilityTimeout: Option[Int] = None
           ): SqsSettings =
    new SqsSettings(
      queueUrl = queueUrl,
      maxNumberOfMessages = maxNumberOfMessages,
      waitTimeSeconds = waitTimeSeconds,
      awsClient = awsClient,
      endpoint = endpoint,
      awsCredentialsProvider = awsCredentialsProvider.getOrElse(defaultAWSCredentialsProvider),
      awsClientConfiguration = awsClientConfiguration.getOrElse(defaultAWSClientConfiguration),
      visibilityTimeout = visibilityTimeout
    )

  def apply(system: ActorSystem): SqsSettings = apply(system, None, None)

  def apply(
             system: ActorSystem,
             awsCredentialsProvider: Option[AWSCredentialsProvider],
             awsClientConfiguration: Option[ClientConfiguration]
           ): SqsSettings =
    apply(system.settings.config.getConfig(configurationRoot), awsCredentialsProvider, awsClientConfiguration)

  def apply(config: Config): SqsSettings = apply(config, None, None)

  def apply(
             config: Config,
             awsCredentialsProvider: Option[AWSCredentialsProvider],
             awsClientConfiguration: Option[ClientConfiguration]
           ): SqsSettings = {
    apply(
      queueUrl = config.getString("queue-url"),
      maxNumberOfMessages =
        if (config.hasPath("max-number-of-messages")) config.getInt("max-number-of-messages") else defaultMaxNumberOfMessages,
      waitTimeSeconds =
        if (config.hasPath("wait-time-seconds")) config.getInt("wait-time-seconds") else defaultWaitTimeSeconds,
      awsCredentialsProvider = awsCredentialsProvider,
      awsClientConfiguration = awsClientConfiguration,
      endpoint = if (config.hasPath("endpoint")) Some(config.getString("endpoint")) else None,
      visibilityTimeout = if (config.hasPath("visibility-timeout")) Some(config.getInt("visibility-timeout")) else None
    )
  }
}

case class SqsSettings(
                              queueUrl: String,
                              maxNumberOfMessages: Int,
                              waitTimeSeconds: Int,
                              awsClient: Option[AmazonSQSAsync],
                              endpoint: Option[String],
                              awsCredentialsProvider: AWSCredentialsProvider,
                              awsClientConfiguration: ClientConfiguration,
                              visibilityTimeout: Option[Int]
                            )
