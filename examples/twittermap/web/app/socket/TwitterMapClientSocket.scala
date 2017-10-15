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
  private var session: Session = _
  private val latch = new CountDownLatch(1)
  private val clientLogger = Logger("client")

  @OnWebSocketConnect
  def onConnect(session: Session): Unit = {
    clientLogger.info("Connected to cloudberry websocket server.")
    this.session = session
    this.latch.countDown()
  }

  @OnWebSocketMessage
  @throws[IOException]
  def onText(session: Session, message: String): Unit = {
    clientLogger.info("Cloudberry response:" + message)
    out ! renderResponse(message)
  }

  def sendMessage(str: String): Unit = {
    try {
      session.getRemote.sendString(str)
    }
    catch {
      case e: IOException =>
        clientLogger.error(e.getCause.getMessage)
    }
  }

  def getLatch: CountDownLatch = latch

  private def renderResponse(response: String): JsValue = {
    //Logic of rendering cloudberry response goes here
    Json.parse(response)
  }
}
