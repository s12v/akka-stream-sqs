[![Build Status](https://travis-ci.org/s12v/akka-stream-sqs.svg?branch=master)](https://travis-ci.org/s12v/akka-stream-sqs)
[![codecov](https://codecov.io/gh/s12v/akka-stream-sqs/branch/master/graph/badge.svg)](https://codecov.io/gh/s12v/akka-stream-sqs)

# akka-stream-sqs

Reactive SQS implementation for [Akka streams](http://doc.akka.io/docs/akka/current/scala/stream/)

## Overview

- Provides building blocks (partial graphs) for Akka streams integration with SQS
- Based on AWS SDK for Java and operates with raw objects from the SDK
- Lightweight, no unnecessary layers over AWS SDK
- Supports Typesafe config
- Consumer automatically reconnects on failure
- Supports delayed message requeue

## Quick example

### Message processing with acknowledgement

Read SQS configuration from config file, pull messages from the queue, process, and acknowledge.
This stream listens for new messages and never stops.

```
val settings = SqsSettings(system) // use existing ActorSystem
Source.fromGraph(SqsSourceShape(settings))
	.mapAsync(parallelism = 4)({ message: Message => Future {
		println(s"Processing ${message.getMessageId}")

		(message, Ack())
	  }
	})
	.runWith(Sink.fromGraph(SqsAckSinkShape(settings)))
```

### Send a message

Send "hello" to the queue and wait for result.

```
val settings = SqsSettings(system)
val future = Source.single(new SendMessageRequest().withMessageBody("236823645"))
	.runWith(pubSink)
val result: SendMessageResult = Await.result(future, 1.second)	
```

## Components

### SqsSourceShape

- Type: `SourceShape`
- Emits `com.amazonaws.services.sqs.model.Message`

Infinite source of SQS messages.
Only queries Amazon services when there's demand from upstream (i.e. all previous messages have been consumed).
Messages are loaded in batches by `maxNumberOfMessages` and pushed one by one.

When SQS is not available, it tries to reconnect infinitely.


### SqsAckSinkShape

- Type: `SinkShape`
- Accepts `(com.amazonaws.services.sqs.model.Message, MessageAction)`

Acknowledges processed messages.

Your flow must decide which action to take and push it with message:
- `Ack` - delete message from the queue.
- `RequeueWithDelay(delaySeconds: Int)` - schedule a retry.


### SqsPublishSinkShape

- Type: ``SinkShape`
- Accepts `com.amazonaws.services.sqs.model.SendMessageRequest`
- Materialized value: `Future[com.amazonaws.services.sqs.model.SendMessageResult]`

Publishes messages to the Amazon service.
Completes with `SendMessageResult` on success or `Exception` on failure.


### Types

`akka-stream-sqs` uses raw types from AWS SDK when possible.  

- `SqsMessageWithAction` - alias for `(SqsMessage, MessageAction)`
- `MessageActionPair` - either `Ack` which means "delete message"
                        or `RequeueWithDelay(delaySeconds: Int)` which means "requeue and try later"


## Configuration

### Typesafe configuration

If you provide `ActorSystem` to `SqsSettings`, it will read your configuration file:

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

### SqsSettings

Wrapper for AWS SDK settings. You can override client and its configuration, credentials provider, and queue options.

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
