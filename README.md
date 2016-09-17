[![Build Status](https://travis-ci.org/s12v/akka-stream-sqs.svg?branch=master)](https://travis-ci.org/s12v/akka-stream-sqs)
[![codecov](https://codecov.io/gh/s12v/akka-stream-sqs/branch/master/graph/badge.svg)](https://codecov.io/gh/s12v/akka-stream-sqs)

# akka-stream-sqs

Reactive SQS implementation for [Akka streams](http://doc.akka.io/docs/akka/current/scala/stream/), based on AWS SDK for Java

## Overview

akka-stream-sqs provides building blocks (stages) for Akka streams integration with SQS.

## Quick example

Read SQS configuration from config file, pull messages from the queue, process, and acknowledge.
This stream listens for new messages and never stops.

```
val settings = SqsSettings(system) // use existing system: ActorSystem
Source.fromGraph(SqsSourceShape(settings))
	.mapAsync(parallelism = 4)({ message: SqsMessage => Future {
		println(s"Processing ${message.getMessageId}")

		(message, Ack())
	  }
	})
	.runWith(Sink.fromGraph(SqsAckSinkShape(settings)))
```

## Configuration


Set up SQS configuration in application.conf:

```
akka-stream-sqs {

  # QueueUrl
  #
  # The URL of the Amazon SQS queue to take action on.
  queue-url = "http://localhost:9324/queue/queue1"

  # MaxNumberOfMessages
  #
  # The maximum number of messages to return. Amazon SQS never returns more messages than this value
  # but may return fewer. Values can be from 1 to 10.
  # Default: 10
  max-number-of-messages = 10

  # WaitTimeSeconds
  #
  # The duration (in seconds) for which the call will wait for a message to arrive in the queue
  # before returning. If a message is available, the call will return sooner than WaitTimeSeconds.
  # Default: 10
  wait-time-seconds = 10

  # VisibilityTimeout
  #
  # The duration (in seconds) that the received messages are hidden from subsequent retrieve requests
  # after being retrieved by a ReceiveMessage request.
  # Optional
  # visibility-timeout = 60

  # AWS endpoint override.
  #
  # Optional
  # endpoint = "http://localhost:9324/"

}
```

## Components

### Types

`akka-stream-sqs` uses raw `Message` type from AWS SDK.  

- `SqsMessage` - alias for `com.amazonaws.services.sqs.model.Message`
- `SqsMessageList` - alias for `util.List[com.amazonaws.services.sqs.model.Message]` - Java-list of AWS messages
- `SqsMessageWithAction` - alias for `(SqsMessage, MessageAction)`
- `MessageAction` - either `Ack` which means "delete message"
                    or `RequeueWithDelay(delaySeconds: Int)` which means "requeue and try later"

### SqsSettings

Wrapper for AWS SDK settings. You can override client and its configuration, credentials provider, and queue options.
It's possible to read configuration from the main configuration file by passing actor `system`. Another option is to
create it in the code.

 - `awsClient` - `AmazonSQSAsync`, by default, `AmazonSQSAsyncClient` is used
 - `awsCredentialsProvider` - `AWSCredentialsProvider`, by default [`DefaultAWSCredentialsProviderChain`](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html)
                              is used
 - `awsClientConfiguration` - `ClientConfiguration`, by default `ClientConfiguration`
 - `queueUrl` - The URL of the Amazon SQS queue to take action on. Queue URLs are case-sensitive.
 - `maxNumberOfMessages` - The maximum number of messages to return. Amazon SQS never returns more messages than this value but may return fewer.
 						   Values can be from 1 to 10. Default is 1. All of the messages are not necessarily returned. Default is `10`
 - `waitTimeSeconds` - The duration (in seconds) for which the call will wait for a message to arrive in the queue before returning.
                       If a message is available, the call will return sooner than WaitTimeSeconds. Default is '10'
 - `visibilityTimeout` - The duration (in seconds) that the received messages are hidden from subsequent retrieve
                         requests after being retrieved by a ReceiveMessage request.

For more information, please refer to [AWS SDK for Java](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/overview-summary.html)

### SqsSourceShape

Infinite source of SQS messages. Emits `com.amazonaws.services.sqs.model.Message`
Only queries the service when there's demand from upstream (i.e. all previous messages have been consumed).
Messages are loaded in batches by `maxNumberOfMessages` and pushed one by one.
When SQS is not available, it tries to reconnect infinitely.

### SqsAckSinkShape

Your application (flow in the stream) must acknowledge each message with `Ack` or postpone with `RequeueWithDelay(delaySeconds: Int)`
To do so, you need to push `SqsMessageWithAction` to the sink - a pair of message and action. 
