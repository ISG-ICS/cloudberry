package websocket

import akka.actor.ActorRef
import org.eclipse.jetty.websocket.client.WebSocketClient

class WebSocketFactory {

  def newClient(): WebSocketClient = {
    val socket = new WebSocketClient()
    //TODO Should be configurable in later PR.
    socket.setMaxTextMessageBufferSize(5*1024*1024)
    socket
  }

  def newSocket(out: ActorRef): TwitterMapServerToCloudBerrySocket = new TwitterMapServerToCloudBerrySocket(out)
}
