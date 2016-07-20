package alerts

import akka.actor._
import alerts.WsServer._
import play.api.libs.json.JsObject
import scala.collection.immutable.HashSet
import akka.stream.actor._
import akka.stream.actor.ActorSubscriberMessage.{OnComplete, OnError, OnNext}

object WsServer {
  case class Subscribe(actor: ActorRef)
  def props(guardianActor: ActorRef, bufferSize: Int) = Props(new WsServer(guardianActor, bufferSize))
}

class WsServer(guardianActor: ActorRef, bufferSize: Int) extends ActorSubscriber {
  var wsClient = HashSet.empty[ActorRef]

  override val requestStrategy = WatermarkRequestStrategy(bufferSize, bufferSize - 5)

  override def receive: Receive = {
    case Subscribe(actor) =>
      (context watch actor)
      wsClient = wsClient + actor

    case Terminated(actor) =>
      wsClient = wsClient - actor

    case OnNext(json: JsObject) =>
      wsClient.foreach { _ ! json }

    case OnError(cause) =>
      guardianActor ! cause
      context.stop(self)

    case OnComplete =>
      guardianActor ! new Exception("ws server has been completed")
      context.stop(self)
  }
}