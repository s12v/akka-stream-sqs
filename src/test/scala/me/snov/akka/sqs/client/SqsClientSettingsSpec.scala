package me.snov.akka.sqs.client

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentialsProvider
import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.mockito.MockitoSugar._

class SqsClientSettingsSpec extends FlatSpec with Matchers {

  def awsCredentialsProvider = mock[AWSCredentialsProvider]
  def awsClientConfiguration = mock[ClientConfiguration]

  it should "parse configuration" in {
    val conf = ConfigFactory.parseString(
      """
        reactive-sqs {
          endpoint = "http://localhost:9324/"
          queue-url = "http://localhost:9324/queue/queue1"
          max-number-of-messages = 10
          visibility-timeout = 60
          wait-time-seconds = 5
        }
      """)
      .getConfig("reactive-sqs")



    val settings = SqsClientSettings(conf, Some(awsCredentialsProvider), Some(awsClientConfiguration))

    settings.endpoint shouldBe Some("http://localhost:9324/")
    settings.queueUrl shouldBe Some("http://localhost:9324/queue/queue1")
    settings.maxNumberOfMessages shouldBe Some(10)
    settings.visibilityTimeout shouldBe Some(60)
    settings.waitTimeSeconds shouldBe Some(5)
  }
}
