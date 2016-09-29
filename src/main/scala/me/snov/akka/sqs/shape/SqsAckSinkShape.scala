package me.snov.akka.sqs.shape

import akka.Done
import akka.stream._
import akka.stream.stage.GraphStageWithMaterializedValue
import me.snov.akka.sqs.MessageActionPair
import me.snov.akka.sqs.client.{SqsClient, SqsSettings}

import scala.concurrent.{Future, Promise}

object SqsAckSinkShape {
  def apply(settings: SqsSettings): SqsAckSinkShape = apply(SqsClient(settings))

  def apply(client: SqsClient): SqsAckSinkShape = new SqsAckSinkShape(client)
}

class SqsAckSinkShape(client: SqsClient)
  extends GraphStageWithMaterializedValue[SinkShape[MessageActionPair], Future[Done]] {
  val in: Inlet[MessageActionPair] = Inlet("SqsAckSinkShape.in")

  override val shape: SinkShape[MessageActionPair] = SinkShape(in)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes) = {
    val promise = Promise[Done]()
    val logic = new SqsAckSinkGraphStageLogic(client, in, shape, promise)

    (logic, promise.future)
  }
}
