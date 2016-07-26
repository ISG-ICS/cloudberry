package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema.{GroupStatement, Query}
import org.specs2.mutable.Specification

class JSONParserTest extends Specification with TestQuery {

  "JSONParser" should {
    "parse the by (spatial, time) count" in {
      val parser = new JSONParser
      val actualQuery = parser.parse(stateHourCountJSON)
      val expectQuery = Query(schema.dataset, Seq.empty, Seq.empty, Seq.empty,
                              Some(GroupStatement(Seq(byHour), Seq(aggrCount))), None)
      actualQuery.groups must_== expectQuery.groups
      actualQuery must_== expectQuery
      ok
    }
  }
}
