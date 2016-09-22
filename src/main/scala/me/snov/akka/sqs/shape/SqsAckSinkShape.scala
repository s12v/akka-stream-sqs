package me.snov.akka.sqs.shape

import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic}
import me.snov.akka.sqs.MessageActionPair
import me.snov.akka.sqs.client.{SqsClient, SqsSettings}

object SqsAckSinkShape {
  def apply(settings: SqsSettings): SqsAckSinkShape = apply(SqsClient(settings))

  def apply(client: SqsClient): SqsAckSinkShape = new SqsAckSinkShape(client)
}

class SqsAckSinkShape(client: SqsClient) extends GraphStage[SinkShape[MessageActionPair]] {
  val in: Inlet[MessageActionPair] = Inlet("SqsAckSinkShape.in")

  override val shape: SinkShape[MessageActionPair] = SinkShape(in)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new SqsAckSinkGraphStageLogic(client, in, shape)
}
