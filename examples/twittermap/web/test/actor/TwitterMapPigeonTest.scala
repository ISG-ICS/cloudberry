package actor

import java.net.URI
import java.util.concurrent.{CountDownLatch, Executors, Future}

import akka.actor.{ActorRef, Props}
import akka.stream.{ActorMaterializer, Materializer}
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.client.WebSocketClient
import org.mockito.Mockito._
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike
import socket.TwitterMapServerToCloudBerrySocket

import scala.concurrent.ExecutionContext

class TwitterMapPigeonTest extends ActorTestBase with SpecificationLike with Mockito {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))
  implicit val materializer: Materializer = ActorMaterializer()

  "TwitterMapPigeon" should {
    "initiate Jetty Websocket Client before started" in {
      val mockLatch = mock[CountDownLatch]
      val mockOut = mock[ActorRef]
      val mockSession = mock[Future[Session]]

      val CloudberryWS: String = "ws://localhost:9000/ws"

      val mockClient = spy(new WebSocketClient())
      val mockSocket = spy(new TwitterMapServerToCloudBerrySocket(mockOut))

      doNothing.when(mockClient).start()
      when(mockClient.connect(mockSocket, new URI(CloudberryWS))).thenReturn(mockSession)
      doNothing.when(mockLatch).await()

      val pigeon = system.actorOf(Props(new TwitterMapPigeon(CloudberryWS, mockOut){
        override val client = mockClient
        override val socket = mockSocket
      }))

      verify(mockClient).start()
      verify(mockClient).connect(mockSocket, new URI(CloudberryWS))
      verify(mockLatch).await()

      ok
    }

    "close Jetty Websocket Client after stopped" in {
      ok
    }

    "render user request and transmit to cloudberry websocket" in {
      ok
    }
  }

}
