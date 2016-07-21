package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema._
import org.specs2.mutable.Specification

class AQLQueryParserTest extends Specification with TestQuery{

  val parser = new AQLQueryParser

  "AQLQueryParser" should {
    "translate a simple filter by time and group by time query" in {
      val filter = Seq(timeFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(schema.dataset, Seq.empty, filter, Some(group), None)
      val result = parser.parse(query, schema).head
      result.trim must_==
        """
          |for $t in dataset twitter.ds_tweet
          |
          |where $t.create_at >= datetime('2016-01-01T00:00:00Z') and $t.create_at < datetime('2016-12-01T00:00:00Z')
          |
          |group by  $g0 := get-interval-start-datetime(interval-bin($t.create_at, datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("PT1H") ))  with $t
          |return {
          |  'hour' : $g0,'count' : count($t)
          |}
          |
          | """.stripMargin.trim
    }

    "translate a text contain filter and group by time query" in {
      val filter = Seq(textFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(schema.dataset, Seq.empty, filter, Some(group), None)
      val result = parser.parse(query, schema).head
      result.trim must_==
        """
          |for $t in dataset twitter.ds_tweet
          |
          |where similarity-jaccard(word-tokens($t.'text'), word-tokens('zika')) > 0.0
          |and contains($t.'text', "virus")
          |
          |group by  $g0 := get-interval-start-datetime(interval-bin($t.create_at, datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("PT1H") ))  with $t
          |return {
          |  'hour' : $g0,'count' : count($t)
          |}
          | """.stripMargin.trim
    }

    "translate a geo id set filter group by time query" in {
      val filter = Seq(stateFilter)
      val group = GroupStatement(Seq(byHour), Seq(aggrCount))
      val query = new Query(schema.dataset, Seq.empty, filter, Some(group), None)
      val result = parser.parse(query, schema).head
      result.trim must_==
        """
          |for $t in dataset twitter.ds_tweet
          |
          |where true
          |for $ts in [ 37,51,24,11,10,34,42,9,44 ]
          |where $t.geo_tag.stateID = $ts
          |
          |
          |group by  $g0 := get-interval-start-datetime(interval-bin($t.create_at, datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("PT1H") ))  with $t
          |return {
          |  'hour' : $g0,'count' : count($t)
          |}
        """.stripMargin.trim
    }

    "translate a text contain + time + geo id set filter and group by time + spatial cube" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val group = GroupStatement(Seq(byHour, byState), Seq(aggrCount))
      val query = new Query(schema.dataset, Seq.empty, filter, Some(group), None)
      val result = parser.parse(query, schema).head
      result.trim must_==
        """
          |for $t in dataset twitter.ds_tweet
          |
          |where similarity-jaccard(word-tokens($t.'text'), word-tokens('zika')) > 0.0
          |and contains($t.'text', "virus") and $t.create_at >= datetime('2016-01-01T00:00:00Z') and $t.create_at < datetime('2016-12-01T00:00:00Z') and true
          |for $ts in [ 37,51,24,11,10,34,42,9,44 ]
          |where $t.geo_tag.stateID = $ts
          |
          |
          |group by  $g0 := get-interval-start-datetime(interval-bin($t.create_at, datetime('1990-01-01T00:00:00.000Z'),  day-time-duration("PT1H") )) ,  $g1 := $t.geo_tag.stateID  with $t
          |return {
          |  'hour' : $g0,'state' : $g1,'count' : count($t)
          |}
        """.stripMargin.trim
    }

    "translate a text contain + time + geo id set filter and sample tweets" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val query = new Query(schema.dataset, Seq.empty, filter, None, Some(selectRecent))
      //val result = parser.parse(query, schema).head
      ok
    }

    "translate a text contain + time + geo id set filter and group by hashtags" in {
      val filter = Seq(textFilter, timeFilter, stateFilter)
      val group = GroupStatement(Seq(byHashTag), Seq(aggrHashTagCount))
      val query = new Query(schema.dataset, Seq.empty, filter, Some(group), Some(selectTopK))
      val result = parser.parse(query, schema).head
      result.trim must_== ""
      ok
    }

    "translate a text contain + time + geo id set filter and group day and state and aggregate topK hashtags" in {
      ok
    }

    "translate a lookup query" in {
      ok
    }
  }

}
