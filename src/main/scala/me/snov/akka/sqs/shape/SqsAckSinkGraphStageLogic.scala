package me.snov.akka.sqs.shape

import akka.stream._
import akka.stream.stage.{GraphStageLogic, InHandler}
import me.snov.akka.sqs._
import me.snov.akka.sqs.client.SqsClient

private[sqs] class SqsAckSinkGraphStageLogic(
                                              client: SqsClient,
                                              in: Inlet[MessageActionPair],
                                              shape: SinkShape[MessageActionPair]
 ) extends GraphStageLogic(shape) with StageLogging {

  // This requests one element at the Sink startup.
  override def preStart(): Unit = pull(in)

  setHandler(in, new InHandler {
    override def onPush(): Unit = {
      val (sqsMessage, action) = grab(in)
      action match {
        case Ack() =>
          client.deleteAsync(sqsMessage)
        case RequeueWithDelay(delaySeconds) =>
          client.requeueWithDelayAsync(sqsMessage, delaySeconds)
      }

      pull(in)
    }
  })
}
