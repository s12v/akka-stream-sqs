package me.snov.akka.sqs.client

import akka.actor.ActorSystem
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.{AWSCredentialsProvider, DefaultAWSCredentialsProviderChain}
import com.typesafe.config.Config

object SqsClientSettings {
  lazy val defaultAWSCredentialsProvider = new DefaultAWSCredentialsProviderChain()
  lazy val defaultAWSClientConfiguration = new ClientConfiguration()

  def apply(
             awsCredentialsProvider: Option[AWSCredentialsProvider] = None,
             awsClientConfiguration: Option[ClientConfiguration] = None,
             endpoint: Option[String] = None,
             queueUrl: Option[String] = None,
             maxNumberOfMessages: Option[Int] = None,
             visibilityTimeout: Option[Int] = None,
             waitTimeSeconds: Option[Int] = None
           ): SqsClientSettings =
    new SqsClientSettings(
      awsCredentialsProvider.getOrElse(defaultAWSCredentialsProvider),
      awsClientConfiguration.getOrElse(defaultAWSClientConfiguration),
      endpoint = endpoint,
      queueUrl = queueUrl,
      maxNumberOfMessages = maxNumberOfMessages,
      visibilityTimeout = visibilityTimeout,
      waitTimeSeconds = waitTimeSeconds
    )

  def apply(system: ActorSystem): SqsClientSettings = apply(system, None, None)

  def apply(
             system: ActorSystem,
             awsCredentialsProvider: Option[AWSCredentialsProvider],
             awsClientConfiguration: Option[ClientConfiguration]
           ): SqsClientSettings =
    apply(system.settings.config.getConfig("reactive-sqs"), awsCredentialsProvider, awsClientConfiguration)

  def apply(config: Config): SqsClientSettings = apply(config, None, None)

  def apply(
             config: Config,
             awsCredentialsProvider: Option[AWSCredentialsProvider],
             awsClientConfiguration: Option[ClientConfiguration]
  ): SqsClientSettings = {
    apply(
      awsCredentialsProvider,
      awsClientConfiguration,
      endpoint = if (config.hasPath("endpoint")) Some(config.getString("endpoint")) else None,
      queueUrl = if (config.hasPath("queue-url")) Some(config.getString("queue-url")) else None,
      maxNumberOfMessages = if (config.hasPath("max-number-of-messages")) Some(config.getInt("max-number-of-messages")) else None,
      visibilityTimeout = if (config.hasPath("visibility-timeout")) Some(config.getInt("visibility-timeout")) else None,
      waitTimeSeconds = if (config.hasPath("wait-time-seconds")) Some(config.getInt("wait-time-seconds")) else None
    )
  }
}

case class SqsClientSettings(
                        awsCredentialsProvider: AWSCredentialsProvider,
                        awsClientConfiguration: ClientConfiguration,
                        endpoint: Option[String],
                        queueUrl: Option[String],
                        maxNumberOfMessages: Option[Int],
                        visibilityTimeout: Option[Int],
                        waitTimeSeconds: Option[Int]
                      )
