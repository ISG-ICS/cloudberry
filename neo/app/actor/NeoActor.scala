package actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import edu.uci.ics.cloudberry.zion.model.schema.TimeField
import models.{GeoLevel, UserRequest}
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.ExecutionContext

class NeoActor(out: Option[ActorRef], val berryClient: ActorRef)(implicit ec: ExecutionContext) extends Actor with ActorLogging {

  import NeoActor._
  import akka.pattern.ask

  import scala.concurrent.duration._

  implicit val timeout: Timeout = Timeout(5.seconds)

  override def receive: Receive = {
    //TODO add the json validator
    case json: JsValue =>
      val userRequest = json.as[UserRequest]
      tellBerry(userRequest, sender())
    case userRequest: UserRequest =>
      tellBerry(userRequest, sender())
  }

  private def tellBerry(userRequest: UserRequest, curSender: ActorRef): Unit = {
    for (berryRequest <- generateCBerryRequest(userRequest)) {
      (berryClient ? berryRequest).mapTo[JsValue].map(out.getOrElse(curSender) ! _)
    }
  }
}

object NeoActor {

  def props(out: ActorRef, berryClient: ActorRef)(implicit ec: ExecutionContext) = Props(new NeoActor(Some(out), berryClient))

  def props(berryClient: ActorRef)(implicit ec: ExecutionContext) = Props(new NeoActor(None, berryClient))

  def generateCBerryRequest(userRequest: UserRequest): Seq[JsValue] = {
    val filterJSON = getFilter(userRequest)

    val byGeo = Json.parse(
      s"""
         |{
         | "dataset": "twitter.ds_tweet",
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
         | "dataset": "twitter.ds_tweet",
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
         | "dataset": "twitter.ds_tweet",
         | $filterJSON,
         | "unnest" : { "hashtags": "tag"},
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
         |  "dataset": "twitter.ds_tweet",
         |  $filterJSON,
         |   "select" : {
         |    "order" : [ "-create_at"],
         |    "limit": 10,
         |    "offset" : 0,
         |    "field": ["create_at", "id", "user.id"]
         |  }
         |}
       """.stripMargin
    )
    Seq(byGeo, byTime, byHashTag, sampleTweet)
  }

  private def getFilter(userRequest: UserRequest): String = {
    val spatialField = getLevel(userRequest.geoLevel)
    val keywords = userRequest.keywords.map(_.replace("\"", "").trim)
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
       |      "${TimeField.TimeFormat.print(userRequest.timeInterval.getStart)}",
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
