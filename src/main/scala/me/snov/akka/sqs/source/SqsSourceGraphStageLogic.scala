package me.snov.akka.sqs.source

import java.util

import akka.stream._
import akka.stream.stage.{GraphStageLogic, OutHandler}
import me.snov.akka.sqs._
import me.snov.akka.sqs.client.SqsClient

class SqsSourceGraphStageLogic(sqsClient: SqsClient, out: Outlet[SqsMessage], shape: SourceShape[SqsMessage])
  extends GraphStageLogic(shape) {

  private val buffer: util.List[SqsMessage] = new util.ArrayList[SqsMessage]()

  def loadMessages() = {
    println(s"Called loadMessages")
    val messages = sqsClient.receiveMessages()
    if (!messages.isEmpty) {
      println(s"Received ${messages.size()} items")
      buffer.addAll(messages)
    } else {
      println(s"Empty receive")
    }
  }

  setHandler(out, new OutHandler {
    override def onPull(): Unit = {
      println("called OnPull")
      while (buffer.isEmpty) {
        println(s"buffer is empty")
        loadMessages()
      }

      if (!buffer.isEmpty) {
        val msg = buffer.remove(0)
        println(s"push ${msg.getMessageId}")
        push(shape.out, msg)
      }
    }
  })
}
