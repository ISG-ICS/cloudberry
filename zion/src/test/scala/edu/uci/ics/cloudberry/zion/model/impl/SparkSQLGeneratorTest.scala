package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema._
import org.specs2.mutable.Specification


class SparkSQLGeneratorTest extends Specification {

  import TestQuery._

  val parser = new SparkSQLGenerator

  "SparkSQLGenerator generate" should {
    "eeee" in {
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

    //self query test1
    "translate a simple query group by hour" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select `hour`(t.`create_at`),count(*) as `count`
           |from twitter.ds_tweet t
           |where t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z'
           |group by `hour`(t.`create_at`);
           |""".stripMargin.trim)
    }

    //self query test2
    "translate a simple query group by day" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byDay), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select `day`(t.`create_at`),count(*) as `count`
           |from twitter.ds_tweet t
           |where t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z'
           |group by `day`(t.`create_at`);
           |""".stripMargin.trim)
    }
    //self query test3
    "translate a simple query group by month" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byMonth), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select `month`(t.`create_at`),count(*) as `count`
           |from twitter.ds_tweet t
           |where t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z'
           |group by `month`(t.`create_at`);
           |""".stripMargin.trim)
    }

    //self query test4
    "translate a geo id set filter group by time query1" in {
      val filter = Seq(timeFilter, textFilter, stateFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      println(query)
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select `hour`(t.`create_at`),count(*) as `count`
           |from twitter.ds_tweet t
           |where t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z' and lower(t.`text`) like '%zika%' and lower(t.`text`) like '%virus%' and t.`geo_tag`.`stateID` in ( 37,51,24,11,10,34,42,9,44 )
           |group by `hour`(t.`create_at`);
           |""".stripMargin.trim)
    }

    //self query test5
    "translate a simple unnest query" in {
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Seq.empty, None, Some(selectAllOrderByTimeDesc))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select *
           |from twitter.ds_tweet t
           |order by t.`create_at` desc
           |limit 100;
           |""".stripMargin.trim)
    }

    //self query test6
    "translate a simple unnest query111" in {
      val filter = Seq(timeFilter, textFilter, stateFilter)
      val group = GroupStatement(Seq(byHashTag), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq(unnestHashTag), Some(group), Some(selectTop10byHashTag))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select `hash` as `hash`,count(*) as `count`
           |from twitter.ds_tweet t
           |lateral view explode(t.`hashtags`) tab as `hash`
           |where t.`hashtags` is not null and t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z' and lower(t.`text`) like '%zika%' and lower(t.`text`) like '%virus%' and t.`geo_tag`.`stateID` in ( 37,51,24,11,10,34,42,9,44 )
           |group by `hash`
           |order by `count` desc
           |limit 10;""".stripMargin.trim)
    }

    //self query test 7
    "translate a count cardinality query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select count(
          |(select `c` from (select value t
          |from twitter.ds_tweet t) as `c`)
          |) as `count`;""".stripMargin)
    }
    //self query test 8
    "translate get min field value query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrMin)
      val query = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select min(t.`id`) as `min` from
          |(select *
          |from twitter.ds_tweet t);""".stripMargin)
    }
    //self query test 9
    "translate get max field value query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrMax)
      val query = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select max(t.`id`) as `max` from
          |(select * t
          |from twitter.ds_tweet t);""".stripMargin)
    }
    //self query test 10
    "translate a count cardinality query with filter without group by" in {
      val filter = Seq(timeFilter)
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = TwitterDataSet, filter = filter, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select count(*) as `count` from
          |(select *
          |from twitter.ds_tweet t
          |where t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z');""".stripMargin)
    }
    //self query test 11
    "translate a min cardinality query with filter without group by" in {
      val filter = Seq(timeFilter)
      val globalAggr = GlobalAggregateStatement(aggrMin)
      val query = new Query(dataset = TwitterDataSet, filter = filter, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select min(t.`id`) as `min` from
          |(select *
          |from twitter.ds_tweet t
          |where t.`create_at` >= '2016-01-01T00:00:00.000Z' and t.`create_at` < '2016-12-01T00:00:00.000Z');""".stripMargin)
    }
    //self query test 12
    "translate a count cardinality query with select" in {
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = TwitterDataSet, select = Some(selectTop10), globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> twitterSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """select count(*) as `count` from
          |(select *
          |from twitter.ds_tweet t
          |limit 10);""".stripMargin.trim)
    }

  }

}
