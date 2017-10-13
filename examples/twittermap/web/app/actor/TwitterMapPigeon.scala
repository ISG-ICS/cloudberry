package actor

import akka.actor._
import akka.stream.Materializer
import play.api.libs.json.JsValue
import akka.stream.scaladsl._
import akka.util.ByteString
import play.api.libs.ws.{StreamedResponse, WSClient}
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

class TwitterMapPigeon (val wsClient: WSClient,
                        val cloudberryWS: String,
                        val out: ActorRef,
                        val maxFrameLength: Int)
                       (implicit ec: ExecutionContext, implicit val materializer: Materializer) extends Actor with ActorLogging {

  override def receive: Receive = {
    case body: JsValue =>
      log.error(body.toString())

//      val response: Future[StreamedResponse] =
//        wsClient.url(cloudberryWS).withMethod("POST").withBody(body).stream()
//
//      response.map { res =>
//        if (res.headers.status == 200) {
//
//          log.error(res.headers.toString)
//
//          val sink = Sink.foreach[ByteString] { bytes =>
//            val json = Json.parse(bytes.utf8String)
//            out ! json
//          }
//          res.body
//            .via(Framing.delimiter(ByteString("\n"),
//              maximumFrameLength = maxFrameLength,
//              allowTruncation = true))
//            .runWith(sink)
//            .onFailure { case e =>
//              log.error("NeoActor websocket receiving ... " + e.getMessage)}
//        } else {
//          log.error("Bad Gate Way. Connection code: " + res.headers.toString)
//        }
//      }
    case e =>
      log.error(e.toString)
  }
}

object TwitterMapPigeon {
  def props(wsClient: WSClient, cloudberryWS: String, out: ActorRef, maxFrameLength: Int)
           (implicit ec: ExecutionContext, materializer: Materializer) = Props(new TwitterMapPigeon(wsClient, cloudberryWS, out, maxFrameLength))
}