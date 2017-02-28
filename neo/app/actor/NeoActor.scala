package actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.Materializer
import akka.stream.scaladsl._
import akka.util.{ByteString, Timeout}
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.schema.TimeField
import models.{GeoLevel, UserRequest}
import org.joda.time.DateTime
import play.Logger
import play.api.libs.json._
import play.api.libs.ws._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}


class NeoActor(out: ActorRef, ws: WSClient, host: String, config: Config)
              (implicit ec: ExecutionContext, implicit val materializer: Materializer) extends Actor with ActorLogging {

  import NeoActor._
  import RequestType._

  implicit val timeout: Timeout = Timeout(20.minutes)
  val url : String = "http://" + host + "/berry"

  override def receive: Receive = {
    case json: JsValue =>
      if ((json \ "cmd").asOpt[String].contains(RequestType.TotalCount.toString)) {
        val dataset = (json \ "dataset").asOpt[String].getOrElse("twitter.ds_tweet")
        val berryJson = generateCountRequest(dataset)
        handleCountResponse(berryJson)
        context.system.scheduler.schedule(1 seconds, 1 seconds, self, TotalCountRequest(berryJson))
      } else {
        json.validate[UserRequest].map { userRequest =>
          val berryRequest = generateCBerryRequest(userRequest)
          handleSamplingResponse(berryRequest)
          handleSlicingResponse(berryRequest)
        }.recoverTotal {
          e =>
            out ! JsError.toJson(e)
        }
      }
    case r: TotalCountRequest =>
      handleCountResponse(r.berryJson)
    case x => log.error("unknown:" + x)
  }

  private def handleSamplingResponse(berryRequest: Map[RequestType.Value, JsValue]): Unit = {
    handleResponse(Sample, berryRequest(Sample))
  }

  private def handleSlicingResponse(berryRequest: Map[RequestType.Value, JsValue]): Unit = {
    val groupTimePlaceTags = Seq(ByTime, ByPlace, ByHashTag).map(berryRequest(_))
    val batchJson = JsObject(Seq(
      "batch" -> JsArray(groupTimePlaceTags),
      "option" -> JsObject(Seq("sliceMillis" -> JsNumber(2000))
      )))
    handleResponse(Batch, batchJson)
  }

  private def handleCountResponse(request: JsValue): Unit = {
    handleResponse(TotalCount, request)
  }

  private def handleResponse(requestType: RequestType.Value, requestBody: JsValue): Unit = {
    val response: Future[StreamedResponse] =
      ws.url(url).withMethod("POST").withBody(requestBody).stream()

    response.map { res =>
      if (res.headers.status == 200) {
        val sink = Sink.foreach[ByteString] { bytes =>
          val json = Json.parse(bytes.utf8String)
          out ! Json.obj("key" -> requestType, "value" -> json)
        }
        res.body.via(Framing.delimiter(ByteString("\n"),
          maximumFrameLength = config.MaxFrameLengthForNeoWS,
          allowTruncation = true))
          .runWith(sink)
          .onFailure { case e => Logger.error("NeoActor websocket receiving ... " + e.getMessage) }
      } else {
        Logger.error("Bad Gate Way. Connection code: " + res.headers.status)
      }
    }
  }

}

object NeoActor {

  case class TotalCountRequest(berryJson: JsValue)

  def props(out: ActorRef, ws: WSClient, host: String, config: Config)
           (implicit ec: ExecutionContext, materializer: Materializer) = Props(new NeoActor(out, ws, host, config))

  object RequestType extends Enumeration {
    val ByPlace = Value("byPlace")
    val ByTime = Value("byTime")
    val ByHashTag = Value("byHashTag")
    val Sample = Value("sample")
    val Batch = Value("batch")
    val TotalCount = Value("totalCount")
  }

  def generateCountRequest(dataset: String): JsValue = {
    Json.parse(
      s"""
         |{
         |  "dataset": "$dataset",
         |  "global": {
         |   "globalAggregate":
         |     {
         |       "field": "*",
         |       "apply": {
         |         "name": "count"
         |       },
         |       "as": "count"
         |     }
         |  },
         |  "estimable" : true
         |}
        """.stripMargin
    )
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
