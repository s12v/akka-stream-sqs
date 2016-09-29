package me.snov.akka.sqs.shape

import akka.Done
import akka.stream._
import akka.stream.stage.{GraphStageLogic, InHandler}
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sqs.model._
import me.snov.akka.sqs.client.SqsClient

import scala.concurrent.Promise
import scala.util.{Failure, Success, Try}

private[sqs] class SqsPublishSinkGraphStageLogic(
                                                  client: SqsClient,
                                                  in: Inlet[SendMessageRequest],
                                                  shape: SinkShape[SendMessageRequest],
                                                  promise: Promise[Done]
                                                ) extends GraphStageLogic(shape) with StageLogging {

  private val MaxInFlight = 10

  private var inFlight = 0
  private var isShutdownInProgress = false
  private var amazonSendMessageHandler: AsyncHandler[SendMessageRequest, SendMessageResult] = _

  setHandler(in, new InHandler {
    override def onPush(): Unit = {
      inFlight += 1
      client.sendMessageAsync(grab(in), amazonSendMessageHandler)

      pullIfPossible()
    }

    @scala.throws[Exception](classOf[Exception])
    override def onUpstreamFailure(ex: Throwable): Unit = {
      failStage(ex)
      promise.tryFailure(ex)
    }

    @scala.throws[Exception](classOf[Exception])
    override def onUpstreamFinish(): Unit = {
      isShutdownInProgress = true
      shutDownIfNoMoreMessagesInFlight()
    }
  })

  override def preStart(): Unit = {
    setKeepGoing(true)

    val handleMessagesCallback = getAsyncCallback[Try[SendMessageResult]](handleResult)

    amazonSendMessageHandler = new AsyncHandler[SendMessageRequest, SendMessageResult] {
      override def onError(exception: Exception): Unit =
        handleMessagesCallback.invoke(Failure(exception))

      override def onSuccess(request: SendMessageRequest, result: SendMessageResult): Unit =
        handleMessagesCallback.invoke(Success(result))
    }

    // This requests one element at the Sink startup.
    pull(in)
  }

  private def shutDownIfNoMoreMessagesInFlight(): Unit = {
    if (inFlight <= 0) {
      completeStage()
      promise.trySuccess(Done)
    }
  }

  private def pullIfPossible(): Unit =
    if (inFlight < MaxInFlight && !isClosed(in) && !hasBeenPulled(in)) {
      pull(in)
    }

  private def handleResult(tryResult: Try[SendMessageResult]): Unit = {
    inFlight -= 1
    tryResult match {
      case Success(result) =>
        log.debug(s"Sent message {}", result.getMessageId)
        if (isShutdownInProgress) {
          shutDownIfNoMoreMessagesInFlight()
        }
      case Failure(exception) =>
        log.error(exception, exception.getMessage)
        failStage(exception)
        promise.tryFailure(exception)
    }

    pullIfPossible()
  }
}
