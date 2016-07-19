package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema._
import org.specs2.mutable.Specification

class AQLQueryParser$Test extends Specification {

  "A simple filter and group by query " should {
    "work" in {
      val schema = TwitterDataStore.TwitterSchema
      val startTime = "2016-01-01T00:00:00Z"
      val endTime = "2016-12-01T00:00:00Z"
      val filter = Seq(new FilterStatement("create_at", None, Relation.inRange, Seq(startTime, endTime)))
      val by = new ByStatement("create_at", Some(new Interval(TimeUnit.Hour)), Some("hour"))
      val aggr = new AggregateStatement("*", Count, "count")
      val group = new GroupStatement(Seq(by), Seq(aggr))
      val query: Query = new Query(schema.dataset, Seq.empty, filter, Some(group), None)
      AQLQueryParser.parse(query, schema) must_== ""
    }
  }

}
