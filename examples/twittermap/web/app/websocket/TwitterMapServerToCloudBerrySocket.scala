package websocket

import java.io.IOException

import akka.actor.ActorRef
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations._
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
//TODO Should be configurable in later PR.
// 5,242,880 = 5 MB
@WebSocket(maxTextMessageSize = 5242880)
class TwitterMapServerToCloudBerrySocket(out: ActorRef) {
  private var session: Session = _
  private val clientLogger = Logger("client")

  @OnWebSocketConnect
  def onConnect(session: Session): Unit = {
    clientLogger.info("Connected to cloudberry websocket server.")
    this.session = session
  }

  /**
    * Handles Websocket received from Cloudberry
    */
  @OnWebSocketMessage
  @throws[IOException]
  def onText(session: Session, message: String): Unit = {
    out ! renderResponse(message)
  }

  @OnWebSocketClose
  def onClose(session: Session, status: Int, reason: String): Unit = {
    clientLogger.info("connection closed.")
  }

  @OnWebSocketError
  def onError(session: Session, cause: Throwable): Unit = {
    clientLogger.error("Websocket to cloudberry error: " + cause.getStackTrace.toString)
  }

  def sendMessage(str: String): Unit = {
    try {
      session.getRemote.sendString(str)
    }
    catch {
      case e: IOException =>
        clientLogger.error(e.getStackTrace.toString)
    }
  }

  private def renderResponse(response: String): JsValue = {
    //Logic of rendering cloudberry response goes here
    Json.parse(response)
  }
}
