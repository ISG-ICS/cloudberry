package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema._
import org.specs2.mutable.Specification

class AQLQueryParserTest extends Specification {

  import TestQuery._

  val parser = new AQLQueryParser

  "AQLQueryParser" should {
    "translate a simple filter by time and group by time query" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.parse(query, schema)
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where $t.'create_at' >= datetime('2016-01-01T00:00:00Z') and $t.'create_at' < datetime('2016-12-01T00:00:00Z')
          |let $taggr := $t
          |group by $g0 := get-interval-start-datetime(interval-bin($t.'create_at', datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("PT1H") )) with $taggr
          |return {
          |   'hour' : $g0,'count' : count($taggr)
          |}
          | """.stripMargin.trim)
    }

    "translate a text contain filter and group by time query" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.parse(query, schema)
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where similarity-jaccard(word-tokens($t.'text'), word-tokens('zika')) > 0.0
          |and contains($t.'text', "virus")
          |let $taggr := $t
          |group by $g0 := get-interval-start-datetime(interval-bin($t.'create_at', datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("PT1H") )) with $taggr
          |return {
          |   'hour' : $g0,'count' : count($taggr)
          |}
          | """.stripMargin.trim)
    }

    "translate a geo id set filter group by time query" in {
      val filter = Seq(stateFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.parse(query, schema)
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where true
          |for $setgeo_tag_stateID in [ 37,51,24,11,10,34,42,9,44 ]
          |where $t.'geo_tag'.'stateID' = $setgeo_tag_stateID
          |let $taggr := $t
          |group by $g0 := get-interval-start-datetime(interval-bin($t.'create_at', datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("PT1H") )) with $taggr
          |return {
          |   'hour' : $g0,'count' : count($taggr)
          |}
          | """.stripMargin.trim)
    }

    "translate a text contain + time + geo id set filter and group by time + spatial cube" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val group = GroupStatement(Seq(byHour, byState), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.parse(query, schema)
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where similarity-jaccard(word-tokens($t.'text'), word-tokens('zika')) > 0.0
          |and contains($t.'text', "virus") and $t.'create_at' >= datetime('2016-01-01T00:00:00Z') and $t.'create_at' < datetime('2016-12-01T00:00:00Z') and true
          |for $setgeo_tag_stateID in [ 37,51,24,11,10,34,42,9,44 ]
          |where $t.'geo_tag'.'stateID' = $setgeo_tag_stateID
          |let $taggr := $t
          |group by $g0 := get-interval-start-datetime(interval-bin($t.'create_at', datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("PT1H") )), $g1 := $t.geo_tag.stateID with $taggr
          |return {
          |   'hour' : $g0, 'state' : $g1,'count' : count($taggr)
          |}
          | """.stripMargin.trim)
    }

    "translate a text contain + time + geo id set filter and sample tweets" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, None, Some(selectRecent))
      val result = parser.parse(query, schema)
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where similarity-jaccard(word-tokens($t.'text'), word-tokens('zika')) > 0.0
          |and contains($t.'text', "virus") and $t.'create_at' >= datetime('2016-01-01T00:00:00Z') and $t.'create_at' < datetime('2016-12-01T00:00:00Z') and true
          |for $setgeo_tag_stateID in [ 37,51,24,11,10,34,42,9,44 ]
          |where $t.'geo_tag'.'stateID' = $setgeo_tag_stateID
          |order by $t.'create_at' desc
          |limit 100
          |offset 0
          |return
          |{ 'create_at': $t.'create_at', 'id': $t.'id', 'user.id': $t.'user'.'id'}
          | """.stripMargin.trim)
    }

    "translate a text contain + time + geo id set filter and group by hashtags" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val group = GroupStatement(Seq(byTag), Seq(aggrCount))
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq(unnestHashTag), Some(group), Some(selectTop10Tag))
      val result = parser.parse(query, schema)
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $g in (
          |for $t in dataset twitter.ds_tweet
          |where similarity-jaccard(word-tokens($t.'text'), word-tokens('zika')) > 0.0
          |and contains($t.'text', "virus") and $t.'create_at' >= datetime('2016-01-01T00:00:00Z') and $t.'create_at' < datetime('2016-12-01T00:00:00Z') and true
          |for $setgeo_tag_stateID in [ 37,51,24,11,10,34,42,9,44 ]
          |where $t.'geo_tag'.'stateID' = $setgeo_tag_stateID
          |where not(is-null($t.'hashtags'))
          |for $unnest0 in $t.'hashtags'
          |let $taggr := $t
          |group by $g0 := $unnest0 with $taggr
          |return {
          |   'tag' : $g0,'count' : count($taggr)
          |}
          |)
          |order by $g.count desc
          |limit 10
          |offset 0
          |return
          |$g
          | """.stripMargin.trim)
    }

    "translate a simple filter by time and group by time query max id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrMax))
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.parse(query, schema)
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where $t.'create_at' >= datetime('2016-01-01T00:00:00Z') and $t.'create_at' < datetime('2016-12-01T00:00:00Z')
          |let $taggr := $t.'id'
          |group by $g0 := get-interval-start-datetime(interval-bin($t.'create_at', datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("PT1H") )) with $taggr
          |return {
          |   'hour' : $g0,'max' : max($taggr)
          |}
          | """.stripMargin.trim)
    }

    "translate a simple filter by time and group by time query min id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrMin))
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.parse(query, schema)
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where $t.'create_at' >= datetime('2016-01-01T00:00:00Z') and $t.'create_at' < datetime('2016-12-01T00:00:00Z')
          |let $taggr := $t.'id'
          |group by $g0 := get-interval-start-datetime(interval-bin($t.'create_at', datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("PT1H") )) with $taggr
          |return {
          |   'hour' : $g0,'min' : min($taggr)
          |}
          | """.stripMargin.trim)
    }

    "translate a simple filter by time and group by time query sum id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrSum))
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.parse(query, schema)
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where $t.'create_at' >= datetime('2016-01-01T00:00:00Z') and $t.'create_at' < datetime('2016-12-01T00:00:00Z')
          |let $taggr := $t.'id'
          |group by $g0 := get-interval-start-datetime(interval-bin($t.'create_at', datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("PT1H") )) with $taggr
          |return {
          |   'hour' : $g0,'sum' : sum($taggr)
          |}
          | """.stripMargin.trim)
    }

    "translate a simple filter by time and group by time query avg id" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrAvg))
      val query = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
      val result = parser.parse(query, schema)
      removeEmptyLine(result) must_== unifyNewLine(
        """
          |for $t in dataset twitter.ds_tweet
          |where $t.'create_at' >= datetime('2016-01-01T00:00:00Z') and $t.'create_at' < datetime('2016-12-01T00:00:00Z')
          |let $taggr := $t.'id'
          |group by $g0 := get-interval-start-datetime(interval-bin($t.'create_at', datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("PT1H") )) with $taggr
          |return {
          |   'hour' : $g0,'avg' : avg($taggr)
          |}
          | """.stripMargin.trim)
    }

    "translate a text contain + time + geo id set filter and group day and state and aggregate topK hashtags" in {
      ok
    }

    "translate a lookup query" in {
      ok
    }
  }

}
