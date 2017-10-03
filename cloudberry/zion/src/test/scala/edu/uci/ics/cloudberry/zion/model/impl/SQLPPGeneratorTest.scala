package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema._
import org.specs2.mutable.Specification


class SQLPPGeneratorTest extends Specification {

  import TestQuery._

  val parser = new SQLPPGenerator

  "SQLPPGenerator generate" should {

    "translate a simple unnest query" in {
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq(unnestHashTag), None, Some(selectTop10))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select t.`favorite_count` as `favorite_count`,t.`geo_tag`.`countyID` as `geo_tag.countyID`,t.`user_mentions` as `user_mentions`,`unnest0` as `tag`,t.`user`.`id` as `user.id`,t.`geo_tag`.`cityID` as `geo_tag.cityID`,t.`is_retweet` as `is_retweet`,t.`text` as `text`,t.`retweet_count` as `retweet_count`,t.`in_reply_to_user` as `in_reply_to_user`,t.`id` as `id`,t.`coordinate` as `coordinate`,t.`in_reply_to_status` as `in_reply_to_status`,t.`user`.`status_count` as `user.status_count`,t.`geo_tag`.`stateID` as `geo_tag.stateID`,t.`create_at` as `create_at`,t.`lang` as `lang`,t.`hashtags` as `hashtags`
          |from twitter.ds_tweet t
          |unnest t.`hashtags` `unnest0`
          |where not(is_null(t.`hashtags`))
          |limit 10
          |offset 0;""".stripMargin.trim)
    }

    "translate a simple filter by time and group by time query" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `hour` as `hour`,coll_count(g) as `count`
          |from twitter.ds_tweet t
          |where t.`create_at` >= datetime('2016-01-01T00:00:00.000Z') and t.`create_at` < datetime('2016-12-01T00:00:00.000Z')
          |group by get_interval_start_datetime(interval_bin(t.`create_at`, datetime('1990-01-01T00:00:00.000Z'),  day_time_duration("PT1H") )) as `hour` group as g;
          | """.stripMargin.trim)
    }


    "translate a simple filter with string not match" in {
      val filter = Seq(langNotMatchFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `hour` as `hour`,coll_count(g) as `count`
          |from twitter.ds_tweet t
          |where t.`lang`!="en"
          |group by get_interval_start_datetime(interval_bin(t.`create_at`, datetime('1990-01-01T00:00:00.000Z'),  day_time_duration("PT1H") )) as `hour` group as g;
          | """.stripMargin.trim)
    }


    "translate a simple filter with string matches" in {
      val filter = Seq(langMatchFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `hour` as `hour`,coll_count(g) as `count`
          |from twitter.ds_tweet t
          |where t.`lang`="en"
          |group by get_interval_start_datetime(interval_bin(t.`create_at`, datetime('1990-01-01T00:00:00.000Z'),  day_time_duration("PT1H") )) as `hour` group as g;
          | """.stripMargin.trim)
    }

    "translate a text contain filter and group by time query" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `hour` as `hour`,coll_count(g) as `count`
          |from twitter.ds_tweet t
          |where ftcontains(t.`text`, ['zika','virus'], {'mode':'all'})
          |group by get_interval_start_datetime(interval_bin(t.`create_at`, datetime('1990-01-01T00:00:00.000Z'),  day_time_duration("PT1H") )) as `hour` group as g;
          | """.stripMargin.trim)
    }

    "translate a geo id set filter group by time query" in {
      val filter = Seq(stateFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `hour` as `hour`,coll_count(g) as `count`
          |from twitter.ds_tweet t
          |where t.`geo_tag`.`stateID` in [ 37,51,24,11,10,34,42,9,44 ]
          |group by get_interval_start_datetime(interval_bin(t.`create_at`, datetime('1990-01-01T00:00:00.000Z'),  day_time_duration("PT1H") )) as `hour` group as g;
          | """.stripMargin.trim)
    }

    "translate a text contain + time + geo id set filter and group by time + spatial cube" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val group = GroupStatement(Seq(byHour, byState), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `hour` as `hour`,`state` as `state`,coll_count(g) as `count`
          |from twitter.ds_tweet t
          |where ftcontains(t.`text`, ['zika','virus'], {'mode':'all'}) and t.`create_at` >= datetime('2016-01-01T00:00:00.000Z') and t.`create_at` < datetime('2016-12-01T00:00:00.000Z') and t.`geo_tag`.`stateID` in [ 37,51,24,11,10,34,42,9,44 ]
          |group by get_interval_start_datetime(interval_bin(t.`create_at`, datetime('1990-01-01T00:00:00.000Z'),  day_time_duration("PT1H") )) as `hour`,t.geo_tag.stateID as `state` group as g;
          | """.stripMargin.trim)
    }

    "translate a text contain + time + geo id set filter and sample tweets" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, None, Some(selectRecent))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select t.`create_at` as `create_at`,t.`id` as `id`,t.`user`.`id` as `user.id`
          |from twitter.ds_tweet t
          |where ftcontains(t.`text`, ['zika','virus'], {'mode':'all'}) and t.`create_at` >= datetime('2016-01-01T00:00:00.000Z') and t.`create_at` < datetime('2016-12-01T00:00:00.000Z') and t.`geo_tag`.`stateID` in [ 37,51,24,11,10,34,42,9,44 ]
          |order by t.`create_at` desc
          |limit 100
          |offset 0;
          | """.stripMargin.trim)
    }

    "translate a text contain + time + geo id set filter and group by hashtags" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val group = GroupStatement(Seq(byTag), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq(unnestHashTag), Some(group), Some(selectTop10Tag))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `tag` as `tag`,coll_count(g) as `count`
          |from twitter.ds_tweet t
          |unnest t.`hashtags` `unnest0`
          |where not(is_null(t.`hashtags`)) and ftcontains(t.`text`, ['zika','virus'], {'mode':'all'}) and t.`create_at` >= datetime('2016-01-01T00:00:00.000Z') and t.`create_at` < datetime('2016-12-01T00:00:00.000Z') and t.`geo_tag`.`stateID` in [ 37,51,24,11,10,34,42,9,44 ]
          |group by `unnest0` as `tag` group as g
          |order by `count` desc
          |limit 10
          |offset 0;
          | """.stripMargin.trim)
    }

    "translate a simple filter by time and group by time query max id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrMax))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `hour` as `hour`,coll_max( (select value g.t.`id` from g) ) as `max`
          |from twitter.ds_tweet t
          |where t.`create_at` >= datetime('2016-01-01T00:00:00.000Z') and t.`create_at` < datetime('2016-12-01T00:00:00.000Z')
          |group by get_interval_start_datetime(interval_bin(t.`create_at`, datetime('1990-01-01T00:00:00.000Z'),  day_time_duration("PT1H") )) as `hour` group as g;
          | """.stripMargin.trim)
    }

    "translate a simple filter by time and group by time query min id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrMin))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `hour` as `hour`,coll_min( (select value g.t.`id` from g) ) as `min`
          |from twitter.ds_tweet t
          |where t.`create_at` >= datetime('2016-01-01T00:00:00.000Z') and t.`create_at` < datetime('2016-12-01T00:00:00.000Z')
          |group by get_interval_start_datetime(interval_bin(t.`create_at`, datetime('1990-01-01T00:00:00.000Z'),  day_time_duration("PT1H") )) as `hour` group as g;
          | """.stripMargin.trim)
    }

    "translate a simple filter by time and group by time query sum id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrSum))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `hour` as `hour`,coll_sum( (select value g.t.`id` from g) ) as `sum`
          |from twitter.ds_tweet t
          |where t.`create_at` >= datetime('2016-01-01T00:00:00.000Z') and t.`create_at` < datetime('2016-12-01T00:00:00.000Z')
          |group by get_interval_start_datetime(interval_bin(t.`create_at`, datetime('1990-01-01T00:00:00.000Z'),  day_time_duration("PT1H") )) as `hour` group as g;
          |  """.stripMargin.trim)
    }

    "translate a simple filter by time and group by time query avg id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrAvg))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `hour` as `hour`,coll_avg( (select value g.t.`id` from g) ) as `avg`
          |from twitter.ds_tweet t
          |where t.`create_at` >= datetime('2016-01-01T00:00:00.000Z') and t.`create_at` < datetime('2016-12-01T00:00:00.000Z')
          |group by get_interval_start_datetime(interval_bin(t.`create_at`, datetime('1990-01-01T00:00:00.000Z'),  day_time_duration("PT1H") )) as `hour` group as g;
          | """.stripMargin.trim)
    }

    "translate a text contain filter and group by geocell 10th" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byGeocell10), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `cell` as `cell`,coll_count(g) as `count`
          |from twitter.ds_tweet t
          |where ftcontains(t.`text`, ['zika','virus'], {'mode':'all'})
          |group by get_points(spatial_cell(t.`coordinate`, create_point(0.0,0.0), 0.1, 0.1))[0] as `cell` group as g;
        """.stripMargin.trim)
    }

    "translate a text contain filter and group by geocell 100th" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byGeocell100), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `cell` as `cell`,coll_count(g) as `count`
          |from twitter.ds_tweet t
          |where ftcontains(t.`text`, ['zika','virus'], {'mode':'all'})
          |group by get_points(spatial_cell(t.`coordinate`, create_point(0.0,0.0), 0.01, 0.01))[0] as `cell` group as g;
        """.
          stripMargin.trim)
    }

    "translate a text contain filter and group by geocell 1000th" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byGeocell1000), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `cell` as `cell`,coll_count(g) as `count`
          |from twitter.ds_tweet t
          |where ftcontains(t.`text`, ['zika','virus'], {'mode':'all'})
          |group by get_points(spatial_cell(t.`coordinate`, create_point(0.0,0.0), 0.001, 0.001))[0] as `cell` group as g;
        """.
          stripMargin.trim)
    }

    "translate a text contain filter and group by bin" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byBin), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `state` as `state`,coll_count(g) as `count`
          |from twitter.ds_tweet t
          |where ftcontains(t.`text`, ['zika','virus'], {'mode':'all'})
          |group by round(t.`geo_tag`.`stateID`/10)*10 as `state` group as g;
          | """.stripMargin.trim)
    }

    "translate a group by geocell without filter" in {
      val group = GroupStatement(Seq(byGeocell1000), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `cell` as `cell`,coll_count(g) as `count`
          |from twitter.ds_tweet t
          |group by get_points(spatial_cell(t.`coordinate`, create_point(0.0,0.0), 0.001, 0.001))[0] as `cell` group as g;
        """.
          stripMargin.trim)
    }

    "translate a text contain filter and select 10" in {
      val filter = Seq(textFilter)
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, None, Some(selectTop10))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select value t
          |from twitter.ds_tweet t
          |where ftcontains(t.`text`, ['zika','virus'], {'mode':'all'})
          |limit 10
          |offset 0;
          | """.stripMargin.trim)
    }
    "translate group by second" in {
      val group = GroupStatement(Seq(bySecond), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `sec` as `sec`,coll_count(g) as `count`
          |from twitter.ds_tweet t
          |group by get_interval_start_datetime(interval_bin(t.`create_at`, datetime('1990-01-01T00:00:00.000Z'),  day_time_duration("PT1S") )) as `sec` group as g;
          | """.stripMargin.trim)
    }
    "translate group by minute" in {
      val group = GroupStatement(Seq(byMinute), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `min` as `min`,coll_count(g) as `count`
          |from twitter.ds_tweet t
          |group by get_interval_start_datetime(interval_bin(t.`create_at`, datetime('1990-01-01T00:00:00.000Z'),  day_time_duration("PT1M") )) as `min` group as g;
          | """.stripMargin.trim)
    }

    "translate group by day" in {
      val group = GroupStatement(Seq(byDay), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `day` as `day`,coll_count(g) as `count`
          |from twitter.ds_tweet t
          |group by get_interval_start_datetime(interval_bin(t.`create_at`, datetime('1990-01-01T00:00:00.000Z'),  day_time_duration("P1D") )) as `day` group as g;
          | """.stripMargin.trim)
    }

    "translate group by week" in {
      val group = GroupStatement(Seq(byWeek), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `week` as `week`,coll_count(g) as `count`
          |from twitter.ds_tweet t
          |group by get_interval_start_datetime(interval_bin(t.`create_at`, datetime('1990-01-01T00:00:00.000Z'),  day_time_duration("P7D") )) as `week` group as g;
          | """.stripMargin.trim)
    }

    "translate group by month" in {
      val group = GroupStatement(Seq(byMonth), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `month` as `month`,coll_count(g) as `count`
          |from twitter.ds_tweet t
          |group by get_interval_start_datetime(interval_bin(t.`create_at`, datetime('1990-01-01T00:00:00.000Z'),  year_month_duration("P1M") )) as `month` group as g;
          | """.stripMargin.trim)
    }

    "translate group by year" in {
      val group = GroupStatement(Seq(byYear), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `year` as `year`,coll_count(g) as `count`
          |from twitter.ds_tweet t
          |group by get_interval_start_datetime(interval_bin(t.`create_at`, datetime('1990-01-01T00:00:00.000Z'),  year_month_duration("P1Y") )) as `year` group as g;
          | """.stripMargin.trim)
    }

    "translate a count cardinality query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select coll_count(
          |(select value c from (select value t
          |from twitter.ds_tweet t) as c)
          |) as `count`;""".stripMargin)
    }

    "translate get min field value query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrMin)
      val query = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select coll_min(
          |(select value c.`id` from (select value t
          |from twitter.ds_tweet t) as c)
          |) as `min`;""".stripMargin)
    }

    "translate get max field value query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrMax)
      val query = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select coll_max(
          |(select value c.`id` from (select value t
          |from twitter.ds_tweet t) as c)
          |) as `max`;""".stripMargin)
    }

    "translate a count cardinality query with filter without group by" in {
      val filter = Seq(timeFilter)
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = TwitterDataSet, filter = filter, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select coll_count(
          |(select value c from (select value t
          |from twitter.ds_tweet t
          |where t.`create_at` >= datetime('2016-01-01T00:00:00.000Z') and t.`create_at` < datetime('2016-12-01T00:00:00.000Z')) as c)
          |) as `count`;""".stripMargin)
    }

    "translate a min cardinality query with filter without group by" in {
      val filter = Seq(timeFilter)
      val globalAggr = GlobalAggregateStatement(aggrMin)
      val query = new Query(dataset = TwitterDataSet, filter = filter, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select coll_min(
          |(select value c.`id` from (select value t
          |from twitter.ds_tweet t
          |where t.`create_at` >= datetime('2016-01-01T00:00:00.000Z') and t.`create_at` < datetime('2016-12-01T00:00:00.000Z')) as c)
          |) as `min`;""".stripMargin)
    }

    "translate a max cardinality query with unnest with group by with select" in {

      val filter = Seq(textFilter, timeFilter, stateFilter)
      val globalAggr = GlobalAggregateStatement(aggrMaxGroupBy)
      val group = GroupStatement(Seq(byTag), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq(unnestHashTag), Some(group), Some(selectTop10Tag), Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select coll_max(
          |(select value c.`count` from (select `tag` as `tag`,coll_count(g) as `count`
          |from twitter.ds_tweet t
          |unnest t.`hashtags` `unnest0`
          |where not(is_null(t.`hashtags`)) and ftcontains(t.`text`, ['zika','virus'], {'mode':'all'}) and t.`create_at` >= datetime('2016-01-01T00:00:00.000Z') and t.`create_at` < datetime('2016-12-01T00:00:00.000Z') and t.`geo_tag`.`stateID` in [ 37,51,24,11,10,34,42,9,44 ]
          |group by `unnest0` as `tag` group as g
          |order by `count` desc
          |limit 10
          |offset 0) as c)
          |) as `max`;""".stripMargin.trim)
    }

    "translate a count cardinality query with select" in {
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = TwitterDataSet, select = Some(selectTop10), globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select coll_count(
          |(select value c from (select value t
          |from twitter.ds_tweet t
          |limit 10
          |offset 0) as c)
          |) as `count`;""".stripMargin.trim)
    }

    "translate lookup one table with one join key" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val selectStatement = SelectStatement(Seq.empty, Seq.empty, 0, 0, Seq(AllField, population))
      val lookup = Seq(lookupPopulation)
      val filter = Seq(textFilter)
      val query = new Query(TwitterDataSet, Seq.empty, lookup, filter, Seq.empty, select = Some(selectStatement))
      val result = parser.generate(query, schemaMap = Map(TwitterDataSet -> twitterSchema, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select t.`favorite_count` as `favorite_count`,t.`geo_tag`.`countyID` as `geo_tag.countyID`,t.`user_mentions` as `user_mentions`,l0.`population` as `population`,t.`user`.`id` as `user.id`,t.`geo_tag`.`cityID` as `geo_tag.cityID`,t.`is_retweet` as `is_retweet`,t.`text` as `text`,t.`retweet_count` as `retweet_count`,t.`in_reply_to_user` as `in_reply_to_user`,t.`id` as `id`,t.`coordinate` as `coordinate`,t.`in_reply_to_status` as `in_reply_to_status`,t.`user`.`status_count` as `user.status_count`,t.`geo_tag`.`stateID` as `geo_tag.stateID`,t.`create_at` as `create_at`,t.`lang` as `lang`,t.`hashtags` as `hashtags`
          |from twitter.ds_tweet t
          |left outer join twitter.US_population l0 on l0.`stateID` = t.`geo_tag`.`stateID`
          |where ftcontains(t.`text`, ['zika','virus'], {'mode':'all'})
          |limit 0
          |offset 0;""".stripMargin.trim
      )
    }

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
        """
          |select t.`favorite_count` as `favorite_count`,t.`geo_tag`.`countyID` as `geo_tag.countyID`,t.`user_mentions` as `user_mentions`,l0.`population` as `population`,l0.`stateID` as `stateID`,t.`user`.`id` as `user.id`,t.`geo_tag`.`cityID` as `geo_tag.cityID`,t.`is_retweet` as `is_retweet`,t.`text` as `text`,t.`retweet_count` as `retweet_count`,t.`in_reply_to_user` as `in_reply_to_user`,t.`id` as `id`,t.`coordinate` as `coordinate`,t.`in_reply_to_status` as `in_reply_to_status`,t.`user`.`status_count` as `user.status_count`,t.`geo_tag`.`stateID` as `geo_tag.stateID`,t.`create_at` as `create_at`,t.`lang` as `lang`,t.`hashtags` as `hashtags`
          |from twitter.ds_tweet t
          |left outer join twitter.US_population l0 on l0.`stateID` = t.`geo_tag`.`stateID`
          |where ftcontains(t.`text`, ['zika','virus'], {'mode':'all'})
          |limit 0
          |offset 0;""".stripMargin.trim
      )
    }

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
        """
          |select t.`favorite_count` as `favorite_count`,t.`geo_tag`.`countyID` as `geo_tag.countyID`,t.`user_mentions` as `user_mentions`,l0.`population` as `population`,t.`user`.`id` as `user.id`,t.`geo_tag`.`cityID` as `geo_tag.cityID`,t.`is_retweet` as `is_retweet`,t.`text` as `text`,t.`retweet_count` as `retweet_count`,l1.`literacy` as `literacy`,t.`in_reply_to_user` as `in_reply_to_user`,t.`id` as `id`,t.`coordinate` as `coordinate`,t.`in_reply_to_status` as `in_reply_to_status`,t.`user`.`status_count` as `user.status_count`,t.`geo_tag`.`stateID` as `geo_tag.stateID`,t.`create_at` as `create_at`,t.`lang` as `lang`,t.`hashtags` as `hashtags`
          |from twitter.ds_tweet t
          |left outer join twitter.US_population l0 on l0.`stateID` = t.`geo_tag`.`stateID`
          |left outer join twitter.US_literacy l1 on l1.`stateID` = t.`geo_tag`.`stateID`
          |where ftcontains(t.`text`, ['zika','virus'], {'mode':'all'})
          |limit 0
          |offset 0;""".stripMargin.trim
      )
    }


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
      val query = new Query(TwitterDataSet, Seq.empty, Seq(lookup), filter, Seq.empty, group)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select `state` as `state`,coll_sum( (select value g.l0.`population` from g) ) as `sum`
          |from twitter.ds_tweet t
          |left outer join twitter.US_population l0 on l0.`stateID` = t.`geo_tag`.`stateID`
          |where ftcontains(t.`text`, ['zika','virus'], {'mode':'all'})
          |group by t.geo_tag.stateID as `state` group as g;""".stripMargin.trim
      )
    }

    "translate a text contain + time + geo id set filter and group day and state and aggregate topK hashtags" in {
      ok
    }


    "translate a filter having point relation with select" in {
      val filter = Seq(pointFilter)
      val select = Option(selectRecent)
      val query = new Query(dataset = TwitterDataSet, filter = filter, select = select)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select t.`create_at` as `create_at`,t.`id` as `id`,t.`user`.`id` as `user.id`
          |from twitter.ds_tweet t
          |where spatial_intersect(t.`coordinate`,
          |  create_rectangle(create_point(0.0,0.0),
          |  create_point(1.0,1.0)))
          |order by t.`create_at` desc
          |limit 100
          |offset 0;""".stripMargin)
    }

    "translate lookup inside group by state and count" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byState), Seq(aggrCount), Seq(lookupPopulationByState))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group))
      val result = parser.generate(query, schemaMap = Map(TwitterDataSet -> twitterSchema, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select tt.`state` as `state`,tt.`count` as `count`,ll0.`population` as `population`
          |from (
          |select `state` as `state`,coll_count(g) as `count`
          |from twitter.ds_tweet t
          |where ftcontains(t.`text`, ['zika','virus'], {'mode':'all'})
          |group by t.geo_tag.stateID as `state` group as g
          |) tt
          |left outer join twitter.US_population ll0 on ll0.`stateID` = tt.`state`;""".stripMargin.trim
      )
    }

    "translate multiple lookups inside group by state and count" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema
      val literacyDataSet = LiteracyDataStore.DatasetName
      val literacySchema = LiteracyDataStore.LiteracySchema

      val selectStatement = SelectStatement(Seq.empty, Seq.empty, 0, 0, Seq(state, count, population, literacy))
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byState), Seq(aggrCount), Seq(lookupPopulationByState, lookupLiteracyByState))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), select = Some(selectStatement))
      val result = parser.generate(query, schemaMap = Map(TwitterDataSet -> twitterSchema, populationDataSet -> populationSchema, literacyDataSet -> literacySchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select tt.`state` as `state`,tt.`count` as `count`,ll0.`population` as `population`,ll1.`literacy` as `literacy`
          |from (
          |select `state` as `state`,coll_count(g) as `count`
          |from twitter.ds_tweet t
          |where ftcontains(t.`text`, ['zika','virus'], {'mode':'all'})
          |group by t.geo_tag.stateID as `state` group as g
          |) tt
          |left outer join twitter.US_population ll0 on ll0.`stateID` = tt.`state`
          |left outer join twitter.US_literacy ll1 on ll1.`stateID` = tt.`state`
          |limit 0
          |offset 0;""".stripMargin.trim
      )
    }

    "translate multiple lookups inside/outside group by state and aggregate population" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema
      val literacyDataSet = LiteracyDataStore.DatasetName
      val literacySchema = LiteracyDataStore.LiteracySchema

      val selectStatement = SelectStatement(Seq.empty, Seq.empty, 0, 0, Seq(state, min, literacy))
      val filter = Seq(textFilter)
      val lookup = Seq(lookupPopulation)
      val group = GroupStatement(Seq(byState), Seq(aggrPopulationMin), Seq(lookupLiteracyByState))
      val query = new Query(TwitterDataSet, Seq.empty, lookup, filter, Seq.empty, Some(group), select = Some(selectStatement))
      val result = parser.generate(query, schemaMap = Map(TwitterDataSet -> twitterSchema, populationDataSet -> populationSchema, literacyDataSet -> literacySchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select tt.`state` as `state`,tt.`min` as `min`,ll0.`literacy` as `literacy`
          |from (
          |select `state` as `state`,coll_min( (select value g.l0.`population` from g) ) as `min`
          |from twitter.ds_tweet t
          |left outer join twitter.US_population l0 on l0.`stateID` = t.`geo_tag`.`stateID`
          |where ftcontains(t.`text`, ['zika','virus'], {'mode':'all'})
          |group by t.geo_tag.stateID as `state` group as g
          |) tt
          |left outer join twitter.US_literacy ll0 on ll0.`stateID` = tt.`state`
          |limit 0
          |offset 0;""".stripMargin.trim
      )
    }

    "translate lookup inside group by state with global aggregate" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val selectStatement = SelectStatement(Seq.empty, Seq.empty, 0, 0, Seq(AllField, population))
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byState), Seq.empty, Seq(lookupPopulationByState))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectStatement), Some(GlobalAggregateStatement(aggrPopulationMin)))
      val result = parser.generate(query, schemaMap = Map(TwitterDataSet -> twitterSchema, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select coll_min(
          |(select value c.`population` from (select tt.`state` as `state`,ll0.`population` as `population`
          |from (
          |select `state` as `state`
          |from twitter.ds_tweet t
          |where ftcontains(t.`text`, ['zika','virus'], {'mode':'all'})
          |group by t.geo_tag.stateID as `state` group as g
          |) tt
          |left outer join twitter.US_population ll0 on ll0.`stateID` = tt.`state`
          |limit 0
          |offset 0) as c)
          |) as `min`;""".
          stripMargin.trim
      )
    }

    "translate a append and filter and group by time query" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrAvgLangLen))
      val query = new Query(TwitterDataSet, Seq(appendLangLen), Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select `hour` as `hour`,coll_avg( (select value g.ta.`lang_len` from g) ) as `avgLangLen`
          |from (select length(lang) as `lang_len`,t
          |from twitter.ds_tweet t
          |where ftcontains(t.`text`, ['zika','virus'], {'mode':'all'})) ta
          |group by get_interval_start_datetime(interval_bin(ta.t.`create_at`, datetime('1990-01-01T00:00:00.000Z'),  day_time_duration("PT1H") )) as `hour` group as g;
          |""".stripMargin.trim)
    }

    "translate append with lookup inside group by state and sum" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byState), Seq(aggrAvgLangLen), Seq(lookupPopulationByState))
      val query = new Query(TwitterDataSet, Seq(appendLangLen), Seq.empty, filter, Seq.empty, Some(group))
      val result = parser.generate(query, schemaMap = Map(TwitterDataSet -> twitterSchema, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select tt.`state` as `state`,tt.`avgLangLen` as `avgLangLen`,ll0.`population` as `population`
          |from (
          |select `state` as `state`,coll_avg( (select value g.ta.`lang_len` from g) ) as `avgLangLen`
          |from (select length(lang) as `lang_len`,t
          |from twitter.ds_tweet t
          |where ftcontains(t.`text`, ['zika','virus'], {'mode':'all'})) ta
          |group by ta.t.geo_tag.stateID as `state` group as g
          |) tt
          |left outer join twitter.US_population ll0 on ll0.`stateID` = tt.`state`;
          |""".stripMargin.trim
      )
    }
  }


  "SQLPPGenerator calcResultSchema" should {
    "return the input schema if the query is subset filter only" in {
      val schema = parser.calcResultSchema(zikaCreateQuery, TwitterDataStore.TwitterSchema)
      schema must_== TwitterDataStore.TwitterSchema
    }
    "return the aggregated schema for aggregation queries" in {
      ok
    }
  }

  "SQLPPGenerator createView" should {
    "generate the ddl for the twitter dataset" in {
      val ddl = parser.generate(CreateView("zika", zikaCreateQuery), Map(TwitterDataSet -> TwitterDataStore.TwitterSchema))
      removeEmptyLine(ddl) must_== unifyNewLine(
        """
          |create type twitter.typeTweet if not exists as open {
          |  favorite_count : double,
          |  geo_tag : {   countyID : double },
          |  user_mentions : {{double}}?,
          |  user : {   id : double },
          |  geo_tag : {   cityID : double },
          |  is_retweet : boolean,
          |  text : string,
          |  retweet_count : double,
          |  in_reply_to_user : double,
          |  id : double,
          |  coordinate : point,
          |  in_reply_to_status : double,
          |  user : {   status_count : double },
          |  geo_tag : {   stateID : double },
          |  create_at : datetime,
          |  lang : string,
          |  hashtags : {{string}}?
          |};
          |drop dataset zika if exists;
          |create dataset zika(twitter.typeTweet) primary key id; //with filter on 'create_at'
          |insert into zika (
          |select value t
          |from twitter.ds_tweet t
          |where ftcontains(t.`text`, ['zika'], {'mode':'all'})
          |);""".stripMargin.trim)
    }
  }

  "SQLPPGenerator appendView" should {
    "generate the upsert query" in {
      val timeFilter = FilterStatement(TimeField(TwitterDataStore.TimeFieldName), None, Relation.inRange, Seq(startTime, endTime))
      val sql = parser.generate(AppendView("zika", zikaCreateQuery.copy(filter = Seq(timeFilter) ++ zikaCreateQuery.filter)), Map("twitter.ds_tweet" -> TwitterDataStore.TwitterSchema))
      removeEmptyLine(sql) must_== unifyNewLine(
        """
          |upsert into zika (
          |select value t
          |from twitter.ds_tweet t
          |where t.`create_at` >= datetime('2016-01-01T00:00:00.000Z') and t.`create_at` < datetime('2016-12-01T00:00:00.000Z') and ftcontains(t.`text`, ['zika'], {'mode':'all'})
          |);
        """.stripMargin.trim)
    }
  }

  "SQLPPGenerator deleteRecord" should {
    "generate the delete query " in {
      val sql = parser.generate(DeleteRecord(TwitterDataSet, Seq(timeFilter)), Map(TwitterDataSet -> TwitterDataStore.TwitterSchema))
      removeEmptyLine(sql) must_== unifyNewLine(
        """
          |delete from twitter.ds_tweet t
          |where t.`create_at` >= datetime('2016-01-01T00:00:00.000Z') and t.`create_at` < datetime('2016-12-01T00:00:00.000Z');
          |""".stripMargin.trim)
    }
  }

  "SQLPPGenerator dropView" should {
    "generate the drop view query" in {
      val sql = parser.generate(DropView(TwitterDataSet), Map(TwitterDataSet -> TwitterDataStore.TwitterSchema))
      removeEmptyLine(sql) must_== unifyNewLine(
        """
          |drop dataset twitter.ds_tweet if exists;
          |""".stripMargin.trim)
    }
  }

}
