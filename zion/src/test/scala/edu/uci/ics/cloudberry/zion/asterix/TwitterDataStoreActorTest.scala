package edu.uci.ics.cloudberry.zion.asterix

import edu.uci.ics.cloudberry.zion.model.{DBQuery, TimePredicate}
import org.specs2.mutable.Specification

class TwitterDataStoreActorTest extends Specification with TestData {

  "TwitterDataStoreActorTest" should {
    "generateAQL" in {

      val dbQuery = DBQuery(TwitterCountyDaySummaryView.SummaryLevel, Seq(idPredicate, keywordPredicate2, timePredicate2))
      val str = TwitterDataStoreActor.generateAQL("ds_tweet", dbQuery)
      str.trim must_==("""
                    |use dataverse twitter
                    |let $common := (
                    |for $t in dataset ds_tweet
                    |
                    |let $set := [ 1,2,3 ]
                    |for $sid in $set
                    |where $t.geo_tag.stateID = $sid
                    |
                    |where similarity-jaccard(word-tokens($t."text"), word-tokens("trump")) > 0.0
                    |where similarity-jaccard(word-tokens($t."text"), word-tokens("hilary")) > 0.0
                    |
                    |where
                    |
                    |($t."create_at">= datetime("2012-01-01T00:00:00.000Z")
                    |and $t."create_at" < datetime("2012-01-08T00:00:00.000Z"))
                    |or
                    |($t."create_at">= datetime("2016-01-01T00:00:00.000Z")
                    |and $t."create_at" < datetime("2016-01-15T00:00:00.000Z"))
                    |
                    |
                    |return $t
                    |)
                    |
                    |let $map := (
                    |for $t in $common
                    |
                    |group by $c := $t.geo_tag.countyID with $t
                    |return { "key": string($c) , "count": count($t) }
                    |
                    |)
                    |
                    |let $time := (
                    |for $t in $common
                    |
                    |group by $c := print-datetime($t.create_at, "YYYY-MM-DD") with $t
                    |let $count := count($t)
                    |return { "key" : $c , "count": $count }
                    |
                    |)
                    |
                    |let $hashtag := (
                    |for $t in $common
                    |where not(is-null($t.hashtags))
                    |
                    |for $h in $t.hashtags
                    |group by $tag := $h with $h
                    |let $c := count($h)
                    |order by $c desc
                    |limit 50
                    |return { "key": $tag, "count" : $c}
                    |
                    |)
                    |
                    |return {"map": $map, "time": $time, "hashtag": $hashtag }
                    |""".stripMargin.trim)
    }

  }
}
