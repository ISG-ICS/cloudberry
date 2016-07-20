package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema._
import org.specs2.mutable.Specification

class AQLQueryParserTest extends Specification {

  val parser = new AQLQueryParser
  val schema = TwitterDataStore.TwitterSchema
  val startTime = "2016-01-01T00:00:00Z"
  val endTime = "2016-12-01T00:00:00Z"

  val timeFilter = FilterStatement("create_at", None, Relation.inRange, Seq(startTime, endTime))
  val textFilter = FilterStatement("text", None, Relation.contains, Seq("zika", "virus"))
  val stateFilter = FilterStatement("geo_tag.stateID", None, Relation.in, Seq(37, 51, 24, 11, 10, 34, 42, 9, 44))

  val byHour = ByStatement("create_at", Some(Interval(TimeUnit.Hour)), Some("hour"))
  val byState = ByStatement("geo", Some(Level("state")), Some("state"))
  val byHashTag = ByStatement("hashTags", Some(Unnest), Some("hashTag"))

  val aggrCount = AggregateStatement("*", Count, "count")
  val aggrHashTagCount = AggregateStatement("hashTag", Count, "count")

  val selectRecent = SelectStatement(Seq("-create_at"), 100, 0, Seq("create_at", "id", "user.id"))
  val selectTopK = SelectStatement(Seq("-count"), 10, 0, Seq("hashTag", "count"))

  "AQLQueryParser" should {
    "translate a simple filter by time and group by time query" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(schema.dataset, Seq.empty, filter, Some(group), None)
      val result = parser.parse(query, schema).head
      result.trim must_==
        """
          |for $t in dataset twitter.ds_tweet
          |
          |where $t.create_at >= datetime('2016-01-01T00:00:00Z') and $t.create_at < datetime('2016-12-01T00:00:00Z')
          |
          |group by  $g0 := get-interval-start-datetime(interval-bin($t.create_at, datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("PT1H") ))  with $t
          |return {
          |  'hour' : $g0,'count' : count($t)
          |}
          |
          | """.stripMargin.trim
    }

    "translate a text contain filter and group by time query" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(schema.dataset, Seq.empty, filter, Some(group), None)
      //val result = parser.parse(query, schema).head
      ok
    }

    "translate a geo id set filter group by time query" in {
      val filter = Seq(stateFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(schema.dataset, Seq.empty, filter, Some(group), None)
      //val result = parser.parse(query, schema).head
      ok
    }

    "translate a text contain + time + geo id set filter and group by time + spatial cube" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val group = GroupStatement(Seq(byHour, byState), Seq(aggrCount))
      val query = new Query(schema.dataset, Seq.empty, filter, Some(group), None)
      //val result = parser.parse(query, schema).head
      ok
    }

    "translate a text contain + time + geo id set filter and sample tweets" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val query = new Query(schema.dataset, Seq.empty, filter, None, Some(selectRecent))
      //val result = parser.parse(query, schema).head
      ok
    }

    "translate a text contain + time + geo id set filter and group by hashtags" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val group = GroupStatement(Seq(byHashTag), Seq(aggrHashTagCount))
      val query = new Query(schema.dataset, Seq.empty, filter, Some(group), Some(selectTopK))
      //val result = parser.parse(query, schema).head
      ok
    }

    "translate a text contain + time + geo id set filter and group day and state and aggregate topK hashtags" in {
      ok
    }

    "translate a lookup query" in {
      ok
    }
  }

}
