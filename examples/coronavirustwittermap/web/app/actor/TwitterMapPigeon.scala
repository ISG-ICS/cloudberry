package actor

import java.net.URI

import actor.TwitterMapPigeon.cache
import actor.TwitterMapPigeon.cachedQueries
import akka.actor._
import akka.stream.Materializer
import controllers.TwitterMapApplication
import org.eclipse.jetty.websocket.client.WebSocketClient
import org.joda.time.DateTime
import play.api.libs.json.{JsError, JsObject, JsSuccess, Json}
import play.api.{Configuration, Logger}
import play.api.libs.json.JsValue
import websocket.{TwitterMapServerToCloudBerrySocket, WebSocketFactory}

import scala.collection.mutable
import scala.concurrent.ExecutionContext

/**
  * A routing actor that servers for rendering user request into cloudberry request
  * and transfer cloudberry request/response through websocket connection.
  *
  * @param factory                Factory of WebSocketClient
  * @param cloudberryWebsocketURL Websocket url of cloudberry
  * @param out                    ActorRef in akka flow representing frontend client
  * @param maxTextMessageSize     Max size of text messages transmit in ws.
  * @param ec                     implicit execution context
  * @param materializer           implicit materializer
  */
class TwitterMapPigeon(val factory: WebSocketFactory,
                       val cloudberryWebsocketURL: String,
                       val out: ActorRef,
                       val config: Configuration,
                       val maxTextMessageSize: Int)
                      (implicit ec: ExecutionContext, implicit val materializer: Materializer) extends Actor with ActorLogging {

  private val client: WebSocketClient = factory.newClient(maxTextMessageSize)
  private val socket: TwitterMapServerToCloudBerrySocket = factory.newSocket(out, config)
  private val clientLogger = Logger("client")
  private val centralCache = TwitterMapApplication.cache
  private val maxCacheAge = 10 //todo don't hardcode this


  override def preStart(): Unit = {
    super.preStart
    client.start()
    client.connect(socket, new URI(cloudberryWebsocketURL))
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
      val key = frontEndRequest.toString().replaceAll("[\\{|\\}|\\[|\\\"|:|\\]]", "")
      if (centralCache.contains(key)) {
        if ((DateTime.now.getMinuteOfHour - centralCache(key)._1.getMinuteOfHour) < maxCacheAge) {
          clientLogger.info("[Cache] Good! Request has been cached, returning responses from cache.")
          val responses = centralCache(key)._2
          for (response <- responses) {
            socket.renderResponse(response)
          }
        }
        else {
          centralCache -= key
          queryCloudberry(key, frontEndRequest)
        }
      }
      else {
        queryCloudberry(key, frontEndRequest)
      }
    case e =>
      log.error("Unknown type of request " + e.toString)
  }

  private def queryCloudberry(key: String, frontEndRequest: JsValue): Unit = {
    val transform = (frontEndRequest \ "transform").as[JsObject]
    val wrap = (transform \ "wrap").as[JsObject]
    val category = (wrap \ "category").as[String]
    if (!category.equalsIgnoreCase("checkQuerySolvableByView") && !category.equalsIgnoreCase("totalCountResult")) {
      clientLogger.info("[Cache] Well, request has not been cached, we will add it to cache once we see the \"Done\" message.")
      cache(key) = (DateTime.now, List[String]())
      cachedQueries(key) = frontEndRequest
      val updatedWrap = wrap ++ Json.obj("id" -> key)
      val updatedTransform = transform ++ Json.obj("wrap" -> updatedWrap)
      val updatedQuery = frontEndRequest.as[JsObject] ++ Json.obj("transform" -> updatedTransform)
      socket.sendMessage(updatedQuery.toString())
    }
    else {
      socket.sendMessage(frontEndRequest.toString())
    }
  }

  //Logic of rendering cloudberry request goes here
  private def renderRequest(frontEndRequest: JsValue): JsValue = frontEndRequest
}

object TwitterMapPigeon {
  private val clientLogger = Logger("client")
  private val cache = new mutable.HashMap[String, (DateTime, List[String])]()
  private val cachedQueries = new mutable.HashMap[String, JsValue]()

  def addToCache(response: String): Unit = {
    val json = Json.parse(response)
    val id = (json \ "id").as[String]
    val updatedId = json.as[JsObject] ++ Json.obj("id" -> "defaultID")
    val updatedResponse = json.as[JsObject] ++ updatedId
    cache(id) = (cache(id)._1, cache(id)._2 :+ updatedResponse.toString())

    (json \ "value" \ "key").validate[String] match {
      case JsSuccess(value, _) => {
        if (value.contains("done")) {
          clientLogger.info("[Cache] Cool! We got the \"Done\" message for this request! \n" + cachedQueries(id))
          TwitterMapApplication.addToCache(id, cache(id))
          cache -= id
          cachedQueries -= id
        }
      }
      case e: JsError => None

    }


  }

  def props(factory: WebSocketFactory, cloudberryWebsocketURL: String, out: ActorRef, config: Configuration, maxTextMessageSize: Int)
           (implicit ec: ExecutionContext, materializer: Materializer) = Props(new TwitterMapPigeon(factory, cloudberryWebsocketURL, out, config, maxTextMessageSize))
}
