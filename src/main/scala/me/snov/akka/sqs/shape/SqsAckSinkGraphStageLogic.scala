package me.snov.akka.sqs.shape

import akka.Done
import akka.stream._
import akka.stream.stage.{GraphStageLogic, InHandler}
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sqs.model.{DeleteMessageRequest, DeleteMessageResult, SendMessageRequest, SendMessageResult}
import me.snov.akka.sqs._
import me.snov.akka.sqs.client.SqsClient

import scala.concurrent.Promise

private[sqs] class SqsAckSinkGraphStageLogic(
                                              client: SqsClient,
                                              in: Inlet[MessageActionPair],
                                              shape: SinkShape[MessageActionPair],
                                              promise: Promise[Done]
                                            ) extends GraphStageLogic(shape) with StageLogging {

  private val MaxInFlight = 10
  private var inFlight = 0
  private var isShutdownInProgress = false
  private var amazonSendMessageHandler: AsyncHandler[SendMessageRequest, SendMessageResult] = _
  private var amazonDeleteMessageHandler: AsyncHandler[DeleteMessageRequest, DeleteMessageResult] = _

  setHandler(in, new InHandler {
    override def onPush(): Unit = {
      inFlight += 1
      val (message, action) = grab(in)
      action match {
        case Ack() =>
          client.deleteAsync(message.getReceiptHandle, amazonDeleteMessageHandler)
        case RequeueWithDelay(delaySeconds) =>
          client.sendWithDelayAsync(message.getBody, delaySeconds, amazonSendMessageHandler)
      }

      tryPull()
    }

    @scala.throws[Exception](classOf[Exception])
    override def onUpstreamFailure(exception: Throwable): Unit = {
      log.error(exception, "Upstream failure: {}", exception.getMessage)
      failStage(exception)
      promise.tryFailure(exception)
    }

    @scala.throws[Exception](classOf[Exception])
    override def onUpstreamFinish(): Unit = {
      log.debug("Upstream finish")
      isShutdownInProgress = true
      tryShutdown()
    }
  })

  override def preStart(): Unit = {
    setKeepGoing(true)

    val failureCallback = getAsyncCallback[Throwable](handleFailure)
    val sendCallback = getAsyncCallback[SendMessageResult](handleSend)
    val deleteCallback = getAsyncCallback[DeleteMessageRequest](handleDelete)

    amazonSendMessageHandler = new AsyncHandler[SendMessageRequest, SendMessageResult] {
      override def onError(exception: Exception): Unit =
        failureCallback.invoke(exception)

      override def onSuccess(request: SendMessageRequest, result: SendMessageResult): Unit =
        sendCallback.invoke(result)
    }

    amazonDeleteMessageHandler = new AsyncHandler[DeleteMessageRequest, DeleteMessageResult] {
      override def onError(exception: Exception): Unit =
        failureCallback.invoke(exception)

      override def onSuccess(request: DeleteMessageRequest, result: DeleteMessageResult): Unit =
        deleteCallback.invoke(request)
    }

    // This requests one element at the Sink startup.
    pull(in)
  }

  private def tryPull(): Unit =
    if (inFlight < MaxInFlight && !isClosed(in) && !hasBeenPulled(in)) {
      pull(in)
    }

  private def tryShutdown(): Unit = {
    if (isShutdownInProgress && inFlight <= 0) {
      completeStage()
      promise.trySuccess(Done)
    }
  }

  private def handleFailure(exception: Throwable): Unit = {
    log.error(exception, "Client failure: {}", exception.getMessage)
    inFlight -= 1
    failStage(exception)
    promise.tryFailure(exception)
  }

  private def handleSend(result: SendMessageResult): Unit = {
    log.debug(s"Sent message {}", result.getMessageId)
    inFlight -= 1
    tryShutdown()
    tryPull()
  }

  private def handleDelete(request: DeleteMessageRequest): Unit = {
    log.debug(s"Deleted message {}", request.getReceiptHandle)
    inFlight -= 1
    tryShutdown()
    tryPull()
  }
}
