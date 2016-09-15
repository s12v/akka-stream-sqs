package me.snov.akka

import java.util

package object sqs {

  sealed trait MessageAction
  case class Ack() extends MessageAction
  case class RequeueWithDelay(delaySeconds: Int) extends MessageAction

  type SqsMessage = com.amazonaws.services.sqs.model.Message
  type SqsMessageList = util.List[SqsMessage]
  type SqsMessageWithAction = (SqsMessage, MessageAction)
}
