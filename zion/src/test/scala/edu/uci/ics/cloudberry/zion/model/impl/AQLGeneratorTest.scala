package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema._
import org.specs2.mutable.Specification

class AQLGeneratorTest extends Specification {

  import TestQuery._

  val parser = new AQLGenerator

  "AQLGenerate generate" should {

    "translate a simple unnest query" in {
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq(unnestHashTag), None, Some(selectTop10))
      val result = parser.generate(query, schema)
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where not(is-null($t.'hashtags'))
          |for $unnest0 in $t.'hashtags'
          |limit 10
          |offset 0
          |return {
          |  'favorite_count' : $t.'favorite_count','geo_tag.countyID' : $t.'geo_tag'.'countyID','user_mentions' : $t.'user_mentions','tag' : $unnest0,'user.id' : $t.'user'.'id','geo_tag.cityID' : $t.'geo_tag'.'cityID','is_retweet' : $t.'is_retweet','text' : $t.'text','retweet_count' : $t.'retweet_count','in_reply_to_user' : $t.'in_reply_to_user','id' : $t.'id','coordinate' : $t.'coordinate','in_reply_to_status' : $t.'in_reply_to_status','user.status_count' : $t.'user'.'status_count','geo_tag.stateID' : $t.'geo_tag'.'stateID','create_at' : $t.'create_at','lang' : $t.'lang','hashtags' : $t.'hashtags'
          |}
        """.stripMargin.trim)
    }

    "translate a simple filter by time and group by time query" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where $t.'create_at' >= datetime('2016-01-01T00:00:00.000Z') and $t.'create_at' < datetime('2016-12-01T00:00:00.000Z')
          |let $taggr := $t
          |group by $g0 := get-interval-start-datetime(interval-bin($t.'create_at', datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("PT1H") )) with $taggr
          |return {
          |  'count' : count($taggr),'hour' : $g0
          |}
          | """.stripMargin.trim)
    }

    "translate a text contain filter and group by time query" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where similarity-jaccard(word-tokens($t.'text'), word-tokens('zika')) > 0.0
          |and contains($t.'text', "virus")
          |let $taggr := $t
          |group by $g0 := get-interval-start-datetime(interval-bin($t.'create_at', datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("PT1H") )) with $taggr
          |return {
          |  'count' : count($taggr),'hour' : $g0
          |}
          | """.stripMargin.trim)
    }

    "translate a geo id set filter group by time query" in {
      val filter = Seq(stateFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where true
          |for $setgeo_tag_stateID in [ 37,51,24,11,10,34,42,9,44 ]
          |where $t.'geo_tag'.'stateID' = $setgeo_tag_stateID
          |let $taggr := $t
          |group by $g0 := get-interval-start-datetime(interval-bin($t.'create_at', datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("PT1H") )) with $taggr
          |return {
          |  'count' : count($taggr),'hour' : $g0
          |}
          | """.stripMargin.trim)
    }

    "translate a text contain + time + geo id set filter and group by time + spatial cube" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val group = GroupStatement(Seq(byHour, byState), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where similarity-jaccard(word-tokens($t.'text'), word-tokens('zika')) > 0.0
          |and contains($t.'text', "virus") and $t.'create_at' >= datetime('2016-01-01T00:00:00.000Z') and $t.'create_at' < datetime('2016-12-01T00:00:00.000Z') and true
          |for $setgeo_tag_stateID in [ 37,51,24,11,10,34,42,9,44 ]
          |where $t.'geo_tag'.'stateID' = $setgeo_tag_stateID
          |let $taggr := $t
          |group by $g0 := get-interval-start-datetime(interval-bin($t.'create_at', datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("PT1H") )), $g1 := $t.geo_tag.stateID with $taggr
          |return {
          |  'count' : count($taggr),'state' : $g1,'hour' : $g0
          |}
          | """.stripMargin.trim)
    }

    "translate a text contain + time + geo id set filter and sample tweets" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, None, Some(selectRecent))
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where similarity-jaccard(word-tokens($t.'text'), word-tokens('zika')) > 0.0
          |and contains($t.'text', "virus") and $t.'create_at' >= datetime('2016-01-01T00:00:00.000Z') and $t.'create_at' < datetime('2016-12-01T00:00:00.000Z') and true
          |for $setgeo_tag_stateID in [ 37,51,24,11,10,34,42,9,44 ]
          |where $t.'geo_tag'.'stateID' = $setgeo_tag_stateID
          |order by $t.'create_at' desc
          |limit 100
          |offset 0
          |return {
          |  'user.id' : $t.'user'.'id','create_at' : $t.'create_at','id' : $t.'id'
          |} """.stripMargin.trim)
    }

    "translate a text contain + time + geo id set filter and group by hashtags" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val group = GroupStatement(Seq(byTag), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq(unnestHashTag), Some(group), Some(selectTop10Tag))
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $g in (
          |for $t in dataset twitter.ds_tweet
          |where similarity-jaccard(word-tokens($t.'text'), word-tokens('zika')) > 0.0
          |and contains($t.'text', "virus") and $t.'create_at' >= datetime('2016-01-01T00:00:00.000Z') and $t.'create_at' < datetime('2016-12-01T00:00:00.000Z') and true
          |for $setgeo_tag_stateID in [ 37,51,24,11,10,34,42,9,44 ]
          |where $t.'geo_tag'.'stateID' = $setgeo_tag_stateID
          |where not(is-null($t.'hashtags'))
          |for $unnest0 in $t.'hashtags'
          |let $taggr := $t
          |group by $g0 := $unnest0 with $taggr
          |return {
          |  'count' : count($taggr),'tag' : $g0
          |}
          |)
          |order by $g.'count' desc
          |limit 10
          |offset 0
          |return $g
          | """.stripMargin.trim)
    }

    "translate a simple filter by time and group by time query max id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrMax))
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where $t.'create_at' >= datetime('2016-01-01T00:00:00.000Z') and $t.'create_at' < datetime('2016-12-01T00:00:00.000Z')
          |let $id_aggr := $t.'id'
          |group by $g0 := get-interval-start-datetime(interval-bin($t.'create_at', datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("PT1H") )) with $id_aggr
          |return {
          |  'max' : max($id_aggr),'hour' : $g0
          |}
          | """.stripMargin.trim)
    }

    "translate a simple filter by time and group by time query min id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrMin))
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where $t.'create_at' >= datetime('2016-01-01T00:00:00.000Z') and $t.'create_at' < datetime('2016-12-01T00:00:00.000Z')
          |let $id_aggr := $t.'id'
          |group by $g0 := get-interval-start-datetime(interval-bin($t.'create_at', datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("PT1H") )) with $id_aggr
          |return {
          |  'hour' : $g0,'min' : min($id_aggr)
          |}
          | """.stripMargin.trim)
    }

    "translate a simple filter by time and group by time query sum id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrSum))
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where $t.'create_at' >= datetime('2016-01-01T00:00:00.000Z') and $t.'create_at' < datetime('2016-12-01T00:00:00.000Z')
          |let $id_aggr := $t.'id'
          |group by $g0 := get-interval-start-datetime(interval-bin($t.'create_at', datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("PT1H") )) with $id_aggr
          |return {
          |  'hour' : $g0,'sum' : sum($id_aggr)
          |}
          | """.stripMargin.trim)
    }

    "translate a simple filter by time and group by time query avg id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrAvg))
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where $t.'create_at' >= datetime('2016-01-01T00:00:00.000Z') and $t.'create_at' < datetime('2016-12-01T00:00:00.000Z')
          |let $id_aggr := $t.'id'
          |group by $g0 := get-interval-start-datetime(interval-bin($t.'create_at', datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("PT1H") )) with $id_aggr
          |return {
          |  'avg' : avg($id_aggr),'hour' : $g0
          |}
          | """.stripMargin.trim)
    }

    "translate a text contain filter and group by geocell 10th" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byGeocell10), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where similarity-jaccard(word-tokens($t.'text'), word-tokens('zika')) > 0.0
          |and contains($t.'text', "virus")
          |let $taggr := $t
          |group by $g0 := get-points(spatial-cell($t.'coordinate', create-point(0.0,0.0), 0.1, 0.1))[0] with $taggr
          |return {
          |  'count' : count($taggr),'cell' : $g0
          |}
          | """.stripMargin.trim)
    }

    "translate a text contain filter and group by geocell 100th" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byGeocell100), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where similarity-jaccard(word-tokens($t.'text'), word-tokens('zika')) > 0.0
          |and contains($t.'text', "virus")
          |let $taggr := $t
          |group by $g0 := get-points(spatial-cell($t.'coordinate', create-point(0.0,0.0), 0.01, 0.01))[0] with $taggr
          |return {
          |  'count' : count($taggr),'cell' : $g0
          |}
          | """.
          stripMargin.trim)
    }

    "translate a text contain filter and group by geocell 1000th" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byGeocell1000), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where similarity-jaccard(word-tokens($t.'text'), word-tokens('zika')) > 0.0
          |and contains($t.'text', "virus")
          |let $taggr := $t
          |group by $g0 := get-points(spatial-cell($t.'coordinate', create-point(0.0,0.0), 0.001, 0.001))[0] with $taggr
          |return {
          |  'count' : count($taggr),'cell' : $g0
          |}
          | """.
          stripMargin.trim)
    }

    "translate a text contain filter and group by bin" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byBin), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where similarity-jaccard(word-tokens($t.'text'), word-tokens('zika')) > 0.0
          |and contains($t.'text', "virus")
          |let $taggr := $t
          |group by $g0 := round($t.'geo_tag'.'stateID'/10)*10 with $taggr
          |return {
          |  'count' : count($taggr),'state' : $g0
          |}
          | """.stripMargin.trim)
    }

    "translate a group by geocell without filter" in {
      val group = GroupStatement(Seq(byGeocell1000), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |let $taggr := $t
          |group by $g0 := get-points(spatial-cell($t.'coordinate', create-point(0.0,0.0), 0.001, 0.001))[0] with $taggr
          |return {
          |  'count' : count($taggr),'cell' : $g0
          |}
          | """.
          stripMargin.trim)
    }

    "translate a text contain filter and select 10" in {
      val filter = Seq(textFilter)
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, None, Some(selectTop10))
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where similarity-jaccard(word-tokens($t.'text'), word-tokens('zika')) > 0.0
          |and contains($t.'text', "virus")
          |limit 10
          |offset 0
          |return $t
          | """.stripMargin.trim)
    }
    "translate group by second" in {
      val group = GroupStatement(Seq(bySecond), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |let $taggr := $t
          |group by $g0 := get-interval-start-datetime(interval-bin($t.'create_at', datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("PT1S") )) with $taggr
          |return {
          |  'count' : count($taggr),'sec' : $g0
          |}
          | """.stripMargin.trim)
    }
    "translate group by minute" in {
      val group = GroupStatement(Seq(byMinute), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |let $taggr := $t
          |group by $g0 := get-interval-start-datetime(interval-bin($t.'create_at', datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("PT1M") )) with $taggr
          |return {
          |  'count' : count($taggr),'min' : $g0
          |}
          | """.stripMargin.trim)
    }

    "translate group by day" in {
      val group = GroupStatement(Seq(byDay), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |let $taggr := $t
          |group by $g0 := get-interval-start-datetime(interval-bin($t.'create_at', datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("P1D") )) with $taggr
          |return {
          |  'count' : count($taggr),'day' : $g0
          |}
          | """.stripMargin.trim)
    }

    "translate group by week" in {
      val group = GroupStatement(Seq(byWeek), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |let $taggr := $t
          |group by $g0 := get-interval-start-datetime(interval-bin($t.'create_at', datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("P7D") )) with $taggr
          |return {
          |  'count' : count($taggr),'week' : $g0
          |}
          | """.stripMargin.trim)
    }

    "translate group by month" in {
      val group = GroupStatement(Seq(byMonth), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |let $taggr := $t
          |group by $g0 := get-interval-start-datetime(interval-bin($t.'create_at', datetime('1990-01-01T00:00:00.000Z'),  year-month-duration("P1M") )) with $taggr
          |return {
          |  'count' : count($taggr),'month' : $g0
          |}
          | """.stripMargin.trim)
    }

    "translate group by year" in {
      val group = GroupStatement(Seq(byYear), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, Seq.empty, Seq.empty, Some(group), None)
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |let $taggr := $t
          |group by $g0 := get-interval-start-datetime(interval-bin($t.'create_at', datetime('1990-01-01T00:00:00.000Z'),  year-month-duration("P1Y") )) with $taggr
          |return {
          |  'count' : count($taggr),'year' : $g0
          |}
          | """.stripMargin.trim)
    }

    "translate a count cardinality query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """{'count': count(
          |for $c in (
          |for $t in dataset twitter.ds_tweet
          |return $t
          |)
          |return $c
          |)
          |}""".stripMargin)
    }

    "translate get min field value query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrMin)
      val query = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """{'min': min(
          |for $c in (
          |for $t in dataset twitter.ds_tweet
          |return $t
          |)
          |return $c.'id'
          |)
          |}""".stripMargin)
    }

    "translate get max field value query without group by" in {
      val globalAggr = GlobalAggregateStatement(aggrMax)
      val query = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """{'max': max(
          |for $c in (
          |for $t in dataset twitter.ds_tweet
          |return $t
          |)
          |return $c.'id'
          |)
          |}""".stripMargin)
    }

    "translate a count cardinality query with filter without group by" in {
      val filter = Seq(timeFilter)
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = TwitterDataSet, filters = filter, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """{'count': count(
          |for $c in (
          |for $t in dataset twitter.ds_tweet
          |where $t.'create_at' >= datetime('2016-01-01T00:00:00.000Z') and $t.'create_at' < datetime('2016-12-01T00:00:00.000Z')
          |return $t
          |)
          |return $c
          |)
          |}""".stripMargin)
    }

    "translate a min cardinality query with filter without group by" in {
      val filter = Seq(timeFilter)
      val globalAggr = GlobalAggregateStatement(aggrMin)
      val query = new Query(dataset = TwitterDataSet, filters = filter, globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """{'min': min(
          |for $c in (
          |for $t in dataset twitter.ds_tweet
          |where $t.'create_at' >= datetime('2016-01-01T00:00:00.000Z') and $t.'create_at' < datetime('2016-12-01T00:00:00.000Z')
          |return $t
          |)
          |return $c.'id'
          |)
          |}""".stripMargin)
    }

    "translate a max cardinality query with unnest with group by with select" in {

      val filter = Seq(textFilter, timeFilter, stateFilter)
      val globalAggr = GlobalAggregateStatement(aggrMaxGroupBy)
      val group = GroupStatement(Seq(byTag), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq(unnestHashTag), Some(group), Some(selectTop10Tag), Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |{'max': max(
          |for $s in (
          |for $g in (
          |for $t in dataset twitter.ds_tweet
          |where similarity-jaccard(word-tokens($t.'text'), word-tokens('zika')) > 0.0
          |and contains($t.'text', "virus") and $t.'create_at' >= datetime('2016-01-01T00:00:00.000Z') and $t.'create_at' < datetime('2016-12-01T00:00:00.000Z') and true
          |for $setgeo_tag_stateID in [ 37,51,24,11,10,34,42,9,44 ]
          |where $t.'geo_tag'.'stateID' = $setgeo_tag_stateID
          |where not(is-null($t.'hashtags'))
          |for $unnest0 in $t.'hashtags'
          |let $taggr := $t
          |group by $g0 := $unnest0 with $taggr
          |return {
          |  'count' : count($taggr),'tag' : $g0
          |}
          |)
          |order by $g.'count' desc
          |limit 10
          |offset 0
          |return $g
          |)
          |return $s.'count'
          |)
          |}""".stripMargin.trim)
    }

    "translate a count cardinality query with select" in {
      val globalAggr = GlobalAggregateStatement(aggrCount)
      val query = new Query(dataset = TwitterDataSet, select = Some(selectTop10), globalAggr = Some(globalAggr))
      val result = parser.generate(query, Map(TwitterDataSet -> schema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |{'count': count(
          |for $c in (
          |for $t in dataset twitter.ds_tweet
          |limit 10
          |offset 0
          |return $t
          |)
          |return $c
          |)
          |}""".stripMargin.trim)
    }

    "translate lookup one table with one join key" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val selectStatement = SelectStatement(Seq.empty, 0, 0, Seq("*", "population"))
      val lookup = Seq(lookupPopulation)
      val filter = Seq(textFilter)
      val query = new Query(TwitterDataSet, lookup, filter, Seq.empty, select = Some(selectStatement))
      val result = parser.generate(query, schemaMap = Map(TwitterDataSet -> schema, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |let $l := {
          |  'favorite_count' : $t.'favorite_count','geo_tag.countyID' : $t.'geo_tag'.'countyID','user_mentions' : $t.'user_mentions','population' : (for $l0 in dataset twitter.US_population
          |where $t.'geo_tag'.'stateID' /* +indexnl */ = $l0.stateId
          |return $l0.'population')[0],'geo' : $t,'user.id' : $t.'user'.'id','geo_tag.cityID' : $t.'geo_tag'.'cityID','is_retweet' : $t.'is_retweet','text' : $t.'text','retweet_count' : $t.'retweet_count','in_reply_to_user' : $t.'in_reply_to_user','id' : $t.'id','coordinate' : $t.'coordinate','in_reply_to_status' : $t.'in_reply_to_status','user.status_count' : $t.'user'.'status_count','geo_tag.stateID' : $t.'geo_tag'.'stateID','create_at' : $t.'create_at','lang' : $t.'lang','hashtags' : $t.'hashtags'
          |}
          |where similarity-jaccard(word-tokens($l.'text'), word-tokens('zika')) > 0.0
          |and contains($l.'text', "virus")
          |limit 0
          |offset 0
          |return {
          |  'favorite_count' : $l.'favorite_count','geo_tag.countyID' : $l.'geo_tag.countyID','user_mentions' : $l.'user_mentions','population' : $l.'population','geo' : $l.'geo','user.id' : $l.'user.id','geo_tag.cityID' : $l.'geo_tag.cityID','is_retweet' : $l.'is_retweet','text' : $l.'text','retweet_count' : $l.'retweet_count','in_reply_to_user' : $l.'in_reply_to_user','id' : $l.'id','coordinate' : $l.'coordinate','in_reply_to_status' : $l.'in_reply_to_status','user.status_count' : $l.'user.status_count','geo_tag.stateID' : $l.'geo_tag.stateID','create_at' : $l.'create_at','lang' : $l.'lang','hashtags' : $l.'hashtags'
          |}""".stripMargin.trim
      )
    }

    "parseLookup should be able to handle multiple fields in the lookup statement" in {
          val populationDataSet = PopulationDataStore.DatasetName
          val populationSchema = PopulationDataStore.PopulationSchema

          val selectStatement = SelectStatement(Seq.empty, 0, 0, Seq("*", "population", "stateId"))
          val lookup = LookupStatement(Seq("geo_tag.stateID"), populationDataSet, Seq("stateId"), Seq("population","stateId"),
            Seq("population", "stateId"))
          val filter = Seq(textFilter)
          val query = new Query(TwitterDataSet, Seq(lookup), filter, Seq.empty, select = Some(selectStatement))
          val result = parser.generate(query, Map(TwitterDataSet -> schema, populationDataSet -> populationSchema))
          removeEmptyLine(result) must_== unifyNewLine(
            """
              |for $t in dataset twitter.ds_tweet
              |let $l := {
              |  'stateId' : (for $l0 in dataset twitter.US_population
              |where $t.'geo_tag'.'stateID' /* +indexnl */ = $l0.stateId
              |return $l0.'stateId')[0],'favorite_count' : $t.'favorite_count','geo_tag.countyID' : $t.'geo_tag'.'countyID','user_mentions' : $t.'user_mentions','population' : (for $l0 in dataset twitter.US_population
              |where $t.'geo_tag'.'stateID' /* +indexnl */ = $l0.stateId
              |return $l0.'population')[0],'geo' : $t,'user.id' : $t.'user'.'id','geo_tag.cityID' : $t.'geo_tag'.'cityID','is_retweet' : $t.'is_retweet','text' : $t.'text','retweet_count' : $t.'retweet_count','in_reply_to_user' : $t.'in_reply_to_user','id' : $t.'id','coordinate' : $t.'coordinate','in_reply_to_status' : $t.'in_reply_to_status','user.status_count' : $t.'user'.'status_count','geo_tag.stateID' : $t.'geo_tag'.'stateID','create_at' : $t.'create_at','lang' : $t.'lang','hashtags' : $t.'hashtags'
              |}
              |where similarity-jaccard(word-tokens($l.'text'), word-tokens('zika')) > 0.0
              |and contains($l.'text', "virus")
              |limit 0
              |offset 0
              |return {
              |  'stateId' : $l.'stateId','favorite_count' : $l.'favorite_count','geo_tag.countyID' : $l.'geo_tag.countyID','user_mentions' : $l.'user_mentions','population' : $l.'population','geo' : $l.'geo','user.id' : $l.'user.id','geo_tag.cityID' : $l.'geo_tag.cityID','is_retweet' : $l.'is_retweet','text' : $l.'text','retweet_count' : $l.'retweet_count','in_reply_to_user' : $l.'in_reply_to_user','id' : $l.'id','coordinate' : $l.'coordinate','in_reply_to_status' : $l.'in_reply_to_status','user.status_count' : $l.'user.status_count','geo_tag.stateID' : $l.'geo_tag.stateID','create_at' : $l.'create_at','lang' : $l.'lang','hashtags' : $l.'hashtags'
              |}
            """.stripMargin.trim
          )
        }

    "translate lookup multiple tables with one join key on each" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val literacyDataSet = LiteracyDataStore.DatasetName
      val literacySchema = LiteracyDataStore.LiteracySchema

      val selectValues = Seq("*", "population", "literacy")
      val selectStatement = SelectStatement(Seq.empty, 0, 0, selectValues)
      val filter = Seq(textFilter)
      val query = new Query(TwitterDataSet,
        lookups = Seq(lookupPopulation, lookupLiteracy),
        filter, Seq.empty,
        select = Some(selectStatement))

      val result = parser.generate(query, Map(TwitterDataSet -> schema, populationDataSet -> populationSchema,
        literacyDataSet -> literacySchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |let $l := {
          |  'favorite_count' : $t.'favorite_count','geo_tag.countyID' : $t.'geo_tag'.'countyID','user_mentions' : $t.'user_mentions','population' : (for $l0 in dataset twitter.US_population
          |where $t.'geo_tag'.'stateID' /* +indexnl */ = $l0.stateId
          |return $l0.'population')[0],'geo' : $t,'user.id' : $t.'user'.'id','geo_tag.cityID' : $t.'geo_tag'.'cityID','is_retweet' : $t.'is_retweet','text' : $t.'text','retweet_count' : $t.'retweet_count','literacy' : (for $l1 in dataset twitter.US_literacy
          |where $t.'geo_tag'.'stateID' /* +indexnl */ = $l1.stateId
          |return $l1.'literacy')[0],'in_reply_to_user' : $t.'in_reply_to_user','id' : $t.'id','coordinate' : $t.'coordinate','in_reply_to_status' : $t.'in_reply_to_status','user.status_count' : $t.'user'.'status_count','geo_tag.stateID' : $t.'geo_tag'.'stateID','create_at' : $t.'create_at','lang' : $t.'lang','hashtags' : $t.'hashtags'
          |}
          |where similarity-jaccard(word-tokens($l.'text'), word-tokens('zika')) > 0.0
          |and contains($l.'text', "virus")
          |limit 0
          |offset 0
          |return {
          |  'favorite_count' : $l.'favorite_count','geo_tag.countyID' : $l.'geo_tag.countyID','user_mentions' : $l.'user_mentions','population' : $l.'population','geo' : $l.'geo','user.id' : $l.'user.id','geo_tag.cityID' : $l.'geo_tag.cityID','is_retweet' : $l.'is_retweet','text' : $l.'text','retweet_count' : $l.'retweet_count','literacy' : $l.'literacy','in_reply_to_user' : $l.'in_reply_to_user','id' : $l.'id','coordinate' : $l.'coordinate','in_reply_to_status' : $l.'in_reply_to_status','user.status_count' : $l.'user.status_count','geo_tag.stateID' : $l.'geo_tag.stateID','create_at' : $l.'create_at','lang' : $l.'lang','hashtags' : $l.'hashtags'
          |}""".stripMargin.trim
      )
    }


    "translate group by query having lookup with one join key" in {
      val populationDataSet = PopulationDataStore.DatasetName
      val populationSchema = PopulationDataStore.PopulationSchema

      val selectValues = Seq("population")
      val group = Some(groupPopulationSum)
      val lookup = LookupStatement(
        sourceKeys = Seq("geo_tag.stateID"),
        dataset = populationDataSet,
        lookupKeys = Seq("stateId"),
        selectValues,
        as = selectValues)
      val filter = Seq(textFilter)
      val query = new Query(TwitterDataSet, Seq(lookup), filter, Seq.empty, group)
      val result = parser.generate(query, Map(TwitterDataSet -> schema, populationDataSet -> populationSchema))
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |let $l := {
          |  'favorite_count' : $t.'favorite_count','geo_tag.countyID' : $t.'geo_tag'.'countyID','user_mentions' : $t.'user_mentions','population' : (for $l0 in dataset twitter.US_population
          |where $t.'geo_tag'.'stateID' /* +indexnl */ = $l0.stateId
          |return $l0.'population')[0],'geo' : $t,'user.id' : $t.'user'.'id','geo_tag.cityID' : $t.'geo_tag'.'cityID','is_retweet' : $t.'is_retweet','text' : $t.'text','retweet_count' : $t.'retweet_count','in_reply_to_user' : $t.'in_reply_to_user','id' : $t.'id','coordinate' : $t.'coordinate','in_reply_to_status' : $t.'in_reply_to_status','user.status_count' : $t.'user'.'status_count','geo_tag.stateID' : $t.'geo_tag'.'stateID','create_at' : $t.'create_at','lang' : $t.'lang','hashtags' : $t.'hashtags'
          |}
          |where similarity-jaccard(word-tokens($l.'text'), word-tokens('zika')) > 0.0
          |and contains($l.'text', "virus")
          |let $population_aggr := $l.'population'
          |group by $g0 := $l.'geo'.geo_tag.stateID with $population_aggr
          |return {
          |  'state' : $g0,'sum' : sum($population_aggr)
          |}
        """.stripMargin.trim
      )
    }


    "translate a text contain + time + geo id set filter and group day and state and aggregate topK hashtags" in {
      ok
    }
  }

  "AQLQueryParser calcResultSchema" should {
    "return the input schema if the query is subset filter only" in {
      val schema = parser.calcResultSchema(zikaCreateQuery, TwitterDataStore.TwitterSchema)
      schema must_== TwitterDataStore.TwitterSchema
    }
    "return the aggregated schema for aggregation queries" in {
      ok
    }
  }

  "AQLQueryParser createView" should {
    "generate the ddl for the twitter dataset" in {
      val ddl = parser.generate(CreateView("zika", zikaCreateQuery), Map("twitter.ds_tweet" -> TwitterDataStore.TwitterSchema))
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
          |}
          |drop dataset zika if exists;
          |create dataset zika(twitter.typeTweet) primary key id //with filter on 'create_at'
          |insert into dataset zika (
          |for $t in dataset twitter.ds_tweet
          |where similarity-jaccard(word-tokens($t.'text'), word-tokens('zika')) > 0.0
          |return $t
          |)
          |
        """.stripMargin.trim)
    }
  }

  "AQLQueryParser appendView" should {
    "generate the upsert query" in {
      val timeFilter = FilterStatement(TwitterDataStore.TimeFieldName, None, Relation.inRange, Seq(startTime, endTime))
      val aql = parser.generate(AppendView("zika", zikaCreateQuery.copy(filters = Seq(timeFilter) ++ zikaCreateQuery.filters)), Map("twitter.ds_tweet" -> TwitterDataStore.TwitterSchema))
      removeEmptyLine(aql) must_== unifyNewLine(
        """
           |upsert into dataset zika (
           |for $t in dataset twitter.ds_tweet
           |where $t.'create_at' >= datetime('2016-01-01T00:00:00.000Z') and $t.'create_at' < datetime('2016-12-01T00:00:00.000Z') and similarity-jaccard(word-tokens($t.'text'), word-tokens('zika')) > 0.0
           |return $t
           |)
        """.stripMargin.trim)
    }
  }
}
