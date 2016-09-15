package me.snov.akka.sqs.sink

import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic}
import me.snov.akka.sqs.SqsMessageWithAction
import me.snov.akka.sqs.client.{SqsClient, SqsSettings}

object SqsAckSinkShape {
  def apply(settings: SqsSettings): SqsAckSinkShape = apply(SqsClient(settings))

  def apply(client: SqsClient): SqsAckSinkShape = new SqsAckSinkShape(client)
}

class SqsAckSinkShape(client: SqsClient)
  extends GraphStage[SinkShape[SqsMessageWithAction]] {
  val in: Inlet[SqsMessageWithAction] = Inlet("SqsAckSinkShape.in")

  override val shape: SinkShape[SqsMessageWithAction] = SinkShape(in)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
    new SqsAckSinkGraphStageLogic(client, in, shape)
  }
}
