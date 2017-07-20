package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema._
import org.specs2.mutable.Specification


class SparkSQLGeneratorTest extends Specification {

  import TestQuery._

  val parser = new SparkSQLGenerator

  "SparkSQLGenerator generate" should {
    //1
    "translate a simple query group by hour" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select `hour`(t.`create_at`) as `hour`,count(*) as `count`
           |from twitter_ds_tweet t
           |where t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z'
           |group by `hour`(t.`create_at`)
           |""".stripMargin.trim)
    }

    //2
    "translate a simple query group by day" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byDay), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select `day`(t.`create_at`) as `day`,count(*) as `count`
           |from twitter_ds_tweet t
           |where t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z'
           |group by `day`(t.`create_at`)
           |""".stripMargin.trim)
    }
    //3
    "translate a simple query group by month" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byMonth), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select `month`(t.`create_at`) as `month`,count(*) as `count`
           |from twitter_ds_tweet t
           |where t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z'
           |group by `month`(t.`create_at`)
           |""".stripMargin.trim)
    }

    //4
    "translate a geo id set filter group by time query" in {
      val filter = Seq(timeFilter, textFilter, stateFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select `hour`(t.`create_at`) as `hour`,count(*) as `count`
           |from twitter_ds_tweet t
           |where t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z' and lower(t.`text`) like '%zika%' and lower(t.`text`) like '%virus%' and t.`geo_tag`.`stateID` in ( 37,51,24,11,10,34,42,9,44 )
           |group by `hour`(t.`create_at`)
           |""".stripMargin.trim)
    }

    //5
    "translate a simple select query order by time with limit 100" in {
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, Seq.empty, Seq.empty, None, Some(selectAllOrderByTimeDesc))
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select *
           |from twitter_ds_tweet t
           |order by t.`create_at` desc
           |limit 100
           |""".stripMargin.trim)
    }

    //6
    "translate a simple unnest query" in {
      val filter = Seq(timeFilter, textFilter, stateFilter)
      val group = GroupStatement(Seq(byHashTag), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, filter, Seq(unnestHashTag), Some(group), Some(selectTop10byHashTag))
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select `hashtag` as `hashtag`,count(*) as `count`
           |from twitter_ds_tweet t
           |lateral view explode(t.`hashtags`) tab as `hashtag`
           |where t.`hashtags` is not null and t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z' and lower(t.`text`) like '%zika%' and lower(t.`text`) like '%virus%' and t.`geo_tag`.`stateID` in ( 37,51,24,11,10,34,42,9,44 )
           |group by `hashtag`
           |order by `count` desc
           |limit 10""".stripMargin.trim)
    }

    //7
    "translate a count cardinality query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = TwitterDataSetForSparkSQL, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select count(*) as `count` from
          |(select *
          |from twitter_ds_tweet t)""".stripMargin)
    }
    //8
    "translate get min field value query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrMin)
      val query = new Query(dataset = TwitterDataSetForSparkSQL, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select min(t.`id`) as `min` from
          |(select *
          |from twitter_ds_tweet t)""".stripMargin)
    }
    //9
    "translate get max field value query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrMax)
      val query = new Query(dataset = TwitterDataSetForSparkSQL, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select max(t.`id`) as `max` from
          |(select *
          |from twitter_ds_tweet t)""".stripMargin)
    }
    //10
    "translate a count cardinality query with filter without group by" in {
      val filter = Seq(timeFilter)
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = TwitterDataSetForSparkSQL, filter = filter, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select count(*) as `count` from
          |(select *
          |from twitter_ds_tweet t
          |where t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z')""".stripMargin)
    }
    //11
    "translate a min cardinality query with filter without group by" in {
      val filter = Seq(timeFilter)
      val globalAggr = GlobalAggregateStatement(aggrMin)
      val query = new Query(dataset = TwitterDataSetForSparkSQL, filter = filter, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select min(t.`id`) as `min` from
          |(select *
          |from twitter_ds_tweet t
          |where t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z')""".stripMargin)
    }
    //12
    "translate a count cardinality query with select" in {
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = TwitterDataSetForSparkSQL, select = Some(selectTop10), globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select count(*) as `count` from
          |(select *
          |from twitter_ds_tweet t
          |limit 10)""".stripMargin.trim)
    }

    //13
    "translate group by minute" in {
      val group = GroupStatement(Seq(byMinuteForSparkSql), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `minute`(t.`create_at`) as `minute`,count(*) as `count`
          |from twitter_ds_tweet t
          |group by `minute`(t.`create_at`)""".stripMargin.trim)
    }


    //14
    "translate a simple filter by time and group by time query max id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrMax))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `hour`(t.`create_at`) as `hour`,max(t.`id`) as `max`
          |from twitter_ds_tweet t
          |where t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z'
          |group by `hour`(t.`create_at`)""".stripMargin.trim)
    }

    //15
    "translate a max cardinality query with unnest with group by with select" in {

      val filter = Seq(textFilter, timeFilter, stateFilter)
      val globalAggr = GlobalAggregateStatement(aggrMaxGroupBy)
      val group = GroupStatement(Seq(byHashTag), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, filter, Seq(unnestHashTag), Some(group), Some(selectTop10Tag), Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select max(`count`) as `max` from
           |(select `hashtag` as `hashtag`,count(*) as `count`
           |from twitter_ds_tweet t
           |lateral view explode(t.`hashtags`) tab as `hashtag`
           |where t.`hashtags` is not null and lower(t.`text`) like '%zika%' and lower(t.`text`) like '%virus%' and t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z' and t.`geo_tag`.`stateID` in ( 37,51,24,11,10,34,42,9,44 )
           |group by `hashtag`
           |order by `count` desc
           |limit 10)""".stripMargin.trim)
    }

    //16
    "translate group by query having lookup with one join key" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val selectValues = Seq(population)
      val group = Some(groupPopulationSum)
      val lookup = LookupStatement(
        sourceKeys = Seq(geoStateID),
        dataset = populationDataSet,
        lookupKeys = Seq(stateID),
        selectValues,
        as = selectValues)
      val filter = Seq(textFilter)
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq(lookup), filter, Seq.empty, group)
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select t.geo_tag.stateID as `state`,sum(l0.`population`) as `sum`
          |from twitter_ds_tweet t
          |left outer join twitter.US_population l0 on l0.`stateID` = t.`geo_tag`.`stateID`
          |where lower(t.`text`) like '%zika%' and lower(t.`text`) like '%virus%'
          |group by t.geo_tag.stateID""".stripMargin.trim
      )
    }

    //17
    "translate a simple filter by time and group by time query" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `hour`(t.`create_at`) as `hour`,count(*) as `count`
          |from twitter_ds_tweet t
          |where t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z'
          |group by `hour`(t.`create_at`)""".stripMargin.trim)
    }

    //18
    "translate a simple filter with string not match" in {
      val filter = Seq(langNotMatchFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `hour`(t.`create_at`) as `hour`,count(*) as `count`
          |from twitter_ds_tweet t
          |where t.`lang`!="en"
          |group by `hour`(t.`create_at`)
          |""".stripMargin.trim)
    }
    //19
    "translate a simple filter with string matches" in {
      val filter = Seq(langMatchFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `hour`(t.`create_at`) as `hour`,count(*) as `count`
          |from twitter_ds_tweet t
          |where t.`lang`="en"
          |group by `hour`(t.`create_at`)""".stripMargin.trim)
    }
    //20
    "translate a text contain filter and group by time query" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `hour`(t.`create_at`) as `hour`,count(*) as `count`
          |from twitter_ds_tweet t
          |where lower(t.`text`) like '%zika%' and lower(t.`text`) like '%virus%'
          |group by `hour`(t.`create_at`)
          |""".stripMargin.trim)
    }
    //21
    "translate a geo id set filter group by time query" in {
      val filter = Seq(stateFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `hour`(t.`create_at`) as `hour`,count(*) as `count`
          |from twitter_ds_tweet t
          |where t.`geo_tag`.`stateID` in ( 37,51,24,11,10,34,42,9,44 )
          |group by `hour`(t.`create_at`)
          | """.stripMargin.trim)
    }

    //    22
    "translate a text contain + time + geo id set filter and group by time + spatial cube" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val group = GroupStatement(Seq(byHour, byState), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `hour`(t.`create_at`) as `hour`,t.geo_tag.stateID as `state`,count(*) as `count`
          |from twitter_ds_tweet t
          |where lower(t.`text`) like '%zika%' and lower(t.`text`) like '%virus%' and t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z' and t.`geo_tag`.`stateID` in ( 37,51,24,11,10,34,42,9,44 )
          |group by `hour`(t.`create_at`),t.geo_tag.stateID
          | """.stripMargin.trim)
    }
    //    23
    "translate a text contain + time + geo id set filter and sample tweets" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, filter, Seq.empty, None, Some(selectRecent))
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select t.`favorite_count` as `favorite_count`,t.`geo_tag`.`countyID` as `geo_tag.countyID`,t.`user_mentions` as `user_mentions`,t.`user`.`id` as `user.id`,t.`geo_tag`.`cityID` as `geo_tag.cityID`,t.`is_retweet` as `is_retweet`,t.`text` as `text`,t.`retweet_count` as `retweet_count`,t.`in_reply_to_user` as `in_reply_to_user`,t.`id` as `id`,t.`coordinate` as `coordinate`,t.`in_reply_to_status` as `in_reply_to_status`,t.`user`.`status_count` as `user.status_count`,t.`geo_tag`.`stateID` as `geo_tag.stateID`,t.`create_at` as `create_at`,t.`lang` as `lang`,t.`hashtags` as `hashtags`
          |from twitter_ds_tweet t
          |where lower(t.`text`) like '%zika%' and lower(t.`text`) like '%virus%' and t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z' and t.`geo_tag`.`stateID` in ( 37,51,24,11,10,34,42,9,44 )
          |order by t.`create_at` desc
          |limit 100
          | """.stripMargin.trim)
    }
    //24
    "translate a text contain + time + geo id set filter and group by hashtags" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val group = GroupStatement(Seq(byTag), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, filter, Seq(unnestHashTag), Some(group), Some(selectTop10Tag))
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `hashtag` as `hashtag`,count(*) as `count`
          |from twitter_ds_tweet t
          |lateral view explode(t.`hashtags`) tab as `hashtag`
          |where t.`hashtags` is not null and lower(t.`text`) like '%zika%' and lower(t.`text`) like '%virus%' and t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z' and t.`geo_tag`.`stateID` in ( 37,51,24,11,10,34,42,9,44 )
          |group by `hashtag`
          |order by `count` desc
          |limit 10
          | """.stripMargin.trim)
    }
    //    25
    "translate a simple filter by time and group by time query min id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrMin))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `hour`(t.`create_at`) as `hour`,min(t.`id`) as `min`
          |from twitter_ds_tweet t
          |where t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z'
          |group by `hour`(t.`create_at`)
          | """.stripMargin.trim)
    }
    //    26
    "translate a simple filter by time and group by time query sum id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrSum))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `hour`(t.`create_at`) as `hour`,sum(t.`id`) as `sum`
          |from twitter_ds_tweet t
          |where t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z'
          |group by `hour`(t.`create_at`)
          |  """.stripMargin.trim)
    }
    //    27
    "translate a simple filter by time and group by time query avg id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrAvg))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `hour`(t.`create_at`) as `hour`,avg(t.`id`) as `avg`
          |from twitter_ds_tweet t
          |where t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z'
          |group by `hour`(t.`create_at`)
          | """.stripMargin.trim)
    }
    // 28
    "translate a text contain filter and group by geocell 10th" in {
      ok
    }
    //29
    "translate a text contain filter and group by geocell 100th" in {
      ok
    }
    //30
    "translate a text contain filter and group by geocell 1000th" in {
      ok
    }
    //31
    "translate a text contain filter and group by bin" in {
      ok
    }
    //32
    "translate a group by geocell without filter" in {
      ok
    }

    //    33
    "translate a text contain filter and select 10" in {
      val filter = Seq(textFilter)
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, filter, Seq.empty, None, Some(selectTop10))
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select *
          |from twitter_ds_tweet t
          |where lower(t.`text`) like '%zika%' and lower(t.`text`) like '%virus%'
          |limit 10
          | """.stripMargin.trim)
    }
    //    34
    "translate group by second" in {
      val group = GroupStatement(Seq(bySecond), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `sec`(t.`create_at`) as `sec`,count(*) as `count`
          |from twitter_ds_tweet t
          |group by `sec`(t.`create_at`)
          | """.stripMargin.trim)
    }
    //35
    "translate group by day" in {
      val group = GroupStatement(Seq(byDay), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `day`(t.`create_at`) as `day`,count(*) as `count`
          |from twitter_ds_tweet t
          |group by `day`(t.`create_at`)
          | """.stripMargin.trim)
    }

    //36
    "translate group by month" in {
      val group = GroupStatement(Seq(byMonth), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `month`(t.`create_at`) as `month`,count(*) as `count`
          |from twitter_ds_tweet t
          |group by `month`(t.`create_at`)
          | """.stripMargin.trim)
    }
    //37
    "translate group by year" in {
      val group = GroupStatement(Seq(byYear), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `year`(t.`create_at`) as `year`,count(*) as `count`
          |from twitter_ds_tweet t
          |group by `year`(t.`create_at`)
          | """.stripMargin.trim)
    }

    //38
    "translate lookup one table with one join key" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val selectStatement = SelectStatement(Seq.empty, Seq.empty, 0, 0, Seq(AllField, population))
      val lookup = Seq(lookupPopulation)
      val filter = Seq(textFilter)
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, lookup, filter, Seq.empty, select = Some(selectStatement))
      val result = parser.generate(query, schemaMap = Map(TwitterDataSetForSparkSQL -> twitterSchema, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select t.`favorite_count` as `favorite_count`,t.`geo_tag`.`countyID` as `geo_tag.countyID`,t.`user_mentions` as `user_mentions`,l0.`population` as `population`,t.`user`.`id` as `user.id`,t.`geo_tag`.`cityID` as `geo_tag.cityID`,t.`is_retweet` as `is_retweet`,t.`text` as `text`,t.`retweet_count` as `retweet_count`,t.`in_reply_to_user` as `in_reply_to_user`,t.`id` as `id`,t.`coordinate` as `coordinate`,t.`in_reply_to_status` as `in_reply_to_status`,t.`user`.`status_count` as `user.status_count`,t.`geo_tag`.`stateID` as `geo_tag.stateID`,t.`create_at` as `create_at`,t.`lang` as `lang`,t.`hashtags` as `hashtags`
          |from twitter_ds_tweet t
          |left outer join twitter.US_population l0 on l0.`stateID` = t.`geo_tag`.`stateID`
          |where lower(t.`text`) like '%zika%' and lower(t.`text`) like '%virus%'
          |""".stripMargin.trim
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
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq(lookup), filter, Seq.empty, select = Some(selectStatement))
      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select t.`favorite_count` as `favorite_count`,t.`geo_tag`.`countyID` as `geo_tag.countyID`,t.`user_mentions` as `user_mentions`,l0.`population` as `population`,l0.`stateID` as `stateID`,t.`user`.`id` as `user.id`,t.`geo_tag`.`cityID` as `geo_tag.cityID`,t.`is_retweet` as `is_retweet`,t.`text` as `text`,t.`retweet_count` as `retweet_count`,t.`in_reply_to_user` as `in_reply_to_user`,t.`id` as `id`,t.`coordinate` as `coordinate`,t.`in_reply_to_status` as `in_reply_to_status`,t.`user`.`status_count` as `user.status_count`,t.`geo_tag`.`stateID` as `geo_tag.stateID`,t.`create_at` as `create_at`,t.`lang` as `lang`,t.`hashtags` as `hashtags`
          |from twitter_ds_tweet t
          |left outer join twitter.US_population l0 on l0.`stateID` = t.`geo_tag`.`stateID`
          |where lower(t.`text`) like '%zika%' and lower(t.`text`) like '%virus%'
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
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty,
        lookup = Seq(lookupPopulation, lookupLiteracy),
        filter, Seq.empty,
        select = Some(selectStatement))

      val result = parser.generate(query, Map(TwitterDataSetForSparkSQL -> twitterSchema, populationDataSet -> populationSchema,
        literacyDataSet -> literacySchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select t.`favorite_count` as `favorite_count`,t.`geo_tag`.`countyID` as `geo_tag.countyID`,t.`user_mentions` as `user_mentions`,l0.`population` as `population`,t.`user`.`id` as `user.id`,t.`geo_tag`.`cityID` as `geo_tag.cityID`,t.`is_retweet` as `is_retweet`,t.`text` as `text`,t.`retweet_count` as `retweet_count`,l1.`literacy` as `literacy`,t.`in_reply_to_user` as `in_reply_to_user`,t.`id` as `id`,t.`coordinate` as `coordinate`,t.`in_reply_to_status` as `in_reply_to_status`,t.`user`.`status_count` as `user.status_count`,t.`geo_tag`.`stateID` as `geo_tag.stateID`,t.`create_at` as `create_at`,t.`lang` as `lang`,t.`hashtags` as `hashtags`
          |from twitter_ds_tweet t
          |left outer join twitter.US_population l0 on l0.`stateID` = t.`geo_tag`.`stateID`
          |left outer join twitter.US_literacy l1 on l1.`stateID` = t.`geo_tag`.`stateID`
          |where lower(t.`text`) like '%zika%' and lower(t.`text`) like '%virus%'
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
      val group = GroupStatement(Seq(byState), Seq(aggrCount), Seq(lookupPopulationByState))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group))
      val result = parser.generate(query, schemaMap = Map(TwitterDataSetForSparkSQL -> twitterSchema, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select tt.`state` as `state`,tt.`count` as `count`,ll0.`population` as `population`
          |from (
          |select t.geo_tag.stateID as `state`,count(*) as `count`
          |from twitter_ds_tweet t
          |where lower(t.`text`) like '%zika%' and lower(t.`text`) like '%virus%'
          |group by t.geo_tag.stateID
          |) tt
          |left outer join twitter.US_population ll0 on ll0.`stateID` = tt.`state`""".stripMargin.trim
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
      val group = GroupStatement(Seq(byState), Seq(aggrCount), Seq(lookupPopulationByState, lookupLiteracyByState))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), select = Some(selectStatement))
      val result = parser.generate(query, schemaMap = Map(TwitterDataSetForSparkSQL -> twitterSchema, populationDataSet -> populationSchema, literacyDataSet -> literacySchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select tt.`state` as `state`,tt.`count` as `count`,ll0.`population` as `population`,ll1.`literacy` as `literacy`
          |from (
          |select t.geo_tag.stateID as `state`,count(*) as `count`
          |from twitter_ds_tweet t
          |where lower(t.`text`) like '%zika%' and lower(t.`text`) like '%virus%'
          |group by t.geo_tag.stateID
          |) tt
          |left outer join twitter.US_population ll0 on ll0.`stateID` = tt.`state`
          |left outer join twitter.US_literacy ll1 on ll1.`stateID` = tt.`state`
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
      val group = GroupStatement(Seq(byState), Seq(aggrPopulationMin), Seq(lookupLiteracyByState))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, lookup, filter, Seq.empty, Some(group), select = Some(selectStatement))
      val result = parser.generate(query, schemaMap = Map(TwitterDataSetForSparkSQL -> twitterSchema, populationDataSet -> populationSchema, literacyDataSet -> literacySchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select tt.`state` as `state`,tt.`min` as `min`,ll0.`literacy` as `literacy`
          |from (
          |select t.geo_tag.stateID as `state`,min(l0.`population`) as `min`
          |from twitter_ds_tweet t
          |left outer join twitter.US_population l0 on l0.`stateID` = t.`geo_tag`.`stateID`
          |where lower(t.`text`) like '%zika%' and lower(t.`text`) like '%virus%'
          |group by t.geo_tag.stateID
          |) tt
          |left outer join twitter.US_literacy ll0 on ll0.`stateID` = tt.`state`
          |""".stripMargin.trim
      )
    }
    //45
    "translate lookup inside group by state with global aggregate" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val selectStatement = SelectStatement(Seq.empty, Seq.empty, 0, 0, Seq(AllField, population))
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byState), Seq.empty, Seq(lookupPopulationByState))
      val query = new Query(TwitterDataSetForSparkSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectStatement), Some(GlobalAggregateStatement(aggrPopulationMin)))
      val result = parser.generate(query, schemaMap = Map(TwitterDataSetForSparkSQL -> twitterSchema, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select min(t.`population`) as `min` from
          |(select tt.`state` as `state`,ll0.`population` as `population`
          |from (
          |select t.geo_tag.stateID as `state`
          |from twitter_ds_tweet t
          |where lower(t.`text`) like '%zika%' and lower(t.`text`) like '%virus%'
          |group by t.geo_tag.stateID
          |) tt
          |left outer join twitter.US_population ll0 on ll0.`stateID` = tt.`state`)
          |""".
          stripMargin.trim
      )
    }

    //46
    "translate append with lookup inside group by state and sum" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byState), Seq(aggrAvgLangLen), Seq(lookupPopulationByState))
      val query = new Query(TwitterDataSetForSparkSQL, Seq(appendLangLen), Seq.empty, filter, Seq.empty, Some(group))
      val result = parser.generate(query, schemaMap = Map(TwitterDataSetForSparkSQL -> twitterSchema, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select tt.`state` as `state`,tt.`avgLangLen` as `avgLangLen`,ll0.`population` as `population`
          |from (
          |select t.geo_tag.stateID as `state`,avg(ta.lang_len) as `avgLangLen`
          |from (select length(lang) as `lang_len`,t.`favorite_count` as `favorite_count`,t.`geo_tag`.`countyID` as `geo_tag.countyID`,t.`user_mentions` as `user_mentions`,t as `geo`,t.`user`.`id` as `user.id`,t.`geo_tag`.`cityID` as `geo_tag.cityID`,t.`is_retweet` as `is_retweet`,t.`text` as `text`,t.`retweet_count` as `retweet_count`,t.`in_reply_to_user` as `in_reply_to_user`,t.`id` as `id`,t.`coordinate` as `coordinate`,t.`in_reply_to_status` as `in_reply_to_status`,t.`user`.`status_count` as `user.status_count`,t.`geo_tag`.`stateID` as `geo_tag.stateID`,t.`create_at` as `create_at`,t.`lang` as `lang`,t.`hashtags` as `hashtags`
          |from twitter_ds_tweet t) ta
          |where lower(ta.text) like '%zika%' and lower(ta.text) like '%virus%'
          |group by ta.geo.geo_tag.stateID
          |) tt
          |left outer join twitter.US_population ll0 on ll0.`stateID` = tt.`state`
          |""".stripMargin.trim
      )
    }

  }
  
  "SparkSQLGenerator calcResultSchema" should {
    "return the input schema if the query is subset filter only" in {
      val schema = parser.calcResultSchema(zikaCreateQuery, TwitterDataStore.TwitterSchema)
      schema must_== TwitterDataStore.TwitterSchema
    }
    "return the aggregated schema for aggregation queries" in {
      ok
    }
  }


  "SparkSQLGenerator deleteRecord" should {
    "generate the delete query " in {
      val sql = parser.generate(DeleteRecord(TwitterDataSetForSparkSQL, Seq(timeFilter)), Map(TwitterDataSetForSparkSQL -> TwitterDataStore.TwitterSchema))
      removeEmptyLine(sql) must_== unifyNewLine(
        """
          |delete from twitter_ds_tweet t
          |where t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z'
          |""".stripMargin.trim)
    }
  }

  "SparkSQLGenerator dropView" should {
    "generate the drop view query" in {
      val sql = parser.generate(DropView(TwitterDataSetForSparkSQL), Map(TwitterDataSetForSparkSQL -> TwitterDataStore.TwitterSchema))
      removeEmptyLine(sql) must_== unifyNewLine(
        """
          |drop dataset twitter_ds_tweet if exists
          |""".stripMargin.trim)
    }
  }

}
