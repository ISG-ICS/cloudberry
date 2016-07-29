package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.JsonRequestException
import edu.uci.ics.cloudberry.zion.model.schema.{FilterStatement, GroupStatement, Query}
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
    "parse long values " in {
      val actualQuery = parser.parse(longValuesJSON)
      val expectQuery = new Query(schema.dataset, Seq.empty, Seq(longFilter), Seq.empty, None, None)
      actualQuery must_== expectQuery
    }
    "parse double values " in {
      val actualQuery = parser.parse(doubleValuesJSON)
      val expectQuery = new Query(schema.dataset, Seq.empty, Seq(doubleFilter), Seq.empty, None, None)
      actualQuery must_== expectQuery
    }
    "parse geo-cell group function " in {
     val actualQuery = parser.parse(geoCellJSON)
      val expectQuery = new Query(schema.dataset, Seq.empty, Seq.empty, Seq.empty,
        Some(GroupStatement(Seq(byGeocell), Seq(aggrCount))), None)
      actualQuery must_== expectQuery
    }
    "parse is_retweet filter request" in {
      val actualQuery = parser.parse(retweetsJSON)
      val expectQuery = new Query(schema.dataset, Seq.empty, Seq(retweetFilter), Seq.empty,
        Some(GroupStatement(Seq(byUser), Seq(aggrCount))), None)
      actualQuery must_== expectQuery
    }
    "throw an exception when there is no dataset name" in {
      parser.parse(missingDatasetJSON) must throwA[JsonRequestException]
    }
    "throw an exception when value is an array" in {
      parser.parse(filterErrorJSON) must throwA[JsonRequestException]
    }
    "throw an exception when relation is unknown" in {
      parser.parse(relationErrorJSON) must throwA[JsonRequestException]
    }
  }
}
