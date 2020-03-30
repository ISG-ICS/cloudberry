package websocket

import java.io.IOException

import actor.TwitterMapPigeon
import akka.actor.ActorRef
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations._
import play.api.Logger
import play.api.libs.json.{JsError, JsObject, JsSuccess, Json}

@WebSocket
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
    val json = Json.parse(message)
    (json \ "category").validate[String] match{
      case JsSuccess(category, _) => {
        if (!category.equalsIgnoreCase("checkQuerySolvableByView") && !category.equalsIgnoreCase("totalCountResult")) {
        TwitterMapPigeon.addToCache(message)
        val updatedResponse = json.as[JsObject] ++ Json.obj("id" -> "defaultID")
          renderResponse(updatedResponse.toString())
      }
      else{
          renderResponse(message)
        }
      }
      case e: JsError => renderResponse(message)

    }
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

  def renderResponse(response: String): Unit = {
    //Logic of rendering cloudberry response goes here
    out ! Json.parse(response)
  }
}
