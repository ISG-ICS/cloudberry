package edu.uci.ics.cloudberry.zion.asterix

import akka.actor.Props
import akka.testkit.{TestActor, TestProbe}
import edu.uci.ics.cloudberry.zion.actor.{MockConnClientOld, TestkitExample, ViewMetaRecord}
import edu.uci.ics.cloudberry.zion.model.{DBQuery, SummaryLevel}
import org.specs2.mutable.SpecificationLike

import scala.concurrent.Future
import scala.concurrent.duration._

class TwitterKeywordViewActorTest extends TestkitExample with SpecificationLike with MockConnClientOld with TestData {

  val queryTemplate = new DBQuery(SummaryLevel.Detail, Seq.empty)

  sequential
  "TwitterKeywordViewActorTest" should {
    "should skip the matching keyword" in {

      val key = "trump"
      val viewRecord = ViewMetaRecord("twitter", "twitter_" + key, SummaryLevel.Detail, startTime, lastVisitTime, lastUpdateTime, visitTimes, updateCycle)
      val fViewRecord = Future(viewRecord)

      val answerAqls = Seq(
        """
          |use dataverse twitter
          |let $common := (
          |for $t in dataset twitter_trump
          |
          |where
          |
          |($t."create_at">= datetime("2012-01-01T00:00:00.000Z")
          |and $t."create_at" <= datetime("2012-01-08T00:00:00.000Z"))
          |
          |
          |
          |for $sid in [ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50 ]
          |where $t.geo_tag.stateID = $sid
          |
          |return $t
          |)
          |
          |let $map := (
          |for $t in $common
          |
          |group by $c := $t.geo_tag.stateID with $t
          |return { "key": string($c) , "count": count($t) }
          |
          |)
          |return $map
          | """.stripMargin
        ,
        """
          |use dataverse twitter
          |let $common := (
          |for $t in dataset twitter_trump
          |
          |where
          |
          |($t."create_at">= datetime("2012-01-01T00:00:00.000Z")
          |and $t."create_at" <= datetime("2012-01-08T00:00:00.000Z"))
          |
          |
          |
          |for $sid in [ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50 ]
          |where $t.geo_tag.stateID = $sid
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
          | """.stripMargin
        ,
        """
          |use dataverse twitter
          |let $common := (
          |for $t in dataset twitter_trump
          |
          |where
          |
          |($t."create_at">= datetime("2012-01-01T00:00:00.000Z")
          |and $t."create_at" <= datetime("2012-01-08T00:00:00.000Z"))
          |
          |
          |
          |for $sid in [ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50 ]
          |where $t.geo_tag.stateID = $sid
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
          | """.stripMargin
      )
      withQueryAQLConn(answerAqls.map(aql => aql.trim -> emptyKeyCountResponse).toMap) { conn =>

        val probeSender = new TestProbe(system)
        val probeSource = new TestProbe(system)
        val viewActor = system.actorOf(Props(classOf[TwitterKeywordViewActor],
                                             conn, queryTemplate, key, probeSource.ref, fViewRecord, cloudberryConfig, ec))
        probeSender.send(viewActor, keywordQuery1)
        val actualMessage = probeSender.receiveOne(5000 millis)
        probeSource.expectNoMsg()
        actualMessage must_!= (null)
      }
    }

    "should ask the rest of the keyword if keywords are more than one" in {
      val key = "trump"
      val viewRecord = ViewMetaRecord("twitter", "twitter_" + key, SummaryLevel.Detail, startTime, lastVisitTime, lastUpdateTime, visitTimes, updateCycle)
      val fViewRecord = Future(viewRecord)
      val probeSender = new TestProbe(system)
      val probeSource = new TestProbe(system)


      val answerAqls = Seq(
        """
          |use dataverse twitter
          |let $common := (
          |for $t in dataset twitter_trump
          |
          |where
          |
          |($t."create_at">= datetime("2012-01-01T00:00:00.000Z")
          |and $t."create_at" <= datetime("2012-01-08T00:00:00.000Z"))
          |
          |
          |
          |for $sid in [ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50 ]
          |where $t.geo_tag.stateID = $sid
          |
          |where similarity-jaccard(word-tokens($t."text"), word-tokens("hilary")) > 0.0
          |return $t
          |)
          |
          |let $map := (
          |for $t in $common
          |
          |group by $c := $t.geo_tag.stateID with $t
          |return { "key": string($c) , "count": count($t) }
          |
          |)
          |return $map
          | """.stripMargin
        ,
        """
          |use dataverse twitter
          |let $common := (
          |for $t in dataset twitter_trump
          |
          |where
          |
          |($t."create_at">= datetime("2012-01-01T00:00:00.000Z")
          |and $t."create_at" <= datetime("2012-01-08T00:00:00.000Z"))
          |
          |
          |
          |for $sid in [ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50 ]
          |where $t.geo_tag.stateID = $sid
          |
          |where similarity-jaccard(word-tokens($t."text"), word-tokens("hilary")) > 0.0
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
          | """.stripMargin
        ,
        """
          |use dataverse twitter
          |let $common := (
          |for $t in dataset twitter_trump
          |
          |where
          |
          |($t."create_at">= datetime("2012-01-01T00:00:00.000Z")
          |and $t."create_at" <= datetime("2012-01-08T00:00:00.000Z"))
          |
          |
          |
          |for $sid in [ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50 ]
          |where $t.geo_tag.stateID = $sid
          |
          |where similarity-jaccard(word-tokens($t."text"), word-tokens("hilary")) > 0.0
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
          | """.stripMargin
      )
      withQueryAQLConn(answerAqls.map(aql => aql.trim -> emptyKeyCountResponse).toMap) { conn =>
        val viewActor = system.actorOf(Props(classOf[TwitterKeywordViewActor],
                                             conn, queryTemplate, key, probeSource.ref, fViewRecord, cloudberryConfig, ec))
        probeSender.send(viewActor, keywordQuery2)
        val actualMessage = probeSender.receiveOne(5000 millis)
        probeSource.expectNoMsg()
        actualMessage must_!= (null)
      }
    }

  }
}
