package me.snov.akka.sqs

import akka.actor.{ActorSystem, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{RequestContext, Route}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class TestHttpProxy(
                     interface: String = "localhost",
                     port: Int,
                     remoteHost: String = "localhost",
                     remotePort: Int = 9324
                   ) {

  implicit var system: ActorSystem = createActorSystem()

  private def createActorSystem() = ActorSystem("test-http-server")

  def start(): Unit = {
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val proxy = Route { context: RequestContext =>
      context.log.debug("Opening connection to %s:%d".format(remoteHost, remotePort))
      Source.single(context.request)
        .via(Http(system).outgoingConnection(remoteHost, remotePort))
        .runWith(Sink.head)
        .flatMap(context.complete(_))
    }

    Http().bindAndHandle(handler = proxy, interface = interface, port = port)
  }

  def stop(): Unit = {
    Await.ready(system.terminate(), 1.second)
  }

  def asyncStartAfter(d: FiniteDuration) = {
    system = createActorSystem()
    system.scheduler.scheduleOnce(d, new Runnable {
      override def run(): Unit = start()
    })(system.dispatcher)
  }
}
