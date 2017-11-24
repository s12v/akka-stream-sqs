package me.snov.akka.sqs.client

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.mockito.MockitoSugar._

class SqsClientSettingsSpec extends FlatSpec with Matchers {

  it should "parse configuration" in {
    val conf = ConfigFactory.parseString(
      """
        reactive-sqs {
          endpoint = "http://localhost:9324/"
          region = "eu-west-1"
          queue-url = "http://localhost:9324/queue/queue1"
          max-number-of-messages = 10
          visibility-timeout = 60
          wait-time-seconds = 5
        }
      """)
      .getConfig("reactive-sqs")

    val settings = SqsSettings(
      conf,
      Some(mock[AWSCredentialsProvider]),
      Some(mock[ClientConfiguration])
    )

    settings.endpoint.get.getServiceEndpoint shouldBe "http://localhost:9324/"
    settings.endpoint.get.getSigningRegion shouldBe "eu-west-1"
    settings.queueUrl shouldBe "http://localhost:9324/queue/queue1"
    settings.maxNumberOfMessages shouldBe 10
    settings.visibilityTimeout shouldBe Some(60)
    settings.waitTimeSeconds shouldBe 5
  }

  it should "support optional parameters" in {
    val conf = ConfigFactory.parseString(
      """
        reactive-sqs {
          queue-url = "http://localhost:9324/queue/queue1"
          wait-time-seconds = 5
        }
      """)
      .getConfig("reactive-sqs")

    val settings = SqsSettings(
      conf,
      Some(mock[AWSCredentialsProvider]),
      Some(mock[ClientConfiguration])
    )

    settings.endpoint shouldBe None
    settings.queueUrl shouldBe "http://localhost:9324/queue/queue1"
    settings.maxNumberOfMessages shouldBe 10
    settings.visibilityTimeout shouldBe None
    settings.waitTimeSeconds shouldBe 5
  }
}
