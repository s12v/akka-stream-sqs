package me.snov.akka.sqs.stage

import java.util

import akka.stream._
import akka.stream.stage.{AsyncCallback, GraphStageLogic, OutHandler}
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sqs.model.{ReceiveMessageRequest, ReceiveMessageResult}
import me.snov.akka.sqs._
import me.snov.akka.sqs.client.SqsClient

import scala.concurrent.duration._

private[sqs] class SqsSourceGraphStageLogic(sqsClient: SqsClient, out: Outlet[SqsMessage], shape: SourceShape[SqsMessage])
  extends GraphStageLogic(shape) with StageLogging {

  private val buffer: util.List[SqsMessage] = new util.ArrayList[SqsMessage]()
  private var handleMessagesCallback: AsyncCallback[SqsMessageList] = _
  private var awsReceiveMessagesHandler: AsyncHandler[ReceiveMessageRequest, ReceiveMessageResult] = _
  private var asyncReceiveMessagesIsInProgress = false
  private val errorCooldown = 5.seconds

  override def preStart(): Unit = {
    handleMessagesCallback = getAsyncCallback[SqsMessageList](handleMessages)
    awsReceiveMessagesHandler = new AsyncHandler[ReceiveMessageRequest, ReceiveMessageResult] {

      override def onError(exception: Exception): Unit = {
        log.error(exception, exception.getMessage)
        materializer.scheduleOnce(errorCooldown, new Runnable {
          override def run(): Unit = handleMessagesCallback.invoke(new util.ArrayList[SqsMessage]())
        })
      }

      override def onSuccess(request: ReceiveMessageRequest, result: ReceiveMessageResult): Unit = {
        handleMessagesCallback.invoke(result.getMessages)
      }
    }
  }

  private def handleMessages(messages: SqsMessageList): Unit = {
    asyncReceiveMessagesIsInProgress = false
    buffer.addAll(messages)
    getHandler(out).onPull()
  }

  private def loadMessagesAsync() = {
    if (!asyncReceiveMessagesIsInProgress) {
      asyncReceiveMessagesIsInProgress = true
      sqsClient.receiveMessagesAsync(awsReceiveMessagesHandler)
    }
  }

  setHandler(out, new OutHandler {
    override def onPull(): Unit = {
      if (!buffer.isEmpty && isAvailable(out)) {
        push(shape.out, buffer.remove(0))
      }

      if (buffer.isEmpty) {
        loadMessagesAsync()
      }
    }
  })
}
