package actor

import java.util.concurrent.Executors

import akka.actor.ActorRef
import akka.stream.{ActorMaterializer, Materializer}
import org.eclipse.jetty.websocket.client.WebSocketClient
import org.mockito.Mockito._
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike
import play.api.libs.json.{JsValue, Json}
import websocket.{TwitterMapServerToCloudBerrySocket, WebSocketFactory}

import scala.concurrent.ExecutionContext

class TwitterMapPigeonTest extends ActorTestBase with SpecificationLike with Mockito {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))
  implicit val materializer: Materializer = ActorMaterializer()

  "TwitterMapPigeon" should {
    "render user request and transmit to cloudberry websocket" in {
      val mockFactory = mock[WebSocketFactory]
      val mockClient = mock[WebSocketClient]
      val mockSocket = mock[TwitterMapServerToCloudBerrySocket]
      val mockOut = mock[ActorRef]
      val cloudberryWS: String = "ws://localhost:9000/ws"

      when(mockFactory.newClient()).thenReturn(mockClient)
      when(mockFactory.newSocket(mockOut)).thenReturn(mockSocket)

      val pigeon = system.actorOf(TwitterMapPigeon.props(mockFactory, cloudberryWS, mockOut))
      val frontEndRequest: JsValue = Json.obj("k1" -> "v1", "k2" -> "v2")

      pigeon ! frontEndRequest
      verify(mockSocket).sendMessage(frontEndRequest.toString())

      ok
    }
  }
}
