package websocket

import java.io.IOException

import actor.TwitterMapPigeon
import akka.actor.ActorRef
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations._
import play.api.{Configuration, Logger}
import play.api.libs.json.{JsError, JsObject, JsSuccess, JsValue, Json}
import util.{BinaryMessageBuilder, Randomizer}

@WebSocket
class TwitterMapServerToCloudBerrySocket(out: ActorRef, config: Configuration) {
  private var session: Session = _
  private val clientLogger = Logger("client")

  @OnWebSocketConnect
  def onConnect(session: Session): Unit = {
    clientLogger.info("Connected to cloudberry websocket server.")
    this.session = session
  }

  /**
    * Handles Websocket received from Cloudberry
    */
  @OnWebSocketMessage
  @throws[IOException]
  def onText(session: Session, message: String): Unit = {
    val json = Json.parse(message)
    (json \ "category").validate[String] match {
      case JsSuccess(category, _) => {
        if (!category.equalsIgnoreCase("checkQuerySolvableByView")
          && !category.equalsIgnoreCase("totalCountResult") &&
          !category.equalsIgnoreCase("pinResult")) {
          TwitterMapPigeon.addToCache(message)
          val updatedResponse = json.as[JsObject] ++ Json.obj("id" -> "defaultID")
          renderResponse(updatedResponse.toString())
        }
        else {
          renderResponse(message)
        }
      }
      case e: JsError => renderResponse(message)

    }
  }

  @OnWebSocketClose
  def onClose(session: Session, status: Int, reason: String): Unit = {
    clientLogger.info("connection closed.")
  }

  @OnWebSocketError
  def onError(session: Session, cause: Throwable): Unit = {
    clientLogger.error("Websocket to cloudberry error: " + cause.getStackTrace.toString)
  }

  def sendMessage(str: String): Unit = {
    try {
      session.getRemote.sendString(str)
    }
    catch {
      case e: IOException =>
        clientLogger.error(e.getStackTrace.toString)
    }
  }

  def renderResponse(response: String): Unit = {
    //Logic of rendering cloudberry response goes here
    val json = Json.parse(response)

    // use binary transfer for pinmap
    if (config.getBoolean("pinmap.binaryTransfer").getOrElse(false)) {
      // TODO - now the attributes and formats are hard coded, make them configurable in Cloudberry's query interface
      (json \ "category").asOpt[String] match {
        case Some(category) =>
          // handle "pinMapResult" category response specially
          if (category.equalsIgnoreCase("pinMapResult")) {
            (json \ "value" \ "key").asOpt[String] match {
              case Some(key) => // done message
                out ! Left(json)
              case None => { // data message
                // transform json into array[byte]
                /**
                  * 1) pinMapResult response from Cloudberry has format as follows,
                  * { id: "defaultID",
                  *   category: "pinMapResult",
                  *   value: [[
                  *     { id: 1236910669389131776, place.bounding_box: [[-120.582586,46.958017], [-120.49726,47.028542]] },
                  *     { id: 1236509727560880128, coordinate: [-121.944442,37.325656], place.bounding_box: [[...]]},
                  *     ... ...
                  *   ]]
                  * }
                  *
                  * 2) transformed pinMapResult output has format as follows,
                  *  ---- header ----
                  *    id        category  start     end
                  * | VARCHAR | VARCHAR | 8 BYTES | 8 BYTES |
                  *  ---- payload ----
                  *   id1       lng1      lat1      id2       lng2      lat2      ...
                  * | 8 BYTES | 8 BYTES | 8 BYTES | 8 BYTES | 8 BYTES | 8 BYTES | ...
                  *
                  * NOTE: VARCHAR = 1 BYTE of length + length BYTES of real UTF-8 characters
                  * */
                val binaryMessageBuilder = new BinaryMessageBuilder(
                  (json \ "id").as[String],
                  (json \ "category").as[String],
                  (json \ "timeInterval" \ "start").as[Long],
                  (json \ "timeInterval" \ "end").as[Long]
                )
                // loop the tuples and build the binary message
                (json \ "value").asOpt[Array[Array[JsValue]]] match {
                  case Some(tuplesArray) =>
                    tuplesArray(0).foreach(tuple => {
                      val id = (tuple \ "id").as[BigDecimal].bigDecimal
                      val boundingBox = (tuple \ "place.bounding_box").asOpt[Array[Array[Double]]]
                      val coordinate = (tuple \ "coordinate").asOpt[Array[Double]]
                      coordinate match {
                        case Some(lnglat) => {
                          binaryMessageBuilder.add(id.toBigInteger, lnglat(0), lnglat(1))
                        }
                        case None =>
                          // use boundingBox to generate a random coordinate
                          boundingBox match {
                            case Some(box) =>
                              val lng0 = box(0)(0)
                              val lat0 = box(0)(1)
                              val lng1 = box(1)(0)
                              val lat1 = box(1)(1)
                              val lat = Randomizer.rangeRandom(id.doubleValue(), lat0, lat1)
                              val lng = Randomizer.rangeRandom(id.doubleValue() + 79, lng0, lng1)
                              binaryMessageBuilder.add(id.toBigInteger, lng, lat)
                            case None =>
                              clientLogger.error("tuple [" + tuple + "] has no coordinate and bounding_box.")
                          }
                      }
                    })
                  case None =>
                    clientLogger.error("response is not a done message, neither a pinMapResult message.\n" + json)
                }
                out ! Right(binaryMessageBuilder.getBuffer)
              }
            }
          }
          else { // other category query response
            out ! Left(json)
          }
        case None =>
          clientLogger.error("Response from Cloudberry does not contain \"category\":\n" + json)
      }
    }
    else {
      out ! Left(json)
    }
  }
}
