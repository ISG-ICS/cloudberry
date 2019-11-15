package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema._
import org.specs2.mutable.Specification


class ElasticsearchGeneratorTest extends Specification {

  import TestQuery._

  val parser = new ElasticsearchGenerator

  "ElasticsearchGenerator generate" should {
    //1
    "translate a simple query group by hour" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""
           | {"method":"search","dataset":"$TwitterDataSet","query":{"bool":{"must":[{"range":{"create_at":{"gte":"$startTime","lt":"$endTime"}}}]}},"size":0,"aggs":{"hour":{"date_histogram":{"field":"create_at","interval":"hour"}}},"groupAsList":["hour"],"aggrAsMap":{"count":"count"}}
           |""".stripMargin.trim)
    }

    //2
    "translate a simple query group by day" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byDay), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""
           | {"method":"search","dataset":"$TwitterDataSet","query":{"bool":{"must":[{"range":{"create_at":{"gte":"$startTime","lt":"$endTime"}}}]}},"size":0,"aggs":{"day":{"date_histogram":{"field":"create_at","interval":"day"}}},"groupAsList":["day"],"aggrAsMap":{"count":"count"}}
           |""".stripMargin.trim)
    }

    //3
    "translate a simple query group by month" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byMonth), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""
           | {"method":"search","dataset":"$TwitterDataSet","query":{"bool":{"must":[{"range":{"create_at":{"gte":"$startTime","lt":"$endTime"}}}]}},"size":0,"aggs":{"month":{"date_histogram":{"field":"create_at","interval":"month"}}},"groupAsList":["month"],"aggrAsMap":{"count":"count"}}
           |""".stripMargin.trim)
    }

    //4
    "translate a geo id set filter group by time query" in {
      val filter = Seq(timeFilter, textFilter, stateFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""
           | {"method":"search","dataset":"$TwitterDataSet","query":{"bool":{"must":[{"range":{"create_at":{"gte":"$startTime","lt":"$endTime"}}},{"match":{"text":{"query":"${textValue.mkString(",")}","operator":"and"}}},{"terms":{"geo_tag.stateID":${stateValue.mkString("[",",","]")}}}]}},"size":0,"aggs":{"hour":{"date_histogram":{"field":"create_at","interval":"hour"}}},"groupAsList":["hour"],"aggrAsMap":{"count":"count"}}
           |""".stripMargin.trim)
    }


    //5
    "translate a simple select query order by time with limit 100" in {
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq.empty, None, Some(selectAllOrderByTimeDesc))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""
           | {"method":"search","dataset":"$TwitterDataSet","size":100,"from":0,"sort":[{"create_at":{"order":"desc"}}]}
           |""".stripMargin.trim)
    }

    //6
    "translate a simple unnest query" in {
      val filter = Seq(timeFilter, textFilter, stateFilter)
      val group = GroupStatement(Seq(byTag), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq(unnestHashTag), Some(group), Some(selectTop10byHashTag))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""
           |{"method":"search","dataset":"$TwitterDataSet","query":{"bool":{"must":[{"range":{"create_at":{"gte":"$startTime","lt":"$endTime"}}},{"match":{"text":{"query":"${textValue.mkString(",")}","operator":"and"}}},{"terms":{"geo_tag.stateID":${stateValue.mkString("[",",","]")}}}]}},"size":0,"aggs":{"tag":{"terms":{"field":"hashtags.keyword","size":10,"order":{"_count":"desc"}}}},"groupAsList":["tag"],"aggrAsMap":{"count":"count"}}
           |""".stripMargin.trim)
    }

    //7
    "translate a count cardinality query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""
           |{"method":"search","dataset":"$TwitterDataSet","aggregation":{"func":"count","as":"count"}}
           |""".stripMargin.trim)
    }

    //8
    "translate get min field value query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrMin)
      val query = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""
           |{"method":"search","dataset":"$TwitterDataSet","aggregation":{"func":"min","as":"min"},"size":0,"aggs":{"min":{"min":{"field":"id"}}}}
           |""".stripMargin.trim)
    }

    //9
    "translate get max field value query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrMax)
      val query = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""
           |{"method":"search","dataset":"$TwitterDataSet","aggregation":{"func":"max","as":"max"},"size":0,"aggs":{"max":{"max":{"field":"id"}}}}
           |""".stripMargin.trim)
    }

    //10
    "translate a count cardinality query with filter without group by" in {
      val filter = Seq(timeFilter)
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = TwitterDataSet, filter = filter, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""
           |{"method":"search","dataset":"$TwitterDataSet","query":{"bool":{"must":[{"range":{"create_at":{"gte":"$startTime","lt":"$endTime"}}}]}},"aggregation":{"func":"count","as":"count"}}
           |""".stripMargin.trim)
    }

    //11
    "translate a min cardinality query with filter without group by" in {
      val filter = Seq(timeFilter)
      val globalAggr = GlobalAggregateStatement(aggrMin)
      val query = new Query(dataset = TwitterDataSet, filter = filter, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""{"method":"search","dataset":"$TwitterDataSet","query":{"bool":{"must":[{"range":{"create_at":{"gte":"$startTime","lt":"$endTime"}}}]}},"aggregation":{"func":"min","as":"min"},"size":0,"aggs":{"min":{"min":{"field":"id"}}}}""".stripMargin.trim)
    }

    //12
    "translate a count cardinality query with select" in {
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = TwitterDataSet, select = Some(selectTop10), globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""{"method":"search","dataset":"$TwitterDataSet","size":10,"from":0,"aggregation":{"func":"count","as":"count"}}""".stripMargin.trim)
    }

    //13
    "translate group by minute" in {
      val group = GroupStatement(Seq(byMinuteForSparkSql), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""{"method":"search","dataset":"$TwitterDataSet","size":0,"aggs":{"minute":{"date_histogram":{"field":"create_at","interval":"minute"}}},"groupAsList":["minute"],"aggrAsMap":{"count":"count"}}""".stripMargin.trim)
    }

    //14
    "translate a simple filter by time and group by time query max id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrMax))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""{"method":"search","dataset":"twitter.ds_tweet","query":{"bool":{"must":[{"range":{"create_at":{"gte":"2016-01-01T00:00:00.000Z","lt":"2016-12-01T00:00:00.000Z"}}}]}},"size":0,"aggs":{"hour":{"date_histogram":{"field":"create_at","interval":"hour"},"aggs":{"max":{"max":{"field":"id"}}}}},"groupAsList":["hour"],"aggrAsMap":{"max":"max"}}""".stripMargin.trim)
    }

    //15
    "translate a max cardinality query with unnest with group by with select" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val globalAggr = GlobalAggregateStatement(aggrMaxGroupBy)
      val group = GroupStatement(Seq(byTag), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq(unnestHashTag), Some(group), Some(selectTop10Tag), Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""{"method":"search","dataset":"$TwitterDataSet","query":{"bool":{"must":[{"match":{"text":{"query":"${textValue.mkString(",")}","operator":"and"}}},{"range":{"create_at":{"gte":"$startTime","lt":"$endTime"}}},{"terms":{"geo_tag.stateID":${stateValue.mkString("[",",","]")}}}]}},"size":0,"aggs":{"max":{"max":{"field":"count"}}},"groupAsList":["tag"],"aggrAsMap":{"count":"count"},"aggregation":{"func":"max","as":"max"}}""".stripMargin.trim)
    }

    //16
    "translate a simple filter by time and group by time query" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""{"method":"search","dataset":"$TwitterDataSet","query":{"bool":{"must":[{"range":{"create_at":{"gte":"$startTime","lt":"$endTime"}}}]}},"size":0,"aggs":{"hour":{"date_histogram":{"field":"create_at","interval":"hour"}}},"groupAsList":["hour"],"aggrAsMap":{"count":"count"}}""".stripMargin.trim)
    }

    //17
    "translate a simple filter with string matches" in {
      val filter = Seq(langMatchFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""{"method":"search","dataset":"$TwitterDataSet","query":{"bool":{"must":[{"match":{"lang":"en"}}]}},"size":0,"aggs":{"hour":{"date_histogram":{"field":"create_at","interval":"hour"}}},"groupAsList":["hour"],"aggrAsMap":{"count":"count"}}""".stripMargin.trim)
    }

    //18
    "translate a text contain filter and group by time query" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""{"method":"search","dataset":"$TwitterDataSet","query":{"bool":{"must":[{"match":{"text":{"query":"${textValue.mkString(",")}","operator":"and"}}}]}},"size":0,"aggs":{"hour":{"date_histogram":{"field":"create_at","interval":"hour"}}},"groupAsList":["hour"],"aggrAsMap":{"count":"count"}}""".stripMargin.trim)
    }

    //19
    "translate a geo id set filter group by time query" in {
      val filter = Seq(stateFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""{"method":"search","dataset":"$TwitterDataSet","query":{"bool":{"must":[{"terms":{"geo_tag.stateID":${stateValue.mkString("[",",","]")}}}]}},"size":0,"aggs":{"hour":{"date_histogram":{"field":"create_at","interval":"hour"}}},"groupAsList":["hour"],"aggrAsMap":{"count":"count"}}""".stripMargin.trim)
    }

    //20
    "translate a text contain + time + geo id set filter and group by time + spatial cube" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val group = GroupStatement(Seq(byHour, byGeoState), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """{"method":"search","dataset":"twitter.ds_tweet","query":{"bool":{"must":[{"match":{"text":{"query":"zika,virus","operator":"and"}}},{"range":{"create_at":{"gte":"2016-01-01T00:00:00.000Z","lt":"2016-12-01T00:00:00.000Z"}}},{"terms":{"geo_tag.stateID":[37,51,24,11,10,34,42,9,44]}}]}},"size":0,"aggs":{"hour":{"date_histogram":{"field":"create_at","interval":"hour"},"aggs":{"state":{"terms":{"field":"geo_tag.stateID","size":2147483647}}}}},"groupAsList":["hour","state"],"aggrAsMap":{"count":"count"}}""".stripMargin.trim)
    }

    //21
    "translate a text contain + time + geo id set filter and sample tweets" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, None, Some(selectRecent))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""{"method":"search","dataset":"$TwitterDataSet","query":{"bool":{"must":[{"match":{"text":{"query":"${textValue.mkString(",")}","operator":"and"}}},{"range":{"create_at":{"gte":"$startTime","lt":"$endTime"}}},{"terms":{"geo_tag.stateID":${stateValue.mkString("[",",","]")}}}]}},"size":100,"from":0,"sort":[{"create_at":{"order":"desc"}}],"_source":["create_at","id","user.id"]}""".stripMargin.trim)
    }

    //22
    "translate a text contain + time + geo id set filter and group by hashtags" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val group = GroupStatement(Seq(byTag), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq(unnestHashTag), Some(group), Some(selectTop10Tag))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""{"method":"search","dataset":"$TwitterDataSet","query":{"bool":{"must":[{"match":{"text":{"query":"${textValue.mkString(",")}","operator":"and"}}},{"range":{"create_at":{"gte":"$startTime","lt":"$endTime"}}},{"terms":{"geo_tag.stateID":${stateValue.mkString("[",",","]")}}}]}},"size":0,"aggs":{"tag":{"terms":{"field":"hashtags.keyword","size":10,"order":{"_count":"desc"}}}},"groupAsList":["tag"],"aggrAsMap":{"count":"count"}}""".stripMargin.trim)
    }

    //23
    "translate a simple filter by time and group by time query min id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrMin))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""{"method":"search","dataset":"twitter.ds_tweet","query":{"bool":{"must":[{"range":{"create_at":{"gte":"2016-01-01T00:00:00.000Z","lt":"2016-12-01T00:00:00.000Z"}}}]}},"size":0,"aggs":{"hour":{"date_histogram":{"field":"create_at","interval":"hour"},"aggs":{"min":{"min":{"field":"id"}}}}},"groupAsList":["hour"],"aggrAsMap":{"min":"min"}}""".stripMargin.trim)
    }

    //24
    "translate a simple filter by time and group by time query sum id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrSum))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""{"method":"search","dataset":"twitter.ds_tweet","query":{"bool":{"must":[{"range":{"create_at":{"gte":"2016-01-01T00:00:00.000Z","lt":"2016-12-01T00:00:00.000Z"}}}]}},"size":0,"aggs":{"hour":{"date_histogram":{"field":"create_at","interval":"hour"},"aggs":{"sum":{"sum":{"field":"id"}}}}},"groupAsList":["hour"],"aggrAsMap":{"sum":"sum"}}""".stripMargin.trim)
    }

    //25
    "translate a simple filter by time and group by time query avg id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrAvg))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """{"method":"search","dataset":"twitter.ds_tweet","query":{"bool":{"must":[{"range":{"create_at":{"gte":"2016-01-01T00:00:00.000Z","lt":"2016-12-01T00:00:00.000Z"}}}]}},"size":0,"aggs":{"hour":{"date_histogram":{"field":"create_at","interval":"hour"},"aggs":{"avg":{"avg":{"field":"id"}}}}},"groupAsList":["hour"],"aggrAsMap":{"avg":"avg"}}""".stripMargin.trim)
    }

    //26
    "translate a text contain filter and group by bin" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byBin), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""{"method":"search","dataset":"$TwitterDataSet","query":{"bool":{"must":[{"match":{"text":{"query":"${textValue.mkString(",")}","operator":"and"}}}]}},"size":0,"aggs":{"state":{"terms":{"field":"geo_tag.stateID","size":2147483647,"script":{"source":"Math.round(_value/10)*10"}}}},"groupAsList":["state"],"aggrAsMap":{"count":"count"}}""".stripMargin.trim)
    }

    //27
    "translate a text contain filter and select 10" in {
      val filter = Seq(textFilter)
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, None, Some(selectTop10))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""{"method":"search","dataset":"$TwitterDataSet","query":{"bool":{"must":[{"match":{"text":{"query":"${textValue.mkString(",")}","operator":"and"}}}]}},"size":10,"from":0}""".stripMargin.trim)
    }

    //28
    "translate group by second" in {
      val group = GroupStatement(Seq(bySecond), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""{"method":"search","dataset":"$TwitterDataSet","size":0,"aggs":{"sec":{"date_histogram":{"field":"create_at","interval":"second"}}},"groupAsList":["sec"],"aggrAsMap":{"count":"count"}}""".stripMargin.trim)
    }

    //29
    "translate group by day" in {
      val group = GroupStatement(Seq(byDay), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""{"method":"search","dataset":"$TwitterDataSet","size":0,"aggs":{"day":{"date_histogram":{"field":"create_at","interval":"day"}}},"groupAsList":["day"],"aggrAsMap":{"count":"count"}}""".stripMargin.trim)
    }

    //30
    "translate group by month" in {
      val group = GroupStatement(Seq(byMonth), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""{"method":"search","dataset":"$TwitterDataSet","size":0,"aggs":{"month":{"date_histogram":{"field":"create_at","interval":"month"}}},"groupAsList":["month"],"aggrAsMap":{"count":"count"}}""".stripMargin.trim)
    }

    //31
    "translate group by year" in {
      val group = GroupStatement(Seq(byYear), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""{"method":"search","dataset":"$TwitterDataSet","size":0,"aggs":{"year":{"date_histogram":{"field":"create_at","interval":"year"}}},"groupAsList":["year"],"aggrAsMap":{"count":"count"}}""".stripMargin.trim)
    }

    //32
    "translate lookup one table with one join key" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val selectStatement = SelectStatement(Seq.empty, Seq.empty, 0, 0, Seq(AllField, population))
      val lookup = Seq(lookupPopulation)
      val filter = Seq(textFilter)
      val query = new Query(TwitterDataSet, Seq.empty, lookup, filter, Seq.empty, select = Some(selectStatement))
      val result = parser.generate(query, schemaMap = Map(TwitterDataSet -> twitterSchema, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""{"method":"search","dataset":"$TwitterDataSet","query":{"bool":{"must":[{"match":{"text":{"query":"${textValue.mkString(",")}","operator":"and"}}}]}},"size":0,"from":0,"_source":["*","population"]}""".stripMargin.trim
      )
    }

    //33
    "parseLookup should be able to handle multiple fields in the lookup statement" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val selectStatement = SelectStatement(Seq.empty, Seq.empty, 0, 0, Seq(AllField, population, stateID))
      val lookup = LookupStatement(Seq(geoStateID), populationDataSet, Seq(stateID), Seq(population, stateID),
        Seq(population, stateID))
      val filter = Seq(textFilter)
      val query = new Query(TwitterDataSet, Seq.empty, Seq(lookup), filter, Seq.empty, select = Some(selectStatement))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""{"method":"search","dataset":"$TwitterDataSet","query":{"bool":{"must":[{"match":{"text":{"query":"${textValue.mkString(",")}","operator":"and"}}}]}},"size":0,"from":0,"_source":["*","population","stateID"]}""".stripMargin.trim
      )
    }

    //34
    "translate lookup multiple tables with one join key on each" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val literacyDataSet = LiteracyDataStore.DatasetName
      val literacySchema = LiteracyDataStore.LiteracySchema

      val selectValues = Seq(AllField, population, literacy)
      val selectStatement = SelectStatement(Seq.empty, Seq.empty, 0, 0, selectValues)
      val filter = Seq(textFilter)
      val query = new Query(TwitterDataSet, Seq.empty,
        lookup = Seq(lookupPopulation, lookupLiteracy),
        filter, Seq.empty,
        select = Some(selectStatement))

      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema, populationDataSet -> populationSchema,
        literacyDataSet -> literacySchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""{"method":"search","dataset":"$TwitterDataSet","query":{"bool":{"must":[{"match":{"text":{"query":"${textValue.mkString(",")}","operator":"and"}}}]}},"size":0,"from":0,"_source":["*","population","literacy"]}""".stripMargin.trim
      )
    }

    //35
    "translate a text contain + time + geo id set filter and group day and state and aggregate topK hashtags" in {
      ok
    }

    //36
    "translate lookup inside group by state and count" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byGeoState), Seq(aggrCount), Seq(lookupPopulationByState))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group))
      val result = parser.generate(query, schemaMap = Map(TwitterDataSet -> twitterSchema, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """{"method":"msearch","queries":[{"index":"twitter.ds_tweet"},{"query":{"bool":{"must":[{"match":{"text":{"query":"zika,virus","operator":"and"}}}]}},"size":0,"aggs":{"state":{"terms":{"field":"geo_tag.stateID","size":2147483647}}}},{"index":"twitter.us_population"},{"_source":"population","size":2147483647,"sort":{"stateID":{"order":"asc"}}}],"groupAsList":["state"],"joinSelectField":"population","joinTermsFilter":[]}""".stripMargin.trim
      )
    }

    //37
    // Elasticsearch adapter does not support nested join query
    "translate multiple lookups inside group by state and count" in {
      ok
    }

    //38
    // Elasticsearch adapter does not support nested join query
    "translate multiple lookups inside/outside group by state and aggregate population" in {
      ok
    }

    //39
    "translate lookup inside group by state with global aggregate" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val selectStatement = SelectStatement(Seq.empty, Seq.empty, 0, 0, Seq(AllField, population))
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byGeoState), Seq.empty, Seq(lookupPopulationByState))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectStatement), Some(GlobalAggregateStatement(aggrPopulationMin)))
      val result = parser.generate(query, schemaMap = Map(TwitterDataSet -> twitterSchema, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        s"""{"method":"msearch","queries":[{"index":"$TwitterDataSet"},{"query":{"bool":{"must":[{"match":{"text":{"query":"${textValue.mkString(",")}","operator":"and"}}}]}},"size":0,"aggs":{"state":{"terms":{"field":"geo_tag.stateID","size":2147483647}}}},{"index":"twitter.us_population"},{"_source":"population","size":2147483647,"sort":{"stateID":{"order":"asc"}}}],"groupAsList":["state"],"joinSelectField":"population","joinTermsFilter":[],"aggregation":{"func":"min","as":"min"},"size":0,"aggs":{"min":{"min":{"field":"population"}}}}""".stripMargin.trim
      )
    }

    //40
    "translate append with lookup inside group by state and sum" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byGeoState), Seq(aggrAvg), Seq(lookupPopulationByState))
      val query = new Query(TwitterDataSet, Seq(appendLangLen), Seq.empty, filter, Seq.empty, Some(group))
      val result = parser.generate(query, schemaMap = Map(TwitterDataSet -> twitterSchema, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """{"method":"msearch","queries":[{"index":"twitter.ds_tweet"},{"query":{"bool":{"must":[{"match":{"text":{"query":"zika,virus","operator":"and"}}}]}},"size":0,"aggs":{"state":{"terms":{"field":"geo_tag.stateID","size":2147483647},"aggs":{"avg":{"avg":{"field":"id"}}}}}},{"index":"twitter.us_population"},{"_source":"population","size":2147483647,"sort":{"stateID":{"order":"asc"}}}],"groupAsList":["state"],"joinSelectField":"population","joinTermsFilter":[]}""".stripMargin.trim
      )
    }
  }

  //41, 42
  "Elasticsearch Generator calcResultSchema" should {
    "return the input schema if the query is subset filter only" in {
      val schema = parser.calcResultSchema(zikaCreateQueryForSQL, TwitterDataStoreWithHashTag.TwitterSchemaForSQL)
      schema must_== TwitterDataStoreWithHashTag.TwitterSchemaForSQL
    }
    "return the aggregated schema for aggregation queries" in {
      ok
    }
  }

  //43
  "Elasticsearch Generator createView" should {
    "generate the ddl for the twitter dataset" in {
      val ddl = parser.generate(CreateView("zika", zikaCreateQueryForSQL), Map(twitterDataSetForSQL -> TwitterDataStoreWithHashTag.TwitterSchemaForSQL))
      removeEmptyLine(ddl) must_== unifyNewLine(
        """[{"method":"drop","dataset":"zika"},{"method":"create","dataset":"zika"},{"method":"reindex","source":{"index":"twitter_ds_tweet","query":{"bool":{"must":[{"match":{"text":{"query":"zika","operator":"and"}}}]}}},"dest":{"index":"zika"}}]""".stripMargin.trim)
    }
  }

  //44
  "Elasticsearch Generator deleteRecord" should {
    "generate the delete query " in {
      val sql = parser.generate(DeleteRecord(twitterDataSetForSQL, Seq(timeFilter)), Map(twitterDataSetForSQL -> TwitterDataStoreWithHashTag.TwitterSchemaForSQL))
      removeEmptyLine(sql) must_== unifyNewLine(
        s"""{"query":{"bool":{"must":[{"range":{"create_at":{"gte":"$startTime","lt":"$endTime"}}}]}},"method":"delete","dataset":"$twitterDataSetForSQL"}""".stripMargin.trim)
    }
  }

  //45
  "Elasticsearch Generator dropView" should {
    "generate the drop view query" in {
      val sql = parser.generate(DropView(twitterDataSetForSQL), Map(twitterDataSetForSQL -> TwitterDataStoreWithHashTag.TwitterSchemaForSQL))
      removeEmptyLine(sql) must_== unifyNewLine(
        """{"method":"drop","dataset":"twitter_ds_tweet"}""".stripMargin.trim)
    }
  }
}