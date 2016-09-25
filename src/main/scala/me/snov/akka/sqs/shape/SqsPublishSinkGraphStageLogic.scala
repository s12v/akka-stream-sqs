package me.snov.akka.sqs.shape

import akka.stream._
import akka.stream.stage.{GraphStageLogic, InHandler}
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sqs.model._
import me.snov.akka.sqs.client.SqsClient

import scala.concurrent.Promise

private[sqs] class SqsPublishSinkGraphStageLogic(
                                                  client: SqsClient,
                                                  in: Inlet[SendMessageRequest],
                                                  shape: SinkShape[SendMessageRequest],
                                                  promise: Promise[SendMessageResult]
 ) extends GraphStageLogic(shape) with StageLogging {

  var amazonSendMessageHandler: AsyncHandler[SendMessageRequest, SendMessageResult] = _

  override def preStart(): Unit = {
    amazonSendMessageHandler = new AsyncHandler[SendMessageRequest, SendMessageResult] {
      override def onError(exception: Exception): Unit = {
        log.error(exception, exception.getMessage)
        promise.tryFailure(exception)
      }

      override def onSuccess(request: SendMessageRequest, result: SendMessageResult): Unit =
        promise.trySuccess(result)
    }

    // This requests one element at the Sink startup.
    pull(in)
  }

  setHandler(in, new InHandler {
    override def onPush(): Unit = {
      client.sendMessageAsync(grab(in), amazonSendMessageHandler)
      pull(in)
    }
  })
}
