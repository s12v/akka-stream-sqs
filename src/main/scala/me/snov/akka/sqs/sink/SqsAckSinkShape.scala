package me.snov.akka.sqs.sink

import akka.Done
import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, GraphStageWithMaterializedValue}
import me.snov.akka.sqs.SqsMessageWithAction
import me.snov.akka.sqs.client.{SqsClient, SqsClientSettings}

import scala.concurrent.{Future, Promise}

object SqsAckSinkShape {
  def apply(settings: SqsClientSettings): SqsAckSinkShape = apply(SqsClient(settings))

  def apply(client: SqsClient): SqsAckSinkShape = new SqsAckSinkShape(client)
}

class SqsAckSinkShape(client: SqsClient)
  extends GraphStageWithMaterializedValue[SinkShape[SqsMessageWithAction], Future[Done]] {
  val in: Inlet[SqsMessageWithAction] = Inlet("SqsAckSinkShape.in")

  override val shape: SinkShape[SqsMessageWithAction] = SinkShape(in)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Done]) = {
    val promise = Promise[Done]()

    (new SqsAckSinkGraphStageLogic(client, in, shape, promise), promise.future)
  }
}
