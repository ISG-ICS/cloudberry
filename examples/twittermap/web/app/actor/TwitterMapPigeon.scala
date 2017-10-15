package actor

import java.net.URI

import akka.actor._
import akka.stream.Materializer
import org.eclipse.jetty.websocket.client.{ClientUpgradeRequest, WebSocketClient}
import play.api.libs.json.JsValue
import socket.TwitterMapClientSocket

import scala.concurrent.ExecutionContext

class TwitterMapPigeon (val cloudberryWS: String,
                        val out: ActorRef)
                       (implicit ec: ExecutionContext, implicit val materializer: Materializer) extends Actor with ActorLogging {

  val twitterMapWebSocketClient: WebSocketClient = new WebSocketClient
  val twitterMapClientSocket: TwitterMapClientSocket = new TwitterMapClientSocket(out)

  override def preStart(): Unit = {
    super.preStart
    twitterMapWebSocketClient.start()
    twitterMapWebSocketClient.connect(twitterMapClientSocket, new URI(cloudberryWS), new ClientUpgradeRequest)
    twitterMapClientSocket.getLatch.await()
  }

  override def postStop(): Unit = {
    super.postStop
    twitterMapWebSocketClient.stop()
  }

  override def receive: Receive = {
    case body: JsValue =>
      //TODO validate input json format
      //TODO render json request
      twitterMapClientSocket.sendMessage(body.toString())
  }
}

object TwitterMapPigeon {
  def props(cloudberryWS: String, out: ActorRef)
           (implicit ec: ExecutionContext, materializer: Materializer) = Props(new TwitterMapPigeon(cloudberryWS, out))
}