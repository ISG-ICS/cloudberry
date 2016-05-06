package edu.uci.ics.cloudberry.zion.asterix

import edu.uci.ics.cloudberry.zion.model.DBQuery
import org.specs2.mutable.Specification

class TwitterCountyDaySummaryViewTest extends Specification with TestData {

  "TwitterCountyDaySummaryViewTest" should {
    "generateAQL" in {
      val dbQuery = DBQuery(TwitterCountyDaySummaryView.SummaryLevel, Seq(idPredicate, keywordPredicate2, timePredicate2))
      val aql = TwitterCountyDaySummaryView.generateAQL(dbQuery)
      aql.trim must_== ("""use dataverse twitter
                          |let $common := (
                          |for $t in dataset ds_tweet_
                          |
                          |let $set := [ 1,2,3 ]
                          |for $sid in $set
                          |where $t.countyID = $sid
                          |
                          |
                          |
                          |where
                          |
                          |(get-interval-start($t.timeBin) >= datetime("2012-01-01T00:00:00.000Z")
                          |and get-interval-start($t.timeBin) < datetime("2012-01-08T00:00:00.000Z"))
                          |or
                          |(get-interval-start($t.timeBin) >= datetime("2016-01-01T00:00:00.000Z")
                          |and get-interval-start($t.timeBin) < datetime("2016-01-15T00:00:00.000Z"))
                          |
                          |
                          |return $t
                          |)
                          |
                          |let $map := (
                          |for $t in $common
                          |
                          |group by $c := $t.countyID with $t
                          |return { "key": $c , "count": sum(for $x in $t return $x.tweetCount) }
                          |
                          |)
                          |
                          |let $time := (
                          |for $t in $common
                          |
                          |group by $c := print-datetime(get-interval-start($t.timeBin), "YYYY-MM-DD") with $t
                          |return { "key" : $c, "count": sum(for $x in $t return $x.tweetCount)}
                          |
                          |)
                          |
                          |let $hashtag := (
                          |for $t in $common
                          |
                          |for $h in $t.topHashTags
                          |group by $tag := $h.tag with $h
                          |let $c := sum(for $x in $h return $x.count)
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
