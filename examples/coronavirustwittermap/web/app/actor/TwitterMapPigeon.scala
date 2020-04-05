package actor

import java.net.URI

import actor.TwitterMapPigeon.{cache, cachedQueries}
import akka.actor._
import akka.stream.Materializer
import controllers.TwitterMapApplication
import org.eclipse.jetty.websocket.client.WebSocketClient
import org.joda.time.DateTime
import play.api.libs.json.{JsArray, JsError, JsObject, JsSuccess, JsValue, Json}
import play.api.{Configuration, Logger}
import websocket.{TwitterMapServerToCloudBerrySocket, WebSocketFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
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
  private val cacheMaxAge = config.getInt("cache.maxAge").getOrElse(10)

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
      val category = (frontEndRequest \ "transform" \ "wrap" \ "category").as[String]
      //before replacing the key, remove the endDate to allow cached result to be retrieved if available
      if (category.equalsIgnoreCase("checkQuerySolvableByView") ||
        category.equalsIgnoreCase("totalCountResult") ||
        category.equalsIgnoreCase("pinResult")) {
        socket.sendMessage(frontEndRequest.toString())
      }
      else {
        (frontEndRequest \ "filter").validate[JsArray] match {
          case JsSuccess(filters, _) => {
            var found = false
            for ((filter, i) <- filters.value.zipWithIndex) {
              if ((filter \ "field").as[String].equalsIgnoreCase("create_at")) {
                val value = (filter \ "values").as[ListBuffer[String]]
                if ((DateTime.now.getMinuteOfHour - DateTime.parse(value(1)).getMinuteOfHour) < cacheMaxAge) { //if the end date is within the last 10 minutes
                  //then remove it from the query and check the cache
                  value -= value(1)
                  val updatedValue = filter.as[JsObject] ++ Json.obj("values" -> value)
                  val updatedQuery = frontEndRequest.as[JsObject] ++ Json.obj("filter" -> filters.value.updated(i, updatedValue))
                  found = true
                  prepareQuery(updatedQuery, frontEndRequest)
                }
                else
                  prepareQuery(frontEndRequest, frontEndRequest)
              }
              if (!found) {
                prepareQuery(frontEndRequest, frontEndRequest)
              }
            }
          }
          case e: JsError => {
            prepareQuery(frontEndRequest, frontEndRequest)
          }
        }
      }
    case e =>
      log.error("Unknown type of request " + e.toString)
  }
  private def prepareQuery(filteredQuery: JsValue, query: JsValue){
      //reformat the json query to be a string to store it as a key in the cache
      val key = filteredQuery.toString().replaceAll("[\\{|\\}|\\[|\\\"|:|\\]]", "")
      if (centralCache.contains(key)) {//check if the query is cached
        if ((DateTime.now.getMinuteOfHour - centralCache(key)._1.getMinuteOfHour) < maxCacheAge) {//check the freshness of the cached query
          clientLogger.info("[Cache] Good! Returning responses from cache for this request! \n" + query)
          val responses = centralCache(key)._2
          for (response <- responses) {
            socket.renderResponse(response)
          }
        }
        else {
          centralCache -= key
          queryCloudberry(key, query)
        }
      }
      else {
        queryCloudberry(key, query)
      }
  }

  private def queryCloudberry(key: String, frontEndRequest: JsValue): Unit = {
    val transform = (frontEndRequest \ "transform").as[JsObject]
    val wrap = (transform \ "wrap").as[JsObject]
    val category = (wrap \ "category").as[String]
      clientLogger.info("[Cache] Well, no cache for this request yet, but will cache it once got \"Done\" message. \n" + frontEndRequest)
      cache(key) = (DateTime.now, List[String]())
      cachedQueries(key) = frontEndRequest
      val updatedWrap = wrap ++ Json.obj("id" -> key)
      val updatedTransform = transform ++ Json.obj("wrap" -> updatedWrap)
      val updatedQuery = frontEndRequest.as[JsObject] ++ Json.obj("transform" -> updatedTransform)
      socket.sendMessage(updatedQuery.toString())

  }

  private def prepareQuery(filteredQuery: JsValue, query: JsValue) {
    //reformat the json query to be a string to store it as a key in the cache
    val key = filteredQuery.toString().replaceAll("[\\{|\\}|\\[|\\\"|:|\\]]", "")
    if (centralCache.contains(key)) { //check if the query is cached
      if ((DateTime.now.getMinuteOfHour - centralCache(key)._1.getMinuteOfHour) < cacheMaxAge) { //check the freshness of the cached query
        clientLogger.info("[Cache] Good! Returning responses from cache for this request! \n" + query)
        val responses = centralCache(key)._2
        for (response <- responses) {
          socket.renderResponse(response)
        }
      }
      else {
        centralCache -= key
        queryCloudberry(key, query)
      }
    }
    else {
      queryCloudberry(key, query)
    }
  }

  private def queryCloudberry(key: String, frontEndRequest: JsValue): Unit = {
    val transform = (frontEndRequest \ "transform").as[JsObject]
    val wrap = (transform \ "wrap").as[JsObject]
    clientLogger.info("[Cache] Well, no cache for this request yet, but will cache it once got \"Done\" message. \n" + frontEndRequest)
    cache(key) = (DateTime.now, List[String]())
    cachedQueries(key) = frontEndRequest
    val updatedWrap = wrap ++ Json.obj("id" -> key)
    val updatedTransform = transform ++ Json.obj("wrap" -> updatedWrap)
    val updatedQuery = frontEndRequest.as[JsObject] ++ Json.obj("transform" -> updatedTransform)
    socket.sendMessage(updatedQuery.toString())

  }
}

object TwitterMapPigeon {
  private val clientLogger = Logger("client")
  private val cache = new mutable.HashMap[String, (DateTime, List[String])]()
  /**
    * cachedQueries is added for debugging purposes to see the cached queries quickly
    */
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
