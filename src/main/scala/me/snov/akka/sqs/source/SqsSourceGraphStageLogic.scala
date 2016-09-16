package me.snov.akka.sqs.source

import java.util

import akka.stream._
import akka.stream.stage.{AsyncCallback, GraphStageLogic, OutHandler}
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sqs.model.{ReceiveMessageRequest, ReceiveMessageResult}
import me.snov.akka.sqs._
import me.snov.akka.sqs.client.SqsClient
import me.snov.akka.sqs.stage.StageLogging

import scala.concurrent.duration._

private[source] class SqsSourceGraphStageLogic(sqsClient: SqsClient, out: Outlet[SqsMessage], shape: SourceShape[SqsMessage])
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

  def handleMessages(messages: SqsMessageList): Unit = {

    println(s"handleMessages(), got ${messages.size()} messages")

    asyncReceiveMessagesIsInProgress = false
    buffer.addAll(messages)

    if (isAvailable(out)) {
      println("Out is available")
      if (buffer.isEmpty) {
        println( if (!isAvailable(out)) s"port is not available" else "buffer is empty")
        loadMessagesAsync()
      } else {
        val msg = buffer.remove(0)
        println(s"push (handleMessages): ${msg.getMessageId}")
        push(shape.out, msg)
      }
    }
  }

  private def loadMessagesAsync() = {
    println(s"called loadMessagesAsync()")
    if (!asyncReceiveMessagesIsInProgress) {
      asyncReceiveMessagesIsInProgress = true
      sqsClient.receiveMessagesAsync(awsReceiveMessagesHandler)
    }
  }

  setHandler(out, new OutHandler {
    override def onPull(): Unit = {
      println("onPull()")

      if (!buffer.isEmpty) {
        val msg = buffer.remove(0)
        println(s"push (onPull): ${msg.getMessageId}")
        push(shape.out, msg)
      }

      if (buffer.isEmpty) {
        loadMessagesAsync()
      }
    }

    override def onDownstreamFinish(): Unit = {
      println(">>>>>>>>>>>> onDownstreamFinish() <<<<<<<<<<<<<<<<")

      completeStage()
    }
  })
}
