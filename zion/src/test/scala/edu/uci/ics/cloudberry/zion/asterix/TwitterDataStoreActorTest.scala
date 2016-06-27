package edu.uci.ics.cloudberry.zion.asterix

import edu.uci.ics.cloudberry.zion.model.{DBQuery, TimePredicate}
import org.specs2.mutable.Specification

class TwitterDataStoreActorTest extends Specification with TestData {

  "TwitterDataStoreActorTest" should {
    val dbQuery = new DBQuery(TwitterCountyDaySummaryView.SummaryLevel, Seq(idPredicate, keywordPredicate2, timePredicate2))
    "generateMapAQL" in {

      TwitterDataStoreActor.generateByMapAQL("ds_tweet", dbQuery).trim must_== (
        """
          |use dataverse twitter
          |let $common := (
          |for $t in dataset ds_tweet
          |
          |for $sid in [ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50 ]
          |where $t.geo_tag.stateID = $sid
          |
          |where similarity-jaccard(word-tokens($t."text"), word-tokens("trump")) > 0.0
          |and contains($t."text", "hilary")
          |
          |where
          |
          |($t."create_at">= datetime("2012-01-01T00:00:00.000Z")
          |and $t."create_at" <= datetime("2012-01-08T00:00:00.000Z"))
          |or
          |($t."create_at">= datetime("2016-01-01T00:00:00.000Z")
          |and $t."create_at" <= datetime("2016-01-15T00:00:00.000Z"))
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
          |return $map
          | """.stripMargin.trim)
    }

    "generateTimeAQL" in {

      TwitterDataStoreActor.generateByTimeAQL("ds_tweet", dbQuery).trim must_== (
        """
          |use dataverse twitter
          |let $common := (
          |for $t in dataset ds_tweet
          |
          |for $sid in [ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50 ]
          |where $t.geo_tag.stateID = $sid
          |
          |where similarity-jaccard(word-tokens($t."text"), word-tokens("trump")) > 0.0
          |and contains($t."text", "hilary")
          |
          |where
          |
          |($t."create_at">= datetime("2012-01-01T00:00:00.000Z")
          |and $t."create_at" <= datetime("2012-01-08T00:00:00.000Z"))
          |or
          |($t."create_at">= datetime("2016-01-01T00:00:00.000Z")
          |and $t."create_at" <= datetime("2016-01-15T00:00:00.000Z"))
          |
          |
          |return $t
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
          |return $time
          | """.stripMargin.trim)
    }

    "generateHashtagAQL" in {
      TwitterDataStoreActor.generateByHashtagAQL("ds_tweet", dbQuery).trim must_== (
        """
          |use dataverse twitter
          |let $common := (
          |for $t in dataset ds_tweet
          |
          |for $sid in [ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50 ]
          |where $t.geo_tag.stateID = $sid
          |
          |where similarity-jaccard(word-tokens($t."text"), word-tokens("trump")) > 0.0
          |and contains($t."text", "hilary")
          |
          |where
          |
          |($t."create_at">= datetime("2012-01-01T00:00:00.000Z")
          |and $t."create_at" <= datetime("2012-01-08T00:00:00.000Z"))
          |or
          |($t."create_at">= datetime("2016-01-01T00:00:00.000Z")
          |and $t."create_at" <= datetime("2016-01-15T00:00:00.000Z"))
          |
          |
          |return $t
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
          |return $hashtag
          | """.stripMargin.trim)
    }

  }
}
