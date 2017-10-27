package actor

import java.net.URI

import akka.actor._
import akka.stream.Materializer
import org.eclipse.jetty.websocket.client.WebSocketClient
import play.api.Logger
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
  private val clientLogger = Logger("client")

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
    case frontEndRequest: JsValue =>
      clientLogger.info("request from frontend: " + frontEndRequest.toString)
      val cloudBerryRequest = renderRequest(frontEndRequest)
      clientLogger.info("request to cloudberry: " + cloudBerryRequest.toString)
      socket.sendMessage(cloudBerryRequest.toString)
    case e =>
      log.error("Unknown type of request " + e.toString)
  }

  //Logic of rendering cloudberry request goes here
  private def renderRequest(frontEndRequest: JsValue): JsValue = frontEndRequest
}

object TwitterMapPigeon {
  def props(cloudberryWS: String, out: ActorRef)
           (implicit ec: ExecutionContext, materializer: Materializer) = Props(new TwitterMapPigeon(cloudberryWS, out))
}