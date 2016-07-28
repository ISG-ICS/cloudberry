package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema.{GroupStatement, Query}
import org.specs2.mutable.Specification

class JSONParserTest extends Specification with TestQuery {

  val parser = new JSONParser

  "JSONParser" should {
    "parse the hourly count request" in {
      val actualQuery = parser.parse(hourCountJSON)
      val expectQuery = Query(schema.dataset, Seq.empty, Seq.empty, Seq.empty,
                              Some(GroupStatement(Seq(byHour), Seq(aggrCount))), None)
      actualQuery must_== expectQuery
    }
    "parse the by (state, hour) count request" in {
      val actualQuery = parser.parse(filterSelectJSON)
      val filter = Seq(stateFilter, timeFilter, textFilter)
      val group = GroupStatement(Seq(byState, byHour), Seq(aggrCount))
      val expectQuery = Query(schema.dataset, Seq.empty, filter, Seq.empty, Some(group), None)
      actualQuery must_== expectQuery
    }
    "parse the by topK hashtag request" in {
      val actualQuery = parser.parse(topKHashTagJSON)
      val filter = Seq(stateFilter, timeFilter, textFilter)
      val group = GroupStatement(Seq(byTag), Seq(aggrCount))
      val expectQuery = Query(schema.dataset, Seq.empty, filter, Seq(unnestHashTag), Some(group), Some(selectTop10Tag))
      actualQuery must_== expectQuery
    }
    "parse the by sample tweets" in {
      val actualQuery = parser.parse(sampleTweetJSON)
      val filter = Seq(stateFilter, timeFilter, textFilter)
      val expectQuery = Query(schema.dataset, Seq.empty, filter, Seq.empty, None, Some(selectRecent))
      actualQuery must_== expectQuery
    }
  }
}
