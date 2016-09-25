package me.snov.akka.sqs.shape

import akka.Done
import akka.stream._
import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue}
import com.amazonaws.services.sqs.model.{SendMessageRequest, SendMessageResult}
import me.snov.akka.sqs.client.{SqsClient, SqsSettings}

import scala.concurrent.{Future, Promise}

object SqsPublishSinkShape {
  def apply(settings: SqsSettings): SqsPublishSinkShape = apply(SqsClient(settings))

  def apply(client: SqsClient): SqsPublishSinkShape = new SqsPublishSinkShape(client)
}

class SqsPublishSinkShape(client: SqsClient)
  extends GraphStageWithMaterializedValue[SinkShape[SendMessageRequest], Future[SendMessageResult]] {

  val in: Inlet[SendMessageRequest] = Inlet("SqsPublishSinkShape.in")

  override val shape: SinkShape[SendMessageRequest] = SinkShape(in)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes):
  (GraphStageLogic, Future[SendMessageResult]) = {
    val promise = Promise[SendMessageResult]()
    val logic = new SqsPublishSinkGraphStageLogic(client, in, shape, promise)

    (logic, promise.future)
  }
}
