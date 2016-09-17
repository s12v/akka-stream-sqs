[![Build Status](https://travis-ci.org/s12v/reactive-sqs.svg?branch=master)](https://travis-ci.org/s12v/reactive-sqs)
[![codecov](https://codecov.io/gh/s12v/reactive-sqs/branch/master/graph/badge.svg)](https://codecov.io/gh/s12v/reactive-sqs)

# reactive-sqs


## Example usage

```
val settings = SqsSettings(system)
val sqsSource = Source.fromGraph(SqsSourceShape(settings))
val sqsAckSink = Sink.fromGraph(SqsAckSinkShape(settings))

sqsSource
.mapAsync(parallelism = 5)({ message: SqsMessage =>
  Future {
	print(s"PROCESSING ${message.getMessageId}")
	Thread.sleep(500)
	(message, Ack())
  }
})
.to(sqsAckSink)
.run()
```

## Example config

```
reactive-sqs {
  # QueueUrl
  # The URL of the Amazon SQS queue to take action on.
  queue-url = "http://localhost:9324/queue/queue1"

  # MaxNumberOfMessages
  # The maximum number of messages to return. Amazon SQS never returns more messages than this value
  # but may return fewer. Values can be from 1 to 10.
  # Default: 10
  max-number-of-messages = 10

  # MaxNumberOfMessages
  # The duration (in seconds) for which the call will wait for a message to arrive in the queue
  # before returning. If a message is available, the call will return sooner than WaitTimeSeconds.
  # Default: 10
  wait-time-seconds = 10

  # VisibilityTimeout
  # The duration (in seconds) that the received messages are hidden from subsequent retrieve requests
  # after being retrieved by a ReceiveMessage request.
  # Optional
  visibility-timeout = 60

  # AWS endpoint override.
  # Optional
  endpoint = "http://localhost:9324/"
}
```
