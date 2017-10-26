package actor

import java.net.URI

import akka.actor._
import akka.stream.Materializer
import org.eclipse.jetty.websocket.client.WebSocketClient
import play.api.libs.json.JsValue
import socket.TwitterMapServerToCloudBerrySocket

import scala.concurrent.ExecutionContext

/**
  * A routing actor that servers for rendering user request into cloudberry request
  *  and transfer cloudberry request/response through websocket connection.
  *
  * @param cloudberryWS Websocket url of cloudberry
  * @param out ActorRef in akka flow representing frontend client
  * @param ec implicit execution context
  * @param materializer implicit materializer
  */
class TwitterMapPigeon (val cloudberryWS: String,
                        val out: ActorRef)
                       (implicit ec: ExecutionContext, implicit val materializer: Materializer) extends Actor with ActorLogging {

  val client: WebSocketClient = new WebSocketClient
  val socket: TwitterMapServerToCloudBerrySocket = new TwitterMapServerToCloudBerrySocket(out)

  override def preStart(): Unit = {
    super.preStart
    client.start()
    client.connect(socket, new URI(cloudberryWS))
  }

  override def postStop(): Unit = {
    super.postStop
    client.stop()
  }

  /**
    * Handles Websocket sending from frontend to twitterMap Server
    */
  override def receive: Receive = {
    case body: JsValue =>
      //TODO validate input json format
      //TODO render json request
      socket.sendMessage(body.toString())
  }
}

object TwitterMapPigeon {
  def props(cloudberryWS: String, out: ActorRef)
           (implicit ec: ExecutionContext, materializer: Materializer) = Props(new TwitterMapPigeon(cloudberryWS, out))
}