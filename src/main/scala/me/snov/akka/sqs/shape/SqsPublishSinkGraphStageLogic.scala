package me.snov.akka.sqs.shape

import akka.stream._
import akka.stream.stage.{AsyncCallback, GraphStageLogic, InHandler}
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

  var handleAmazonResultCallback: AsyncCallback[SendMessageResult] = _
  var amazonSendMessageHandler: AsyncHandler[SendMessageRequest, SendMessageResult] = _

  override def preStart(): Unit = {
    handleAmazonResultCallback = getAsyncCallback[SendMessageResult](handleAmazonResult)
    amazonSendMessageHandler = new AsyncHandler[SendMessageRequest, SendMessageResult] {
      override def onError(exception: Exception): Unit = {
        log.error(exception, exception.getMessage)
        promise.tryFailure(exception)
      }

      override def onSuccess(request: SendMessageRequest, result: SendMessageResult): Unit =
        handleAmazonResultCallback.invoke(result)
    }

    // This requests one element at the Sink startup.
    pull(in)
  }

  private def handleAmazonResult(result: SendMessageResult): Unit = {
    promise.trySuccess(result)
  }

  setHandler(in, new InHandler {
    override def onPush(): Unit = {
      val sendMessageRequest: SendMessageRequest = grab(in)

      client.sendMessageAsync(sendMessageRequest, amazonSendMessageHandler)

      pull(in)
    }
  })
}
