package me.snov.akka

import java.util

package object sqs {

  sealed trait MessageAction
  final case class Ack() extends MessageAction
  final case class RequeueWithDelay(delaySeconds: Int) extends MessageAction

  type SqsMessage = com.amazonaws.services.sqs.model.Message
  type SqsMessageList = util.List[SqsMessage]
  type SqsMessageWithAction = (SqsMessage, MessageAction)
}
