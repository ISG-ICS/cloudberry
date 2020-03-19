package websocket

import akka.actor.ActorRef
import org.eclipse.jetty.websocket.client.WebSocketClient

class WebSocketFactory {

  def newClient(maxTextMessageSize: Int): WebSocketClient = {
    val socket = new WebSocketClient()
    socket.setMaxTextMessageBufferSize(maxTextMessageSize)
    socket.getPolicy.setMaxTextMessageSize(maxTextMessageSize)
    socket
  }

  def newSocket(out: ActorRef): TwitterMapServerToCloudBerrySocket = new TwitterMapServerToCloudBerrySocket(out)
}
