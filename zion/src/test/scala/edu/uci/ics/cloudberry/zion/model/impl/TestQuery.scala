package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema._
import play.api.libs.json.{JsValue, Json}

trait TestQuery {

  val schema = TwitterDataStore.TwitterSchema
  val startTime = "2016-01-01T00:00:00Z"
  val endTime = "2016-12-01T00:00:00Z"

  val timeFilter = FilterStatement("create_at", None, Relation.inRange, Seq(startTime, endTime))
  val textFilter = FilterStatement("text", None, Relation.contains, Seq("zika", "virus"))
  val stateFilter = FilterStatement("geo_tag.stateID", None, Relation.in, Seq(37, 51, 24, 11, 10, 34, 42, 9, 44))

  val byHour = ByStatement("create_at", Some(Interval(TimeUnit.Hour)), Some("hour"))
  val byState = ByStatement("geo", Some(Level("state")), Some("state"))
  val byHashTag = ByStatement("hashtags", Some(Unnest), Some("tag"))

  val aggrCount = AggregateStatement("*", Count, "count")
  val aggrHashTagCount = AggregateStatement("tag", Count, "count")

  val selectRecent = SelectStatement(Seq("-create_at"), 100, 0, Seq("create_at", "id", "user.id"))
  val selectTopK = SelectStatement(Seq("-count"), 10, 0, Seq("hashTag", "count"))

  val stateHourCountJSON = Json.parse(
    """
      |{
      |  "dataset": "twitter.ds_tweet",
      |  "group": {
      |    "by": [
      |      {
      |        "field": "geo",
      |        "apply": "state",
      |        "as": "state"
      |      },
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
      |        "apply": "count",
      |        "as": "count"
      |      }
      |    ]
      |  }
      |}
    """.stripMargin)
}
