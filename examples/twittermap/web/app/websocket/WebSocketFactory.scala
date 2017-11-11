package websocket

import akka.actor.ActorRef
import org.eclipse.jetty.websocket.client.WebSocketClient

class WebSocketFactory {

  def newClient(): WebSocketClient = new WebSocketClient()

  def newSocket(out: ActorRef): TwitterMapServerToCloudBerrySocket = new TwitterMapServerToCloudBerrySocket(out)
}
