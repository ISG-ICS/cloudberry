package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema._
import play.api.libs.json.Json

trait TestQuery {

  val schema = TwitterDataStore.TwitterSchema
  val startTime = "2016-01-01T00:00:00Z"
  val endTime = "2016-12-01T00:00:00Z"

  val textValue = Seq("zika", "virus")
  val stateValue = Seq(37, 51, 24, 11, 10, 34, 42, 9, 44)
  val timeFilter = FilterStatement("create_at", None, Relation.inRange, Seq(startTime, endTime))
  val textFilter = FilterStatement("text", None, Relation.contains, textValue)
  val stateFilter = FilterStatement("geo_tag.stateID", None, Relation.in, stateValue)

  val unnestHashTag = UnnestStatement("hashtags", "tag")
  val byTag = ByStatement("tag", None, None)
  val byHour = ByStatement("create_at", Some(Interval(TimeUnit.Hour)), Some("hour"))
  val byState = ByStatement("geo", Some(Level("state")), Some("state"))

  val aggrCount = AggregateStatement("*", Count, "count")

  val selectRecent = SelectStatement(Seq("-create_at"), 100, 0, Seq("create_at", "id", "user.id"))
  val selectTop10Tag = SelectStatement(Seq("-count"), 10, 0, Seq.empty)

  val filterJSON =
    s"""
       |"filter": [
       |  {
       |    "field": "geo_tag.stateID",
       |    "relation": "in",
       |    "values": [${stateValue.mkString(",")}]
       |  },
       |  {
       |    "field": "create_at",
       |    "relation": "inRange",
       |    "values": [
       |      "$startTime",
       |      "$endTime"
       |    ]
       |  },
       |  {
       |    "field": "text",
       |    "relation": "contains",
       |    "values": [
       |      ${textValue.map("\"" + _ + "\"").mkString(",")}
       |    ]
       |  }
       | ]
     """.stripMargin

  val filterSelectJSON = Json.parse(
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
       |            "level": "state"
       |          }
       |        },
       |        "as": "state"
       |      },
       |      {
       |        "field": "create_at",
       |        "apply": {
       |          "name": "interval",
       |          "args": {
       |            "unit": "hour"
       |          }
       |        },
       |        "as": "hour"
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

  val topKHashTagJSON = Json.parse(
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
       |    "limit": 10,
       |    "offset" : 0
       |  }
       |}
     """.stripMargin
  )

  val sampleTweetJSON = Json.parse(
    s"""
       |{
       |  "dataset": "twitter.ds_tweet",
       |  $filterJSON,
       |   "select" : {
       |    "order" : [ "-create_at"],
       |    "limit": 100,
       |    "offset" : 0,
       |    "field": ["create_at", "id", "user.id"]
       |  }
       |}
       | """.stripMargin)


  val hourCountJSON = Json.parse(
    """
      |{
      |  "dataset": "twitter.ds_tweet",
      |  "group": {
      |    "by": [
      |      {
      |        "field": "create_at",
      |        "apply": {
      |          "name": "interval",
      |          "args" : {
      |            "unit": "hour"
      |          }
      |        },
      |        "as": "hour"
      |      }
      |    ],
      |    "aggregate": [
      |      {
      |        "field": "*",
      |        "apply": {
      |          "name" : "count"
      |        },
      |        "as": "count"
      |      }
      |    ]
      |  }
      |}
    """.stripMargin)

  def removeEmptyLine(string: String): String = string.split("\\r?\\n").filterNot(_.trim.isEmpty).mkString("\n")
}
