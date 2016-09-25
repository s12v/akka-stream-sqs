package me.snov.akka.sqs.shape

import akka.event.{LoggingAdapter, NoLogging}
import akka.stream.ActorMaterializer
import akka.stream.stage.GraphStageLogic

private[sqs] trait StageLogging { self: GraphStageLogic =>

  private var loggingAdapter: LoggingAdapter = _

  def log: LoggingAdapter = {
    if (loggingAdapter eq null) {
      materializer match {
        case actorMaterializer: ActorMaterializer =>
          loggingAdapter = akka.event.Logging(actorMaterializer.system, self.getClass)
        case _ =>
          loggingAdapter = NoLogging
      }
    }

    loggingAdapter
  }
}
