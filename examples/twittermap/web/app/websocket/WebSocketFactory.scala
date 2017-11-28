package websocket

import akka.actor.ActorRef
import org.eclipse.jetty.websocket.client.WebSocketClient

class WebSocketFactory {

  def newClient(): WebSocketClient = {
    val socket = new WebSocketClient()
    socket.setMaxTextMessageBufferSize(5*1024*1024)
    socket
  }

  def newSocket(out: ActorRef): TwitterMapServerToCloudBerrySocket = new TwitterMapServerToCloudBerrySocket(out)
}
