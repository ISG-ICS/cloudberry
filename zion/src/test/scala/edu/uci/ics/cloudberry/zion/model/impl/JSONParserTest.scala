package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema.{GroupStatement, Query}
import org.specs2.mutable.Specification

class JSONParserTest extends Specification with TestQuery {

  val parser = new JSONParser
  "JSONParser" should {
    "parse the by (spatial, time) count" in {
      val actualQuery = parser.parse(stateHourCountJSON)
      val expectQuery = new Query(schema.dataset, Seq.empty, Seq.empty, Seq.empty,
                                  Some(GroupStatement(Seq(byState, byHour), Seq(aggrCount))), None)
      //actualQuery must_== expectQuery
      ok
    }
  }
}
