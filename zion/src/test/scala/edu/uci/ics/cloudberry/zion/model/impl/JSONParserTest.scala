package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.JsonRequestException
import edu.uci.ics.cloudberry.zion.model.schema.{GlobalAggregateStatement, GroupStatement, Query}
import org.specs2.mutable.Specification

class JSONParserTest extends Specification {

  import TestQuery._

  val parser = new JSONParser

  "JSONParser" should {
    "parse the hourly count request" in {
      val actualQuery = parser.parse(hourCountJSON)
      val expectQuery = Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty,
                              Some(GroupStatement(Seq(byHour), Seq(aggrCount))), None)
      actualQuery must_== expectQuery
    }
    "parse the by (state, hour) count request" in {
      val actualQuery = parser.parse(filterSelectJSON)
      val filter = Seq(stateFilter, timeFilter, textFilter)
      val group = GroupStatement(Seq(byState, byHour), Seq(aggrCount))
      val expectQuery = Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
      actualQuery must_== expectQuery
    }
    "parse the by topK hashtag request" in {
      val actualQuery = parser.parse(topKHashTagJSON)
      val filter = Seq(stateFilter, timeFilter, textFilter)
      val group = GroupStatement(Seq(byTag), Seq(aggrCount))
      val expectQuery = Query(TwitterDataSet, Seq.empty, filter, Seq(unnestHashTag), Some(group), Some(selectTop10Tag))
      actualQuery must_== expectQuery
    }
    "parse the by sample tweets" in {
      val actualQuery = parser.parse(sampleTweetJSON)
      val filter = Seq(stateFilter, timeFilter, textFilter)
      val expectQuery = Query(TwitterDataSet, Seq.empty, filter, Seq.empty, None, Some(selectRecent))
      actualQuery must_== expectQuery
    }
    "parse the group by bin" in {
      val actualQuery = parser.parse(groupByBinJSON)
      val expectQuery = Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Some(GroupStatement(Seq(byBin), Seq(aggrCount))), None)
      actualQuery must_== expectQuery
    }
    "parse int values " in {
      val actualQuery = parser.parse(intValuesJSON)
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq(intFilter), Seq.empty, None, None)
      actualQuery must_== expectQuery
    }
    "parse string values " in {
      val actualQuery = parser.parse(stringValueJSON)
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq(stringFilter), Seq.empty, None, None)
      actualQuery must_== expectQuery
    }
    "parse long values " in {
      val actualQuery = parser.parse(longValuesJSON)
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq(longFilter), Seq.empty, None, None)
      actualQuery must_== expectQuery
    }
    "parse double values " in {
      val actualQuery = parser.parse(doubleValuesJSON)
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq(doubleFilter), Seq.empty, None, None)
      actualQuery must_== expectQuery
    }
    "parse geoCellTenth group function " in {
      val actualQuery = parser.parse(geoCell10JSON)
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty,
                                  Some(GroupStatement(Seq(byGeocell10), Seq(aggrCount))), None)
      actualQuery must_== expectQuery
    }
    "parse geoCellHundredth group function " in {
      val actualQuery = parser.parse(geoCell100JSON)
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty,
                                  Some(GroupStatement(Seq(byGeocell100), Seq(aggrCount))), None)
      actualQuery must_== expectQuery
    }
    "parse geoCellThousandth group function " in {
      val actualQuery = parser.parse(geoCell1000JSON)
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty,
                                  Some(GroupStatement(Seq(byGeocell1000), Seq(aggrCount))), None)
      actualQuery must_== expectQuery
    }
    "parse boolean filter request" in {
      val actualQuery = parser.parse(booleanFilterJSON)
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq(retweetFilter), Seq.empty,
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

    "parse a count cardinality without group by" in {
      val actualQuery = parser.parse(globalCountJSON)
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val expectQuery = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
      actualQuery must_== expectQuery
    }
    "parse a max cardinality without group by" in {
      val actualQuery = parser.parse(globalMaxJSON)
      val globalAggr = GlobalAggregateStatement(aggrMax)
      val expectQuery = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
      actualQuery must_== expectQuery
    }
    "parse a min cardinality without group by" in {
      val actualQuery = parser.parse(globalMinJSON)
      val globalAggr = GlobalAggregateStatement(aggrMin)
      val expectQuery = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
      actualQuery must_== expectQuery
    }
  }
}
