package me.snov.akka.sqs.sink

import akka.Done
import akka.stream._
import akka.stream.stage.{GraphStageLogic, InHandler}
import me.snov.akka.sqs._
import me.snov.akka.sqs.client.SqsClient

import scala.concurrent.Promise

class SqsAckSinkGraphStageLogic(
                                 sqsClient: SqsClient,
                                 in: Inlet[SqsMessageWithAction],
                                 shape: SinkShape[SqsMessageWithAction],
                                 promise: Promise[Done]
 ) extends GraphStageLogic(shape) {

  // This requests one element at the Sink startup.
  override def preStart(): Unit = pull(in)

  setHandler(in, new InHandler {
    override def onPush(): Unit = {

      println("called onPush")

      val result: SqsMessageWithAction = grab(in)
      val sqsMessage = result._1
      result._2 match {
        case Ack() =>
          sqsClient.deleteMessage(sqsMessage)
        case RequeueWithDelay(delaySeconds) =>
          sqsClient.requeueWithDelay(sqsMessage, delaySeconds)
      }

      promise.success(Done)
      pull(in)
    }
  })
}
