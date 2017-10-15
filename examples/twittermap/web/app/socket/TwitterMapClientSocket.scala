package socket

import java.io.IOException
import java.util.concurrent.CountDownLatch

import akka.actor.ActorRef
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.{OnWebSocketConnect, OnWebSocketMessage, WebSocket}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}

@WebSocket
class TwitterMapClientSocket(out: ActorRef) {
  private var sessionOpt: Option[Session] = None
  private val latch = new CountDownLatch(1)
  private val clientLogger = Logger("client")

  @OnWebSocketConnect
  def onConnect(session: Session): Unit = {
    clientLogger.info("Connected to cloudberry websocket server.")
    sessionOpt = Some(session)
    latch.countDown()
  }

  @OnWebSocketMessage
  @throws[IOException]
  def onText(session: Session, message: String): Unit = {
    clientLogger.info("Cloudberry response:" + message)
    out ! renderResponse(message)
  }

  def sendMessage(str: String): Unit = {
    try {
      sessionOpt.get.getRemote.sendString(str)
    }
    catch {
      case e: NoSuchElementException =>
        clientLogger.error("session is not initialized: " + e.getMessage)
      case e: IOException =>
        clientLogger.error(e.getStackTrace.toString)
    }
  }

  def getLatch: CountDownLatch = latch

  private def renderResponse(response: String): JsValue = {
    //Logic of rendering cloudberry response goes here
    Json.parse(response)
  }
}
