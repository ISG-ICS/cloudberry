package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema._
import org.specs2.mutable.Specification


class MySQLGeneratorTest extends Specification {

  import TestQuery._

  val parser = new MySQLGenerator

  "MySQLGenerator generate" should {
    //1
    "translate a simple query group by hour" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select hour(t.`create_at`) as `hour`,count(*) as `count`
           |from `twitter_ds_tweet` t
           |where t.`create_at` >= '2016-01-01 00:00:00.000' and t.`create_at` < '2016-12-01 00:00:00.000'
           |group by `hour`
           |""".stripMargin.trim)
    }

    //2
    "translate a simple query group by day" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byDay), Seq(aggrCount))
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select date(t.`create_at`) as `day`,count(*) as `count`
           |from `twitter_ds_tweet` t
           |where t.`create_at` >= '2016-01-01 00:00:00.000' and t.`create_at` < '2016-12-01 00:00:00.000'
           |group by `day`
           |""".stripMargin.trim)
    }

    //3
    "translate a simple query group by month" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byMonth), Seq(aggrCount))
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select month(t.`create_at`) as `month`,count(*) as `count`
           |from `twitter_ds_tweet` t
           |where t.`create_at` >= '2016-01-01 00:00:00.000' and t.`create_at` < '2016-12-01 00:00:00.000'
           |group by `month`
           |""".stripMargin.trim)
    }

    //4
    "translate a geo id set filter group by time query" in {
      val filter = Seq(timeFilter, textFilter, stateFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select hour(t.`create_at`) as `hour`,count(*) as `count`
           |from `twitter_ds_tweet` t
           |where t.`create_at` >= '2016-01-01 00:00:00.000' and t.`create_at` < '2016-12-01 00:00:00.000' and match(t.`text`) against ('+zika +virus' in boolean mode) and t.`geo_tag.stateID` in ( 37,51,24,11,10,34,42,9,44 )
           |group by `hour`
           |""".stripMargin.trim)
    }

    //5
    "translate a simple select query order by time with limit 100" in {
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, Seq.empty, Seq.empty, None, Some(selectAllOrderByTimeDesc))
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select *
           |from `twitter_ds_tweet` t
           |order by t.`create_at` desc
           |limit 100
           |""".stripMargin.trim)
    }

    //6 //TODO: parseUnnest
    "translate a simple unnest query" in {
      ok
    }

    //7
    "translate a count cardinality query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = twitterDataSetForSQL, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """select count(*) as `count` from
          |(select *
          |from `twitter_ds_tweet` t) t""".stripMargin)
    }

    //8
    "translate get min field value query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrMin)
      val query = new Query(dataset = twitterDataSetForSQL, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """select min(t.`id`) as `min` from
          |(select *
          |from `twitter_ds_tweet` t) t""".stripMargin)
    }

    //9
    "translate get max field value query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrMax)
      val query = new Query(dataset = twitterDataSetForSQL, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """select max(t.`id`) as `max` from
          |(select *
          |from `twitter_ds_tweet` t) t""".stripMargin)
    }

    //10
    "translate a count cardinality query with filter without group by" in {
      val filter = Seq(timeFilter)
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = twitterDataSetForSQL, filter = filter, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """select count(*) as `count` from
          |(select *
          |from `twitter_ds_tweet` t
          |where t.`create_at` >= '2016-01-01 00:00:00.000' and t.`create_at` < '2016-12-01 00:00:00.000') t""".stripMargin)
    }

    //11
    "translate a min cardinality query with filter without group by" in {
      val filter = Seq(timeFilter)
      val globalAggr = GlobalAggregateStatement(aggrMin)
      val query = new Query(dataset = twitterDataSetForSQL, filter = filter, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """select min(t.`id`) as `min` from
          |(select *
          |from `twitter_ds_tweet` t
          |where t.`create_at` >= '2016-01-01 00:00:00.000' and t.`create_at` < '2016-12-01 00:00:00.000') t""".stripMargin)
    }

    //12
    "translate a count cardinality query with select" in {
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = twitterDataSetForSQL, select = Some(selectTop10), globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """select count(*) as `count` from
          |(select *
          |from `twitter_ds_tweet` t
          |limit 10) t""".stripMargin.trim)
    }

    //13
    "translate group by minute" in {
      val group = GroupStatement(Seq(byMinuteForSparkSql), Seq(aggrCount))
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select minute(t.`create_at`) as `minute`,count(*) as `count`
          |from `twitter_ds_tweet` t
          |group by `minute`""".stripMargin.trim)
    }

    //14
    "translate a simple filter by time and group by time query max id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrMax))
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select hour(t.`create_at`) as `hour`,max(t.`id`) as `max`
          |from `twitter_ds_tweet` t
          |where t.`create_at` >= '2016-01-01 00:00:00.000' and t.`create_at` < '2016-12-01 00:00:00.000'
          |group by `hour`""".stripMargin.trim)
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
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq(lookup), filter, Seq.empty, group)
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select t.`geo_tag.stateID` as `state`,sum(l0.`population`) as `sum`
          |from `twitter_ds_tweet` t
          |left outer join `twitter.US_population` l0 on l0.`stateID` = t.`geo_tag.stateID`
          |where match(t.`text`) against ('+zika +virus' in boolean mode)
          |group by `state`""".stripMargin.trim
      )
    }

    //17
    "translate a simple filter by time and group by time query" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select hour(t.`create_at`) as `hour`,count(*) as `count`
          |from `twitter_ds_tweet` t
          |where t.`create_at` >= '2016-01-01 00:00:00.000' and t.`create_at` < '2016-12-01 00:00:00.000'
          |group by `hour`""".stripMargin.trim)
    }

    //18
    "translate a simple filter with string not match" in {
      val filter = Seq(langNotMatchFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select hour(t.`create_at`) as `hour`,count(*) as `count`
          |from `twitter_ds_tweet` t
          |where t.`lang`!='en'
          |group by `hour`
          |""".stripMargin.trim)
    }

    //19
    "translate a simple filter with string matches" in {
      val filter = Seq(langMatchFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select hour(t.`create_at`) as `hour`,count(*) as `count`
          |from `twitter_ds_tweet` t
          |where t.`lang`='en'
          |group by `hour`""".stripMargin.trim)
    }

    //20
    "translate a text contain filter and group by time query" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select hour(t.`create_at`) as `hour`,count(*) as `count`
          |from `twitter_ds_tweet` t
          |where match(t.`text`) against ('+zika +virus' in boolean mode)
          |group by `hour`
          |""".stripMargin.trim)
    }

    //21
    "translate a geo id set filter group by time query" in {
      val filter = Seq(stateFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select hour(t.`create_at`) as `hour`,count(*) as `count`
          |from `twitter_ds_tweet` t
          |where t.`geo_tag.stateID` in ( 37,51,24,11,10,34,42,9,44 )
          |group by `hour`
          | """.stripMargin.trim)
    }

    //22
    "translate a text contain + time + geo id set filter and group by time + spatial cube" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val group = GroupStatement(Seq(byHour, byGeoState), Seq(aggrCount))
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select hour(t.`create_at`) as `hour`,t.`geo_tag.stateID` as `state`,count(*) as `count`
          |from `twitter_ds_tweet` t
          |where match(t.`text`) against ('+zika +virus' in boolean mode) and t.`create_at` >= '2016-01-01 00:00:00.000' and t.`create_at` < '2016-12-01 00:00:00.000' and t.`geo_tag.stateID` in ( 37,51,24,11,10,34,42,9,44 )
          |group by `hour`,`state`
          | """.stripMargin.trim)
    }

    //23
    "translate a text contain + time + geo id set filter and sample tweets" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, None, Some(selectRecent))
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select t.`create_at` as `create_at`,t.`id` as `id`,t.`user.id` as `user.id`
          |from `twitter_ds_tweet` t
          |where match(t.`text`) against ('+zika +virus' in boolean mode) and t.`create_at` >= '2016-01-01 00:00:00.000' and t.`create_at` < '2016-12-01 00:00:00.000' and t.`geo_tag.stateID` in ( 37,51,24,11,10,34,42,9,44 )
          |order by t.`create_at` desc
          |limit 100
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
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select hour(t.`create_at`) as `hour`,min(t.`id`) as `min`
          |from `twitter_ds_tweet` t
          |where t.`create_at` >= '2016-01-01 00:00:00.000' and t.`create_at` < '2016-12-01 00:00:00.000'
          |group by `hour`
          | """.stripMargin.trim)
    }

    //26
    "translate a simple filter by time and group by time query sum id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrSum))
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select hour(t.`create_at`) as `hour`,sum(t.`id`) as `sum`
          |from `twitter_ds_tweet` t
          |where t.`create_at` >= '2016-01-01 00:00:00.000' and t.`create_at` < '2016-12-01 00:00:00.000'
          |group by `hour`
          |  """.stripMargin.trim)
    }

    //27
    "translate a simple filter by time and group by time query avg id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrAvg))
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select hour(t.`create_at`) as `hour`,avg(t.`id`) as `avg`
          |from `twitter_ds_tweet` t
          |where t.`create_at` >= '2016-01-01 00:00:00.000' and t.`create_at` < '2016-12-01 00:00:00.000'
          |group by `hour`
          | """.stripMargin.trim)
    }

    //28
    "translate a text contain filter and group by geocell 10th" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byGeocell10), Seq(aggrCount))
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select st_astext(Point(truncate(st_x(t.`coordinate`),1),truncate(st_y(t.`coordinate`),1)))  as `cell`,count(*) as `count`
          |from `twitter_ds_tweet` t
          |where match(t.`text`) against ('+zika +virus' in boolean mode)
          |group by `cell`
        """.stripMargin.trim)
    }

    //29
    "translate a text contain filter and group by geocell 100th" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byGeocell100), Seq(aggrCount))
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select st_astext(Point(truncate(st_x(t.`coordinate`),2),truncate(st_y(t.`coordinate`),2)))  as `cell`,count(*) as `count`
          |from `twitter_ds_tweet` t
          |where match(t.`text`) against ('+zika +virus' in boolean mode)
          |group by `cell`
        """.stripMargin.trim)
    }

    //30  //TODO: return array instead of text. e.g., return [1,2] instead of "POINT(1,2)"
    "translate a text contain filter and group by geocell 1000th" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byGeocell1000), Seq(aggrCount))
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select st_astext(Point(truncate(st_x(t.`coordinate`),3),truncate(st_y(t.`coordinate`),3)))  as `cell`,count(*) as `count`
          |from `twitter_ds_tweet` t
          |where match(t.`text`) against ('+zika +virus' in boolean mode)
          |group by `cell`
        """.stripMargin.trim)
    }

    //31
    "translate a text contain filter and group by bin" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byBin), Seq(aggrCount))
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select round(t.`geo_tag.stateID`/10)*10 as `state`,count(*) as `count`
          |from `twitter_ds_tweet` t
          |where match(t.`text`) against ('+zika +virus' in boolean mode)
          |group by `state`""".stripMargin.trim)
    }

    //32
    "translate a group by geocell without filter" in {
      val group = GroupStatement(Seq(byGeocell1000), Seq(aggrCount))
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select st_astext(Point(truncate(st_x(t.`coordinate`),3),truncate(st_y(t.`coordinate`),3)))  as `cell`,count(*) as `count`
          |from `twitter_ds_tweet` t
          |group by `cell`""".stripMargin.trim)
    }

    //33
    "translate a text contain filter and select 10" in {
      val filter = Seq(textFilter)
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, None, Some(selectTop10))
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select *
          |from `twitter_ds_tweet` t
          |where match(t.`text`) against ('+zika +virus' in boolean mode)
          |limit 10
          | """.stripMargin.trim)
    }

    //34
    "translate group by second" in {
      val group = GroupStatement(Seq(bySecond), Seq(aggrCount))
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select second(t.`create_at`) as `sec`,count(*) as `count`
          |from `twitter_ds_tweet` t
          |group by `sec`
          | """.stripMargin.trim)
    }

    //35
    "translate group by day" in {
      val group = GroupStatement(Seq(byDay), Seq(aggrCount))
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select date(t.`create_at`) as `day`,count(*) as `count`
          |from `twitter_ds_tweet` t
          |group by `day`
          | """.stripMargin.trim)
    }

    //36
    "translate group by month" in {
      val group = GroupStatement(Seq(byMonth), Seq(aggrCount))
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select month(t.`create_at`) as `month`,count(*) as `count`
          |from `twitter_ds_tweet` t
          |group by `month`
          | """.stripMargin.trim)
    }

    //37
    "translate group by year" in {
      val group = GroupStatement(Seq(byYear), Seq(aggrCount))
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select year(t.`create_at`) as `year`,count(*) as `count`
          |from `twitter_ds_tweet` t
          |group by `year`
          | """.stripMargin.trim)
    }

    //38
    "translate lookup one table with one join key" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val selectStatement = SelectStatement(Seq.empty, Seq.empty, 0, 0, Seq(AllField, population))
      val lookup = Seq(lookupPopulation)
      val filter = Seq(textFilter)
      val query = new Query(twitterDataSetForSQL, Seq.empty, lookup, filter, Seq.empty, select = Some(selectStatement))
      val result = parser.generate(query, schemaMap = Map(twitterDataSetForSQL -> twitterSchemaForSQL, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select t.`*` as `*`,l0.`population` as `population`
          |from `twitter_ds_tweet` t
          |left outer join `twitter.US_population` l0 on l0.`stateID` = t.`geo_tag.stateID`
          |where match(t.`text`) against ('+zika +virus' in boolean mode)""".stripMargin.trim
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
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq(lookup), filter, Seq.empty, select = Some(selectStatement))
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select t.`*` as `*`,l0.`population` as `population`,l0.`stateID` as `stateID`
          |from `twitter_ds_tweet` t
          |left outer join `twitter.US_population` l0 on l0.`stateID` = t.`geo_tag.stateID`
          |where match(t.`text`) against ('+zika +virus' in boolean mode)
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
      val query = new Query(twitterDataSetForSQL, Seq.empty,
        lookup = Seq(lookupPopulation, lookupLiteracy),
        filter, Seq.empty,
        select = Some(selectStatement))

      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL, populationDataSet -> populationSchema,
        literacyDataSet -> literacySchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select t.`*` as `*`,l0.`population` as `population`,l1.`literacy` as `literacy`
          |from `twitter_ds_tweet` t
          |left outer join `twitter.US_population` l0 on l0.`stateID` = t.`geo_tag.stateID`
          |left outer join `twitter.US_literacy` l1 on l1.`stateID` = t.`geo_tag.stateID`
          |where match(t.`text`) against ('+zika +virus' in boolean mode)
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
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group))
      val result = parser.generate(query, schemaMap = Map(twitterDataSetForSQL -> twitterSchemaForSQL, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select tt.`state` as `state`,tt.`count` as `count`,ll0.`population` as `population`
          |from (
          |select t.`geo_tag.stateID` as `state`,count(*) as `count`
          |from `twitter_ds_tweet` t
          |where match(t.`text`) against ('+zika +virus' in boolean mode)
          |group by `state`
          |) tt
          |left outer join `twitter.US_population` ll0 on ll0.`stateID` = tt.`state`
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
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), select = Some(selectStatement))
      val result = parser.generate(query, schemaMap = Map(twitterDataSetForSQL -> twitterSchemaForSQL, populationDataSet -> populationSchema, literacyDataSet -> literacySchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select tt.`state` as `state`,tt.`count` as `count`,ll0.`population` as `population`,ll1.`literacy` as `literacy`
          |from (
          |select t.`geo_tag.stateID` as `state`,count(*) as `count`
          |from `twitter_ds_tweet` t
          |where match(t.`text`) against ('+zika +virus' in boolean mode)
          |group by `state`
          |) tt
          |left outer join `twitter.US_population` ll0 on ll0.`stateID` = tt.`state`
          |left outer join `twitter.US_literacy` ll1 on ll1.`stateID` = tt.`state`
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
      val query = new Query(twitterDataSetForSQL, Seq.empty, lookup, filter, Seq.empty, Some(group), select = Some(selectStatement))
      val result = parser.generate(query, schemaMap = Map(twitterDataSetForSQL -> twitterSchemaForSQL, populationDataSet -> populationSchema, literacyDataSet -> literacySchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select tt.`state` as `state`,tt.`min` as `min`,ll0.`literacy` as `literacy`
          |from (
          |select t.`geo_tag.stateID` as `state`,min(l0.`population`) as `min`
          |from `twitter_ds_tweet` t
          |left outer join `twitter.US_population` l0 on l0.`stateID` = t.`geo_tag.stateID`
          |where match(t.`text`) against ('+zika +virus' in boolean mode)
          |group by `state`
          |) tt
          |left outer join `twitter.US_literacy` ll0 on ll0.`stateID` = tt.`state`
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
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectStatement), Some(GlobalAggregateStatement(aggrPopulationMin)))
      val result = parser.generate(query, schemaMap = Map(twitterDataSetForSQL -> twitterSchemaForSQL, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select min(t.`population`) as `min` from
          |(select tt.`state` as `state`,ll0.`population` as `population`
          |from (
          |select t.`geo_tag.stateID` as `state`
          |from `twitter_ds_tweet` t
          |where match(t.`text`) against ('+zika +virus' in boolean mode)
          |group by `state`
          |) tt
          |left outer join `twitter.US_population` ll0 on ll0.`stateID` = tt.`state`) t
          |""".stripMargin.trim
      )
    }

    //46
    "translate append with lookup inside group by state and sum" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byGeoState), Seq(aggrAvgLangLen), Seq(lookupPopulationByState))
      val query = new Query(twitterDataSetForSQL, Seq(appendLangLen), Seq.empty, filter, Seq.empty, Some(group))
      val result = parser.generate(query, schemaMap = Map(twitterDataSetForSQL -> twitterSchemaForSQL, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select tt.`state` as `state`,tt.`avgLangLen` as `avgLangLen`,ll0.`population` as `population`
          |from (
          |select t.`geo_tag.stateID` as `state`,avg(t.`lang_len`) as `avgLangLen`
          |from (select length(lang) as `lang_len`,t.`*` as `*`
          |from `twitter_ds_tweet` t) t
          |where match(t.`text`) against ('+zika +virus' in boolean mode)
          |group by `state`
          |) tt
          |left outer join `twitter.US_population` ll0 on ll0.`stateID` = tt.`state`
          |""".stripMargin.trim
      )
    }

    //47
    "translate a simple filter with is null" in {
      val filter = Seq(isNullFilter)
      val select = Option(selectSimple)
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, None, select)
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select t.`create_at` as `create_at`,t.`id` as `id`,t.`user.id` as `user.id`
          |from `twitter_ds_tweet` t
          |where t.`text` is null
          |""".stripMargin.trim)
    }

    //48
    "translate a simple filter with is not null" in {
      val filter = Seq(isNotNullFilter)
      val select = Option(selectSimple)
      val query = new Query(twitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, None, select)
      val result = parser.generate(query, Map(twitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select t.`create_at` as `create_at`,t.`id` as `id`,t.`user.id` as `user.id`
          |from `twitter_ds_tweet` t
          |where t.`text` is not null
          |""".stripMargin.trim)
    }

  }

  "MySQLGenerator calcResultSchema" should {
    "return the input schema if the query is subset filter only" in {
      val schema = parser.calcResultSchema(zikaCreateQueryForSQL, TwitterDataStoreWithoutHashTag.TwitterSchemaForSQL)
      schema must_== TwitterDataStoreWithoutHashTag.TwitterSchemaForSQL
    }
    "return the aggregated schema for aggregation queries" in {
      ok
    }
  }

  "MySQLGenerator createView" should {
    "generate the ddl for the twitter dataset" in {
      val ddl = parser.generate(CreateView("zika", zikaCreateQueryForSQL), Map(twitterDataSetForSQL -> TwitterDataStoreWithoutHashTag.TwitterSchemaForSQL))
      removeEmptyLine(ddl) must_== unifyNewLine(
        """
          |create table if not exists `zika` (
          |  `place.full_name` varchar(255) default null,
          |  `place.bounding_box` varchar(255) default null,
          |  `place.country_code` varchar(255) default null,
          |  `user.friends_count` bigint not null,
          |  `user.description` text not null,
          |  `favorite_count` bigint not null,
          |  `geo_tag.countyID` bigint default null,
          |  `user.location` varchar(255) not null,
          |  `place.type` varchar(255) default null,
          |  `geo_tag.cityName` varchar(255) default null,
          |  `user.id` bigint not null,
          |  `geo_tag.stateName` varchar(255) default null,
          |  `geo_tag.cityID` bigint default null,
          |  `is_retweet` bigint not null,
          |  `text` text not null,
          |  `user.screen_name` varchar(255) not null,
          |  `retweet_count` bigint not null,
          |  `place.country` varchar(255) default null,
          |  `in_reply_to_user` bigint not null,
          |  `user.statues_count` varchar(255) not null,
          |  `id` bigint not null,
          |  `coordinate` point default null,
          |  `place.id` varchar(255) default null,
          |  `in_reply_to_status` bigint not null,
          |  `geo_tag.stateID` bigint default null,
          |  `create_at` datetime not null,
          |  `user.create_at` datetime not null,
          |  `place.name` varchar(255) default null,
          |  `lang` varchar(255) not null,
          |  `user.lang` varchar(255) not null,
          |  `user.name` varchar(255) not null,
          |  `geo_tag.countyName` varchar(255) default null, primary key (`id`)
          |);
          |replace into `zika`
          |(
          |select *
          |from `twitter_ds_tweet` t
          |where match(t.`text`) against ('+zika' in boolean mode)
          |)""".stripMargin.trim)
    }
  }

  "MySQLGenerator deleteRecord" should {
    "generate the delete query " in {
      val sql = parser.generate(DeleteRecord(twitterDataSetForSQL, Seq(timeFilter)), Map(twitterDataSetForSQL -> TwitterDataStoreWithoutHashTag.TwitterSchemaForSQL))
      removeEmptyLine(sql) must_== unifyNewLine(
        """
          |delete from `twitter_ds_tweet` t
          |where t.`create_at` >= '2016-01-01 00:00:00.000' and t.`create_at` < '2016-12-01 00:00:00.000'
          |""".stripMargin.trim)
    }
  }

  "MySQLGenerator dropView" should {
    "generate the drop view query" in {
      val sql = parser.generate(DropView(twitterDataSetForSQL), Map(twitterDataSetForSQL -> TwitterDataStoreWithoutHashTag.TwitterSchemaForSQL))
      removeEmptyLine(sql) must_== unifyNewLine(
        """
          |drop table if exists `twitter_ds_tweet`
          |""".stripMargin.trim)
    }
  }

}
