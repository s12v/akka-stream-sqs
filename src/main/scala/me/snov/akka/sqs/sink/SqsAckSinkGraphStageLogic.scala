package me.snov.akka.sqs.sink

import akka.stream._
import akka.stream.stage.{GraphStageLogic, InHandler}
import me.snov.akka.sqs._
import me.snov.akka.sqs.client.SqsClient

class SqsAckSinkGraphStageLogic(
                                 sqsClient: SqsClient,
                                 in: Inlet[SqsMessageWithAction],
                                 shape: SinkShape[SqsMessageWithAction]
 ) extends GraphStageLogic(shape) {

  // This requests one element at the Sink startup.
  override def preStart(): Unit = pull(in)

  setHandler(in, new InHandler {
    override def onPush(): Unit = {

      println("onPush()")

      val (sqsMessage, action) = grab(in)
      action match {
        case Ack() =>
          sqsClient.deleteMessage(sqsMessage)
        case RequeueWithDelay(delaySeconds) =>
          sqsClient.requeueWithDelay(sqsMessage, delaySeconds)
      }

      pull(in)
    }
  })
}
