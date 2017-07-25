package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema._
import org.specs2.mutable.Specification

class SQLGeneratorTest extends Specification {

  import TestQueryForSQL._

  val parser = new SQLGenerator

  "SQLGenerator generate" should {
    //1: pass
    "translate a simple query group by hour" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select hour(t.create_at) as hour,count(*) as count
           |from `twitter_ds_tweet` t
           |where t.create_at >= '2016-01-01T00:00:00.000Z' and t.create_at < '2016-12-01T00:00:00.000Z'
           |group by hour
           |""".stripMargin.trim)
    }

    //2: pass
    "translate a simple query group by day" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byDay), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select day(t.create_at) as day,count(*) as count
           |from `twitter_ds_tweet` t
           |where t.create_at >= '2016-01-01T00:00:00.000Z' and t.create_at < '2016-12-01T00:00:00.000Z'
           |group by day
           |""".stripMargin.trim)
    }

    //3: pass
    "translate a simple query group by month" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byMonth), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select month(t.create_at) as month,count(*) as count
           |from `twitter_ds_tweet` t
           |where t.create_at >= '2016-01-01T00:00:00.000Z' and t.create_at < '2016-12-01T00:00:00.000Z'
           |group by month
           |""".stripMargin.trim)
    }

    //4: pass
    "translate a geo id set filter group by time query" in {
      val filter = Seq(timeFilter, textFilter, stateFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select hour(t.create_at) as hour,count(*) as count
           |from `twitter_ds_tweet` t
           |where t.create_at >= '2016-01-01T00:00:00.000Z' and t.create_at < '2016-12-01T00:00:00.000Z' and lower(t.text) like '%zika%' and lower(t.text) like '%virus%' and t.geo_tag->"$.stateID" in ( 37,51,24,11,10,34,42,9,44 )
           |group by hour
           |""".stripMargin.trim)
    }

    //5: pass
    "translate a simple select query order by time with limit 100" in {
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, Seq.empty, Seq.empty, None, Some(selectAllOrderByTimeDesc))
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select *
           |from `twitter_ds_tweet` t
           |order by t.create_at desc
           |limit 100
           |""".stripMargin.trim)
    }

    //6: pass  //TODO: hashtag's split,count should be len(hashtags)
    "translate a simple unnest query" in {
      val filter = Seq(timeFilter, textFilter, stateFilter)
      val group = GroupStatement(Seq(byHashTag), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq(unnestHashTag), Some(group), Some(selectTop10byHashTag))
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select t.hashtags as hashtags,count(*) as count
           |from `twitter_ds_tweet` t
           |where t.hashtags is not null and t.create_at >= '2016-01-01T00:00:00.000Z' and t.create_at < '2016-12-01T00:00:00.000Z' and lower(t.text) like '%zika%' and lower(t.text) like '%virus%' and t.geo_tag->"$.stateID" in ( 37,51,24,11,10,34,42,9,44 )
           |group by hashtags
           |order by count desc
           |limit 10""".stripMargin.trim)
    }

    //7: pass
    "translate a count cardinality query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = TwitterDataSetForSQL, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """select count(*) as count from
          |(select *
          |from `twitter_ds_tweet` t) t""".stripMargin)
    }

    //8: pass
    "translate get min field value query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrMin)
      val query = new Query(dataset = TwitterDataSetForSQL, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """select min(t.id) as min from
          |(select *
          |from `twitter_ds_tweet` t) t""".stripMargin)
    }

    //9: pass
    "translate get max field value query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrMax)
      val query = new Query(dataset = TwitterDataSetForSQL, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """select max(t.id) as max from
          |(select *
          |from `twitter_ds_tweet` t) t""".stripMargin)
    }

    //10: pass
    "translate a count cardinality query with filter without group by" in {
      val filter = Seq(timeFilter)
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = TwitterDataSetForSQL, filter = filter, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """select count(*) as count from
          |(select *
          |from `twitter_ds_tweet` t
          |where t.create_at >= '2016-01-01T00:00:00.000Z' and t.create_at < '2016-12-01T00:00:00.000Z') t""".stripMargin)
    }

    //11: pass
    "translate a min cardinality query with filter without group by" in {
      val filter = Seq(timeFilter)
      val globalAggr = GlobalAggregateStatement(aggrMin)
      val query = new Query(dataset = TwitterDataSetForSQL, filter = filter, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """select min(t.id) as min from
          |(select *
          |from `twitter_ds_tweet` t
          |where t.create_at >= '2016-01-01T00:00:00.000Z' and t.create_at < '2016-12-01T00:00:00.000Z') t""".stripMargin)
    }

    //12: pass
    "translate a count cardinality query with select" in {
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = TwitterDataSetForSQL, select = Some(selectTop10), globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """select count(*) as count from
          |(select *
          |from `twitter_ds_tweet` t
          |limit 10) t""".stripMargin.trim)
    }

    //13: pass
    "translate group by minute" in {
      val group = GroupStatement(Seq(byMinuteForSql), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select minute(t.create_at) as minute,count(*) as count
          |from `twitter_ds_tweet` t
          |group by minute""".stripMargin.trim)
    }

    //14: pass
    "translate a simple filter by time and group by time query max id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrMax))
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), Some(selectCreateTimeByRange))
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select hour(t.create_at) as hour,max(t.id) as max
          |from `twitter_ds_tweet` t
          |where t.create_at >= '2016-01-01T00:00:00.000Z' and t.create_at < '2016-12-01T00:00:00.000Z'
          |group by hour""".stripMargin.trim)
    }

    //15: pass
    "translate a max cardinality query with unnest with group by with select" in {

      val filter = Seq(textFilter, timeFilter, stateFilter)
      val globalAggr = GlobalAggregateStatement(aggrMaxGroupBy)
      val group = GroupStatement(Seq(byHashTag), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq(unnestHashTag), Some(group), Some(selectTop10Tag), Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """|select max(count) as max from
           |(select t.hashtags as hashtags,count(*) as count
           |from `twitter_ds_tweet` t
           |where t.hashtags is not null and lower(t.text) like '%zika%' and lower(t.text) like '%virus%' and t.create_at >= '2016-01-01T00:00:00.000Z' and t.create_at < '2016-12-01T00:00:00.000Z' and t.geo_tag->"$.stateID" in ( 37,51,24,11,10,34,42,9,44 )
           |group by hashtags
           |order by count desc
           |limit 10) t""".stripMargin.trim)
    }

    //16:
    "translate group by query having lookup with one join key" in {
      ok
    }

    //17: pass
    "translate a simple filter by time and group by time query" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select hour(t.create_at) as hour,count(*) as count
          |from `twitter_ds_tweet` t
          |where t.create_at >= '2016-01-01T00:00:00.000Z' and t.create_at < '2016-12-01T00:00:00.000Z'
          |group by hour""".stripMargin.trim)
    }

    //18: pass
    "translate a simple filter with string not match" in {
      val filter = Seq(langNotMatchFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select hour(t.create_at) as hour,count(*) as count
          |from `twitter_ds_tweet` t
          |where lower(t.lang) like '%en%'
          |group by hour
          |""".stripMargin.trim)
    }

    //19: pass
    "translate a simple filter with string matches" in {
      val filter = Seq(langMatchFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select hour(t.create_at) as hour,count(*) as count
          |from `twitter_ds_tweet` t
          |where lower(t.lang) like '%en%'
          |group by hour""".stripMargin.trim)
    }

    //20: pass
    "translate a text contain filter and group by time query" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select hour(t.create_at) as hour,count(*) as count
          |from `twitter_ds_tweet` t
          |where lower(t.text) like '%zika%' and lower(t.text) like '%virus%'
          |group by hour
          |""".stripMargin.trim)
    }

    //21: pass
    "translate a geo id set filter group by time query" in {
      val filter = Seq(stateFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select hour(t.create_at) as hour,count(*) as count
          |from `twitter_ds_tweet` t
          |where t.geo_tag->"$.stateID" in ( 37,51,24,11,10,34,42,9,44 )
          |group by hour
          | """.stripMargin.trim)
    }

    //22: pass
    "translate a text contain + time + geo id set filter and group by time + spatial cube" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val group = GroupStatement(Seq(byHour, byGeoState), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select hour(t.create_at) as hour,geo_tag->"$.stateID" as state,count(*) as count
          |from `twitter_ds_tweet` t
          |where lower(t.text) like '%zika%' and lower(t.text) like '%virus%' and t.create_at >= '2016-01-01T00:00:00.000Z' and t.create_at < '2016-12-01T00:00:00.000Z' and t.geo_tag->"$.stateID" in ( 37,51,24,11,10,34,42,9,44 )
          |group by hour,state
          | """.stripMargin.trim)
    }

    //23: pass
    "translate a text contain + time + geo id set filter and sample tweets" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, None, Some(selectRecent))
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
      """
        |select t.create_at as create_at,t.id as id,t.user->"$.id" as user_id
        |from `twitter_ds_tweet` t
        |where lower(t.text) like '%zika%' and lower(t.text) like '%virus%' and t.create_at >= '2016-01-01T00:00:00.000Z' and t.create_at < '2016-12-01T00:00:00.000Z' and t.geo_tag->"$.stateID" in ( 37,51,24,11,10,34,42,9,44 )
        |order by t.create_at desc
        |limit 100
        | """.stripMargin.trim)
    }

    //24:pass
    "translate a text contain + time + geo id set filter and group by hashtags" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val group = GroupStatement(Seq(byTag), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq(unnestHashTag), Some(group), Some(selectTop10Tag))
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select hashtags as tag,count(*) as count
          |from `twitter_ds_tweet` t
          |where t.hashtags is not null and lower(t.text) like '%zika%' and lower(t.text) like '%virus%' and t.create_at >= '2016-01-01T00:00:00.000Z' and t.create_at < '2016-12-01T00:00:00.000Z' and t.geo_tag->"$.stateID" in ( 37,51,24,11,10,34,42,9,44 )
          |group by hashtags
          |order by count desc
          |limit 10
          | """.stripMargin.trim)
    }

    //25: pass
    "translate a simple filter by time and group by time query min id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrMin))
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select hour(t.create_at) as hour,min(t.id) as min
          |from `twitter_ds_tweet` t
          |where t.create_at >= '2016-01-01T00:00:00.000Z' and t.create_at < '2016-12-01T00:00:00.000Z'
          |group by hour
          | """.stripMargin.trim)
    }

    //26: pass
    "translate a simple filter by time and group by time query sum id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrSum))
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select hour(t.create_at) as hour,sum(t.id) as sum
          |from `twitter_ds_tweet` t
          |where t.create_at >= '2016-01-01T00:00:00.000Z' and t.create_at < '2016-12-01T00:00:00.000Z'
          |group by hour
          |  """.stripMargin.trim)
    }

    //27: pass
    "translate a simple filter by time and group by time query avg id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrAvg))
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select hour(t.create_at) as hour,avg(t.id) as avg
          |from `twitter_ds_tweet` t
          |where t.create_at >= '2016-01-01T00:00:00.000Z' and t.create_at < '2016-12-01T00:00:00.000Z'
          |group by hour
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

    //33: pass
    "translate a text contain filter and select 10" in {
      val filter = Seq(textFilter)
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, filter, Seq.empty, None, Some(selectTop10))
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select *
          |from `twitter_ds_tweet` t
          |where lower(t.text) like '%zika%' and lower(t.text) like '%virus%'
          |limit 10
          | """.stripMargin.trim)
    }

    //34: pass
    "translate group by second" in {
      val group = GroupStatement(Seq(bySecond), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select second(t.create_at) as sec,count(*) as count
          |from `twitter_ds_tweet` t
          |group by sec
          | """.stripMargin.trim)
    }

    //35: pass
    "translate group by day" in {
      val group = GroupStatement(Seq(byDay), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select day(t.create_at) as day,count(*) as count
          |from `twitter_ds_tweet` t
          |group by day
          | """.stripMargin.trim)
    }

    //36: pass
    "translate group by month" in {
      val group = GroupStatement(Seq(byMonth), Seq(aggrCount))
      val query = new Query(TwitterDataSetForSQL, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSetForSQL -> twitterSchemaForSQL))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |select month(t.create_at) as month,count(*) as count
          |from `twitter_ds_tweet` t
          |group by month
          | """.stripMargin.trim)
    }

    //37: pass
    "translate group by year" in {
      ok
    }

    //38:
    "translate lookup one table with one join key" in {
      ok
    }

    //39
    "parseLookup should be able to handle multiple fields in the lookup statement" in {
      ok
    }

    //40
    "translate lookup multiple tables with one join key on each" in {
      ok
    }

    //41
    "translate a text contain + time + geo id set filter and group day and state and aggregate topK hashtags" in {
      ok
    }

    //42
    "translate lookup inside group by state and count" in {
      ok
    }

    //43
    "translate multiple lookups inside group by state and count" in {
      ok
    }

    //44
    "translate multiple lookups inside/outside group by state and aggregate population" in {
      ok
    }

    //45
    "translate lookup inside group by state with global aggregate" in {
      ok
    }

    //46
    "translate append with lookup inside group by state and sum" in {
      ok
    }
  }


  //calcResultSchema: pass
  "SQLGenerator calcResultSchema" should {
    "return the input schema if the query is subset filter only" in {
      val schema = parser.calcResultSchema(zikaCreateQuery, TwitterDataStoreForSQL.TwitterSchemaForSQL)
      schema must_== TwitterDataStoreForSQL.TwitterSchemaForSQL
    }
    "return the aggregated schema for aggregation queries" in {
      ok
    }
  }

  //deleteRecord: pass
  "SQLGenerator deleteRecord" should {
    "generate the delete query " in {
      val sql = parser.generate(DeleteRecord(TwitterDataSetForSQL, Seq(timeFilter)), Map(TwitterDataSetForSQL -> TwitterDataStoreForSQL.TwitterSchemaForSQL))
      removeEmptyLine(sql) must_== unifyNewLine(
        """
          |delete from `twitter_ds_tweet` t
          |where t.create_at >= '2016-01-01T00:00:00.000Z' and t.create_at < '2016-12-01T00:00:00.000Z'
          |""".stripMargin.trim)
    }
  }

  // dropView: pass
  "SQLGenerator dropView" should {
    "generate the drop view query" in {
      val sql = parser.generate(DropView(TwitterDataSetForSQL), Map(TwitterDataSetForSQL -> TwitterDataStoreForSQL.TwitterSchemaForSQL))
      removeEmptyLine(sql) must_== unifyNewLine(
        """
          |drop table if exists twitter_ds_tweet
          |""".stripMargin.trim)
    }
  }

  "SQLQueryParser createView" should {
    "generate the ddl for the twitter dataset" in {
      val ddl = parser.parseCreate(CreateView("zika", zikaCreateQuery), Map(TwitterDataSetForSQL -> TwitterDataStoreForSQL.TwitterSchemaForSQL))
      removeEmptyLine(ddl) must startWith (unifyNewLine(
        """
          |create table if not exists `zika` (
          |  favorite_count double not null,
          |  is_retweet double not null,
          |  text text not null,
          |  retweet_count double not null,
          |  in_reply_to_user double not null,
          |  id double not null,
          |  in_reply_to_status double not null,
          |  geo_tag json not null,
          |  place json not null,
          |  create_at datetime not null,
          |  lang text not null,
          |  user json not null,
          |  hashtags json default null, primary key (id)
          |);
          |insert into `zika` ()
          |(
          |select *
          |from `twitter_ds_tweet` t
          |where lower(t.text) like '%zika%'
          |);
          |insert into `berry.meta` values(
          |"zika","zika",
        """.stripMargin.trim))
//      removeEmptyLine(ddl) must beMatching(""".*'([0-9]{4}-[0-9]{2}-[0-9]{2}) ([0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3})'.*""".r)
    }
  }

  "SQLQueryParser appendView" should {
    "generate the upsert query" in {
      val timeFilter = FilterStatement(TwitterDataStore.TwitterSchema.timeField, None, Relation.inRange, Seq(startTime, endTime))
      val sql = parser.parseAppend(AppendView("zika", zikaCreateQuery.copy(filter = Seq(timeFilter) ++ zikaCreateQuery.filter)), Map(TwitterDataSetForSQL -> TwitterDataStoreForSQL.TwitterSchemaForSQL))
      removeEmptyLine(sql) must_== unifyNewLine(
        """
          |insert into `zika` (
          |select *
          |from `twitter_ds_tweet` t
          |where t.create_at >= '2016-01-01 00:00:00.000' and t.create_at < '2016-12-01 00:00:00.000' and lower(t.text) like '%zika%'
          |)
        """.stripMargin.trim)
    }
  }

}
