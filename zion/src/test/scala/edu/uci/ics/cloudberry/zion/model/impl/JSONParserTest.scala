package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.JsonRequestException
import edu.uci.ics.cloudberry.zion.model.schema.{GlobalAggregateStatement, GroupStatement, Query, QueryExeOption}
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import play.api.libs.json._

class JSONParserTest extends Specification {

  import TestQuery._

  val parser = new JSONParser


  "JSONParser parse query" should {
    def checkQueryOnly(json: JsValue, expect: Query): MatchResult[Any] = {
      val (actualQuery, _) = parser.parse(json)
      actualQuery.size must_== 1
      actualQuery.head must_== expect
    }

    "parse the hourly count request" in {
      val expect = Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Some(GroupStatement(Seq(byHour), Seq(aggrCount))), None)
      checkQueryOnly(hourCountJSON, expect)
    }
    "parse the by (state, hour) count request" in {
      val filter = Seq(stateFilter, timeFilter, textFilter)
      val group = GroupStatement(Seq(byState, byHour), Seq(aggrCount))
      val expectQuery = Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
      checkQueryOnly(filterSelectJSON, expectQuery)
    }
    "parse the by topK hashtag request" in {
      val filter = Seq(stateFilter, timeFilter, textFilter)
      val group = GroupStatement(Seq(byTag), Seq(aggrCount))
      val expectQuery = Query(TwitterDataSet, Seq.empty, filter, Seq(unnestHashTag), Some(group), Some(selectTop10Tag))
      checkQueryOnly(topKHashTagJSON, expectQuery)
    }
    "parse the by sample tweets" in {
      val filter = Seq(stateFilter, timeFilter, textFilter)
      val expectQuery = Query(TwitterDataSet, Seq.empty, filter, Seq.empty, None, Some(selectRecent))
      checkQueryOnly(sampleTweetJSON, expectQuery)
    }
    "parse the group by bin" in {
      val expectQuery = Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Some(GroupStatement(Seq(byBin), Seq(aggrCount))), None)
      checkQueryOnly(groupByBinJSON, expectQuery)
    }
    "parse int values " in {
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq(intFilter), Seq.empty, None, None)
      checkQueryOnly(intValuesJSON, expectQuery)
    }
    "parse string values " in {
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq(stringFilter), Seq.empty, None, None)
      checkQueryOnly(stringValueJSON, expectQuery)
    }
    "parse long values " in {
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq(longFilter), Seq.empty, None, None)
      checkQueryOnly(longValuesJSON, expectQuery)
    }
    "parse double values " in {
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq(doubleFilter), Seq.empty, None, None)
      checkQueryOnly(doubleValuesJSON, expectQuery)
    }
    "parse geoCellTenth group function " in {
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty,
        Some(GroupStatement(Seq(byGeocell10), Seq(aggrCount))), None)
      checkQueryOnly(geoCell10JSON, expectQuery)
    }
    "parse geoCellHundredth group function " in {
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty,
        Some(GroupStatement(Seq(byGeocell100), Seq(aggrCount))), None)
      checkQueryOnly(geoCell100JSON, expectQuery)
    }
    "parse geoCellThousandth group function " in {
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty,
        Some(GroupStatement(Seq(byGeocell1000), Seq(aggrCount))), None)
      checkQueryOnly(geoCell1000JSON, expectQuery)
    }
    "parse boolean filter request" in {
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq(retweetFilter), Seq.empty,
        Some(GroupStatement(Seq(byUser), Seq(aggrCount))), None)
      checkQueryOnly(booleanFilterJSON, expectQuery)
    }
    "parse a count cardinality without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val expectQuery = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
      checkQueryOnly(globalCountJSON, expectQuery)
    }
    "parse a max cardinality without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrMax)
      val expectQuery = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
      checkQueryOnly(globalMaxJSON, expectQuery)
    }
    "parse a min cardinality without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrMin)
      val expectQuery = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
      checkQueryOnly(globalMinJSON, expectQuery)
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

  "JSONParser" should {
    "parse query without option" in {
      val (_, option) = parser.parse(hourCountJSON)
      option must_== QueryExeOption.NoSliceNoContinue
    }
    "parse slicing option" in {
      val millis = 1234
      val optionJson = Json.obj(QueryExeOption.TagSliceMillis -> JsNumber(millis))
      val (_, option) = parser.parse(hourCountJSON.asInstanceOf[JsObject] + ("option" -> optionJson))
      option.sliceMills must_== millis
      option.continueSeconds must be_<=(0)
    }
    "parse continue option" in {
      val seconds = 4321
      val optionJson = Json.obj(QueryExeOption.TagContinueSeconds -> JsNumber(4321))
      val (_, option) = parser.parse(hourCountJSON.asInstanceOf[JsObject] + ("option" -> optionJson))
      option.continueSeconds must_== seconds
      option.sliceMills must be_<=(0)
    }
    "parse continue and slicing option" in {

      val optionJson = Json.obj(
        QueryExeOption.TagSliceMillis -> JsNumber(1234),
        QueryExeOption.TagContinueSeconds -> JsNumber(4321))
      val (_, option) = parser.parse(hourCountJSON.asInstanceOf[JsObject] + ("option" -> optionJson))
      option.continueSeconds must_== 4321
      option.sliceMills must_== 1234
    }
  }

  "JSONParser" should {
    "parse a batch of queries" in {
      val batchQueryJson = Json.obj("batch" -> JsArray(Seq(hourCountJSON, groupByBinJSON)))
      val (query, option) = parser.parse(batchQueryJson)
      query.size must_== 2
      query.head must_== Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Some(GroupStatement(Seq(byHour), Seq(aggrCount))), None)
      query.last must_== Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Some(GroupStatement(Seq(byBin), Seq(aggrCount))), None)
      option must_== QueryExeOption.NoSliceNoContinue
    }
    "parse a batch of queries with option" in {
      val batchQueryJson = Json.obj("batch" -> JsArray(Seq(hourCountJSON, groupByBinJSON)))
      val optionJson = Json.obj(QueryExeOption.TagSliceMillis -> JsNumber(1234))
      val (query, option) = parser.parse(batchQueryJson + ("option" -> optionJson))
      query.size must_== 2
      query.head must_== Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Some(GroupStatement(Seq(byHour), Seq(aggrCount))), None)
      query.last must_== Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Some(GroupStatement(Seq(byBin), Seq(aggrCount))), None)
      option.sliceMills must_== 1234
    }
  }
}
