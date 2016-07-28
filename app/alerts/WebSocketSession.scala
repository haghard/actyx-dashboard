package alerts

import java.util.UUID
import play.api.libs.json.JsObject
import akka.actor.{ActorLogging, Props, ActorRef, Actor}

object WebSocketSession {
  def props(webSocketSource: ActorRef) =
    Props(new WebSocketSession(webSocketSource)).withDispatcher("ws-dispatcher")
}

class WebSocketSession(wsSource: ActorRef) extends Actor with ActorLogging {
  val clientUuid = UUID.randomUUID().toString
  log.debug("web-socket client has been activated for {}", clientUuid)

  override def receive: Receive = {
    case reading: JsObject => (wsSource forward reading)
    case completeMsg: akka.actor.Status.Success =>
      log.debug(s"web-socket client has been disconnected $clientUuid")
      context.stop(self)
  }
}
