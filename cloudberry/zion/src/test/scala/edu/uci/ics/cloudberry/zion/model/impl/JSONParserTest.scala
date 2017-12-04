package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.JsonRequestException
import edu.uci.ics.cloudberry.zion.model.schema._
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import play.api.libs.json._

class JSONParserTest extends Specification {

  import TestQuery._

  val parser = new JSONParser


  "JSONParser parse query" should {
    def checkQueryOnly(json: JsValue, schemaMap: Map[String, AbstractSchema], expect: Query): MatchResult[Any] = {
      val (actualQuery, _) = parser.parse(json, schemaMap)
      actualQuery.size must_== 1
      actualQuery.head must_== expect
    }

    "parse the hourly count request" in {
      val expect = Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(GroupStatement(Seq(byHour), Seq(aggrCount))), None)
      checkQueryOnly(hourCountJSON, twitterSchemaMap, expect)
    }
    "parse the by (state, hour) count request" in {
      val filter = Seq(stateFilter, timeFilter, textFilter)
      val group = GroupStatement(Seq(byState, byHour), Seq(aggrCount))
      val expectQuery = Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      checkQueryOnly(filterSelectJSON, twitterSchemaMap, expectQuery)
    }
    "parse the by topK hashtag request" in {
      val filter = Seq(stateFilter, timeFilter, textFilter)
      val group = GroupStatement(Seq(byTag), Seq(aggrCount))
      val expectQuery = Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq(unnestHashTag), Some(group), Some(selectTop10Tag))
      checkQueryOnly(topKHashTagJSON, twitterSchemaMap, expectQuery)
    }
    "parse the by sample tweets" in {
      val filter = Seq(stateFilter, timeFilter, textFilter)
      val expectQuery = Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, None, Some(selectRecent))
      checkQueryOnly(sampleTweetJSON, twitterSchemaMap, expectQuery)
    }
    "parse the group by bin" in {
      val expectQuery = Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(GroupStatement(Seq(byBin), Seq(aggrCount))), None)
      checkQueryOnly(groupByBinJSON, twitterSchemaMap, expectQuery)
    }
    "parse int values " in {
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq(intFilter), Seq.empty, None, None)
      checkQueryOnly(intValuesJSON, twitterSchemaMap, expectQuery)
    }
    "parse string values " in {
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq(stringFilter), Seq.empty, None, None)
      checkQueryOnly(stringValueJSON, twitterSchemaMap, expectQuery)
    }
    "parse long values " in {
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq(longFilter), Seq.empty, None, None)
      checkQueryOnly(longValuesJSON, twitterSchemaMap, expectQuery)
    }
    "parse double values " in {
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq(doubleFilter), Seq.empty, None, None)
      checkQueryOnly(doubleValuesJSON, twitterSchemaMap, expectQuery)
    }
    "parse geoCellTenth group function " in {
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq.empty,
        Some(GroupStatement(Seq(byGeocell10), Seq(aggrCount))), None)
      checkQueryOnly(geoCell10JSON, twitterSchemaMap, expectQuery)
    }
    "parse geoCellHundredth group function " in {
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq.empty,
        Some(GroupStatement(Seq(byGeocell100), Seq(aggrCount))), None)
      checkQueryOnly(geoCell100JSON, twitterSchemaMap, expectQuery)
    }
    "parse geoCellThousandth group function " in {
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq.empty,
        Some(GroupStatement(Seq(byGeocell1000), Seq(aggrCount))), None)
      checkQueryOnly(geoCell1000JSON, twitterSchemaMap, expectQuery)
    }
    "parse boolean filter request" in {
      val expectQuery = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq(retweetFilter), Seq.empty,
        Some(GroupStatement(Seq(byUser), Seq(aggrCount))), None)
      checkQueryOnly(booleanFilterJSON, twitterSchemaMap, expectQuery)
    }
    "parse a count cardinality without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val expectQuery = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
      checkQueryOnly(globalCountJSON, twitterSchemaMap, expectQuery)
    }
    "parse a max cardinality without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrMax)
      val expectQuery = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
      checkQueryOnly(globalMaxJSON, twitterSchemaMap, expectQuery)
    }
    "parse a min cardinality without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrMin)
      val expectQuery = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
      checkQueryOnly(globalMinJSON, twitterSchemaMap, expectQuery)
    }
    "throw an exception when there is no dataset name" in {
      parser.parse(missingDatasetJSON, twitterSchemaMap) must throwA[JsonRequestException]
    }
    "throw an exception when value is an array" in {
      parser.parse(filterErrorJSON, twitterSchemaMap) must throwA[JsonRequestException]
    }
    "throw an exception when relation is unknown" in {
      parser.parse(relationErrorJSON, twitterSchemaMap) must throwA[JsonRequestException]
    }

    "parse lookup query with select statement" in {
      val lookup = Seq(lookupPopulation)
      val filter = Seq(textFilter)
      val selectStatement = Some(selectPopulation)
      val expectedQuery = new Query(TwitterDataSet, Seq.empty, lookup, filter, Seq.empty, select = selectStatement)
      checkQueryOnly(simpleLookupFilterJSON, allSchemaMap, expectedQuery)
    }

    "parse lookup query having multiple fields with select statement" in {
      val lookup = Seq(lookupPopulationMultiple)
      val filter = Seq(textFilter)
      val selectStatement = Some(selectPopulation)
      val expectedQuery = new Query(TwitterDataSet, Seq.empty, lookup, filter, Seq.empty, select = selectStatement)
      checkQueryOnly(multiFieldLookupFilterJSON, allSchemaMap, expectedQuery)
    }

    "parse multiple lookup query with select statement" in {
      val lookup = Seq(lookupPopulation, lookupLiteracy)
      val filter = Seq(textFilter)
      val selectStatement = Some(selectPopulationLiteracy)
      val expectedQuery = new Query(TwitterDataSet, Seq.empty, lookup, filter, Seq.empty, select = selectStatement)
      checkQueryOnly(multiLookupFilterJSON, allSchemaMap, expectedQuery)
    }


    "parse lookup inside group by state and count" in {
      val lookup = Seq(lookupPopulationByState)
      val filter = Seq(textFilter)
      val select = SelectStatement(Seq.empty, Seq.empty, 0, 0, Seq(state, count, population))
      val group = GroupStatement(Seq(byState), Seq(aggrCount), Seq(lookupPopulationByState))
      val expectedQuery = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), select = Some(select))
      checkQueryOnly(groupLookupJSON, allSchemaMap, expectedQuery)
    }

    "parse multiple lookups inside group by state and count" in {
      val lookup = Seq(lookupPopulationByState)
      val filter = Seq(textFilter)
      val select = SelectStatement(Seq.empty, Seq.empty, 0, 0, Seq(state, count, population, literacy))
      val group = GroupStatement(Seq(byState), Seq(aggrCount), Seq(lookupPopulationByState, lookupLiteracyByState))
      val expectedQuery = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), select = Some(select))
      checkQueryOnly(groupMultipleLookupJSON, allSchemaMap, expectedQuery)
    }

    "parse multiple lookups inside/outside group by state and count" in {
      val select = SelectStatement(Seq.empty, Seq.empty, 0, 0, Seq(state, min, literacy))
      val filter = Seq(textFilter)
      val lookup = Seq(lookupPopulation)
      val group = GroupStatement(Seq(byState), Seq(aggrPopulationMin), Seq(lookupLiteracyByState))
      val expectedQuery = new Query(TwitterDataSet, Seq.empty, lookup, filter, Seq.empty, Some(group), select = Some(select))
      checkQueryOnly(lookupsInOutGroupJSON, allSchemaMap, expectedQuery)
    }

    "parse a append and filter and group by time query" in {
      val append = Seq(appendLangLen)
      val filter = Seq(langLenFilter)
      val group = Some(GroupStatement(Seq(byHour), Seq(aggrCount)))
      val expectedQuery = new Query(TwitterDataSet, append, filter= filter, groups = group)
      checkQueryOnly(appendFilterGroupbyJSON, allSchemaMap, expectedQuery)
    }


    "parse append with lookup inside group by state and sum" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byState), Seq(aggrAvgLangLen), Seq(lookupPopulationByState))
      val expectedQuery = new Query(TwitterDataSet, Seq(appendLangLen), Seq.empty, filter, Seq.empty, Some(group))
      checkQueryOnly(appendGroupLookupJSON, allSchemaMap, expectedQuery)
    }

  }

  "JSONParser" should {
    "parse query without option" in {
      val (_, option) = parser.parse(hourCountJSON, twitterSchemaMap)
      option must_== QueryExeOption.NoSliceNoContinue
    }
    "parse slicing option" in {
      val millis = 1234
      val optionJson = Json.obj(QueryExeOption.TagSliceMillis -> JsNumber(millis))
      val (_, option) = parser.parse(hourCountJSON.asInstanceOf[JsObject] + ("option" -> optionJson), twitterSchemaMap)
      option must_== QueryExeOption(millis, -1, None)
    }
    "parse continue option" in {
      val seconds = 4321
      val optionJson = Json.obj(QueryExeOption.TagContinueSeconds -> JsNumber(4321))
      val (_, option) = parser.parse(hourCountJSON.asInstanceOf[JsObject] + ("option" -> optionJson), twitterSchemaMap)
      option must_== QueryExeOption(-1, seconds, None)
    }
    "parse slicing and continue option" in {
      val optionJson = Json.obj(
        QueryExeOption.TagSliceMillis -> JsNumber(1234),
        QueryExeOption.TagContinueSeconds -> JsNumber(4321)
      )
      val (_, option) = parser.parse(hourCountJSON.asInstanceOf[JsObject] + ("option" -> optionJson), twitterSchemaMap)
      option must_== QueryExeOption(1234, 4321, None)
    }
    "parse estimable query if estimable field appears" in {
      val (queries, _) = parser.parse(hourCountJSON.asInstanceOf[JsObject] + ("estimable" -> JsBoolean(true)), twitterSchemaMap)
      queries.forall(q => q.isEstimable) must beTrue

      val (queriesFalse, _) = parser.parse(hourCountJSON.asInstanceOf[JsObject] + ("estimable" -> JsBoolean(false)), twitterSchemaMap)
      queriesFalse.forall(q => !q.isEstimable) must beTrue
    }
    "parse estimable to false by default" in {
      val (queries, _) = parser.parse(hourCountJSON.asInstanceOf[JsObject], twitterSchemaMap)
      queries.forall(q => !q.isEstimable) must beTrue
    }
    "parse limit in single query" in {
      val optionJson = Json.obj(
        QueryExeOption.TagSliceMillis -> JsNumber(1234)
      )
      val filter = Seq(stateFilter, timeFilter, textFilter)
      val group = GroupStatement(Seq(byTag), Seq(aggrCount))
      val expectQuery = Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq(unnestHashTag), Some(group), Some(selectTagWithoutLimit))

      val (actualQuery, option) = parser.parse(topKHashTagJSON.as[JsObject] + ("option" -> optionJson), twitterSchemaMap)
      option must_== QueryExeOption(1234, -1, Some(10))
      actualQuery.size must_== 1
      actualQuery.head must_== expectQuery
      ok
    }
    "throw exception if limits in batch query" in {
      val batchQueryJson = Json.obj("batch" -> JsArray(Seq(simpleLookupFilterJSON, multiFieldLookupFilterJSON)))
      val optionJson = Json.obj(
        QueryExeOption.TagSliceMillis -> JsNumber(1234)
      )
      try {
        val (query, option) = parser.parse(batchQueryJson + ("option" -> optionJson), twitterSchemaMap)
        throw new IllegalStateException("should not be here")
      } catch {
        case e:JsonRequestException =>
          e.msg must_== "Batch Requests cannot contain \"limit\" field"
        case _ =>
          throw new IllegalStateException("should not be here")
      }
      ok
    }
  }

  "JSONParser" should {
    "parse a batch of queries" in {
      val batchQueryJson = Json.obj("batch" -> JsArray(Seq(hourCountJSON, groupByBinJSON)))
      val (query, option) = parser.parse(batchQueryJson, twitterSchemaMap)
      query.size must_== 2
      query.head must_== Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(GroupStatement(Seq(byHour), Seq(aggrCount))), None)
      query.last must_== Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(GroupStatement(Seq(byBin), Seq(aggrCount))), None)
      option must_== QueryExeOption.NoSliceNoContinue
    }
    "parse a batch of queries contains different estimable settings" in {
      val batchQueryJson = Json.obj("batch" -> JsArray(Seq(
        hourCountJSON.as[JsObject] + ("estimable" -> JsBoolean(true)),
        groupByBinJSON.as[JsObject] + ("estimable" -> JsBoolean(false)))))
      val (query, option) = parser.parse(batchQueryJson, twitterSchemaMap)
      query.size must_== 2
      query.head must_== Query(TwitterDataSet, Seq.empty, groups = Some(GroupStatement(Seq(byHour), Seq(aggrCount))), isEstimable = true)
      query.last must_== Query(TwitterDataSet, Seq.empty, groups = Some(GroupStatement(Seq(byBin), Seq(aggrCount))), isEstimable = false)
      option must_== QueryExeOption.NoSliceNoContinue
    }
    "parse a batch of queries with option" in {
      val batchQueryJson = Json.obj("batch" -> JsArray(Seq(hourCountJSON, groupByBinJSON)))
      val optionJson = Json.obj(
        QueryExeOption.TagSliceMillis -> JsNumber(1234)
      )
      val (query, option) = parser.parse(batchQueryJson + ("option" -> optionJson), twitterSchemaMap)
      query.size must_== 2
      query.head must_== Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(GroupStatement(Seq(byHour), Seq(aggrCount))), None)
      query.last must_== Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(GroupStatement(Seq(byBin), Seq(aggrCount))), None)
      option must_== QueryExeOption(1234, -1, None)
    }
  }

  "JSONParser getDatasets" should {
    "parse a single dataset" in {
      val datasets = parser.getDatasets(zikaJSON)
      datasets must_== Seq("twitter.ds_tweet")
    }

    "parse two datasets" in {
      val datasets = parser.getDatasets(groupLookupJSON)
      datasets must_== Seq("twitter.ds_tweet", "twitter.US_population")
    }
  }

}
