package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema._
import org.specs2.mutable.Specification

class AQLQueryParserTest extends Specification {

  "AQLQueryParser" should {
    "translate a simple filter by time and group by time query" in {
      val schema = TwitterDataStore.TwitterSchema
      val startTime = "2016-01-01T00:00:00Z"
      val endTime = "2016-12-01T00:00:00Z"
      val filter = Seq(new FilterStatement("create_at", None, Relation.inRange, Seq(startTime, endTime)))
      val by = new ByStatement("create_at", Some(new Interval(TimeUnit.Hour)), Some("hour"))
      val aggr = new AggregateStatement("*", Count, "count")
      val group = new GroupStatement(Seq(by), Seq(aggr))
      val query: Query = new Query(schema.dataset, Seq.empty, filter, Some(group), None)
      val parser = new AQLQueryParser
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
  }

}
