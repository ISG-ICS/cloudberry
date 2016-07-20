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

  val aggrCount = AggregateStatement("*", Count, "count")

  "AQLQueryParser" should {
    "translate a simple filter by time and group by time query" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query: Query = new Query(schema.dataset, Seq.empty, filter, Some(group), None)
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
      ok
    }

    "translate a geo id set filter group by time query" in {
      ok
    }

    "translate a text contain + time + geo id set filter and group by time + spatial cube" in {
      ok
    }

    "translate a text contain + time + geo id set filter and sample tweets" in {
      ok
    }

    "translate a text contain + time + geo id set filter and group by hashtags" in {
      ok
    }
  }

}
