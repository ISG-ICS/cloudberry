package actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import edu.uci.ics.cloudberry.zion.actor.BerryClient
import edu.uci.ics.cloudberry.zion.model.schema.TimeField
import models.{GeoLevel, UserRequest}
import org.joda.time.DateTime
import play.api.libs.json._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class NeoActor(out: ActorRef, val berryClientProps: Props)(implicit ec: ExecutionContext) extends Actor with ActorLogging {

  val berryClient = context.watch(context.actorOf(berryClientProps))

  import NeoActor._

  implicit val timeout: Timeout = Timeout(20.minutes)

  override def receive: Receive = {
    case json: JsValue =>
      json.validate[UserRequest].map { userRequest =>
        val allRequest = generateCBerryRequest(userRequest)
        import RequestType._

        // Example of sending the non-slicing query using ask pattern
        val sampleJson = allRequest(Sample)
        val fAnswer = (berryClient ? sampleJson).mapTo[JsValue].map(json => Json.obj("key" -> "sample", "value" -> json))
        fAnswer pipeTo out

        // Example of sending the group of slicing ones using actor stream pattern
        val groupTimePlaceTags = Seq(ByTime, ByPlace, ByHashTag).map(allRequest(_))
        val json = JsObject(Seq(
          "batch" -> JsArray(groupTimePlaceTags),
          "option" -> JsObject(Seq("sliceMillis" -> JsNumber(2000))
          )))
        berryClient.tell(BerryClient.Request(json, NeoTransformer("batch")), out)
      }.recoverTotal {
        e => out ! JsError.toJson(e)
      }
  }
}

object NeoActor {
  def props(out: ActorRef, berryClientProp: Props)(implicit ec: ExecutionContext) = Props(new NeoActor(out, berryClientProp))

  case class NeoTransformer(key: String) extends BerryClient.IPostTransform {
    override def transform(jsValue: JsValue): JsValue = {
      Json.obj("key" -> key, "value" -> jsValue)
    }
  }

  object RequestType extends Enumeration {
    val ByPlace = Value("byPlace")
    val ByTime = Value("byTime")
    val ByHashTag = Value("byHashTag")
    val Sample = Value("sample")
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
