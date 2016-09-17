package me.snov.akka.sqs.stage

import akka.stream._
import akka.stream.stage.{GraphStageLogic, InHandler}
import me.snov.akka.sqs._
import me.snov.akka.sqs.client.SqsClient

private[sqs] class SqsAckSinkGraphStageLogic(
                                 sqsClient: SqsClient,
                                 in: Inlet[SqsMessageWithAction],
                                 shape: SinkShape[SqsMessageWithAction]
 ) extends GraphStageLogic(shape) with StageLogging {

  // This requests one element at the Sink startup.
  override def preStart(): Unit = pull(in)

  setHandler(in, new InHandler {
    override def onPush(): Unit = {
      val (sqsMessage, action) = grab(in)
      action match {
        case Ack() =>
          sqsClient.deleteAsync(sqsMessage)
        case RequeueWithDelay(delaySeconds) =>
          sqsClient.requeueWithDelayAsync(sqsMessage, delaySeconds)
      }

      pull(in)
    }
  })
}
