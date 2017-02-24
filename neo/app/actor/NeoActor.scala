package actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.Materializer
import akka.util.{ByteString, _}
import akka.stream.scaladsl._
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.schema.TimeField
import models.{GeoLevel, UserRequest}
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class NeoActor(out: ActorRef, ws: WSClient, host: String, config: Config)
              (implicit ec: ExecutionContext, implicit val materializer: Materializer) extends Actor with ActorLogging {

  import NeoActor._
  import RequestType._
  implicit val timeout: Timeout = Timeout(20.minutes)

  override def receive: Receive = {
    case json: JsValue =>
      json.validate[UserRequest].map { userRequest =>
        val berryRequest = generateCBerryRequest(userRequest)
        val url = "http://" + host + "/berry"
        handleSamplingResponse(url, berryRequest)
        handleSlicingResponse(url, berryRequest)
      }.recoverTotal {
        e => out ! JsError.toJson(e)
      }
  }

  private def handleSamplingResponse(url: String, berryRequest: Map[RequestType.Value, JsValue]): Unit = {
    handleResponse(url, Sample, berryRequest(Sample))
  }

  private def handleSlicingResponse(url: String, berryRequest: Map[RequestType.Value, JsValue]): Unit = {
    val groupTimePlaceTags = Seq(ByTime, ByPlace, ByHashTag).map(berryRequest(_))
    val batchJson = JsObject(Seq(
      "batch" -> JsArray(groupTimePlaceTags),
      "option" -> JsObject(Seq("sliceMillis" -> JsNumber(2000))
      )))
    handleResponse(url, Batch, batchJson)
  }

  private def handleResponse(url: String, requestType: RequestType.Value, requestBody: JsValue): Unit = {
    val response: Future[StreamedResponse] =
      ws.url(url).withMethod("POST").withBody(requestBody).stream()

    response.map{ res =>
      if(res.headers.status == 200){
        val sink = Sink.foreach[ByteString] { bytes =>
          val json = Json.parse(bytes.utf8String)
          out ! Json.obj("key" -> requestType, "value" -> json)
        }
        res.body.via(Framing.delimiter(ByteString("\n"),
                     maximumFrameLength = config.MaxFrameLengthForNeoWS,
                     allowTruncation = true))
                .runWith(sink)
                .onFailure { case e => Logger.logger.error("NeoActor websocket receiving ... " + e.getMessage) }
      } else {
        Logger.logger.error("Bad Gate Way. Connection code: " + res.headers.status)
      }
    }
  }
}

object NeoActor {
  def props(out: ActorRef, ws: WSClient, host: String, config: Config)
           (implicit ec: ExecutionContext, materializer: Materializer) = Props(new NeoActor(out, ws, host, config))

  object RequestType extends Enumeration {
    val ByPlace = Value("byPlace")
    val ByTime = Value("byTime")
    val ByHashTag = Value("byHashTag")
    val Sample = Value("sample")
    val Batch = Value("batch")
  }

  def generateCBerryRequest(userRequest: UserRequest): Map[RequestType.Value, JsValue] = {
    val filterJSON = getFilter(userRequest)

    val byGeo = Json.parse(
      s"""
         |{
         | "dataset": "${userRequest.dataset}",
         | $filterJSON,
         | "group": {
         |   "by": [
         |      {
         |        "field": "geo",
         |        "apply": {
         |          "name": "level",
         |          "args": {
         |            "level": "${userRequest.geoLevel}"
         |          }
         |        },
         |        "as": "${userRequest.geoLevel}"
         |      }
         |   ],
         |   "aggregate": [
         |     {
         |       "field": "*",
         |       "apply": {
         |         "name": "count"
         |       },
         |       "as": "count"
         |     }
         |    ]
         |  }
         |}
         |""".stripMargin)

    val byTime = Json.parse(
      s"""
         |{
         | "dataset": "${userRequest.dataset}",
         | $filterJSON,
         | "group": {
         |   "by": [
         |      {
         |        "field": "create_at",
         |        "apply": {
         |          "name": "interval",
         |          "args": {
         |            "unit": "${userRequest.timeBin}"
         |          }
         |        },
         |        "as": "${userRequest.timeBin}"
         |      }
         |    ],
         |   "aggregate": [
         |     {
         |       "field": "*",
         |       "apply": {
         |         "name": "count"
         |       },
         |       "as": "count"
         |     }
         |    ]
         |  }
         |}
    """.stripMargin
    )

    val byHashTag = Json.parse(
      s"""
         |{
         | "dataset": "${userRequest.dataset}",
         | $filterJSON,
         | "unnest" : [{ "hashtags": "tag"}],
         | "group": {
         |    "by": [
         |      {
         |        "field": "tag"
         |      }
         |    ],
         |    "aggregate": [
         |      {
         |        "field" : "*",
         |        "apply" : {
         |          "name": "count"
         |        },
         |        "as" : "count"
         |      }
         |    ]
         |  },
         |  "select" : {
         |    "order" : [ "-count"],
         |    "limit": 50,
         |    "offset" : 0
         |  }
         |}
       """.stripMargin
    )

    val sampleTweet = Json.parse(
      s"""
         |{
         |  "dataset": "${userRequest.dataset}",
         |  ${getFilter(userRequest, 1)},
         |   "select" : {
         |    "order" : [ "-create_at"],
         |    "limit": 10,
         |    "offset" : 0,
         |    "field": ["create_at", "id", "user.id"]
         |  }
         |}
       """.stripMargin
    )
    import RequestType._
    Map(ByPlace -> byGeo, ByTime -> byTime, ByHashTag -> byHashTag, Sample -> sampleTweet)
  }

  private def getFilter(userRequest: UserRequest, maxDay: Int = 1500): String = {
    val spatialField = getLevel(userRequest.geoLevel)
    val keywords = userRequest.keywords.map(_.replace("\"", "").trim)
    val startDateInMillis = Math.max(userRequest.timeInterval.getEnd.minusDays(maxDay).getMillis, userRequest.timeInterval.getStart.getMillis)
    s"""
       |"filter": [
       |  {
       |    "field": "geo_tag.$spatialField",
       |    "relation": "in",
       |    "values": [${userRequest.geoIds.mkString(",")}]
       |  },
       |  {
       |    "field": "create_at",
       |    "relation": "inRange",
       |    "values": [
       |      "${TimeField.TimeFormat.print(new DateTime(startDateInMillis))}",
       |      "${TimeField.TimeFormat.print(userRequest.timeInterval.getEnd)}"
       |    ]
       |  },
       |  {
       |    "field": "text",
       |    "relation": "contains",
       |    "values": [
       |      ${keywords.map("\"" + _ + "\"").mkString(",")}
       |    ]
       |  }
       | ]
     """.stripMargin
  }

  private def getLevel(level: GeoLevel.Level): String = {
    level match {
      case GeoLevel.State => "stateID"
      case GeoLevel.County => "countyID"
      case GeoLevel.City => "cityID"
    }
  }
}
