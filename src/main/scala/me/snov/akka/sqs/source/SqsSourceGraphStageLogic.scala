package me.snov.akka.sqs.source

import java.util

import akka.stream._
import akka.stream.stage.{AsyncCallback, GraphStageLogic, OutHandler}
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sqs.model.{ReceiveMessageRequest, ReceiveMessageResult}
import me.snov.akka.sqs._
import me.snov.akka.sqs.client.SqsClient

class SqsSourceGraphStageLogic(sqsClient: SqsClient, out: Outlet[SqsMessage], shape: SourceShape[SqsMessage])
  extends GraphStageLogic(shape) {

  private var handleMessagesCallback: AsyncCallback[SqsMessageList] = _
  private var awsReceiveMessagesHandler: AsyncHandler[ReceiveMessageRequest, ReceiveMessageResult] = _
  private val buffer: util.List[SqsMessage] = new util.ArrayList[SqsMessage]()

  override def preStart(): Unit = {
    handleMessagesCallback = getAsyncCallback[SqsMessageList](handleMessages)
    awsReceiveMessagesHandler = new AsyncHandler[ReceiveMessageRequest, ReceiveMessageResult] {

      override def onError(exception: Exception): Unit = ???

      override def onSuccess(request: ReceiveMessageRequest, result: ReceiveMessageResult): Unit = {
        handleMessagesCallback.invoke(result.getMessages)
      }
    }
  }

  def handleMessages(messages: SqsMessageList): Unit = {

    println("handleMessages()")

    if (!messages.isEmpty) {
      buffer.addAll(messages)
    }

    if (!buffer.isEmpty && isAvailable(out)) {
      println("Out is available")

      val msg = buffer.remove(0)
      println(s"push (handleMessages): ${msg.getMessageId}")
      push(shape.out, msg)
    }
  }

  private def loadMessagesAsync() = {
    println(s"Called loadMessages")
    sqsClient.receiveMessagesAsync(awsReceiveMessagesHandler)
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
