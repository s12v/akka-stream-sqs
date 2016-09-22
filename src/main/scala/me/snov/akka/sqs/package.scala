package me.snov.akka

package object sqs {

  sealed trait MessageAction
  final case class Ack() extends MessageAction
  final case class RequeueWithDelay(delaySeconds: Int) extends MessageAction

  type MessageActionPair = (com.amazonaws.services.sqs.model.Message, MessageAction)
}
