package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema._
import org.specs2.mutable.Specification



class OracleGeneratorTest extends Specification {

  import TestQuery._

  val parser = new OracleGenerator

  "OracleGenerator generate" should {
    "translate a simple query group by hour" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select to_char(t."create_at",'yyyy-mm-dd hh24') as "hour",count(*) as "count"
           |from "twitter.ds_tweet" t
           |where t."create_at" >= to_date('2016-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS') and t."create_at" < to_date('2016-12-01 00:00:00','YYYY-MM-DD HH24:MI:SS')
           |group by to_char(t."create_at",'yyyy-mm-dd hh24')
           |""".stripMargin.trim)

    }


    "translate a simple query group by day" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byDay), Seq(aggrCount))
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select to_char(t."create_at",'yyyy-mm-dd') as "day",count(*) as "count"
           |from "twitter.ds_tweet" t
           |where t."create_at" >= to_date('2016-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS') and t."create_at" < to_date('2016-12-01 00:00:00','YYYY-MM-DD HH24:MI:SS')
           |group by to_char(t."create_at",'yyyy-mm-dd')
           |""".stripMargin.trim)
    }

    //3
    "translate a simple query group by month" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byMonth), Seq(aggrCount))
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select to_char(t."create_at",'yyyy-mm') as "month",count(*) as "count"
           |from "twitter.ds_tweet" t
           |where t."create_at" >= to_date('2016-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS') and t."create_at" < to_date('2016-12-01 00:00:00','YYYY-MM-DD HH24:MI:SS')
           |group by to_char(t."create_at",'yyyy-mm')
           |""".stripMargin.trim)
    }

    //4
    "translate a geo id set filter group by time query" in {
      val filter = Seq(timeFilter, textFilter, stateFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select to_char(t."create_at",'yyyy-mm-dd hh24') as "hour",count(*) as "count"
           |from "twitter.ds_tweet" t
           |where t."create_at" >= to_date('2016-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS') and t."create_at" < to_date('2016-12-01 00:00:00','YYYY-MM-DD HH24:MI:SS') and contains(t."text", 'zika and virus',1)>0 and t."geo_tag.stateID" in ( 37,51,24,11,10,34,42,9,44 )
           |group by to_char(t."create_at",'yyyy-mm-dd hh24')
           |""".stripMargin.trim)
    }

    //5
    "translate a simple select query order by time with limit 100" in {
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, Seq.empty, Seq.empty, None, Some(selectAllOrderByTimeDesc))
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select *
           |from "twitter.ds_tweet" t
           |order by t."create_at" desc
           |fetch first 100 rows only
           |""".stripMargin.trim)
    }

    //6 //TODO: parseUnnest
    "translate a simple unnest query" in {
      ok
    }

    //7
    "translate a count cardinality query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = twitterDataSetForOracle, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """select count(*) as "count" from
          |(select *
          |from "twitter.ds_tweet" t) t""".stripMargin)
    }

    //8
    "translate get min field value query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrMin)
      val query = new Query(dataset = twitterDataSetForOracle, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """select min(t."id") as "min" from
          |(select *
          |from "twitter.ds_tweet" t) t""".stripMargin)
    }

    //9
    "translate get max field value query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrMax)
      val query = new Query(dataset = twitterDataSetForOracle, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """select max(t."id") as "max" from
          |(select *
          |from "twitter.ds_tweet" t) t""".stripMargin)
    }

    //10
    "translate a count cardinality query with filter without group by" in {
      val filter = Seq(timeFilter)
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = twitterDataSetForOracle, filter = filter, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """select count(*) as "count" from
          |(select *
          |from "twitter.ds_tweet" t
          |where t."create_at" >= to_date('2016-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS') and t."create_at" < to_date('2016-12-01 00:00:00','YYYY-MM-DD HH24:MI:SS')) t""".stripMargin)
    }

    //11
    "translate a min cardinality query with filter without group by" in {
      val filter = Seq(timeFilter)
      val globalAggr = GlobalAggregateStatement(aggrMin)
      val query = new Query(dataset = twitterDataSetForOracle, filter = filter, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """select min(t."id") as "min" from
          |(select *
          |from "twitter.ds_tweet" t
          |where t."create_at" >= to_date('2016-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS') and t."create_at" < to_date('2016-12-01 00:00:00','YYYY-MM-DD HH24:MI:SS')) t""".stripMargin)
    }

    //12
    "translate a count cardinality query with select" in {
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = twitterDataSetForOracle, select = Some(selectTop10), globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """select count(*) as "count" from
          |(select *
          |from "twitter.ds_tweet" t
          |fetch first 10 rows only) t""".stripMargin.trim)
    }

    //13
    "translate group by minute" in {
      val group = GroupStatement(Seq(byMinuteForSparkSql), Seq(aggrCount))
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select to_char(t."create_at",'yyyy-mm-dd hh24:mi') as "minute",count(*) as "count"
          |from "twitter.ds_tweet" t
          |group by to_char(t."create_at",'yyyy-mm-dd hh24:mi')""".stripMargin.trim)
    }

    //14
    "translate a simple filter by time and group by time query max id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrMax))
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select to_char(t."create_at",'yyyy-mm-dd hh24') as "hour",max(t."id") as "max"
          |from "twitter.ds_tweet" t
          |where t."create_at" >= to_date('2016-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS') and t."create_at" < to_date('2016-12-01 00:00:00','YYYY-MM-DD HH24:MI:SS')
          |group by to_char(t."create_at",'yyyy-mm-dd hh24')""".stripMargin.trim)
    }

    //15 //TODO: parseUnnest
    "translate a max cardinality query with unnest with group by with select" in {
      ok
    }

    //16
    "translate group by query having lookup with one join key" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val selectValues = Seq(population)
      val group = Some(groupPopulationSumForSQL)
      val lookup = LookupStatement(
        sourceKeys = Seq(geoStateID),
        dataset = populationDataSet,
        lookupKeys = Seq(stateID),
        selectValues,
        as = selectValues)
      val filter = Seq(textFilter)
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq(lookup), filter, Seq.empty, group)
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select t."geo_tag.stateID" as "state",sum(l0."population") as "sum"
          |from "twitter.ds_tweet" t
          |left outer join "twitter.US_population" l0 on l0."stateID" = t."geo_tag.stateID"
          |where contains(t."text", 'zika and virus',1)>0
          |group by t."geo_tag.stateID"""".stripMargin.trim
      )
    }

    //17
    "translate a simple filter by time and group by time query" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select to_char(t."create_at",'yyyy-mm-dd hh24') as "hour",count(*) as "count"
          |from "twitter.ds_tweet" t
          |where t."create_at" >= to_date('2016-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS') and t."create_at" < to_date('2016-12-01 00:00:00','YYYY-MM-DD HH24:MI:SS')
          |group by to_char(t."create_at",'yyyy-mm-dd hh24')""".stripMargin.trim)
    }

    //18
    "translate a simple filter with string not match" in {
      val filter = Seq(langNotMatchFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select to_char(t."create_at",'yyyy-mm-dd hh24') as "hour",count(*) as "count"
          |from "twitter.ds_tweet" t
          |where t."lang"!='en'
          |group by to_char(t."create_at",'yyyy-mm-dd hh24')
          |""".stripMargin.trim)
    }

    //19
    "translate a simple filter with string matches" in {
      val filter = Seq(langMatchFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select to_char(t."create_at",'yyyy-mm-dd hh24') as "hour",count(*) as "count"
          |from "twitter.ds_tweet" t
          |where t."lang"='en'
          |group by to_char(t."create_at",'yyyy-mm-dd hh24')""".stripMargin.trim)
    }

    //20
    "translate a text contain filter and group by time query" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select to_char(t."create_at",'yyyy-mm-dd hh24') as "hour",count(*) as "count"
          |from "twitter.ds_tweet" t
          |where contains(t."text", 'zika and virus',1)>0
          |group by to_char(t."create_at",'yyyy-mm-dd hh24')
          |""".stripMargin.trim)
    }

    //21
    "translate a geo id set filter group by time query" in {
      val filter = Seq(stateFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select to_char(t."create_at",'yyyy-mm-dd hh24') as "hour",count(*) as "count"
          |from "twitter.ds_tweet" t
          |where t."geo_tag.stateID" in ( 37,51,24,11,10,34,42,9,44 )
          |group by to_char(t."create_at",'yyyy-mm-dd hh24')
          | """.stripMargin.trim)
    }

    //22
    "translate a text contain + time + geo id set filter and group by time + spatial cube" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val group = GroupStatement(Seq(byHour, byGeoState), Seq(aggrCount))
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select to_char(t."create_at",'yyyy-mm-dd hh24') as "hour",t."geo_tag.stateID" as "state",count(*) as "count"
          |from "twitter.ds_tweet" t
          |where contains(t."text", 'zika and virus',1)>0 and t."create_at" >= to_date('2016-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS') and t."create_at" < to_date('2016-12-01 00:00:00','YYYY-MM-DD HH24:MI:SS') and t."geo_tag.stateID" in ( 37,51,24,11,10,34,42,9,44 )
          |group by to_char(t."create_at",'yyyy-mm-dd hh24'),t."geo_tag.stateID"
          | """.stripMargin.trim)
    }

    //23
    "translate a text contain + time + geo id set filter and sample tweets" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, filter, Seq.empty, None, Some(selectRecent))
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select t."create_at" as "create_at",t."id" as "id",t."user.id" as "user.id"
          |from "twitter.ds_tweet" t
          |where contains(t."text", 'zika and virus',1)>0 and t."create_at" >= to_date('2016-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS') and t."create_at" < to_date('2016-12-01 00:00:00','YYYY-MM-DD HH24:MI:SS') and t."geo_tag.stateID" in ( 37,51,24,11,10,34,42,9,44 )
          |order by t."create_at" desc
          |fetch first 100 rows only
          | """.stripMargin.trim)
    }

    //24 //TODO: parseUnnest
    "translate a text contain + time + geo id set filter and group by hashtags" in {
      ok
    }

    //25
    "translate a simple filter by time and group by time query min id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrMin))
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select to_char(t."create_at",'yyyy-mm-dd hh24') as "hour",min(t."id") as "min"
          |from "twitter.ds_tweet" t
          |where t."create_at" >= to_date('2016-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS') and t."create_at" < to_date('2016-12-01 00:00:00','YYYY-MM-DD HH24:MI:SS')
          |group by to_char(t."create_at",'yyyy-mm-dd hh24')
          | """.stripMargin.trim)
    }

    //26
    "translate a simple filter by time and group by time query sum id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrSum))
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select to_char(t."create_at",'yyyy-mm-dd hh24') as "hour",sum(t."id") as "sum"
          |from "twitter.ds_tweet" t
          |where t."create_at" >= to_date('2016-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS') and t."create_at" < to_date('2016-12-01 00:00:00','YYYY-MM-DD HH24:MI:SS')
          |group by to_char(t."create_at",'yyyy-mm-dd hh24')
          |  """.stripMargin.trim)
    }

    //27
    "translate a simple filter by time and group by time query avg id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrAvg))
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select to_char(t."create_at",'yyyy-mm-dd hh24') as "hour",avg(t."id") as "avg"
          |from "twitter.ds_tweet" t
          |where t."create_at" >= to_date('2016-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS') and t."create_at" < to_date('2016-12-01 00:00:00','YYYY-MM-DD HH24:MI:SS')
          |group by to_char(t."create_at",'yyyy-mm-dd hh24')
          | """.stripMargin.trim)
    }

    //32
    "translate a select 10 coordinates" in {
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, Seq.empty, Seq.empty, None,Some(select10Coordinates))
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select t."coordinate" as "coordinate"
          |from "twitter.ds_tweet" t
          |fetch first 10 rows only""".stripMargin.trim)
    }




    //33
    "translate a text contain filter and select 10" in {
      val filter = Seq(textFilter)
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, filter, Seq.empty, None, Some(selectTop10))
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select *
          |from "twitter.ds_tweet" t
          |where contains(t."text", 'zika and virus',1)>0
          |fetch first 10 rows only
          | """.stripMargin.trim)
    }

    //34
    "translate group by second" in {
      val group = GroupStatement(Seq(bySecond), Seq(aggrCount))
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select to_char(t."create_at",'yyyy-mm-dd hh24:mi:ss') as "sec",count(*) as "count"
          |from "twitter.ds_tweet" t
          |group by to_char(t."create_at",'yyyy-mm-dd hh24:mi:ss')
          | """.stripMargin.trim)
    }

    //35
    "translate group by day" in {
      val group = GroupStatement(Seq(byDay), Seq(aggrCount))
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select to_char(t."create_at",'yyyy-mm-dd') as "day",count(*) as "count"
          |from "twitter.ds_tweet" t
          |group by to_char(t."create_at",'yyyy-mm-dd')
          | """.stripMargin.trim)
    }

    //36
    "translate group by month" in {
      val group = GroupStatement(Seq(byMonth), Seq(aggrCount))
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select to_char(t."create_at",'yyyy-mm') as "month",count(*) as "count"
          |from "twitter.ds_tweet" t
          |group by to_char(t."create_at",'yyyy-mm')
          | """.stripMargin.trim)
    }

    //37
    "translate group by year" in {
      val group = GroupStatement(Seq(byYear), Seq(aggrCount))
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select to_char(t."create_at",'yyyy') as "year",count(*) as "count"
          |from "twitter.ds_tweet" t
          |group by to_char(t."create_at",'yyyy')
          | """.stripMargin.trim)
    }

    //38
    "translate lookup one table with one join key" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val selectStatement = SelectStatement(Seq.empty, Seq.empty, 0, 0, Seq(AllField, population))
      val lookup = Seq(lookupPopulation)
      val filter = Seq(textFilter)
      val query = new Query(twitterDataSetForOracle, Seq.empty, lookup, filter, Seq.empty, select = Some(selectStatement))
      val result = parser.generate(query, schemaMap = Map(twitterDataSetForOracle -> twitterSchemaForOracle, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select t."*" as "*",l0."population" as "population"
          |from "twitter.ds_tweet" t
          |left outer join "twitter.US_population" l0 on l0."stateID" = t."geo_tag.stateID"
          |where contains(t."text", 'zika and virus',1)>0""".stripMargin.trim
      )
    }

    //39
    "parseLookup should be able to handle multiple fields in the lookup statement" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val selectStatement = SelectStatement(Seq.empty, Seq.empty, 0, 0, Seq(AllField, population, stateID))
      val lookup = LookupStatement(Seq(geoStateID), populationDataSet, Seq(stateID), Seq(population, stateID),
        Seq(population, stateID))
      val filter = Seq(textFilter)
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq(lookup), filter, Seq.empty, select = Some(selectStatement))
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select t."*" as "*",l0."population" as "population",l0."stateID" as "stateID"
          |from "twitter.ds_tweet" t
          |left outer join "twitter.US_population" l0 on l0."stateID" = t."geo_tag.stateID"
          |where contains(t."text", 'zika and virus',1)>0
          |""".stripMargin.trim
      )
    }

    //40
    "translate lookup multiple tables with one join key on each" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val literacyDataSet = LiteracyDataStore.DatasetName
      val literacySchema = LiteracyDataStore.LiteracySchema

      val selectValues = Seq(AllField, population, literacy)
      val selectStatement = SelectStatement(Seq.empty, Seq.empty, 0, 0, selectValues)
      val filter = Seq(textFilter)
      val query = new Query(twitterDataSetForOracle, Seq.empty,
        lookup = Seq(lookupPopulation, lookupLiteracy),
        filter, Seq.empty,
        select = Some(selectStatement))

      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle, populationDataSet -> populationSchema,
        literacyDataSet -> literacySchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select t."*" as "*",l0."population" as "population",l1."literacy" as "literacy"
          |from "twitter.ds_tweet" t
          |left outer join "twitter.US_population" l0 on l0."stateID" = t."geo_tag.stateID"
          |left outer join "twitter.US_literacy" l1 on l1."stateID" = t."geo_tag.stateID"
          |where contains(t."text", 'zika and virus',1)>0
          |""".stripMargin.trim
      )
    }

    //41
    "translate a text contain + time + geo id set filter and group day and state and aggregate topK hashtags" in {
      ok
    }

    //42
    "translate lookup inside group by state and count" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byGeoState), Seq(aggrCount), Seq(lookupPopulationByState))
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, filter, Seq.empty, Some(group))
      val result = parser.generate(query, schemaMap = Map(twitterDataSetForOracle -> twitterSchemaForOracle, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select tt."state" as "state",tt."count" as "count",ll0."population" as "population"
          |from (
          |select t."geo_tag.stateID" as "state",count(*) as "count"
          |from "twitter.ds_tweet" t
          |where contains(t."text", 'zika and virus',1)>0
          |group by t."geo_tag.stateID"
          |) tt
          |left outer join "twitter.US_population" ll0 on ll0."stateID" = tt."state"
          |""".stripMargin.trim
      )
    }

    //43
    "translate multiple lookups inside group by state and count" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema
      val literacyDataSet = LiteracyDataStore.DatasetName
      val literacySchema = LiteracyDataStore.LiteracySchema

      val selectStatement = SelectStatement(Seq.empty, Seq.empty, 0, 0, Seq(state, count, population, literacy))
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byGeoState), Seq(aggrCount), Seq(lookupPopulationByState, lookupLiteracyByState))
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), select = Some(selectStatement))
      val result = parser.generate(query, schemaMap = Map(twitterDataSetForOracle -> twitterSchemaForOracle, populationDataSet -> populationSchema, literacyDataSet -> literacySchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select tt."state" as "state",tt."count" as "count",ll0."population" as "population",ll1."literacy" as "literacy"
          |from (
          |select t."geo_tag.stateID" as "state",count(*) as "count"
          |from "twitter.ds_tweet" t
          |where contains(t."text", 'zika and virus',1)>0
          |group by t."geo_tag.stateID"
          |) tt
          |left outer join "twitter.US_population" ll0 on ll0."stateID" = tt."state"
          |left outer join "twitter.US_literacy" ll1 on ll1."stateID" = tt."state"
          |""".stripMargin.trim
      )
    }

    //44
    "translate multiple lookups inside/outside group by state and aggregate population" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema
      val literacyDataSet = LiteracyDataStore.DatasetName
      val literacySchema = LiteracyDataStore.LiteracySchema

      val selectStatement = SelectStatement(Seq.empty, Seq.empty, 0, 0, Seq(state, min, literacy))
      val filter = Seq(textFilter)
      val lookup = Seq(lookupPopulation)
      val group = GroupStatement(Seq(byGeoState), Seq(aggrPopulationMin), Seq(lookupLiteracyByState))
      val query = new Query(twitterDataSetForOracle, Seq.empty, lookup, filter, Seq.empty, Some(group), select = Some(selectStatement))
      val result = parser.generate(query, schemaMap = Map(twitterDataSetForOracle -> twitterSchemaForOracle, populationDataSet -> populationSchema, literacyDataSet -> literacySchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select tt."state" as "state",tt."min" as "min",ll0."literacy" as "literacy"
          |from (
          |select t."geo_tag.stateID" as "state",min(l0."population") as "min"
          |from "twitter.ds_tweet" t
          |left outer join "twitter.US_population" l0 on l0."stateID" = t."geo_tag.stateID"
          |where contains(t."text", 'zika and virus',1)>0
          |group by t."geo_tag.stateID"
          |) tt
          |left outer join "twitter.US_literacy" ll0 on ll0."stateID" = tt."state"
          |""".stripMargin.trim
      )
    }

    //45
    "translate lookup inside group by state with global aggregate" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val selectStatement = SelectStatement(Seq.empty, Seq.empty, 0, 0, Seq(AllField, population))
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byGeoState), Seq.empty, Seq(lookupPopulationByState))
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectStatement), Some(GlobalAggregateStatement(aggrPopulationMin)))
      val result = parser.generate(query, schemaMap = Map(twitterDataSetForOracle -> twitterSchemaForOracle, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select min(t."population") as "min" from
          |(select tt."state" as "state",ll0."population" as "population"
          |from (
          |select t."geo_tag.stateID" as "state"
          |from "twitter.ds_tweet" t
          |where contains(t."text", 'zika and virus',1)>0
          |group by t."geo_tag.stateID"
          |) tt
          |left outer join "twitter.US_population" ll0 on ll0."stateID" = tt."state") t
          |""".stripMargin.trim
      )
    }

    //46
    "translate append with lookup inside group by state and sum" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byGeoState), Seq(aggrAvgLangLen), Seq(lookupPopulationByState))
      val query = new Query(twitterDataSetForOracle, Seq(appendLangLen), Seq.empty, filter, Seq.empty, Some(group))
      val result = parser.generate(query, schemaMap = Map(twitterDataSetForOracle -> twitterSchemaForOracle, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select tt."state" as "state",tt."avgLangLen" as "avgLangLen",ll0."population" as "population"
          |from (
          |select t."geo_tag.stateID" as "state",avg(t."lang_len") as "avgLangLen"
          |from (select length(lang) as "lang_len",t."*" as "*"
          |from "twitter.ds_tweet" t) t
          |where contains(t."text", 'zika and virus',1)>0
          |group by t."geo_tag.stateID"
          |) tt
          |left outer join "twitter.US_population" ll0 on ll0."stateID" = tt."state"
          |""".stripMargin.trim
      )
    }

    //47
    "translate a simple filter with is null" in {
      val filter = Seq(isNullFilter)
      val select = Option(selectSimple)
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, filter, Seq.empty, None, select)
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select t."create_at" as "create_at",t."id" as "id",t."user.id" as "user.id"
          |from "twitter.ds_tweet" t
          |where t."text" is null
          |""".stripMargin.trim)
    }

    //48
    "translate a simple filter with is not null" in {
      val filter = Seq(isNotNullFilter)
      val select = Option(selectSimple)
      val query = new Query(twitterDataSetForOracle, Seq.empty, Seq.empty, filter, Seq.empty, None, select)
      val result = parser.generate(query, Map(twitterDataSetForOracle -> twitterSchemaForOracle))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select t."create_at" as "create_at",t."id" as "id",t."user.id" as "user.id"
          |from "twitter.ds_tweet" t
          |where t."text" is not null
          |""".stripMargin.trim)
    }

  }
   //49
  "OracleGenerator calcResultSchema" should {
    "return the input schema if the query is subset filter only" in {
      val schema = parser.calcResultSchema(zikaCreateQueryForOracle, TwitterDataStoreWithoutHashTagOracle.TwitterSchemaForOracle)
      schema must_== TwitterDataStoreWithoutHashTagOracle.TwitterSchemaForOracle
    }
    "return the aggregated schema for aggregation queries" in {
      ok
    }
  }
  //50
  "OracleGenerator createView" should {
    "generate the ddl for the twitter dataset" in {
      val ddl = parser.generate(CreateView("zika", zikaCreateQueryForOracle), Map(twitterDataSetForOracle -> TwitterDataStoreWithoutHashTagOracle.TwitterSchemaForOracle))
      removeEmptyLine(ddl) must_== unifyNewLine(
        """
          |declare
          |    result1 number(8);
          |begin
          |    select count(*) into result1 from all_tables where owner = 'BERRY' and table_name = 'zika';
          |if result1 = 0 then
          |execute immediate 'create table "zika" (
          |  "place.bounding_box" VARCHAR2(255) default null,
          |  "favorite_count" NUMBER default null,
          |  "geo_tag.countyID" NUMBER default null,
          |  "user_mentions" VARCHAR(1000) default null,
          |  "user.id" NUMBER default null,
          |  "geo_tag.cityID" NUMBER default null,
          |  "is_retweet" NUMBER(1) default null,
          |  "text" CLOB default null,
          |  "retweet_count" NUMBER default null,
          |  "in_reply_to_user" NUMBER default null,
          |  "id" NUMBER default null,
          |  "coordinate" SDO_GEOMETRY default null,
          |  "in_reply_to_status" NUMBER default null,
          |  "user.status_count" NUMBER default null,
          |  "geo_tag.stateID" NUMBER default null,
          |  "create_at" TIMESTAMP default null,
          |  "lang" VARCHAR2(255) default null,
          |  "user.profile_image_url" VARCHAR2(255) default null,
          |  "user.name" VARCHAR2(255) default null,
          |  "hashtags" VARCHAR(1000) default null, primary key ("id")
          |)';
          |end if;
          |end;
          |/
          |merge into "zika" d
          |using (select *
          |from "twitter.ds_tweet" t
          |where contains(t."text", 'zika',1)>0) s
          |on (d."id" = s."id")
          |when not matched then
          |insert (d."create_at",d."id",d."coordinate",d."lang",d."is_retweet",d."hashtags",d."user_mentions",d."user.id",d."user.name",d."user.profile_image_url",d."geo_tag.stateID",d."geo_tag.countyID",d."geo_tag.cityID",d."place.bounding_box",d."text",d."in_reply_to_status",d."in_reply_to_user",d."favorite_count",d."retweet_count",d."user.status_count")
          |values (s."create_at",s."id",s."coordinate",s."lang",s."is_retweet",s."hashtags",s."user_mentions",s."user.id",s."user.name",s."user.profile_image_url",s."geo_tag.stateID",s."geo_tag.countyID",s."geo_tag.cityID",s."place.bounding_box",s."text",s."in_reply_to_status",s."in_reply_to_user",s."favorite_count",s."retweet_count",s."user.status_count")
          |""".stripMargin.trim)
    }
  }
  //51
  "OracleGenerator deleteRecord" should {
    "generate the delete query " in {
      val sql = parser.generate(DeleteRecord(twitterDataSetForOracle, Seq(timeFilter)), Map(twitterDataSetForOracle -> TwitterDataStoreWithoutHashTagOracle.TwitterSchemaForOracle))
      removeEmptyLine(sql) must_== unifyNewLine(
        """
          |delete from "twitter.ds_tweet" t
          |where t."create_at" >= to_date('2016-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS') and t."create_at" < to_date('2016-12-01 00:00:00','YYYY-MM-DD HH24:MI:SS')
          |""".stripMargin.trim)
    }
  }
  //52
  "OracleGenerator dropView" should {
    "generate the drop view query" in {
      val sql = parser.generate(DropView(twitterDataSetForOracle), Map(twitterDataSetForOracle -> TwitterDataStoreWithoutHashTagOracle.TwitterSchemaForOracle))
      removeEmptyLine(sql) must_== unifyNewLine(
        """
          |declare
          |     result1 number(8);
          |begin
          |   select count(*)into result1 from all_tables where owner = 'BERRY' and table_name = 'twitter.ds_tweet';
          |if result1 > 0 then
          |execute immediate 'drop table "twitter.ds_tweet" ';
          |end if;
          |end;
          |""".stripMargin.trim)
    }
  }

}