package controllers

import javax.inject.{Inject, Singleton}

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.Cluster
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import alerts.WsServer.Subscribe
import alerts.{AlertGuardian, WebSocketSession}
import org.reactivestreams.Publisher
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{Action, Controller, WebSocket}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AlertsController @Inject()(val conf: play.api.Configuration)(
    implicit system: ActorSystem,
    mat: Materializer,
    ec: ExecutionContext)
    extends Controller {
  val cluster = Cluster(system)
  val hostname = conf.underlying.getString("akka.remote.netty.tcp.hostname")
  val port = conf.underlying.getInt("akka.remote.netty.tcp.port")
  Logger.info(s"★ ★ ★ ★ ★ ★ Akka-system $hostname:$port ★ ★ ★ ★ ★ ★")

  val bufferSize = conf.getInt("buffer.size").getOrElse(1 << 8)
  val guardian =
    system.actorOf(AlertGuardian.props(conf, bufferSize), "guardian")

  def index = Action { implicit request =>
    Ok(views.html.alerts())
  }

  def ws = WebSocket.acceptOrResult[JsValue, JsValue] { header =>
    val (webSrc, webSink) = createWebSocketFlow
    createFlow(webSrc, webSink).map(Right(_)).recover {
      case e: Exception =>
        Logger.error("Websocket creation error", e)
        val result = InternalServerError(
            Json.obj("error-message" -> "Cannot create websocket"))
        Left(result)
    }
  }

  private def createFlow(webSrc: ActorRef, wsSink: Publisher[JsValue]) = {
    val session = system.actorOf(WebSocketSession.props(webSrc))
    guardian ! Subscribe(session)

    val wsFlow = {
      val sink = Sink.actorRef(session, akka.actor.Status.Success(()))
      val source = Source.fromPublisher[JsValue](wsSink)
      Flow.fromSinkAndSource[JsValue, JsValue](sink, source)
    }

    Future[Flow[JsValue, JsValue, akka.NotUsed]](
        wsFlow.watchTermination() { (_, termination) =>
          termination.foreach { done =>
            Logger.info(s"ws-flow ${webSrc.path.name} has been terminated")
          }
          NotUsed
        }
    )
  }

  private def createWebSocketFlow(): (ActorRef, Publisher[JsValue]) = {
    val source: Source[JsValue, ActorRef] =
      Source.actorRef[JsValue](bufferSize, OverflowStrategy.dropHead)
    val sink = Sink.asPublisher[JsValue](fanout = false)
    source.toMat(sink)(Keep.both).run()
  }
}

//http://doc.akka.io/docs/akka/2.4.7/scala/http/client-side/websocket-support.html#Half-Closed_WebSockets
