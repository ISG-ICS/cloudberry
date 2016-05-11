package edu.uci.ics.cloudberry.zion.asterix

import akka.actor.{Actor, ActorRef, Props}
import akka.testkit.TestProbe
import edu.uci.ics.cloudberry.zion.actor._
import edu.uci.ics.cloudberry.zion.model.{DBQuery, SpatialTimeCount}
import org.joda.time.{DateTime, Duration}
import org.specs2.matcher.MatchResult
import org.specs2.mutable.SpecificationLike
import play.api.libs.json.{JsArray, JsObject, JsValue}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class TwitterCountyDaySummaryViewTest extends TestkitExample with SpecificationLike with MockConnClient with TestData {

  //It's usually safer to run the tests sequentially for Actors
  import TwitterCountyDaySummaryView._

  sequential

  val queryUpdateTemp: DBQuery = DBQuery(SummaryLevel, Seq.empty)
  val viewRecord = ViewMetaRecord("twitter", "rain", SummaryLevel, startTime, lastVisitTime, lastUpdateTime, visitTimes, updateCycle)
  val fViewRecord = Future(viewRecord)

  "TwitterCountyDaySummaryView" should {

    val probeSender = new TestProbe(system)
    val probeSource = new TestProbe(system)

    def runSummaryView(dbQuery: DBQuery, wsResponse: JsArray, result: SpatialTimeCount): MatchResult[Any] = {

      withAsterixBugConn(wsResponse) { conn =>
        val viewActor = system.actorOf(Props(classOf[TwitterCountyDaySummaryView],
                                             conn, queryUpdateTemp, probeSource.ref, fViewRecord, ec))
        probeSender.send(viewActor, dbQuery)
        val actualMessage = probeSender.receiveOne(500 millis)
        probeSource.expectNoMsg()
        actualMessage must_== result
      }
    }

    "answer the state summary query by aggregate those counties" in {
      runSummaryView(byStateByDayQuery, byStateByDayResponse, byStateByDayResult)
    }
    "answer the month summary query by aggregate those days " in {
      runSummaryView(byCountyMonthQuery, byCountyMonthResponse, byCountyMonthResult)
    }
    "split the query to ask the source if can not answer by view only" in {
      withAsterixBugConn(byStateByDayResponse) { conn =>
        val viewActor = system.actorOf(Props(classOf[TwitterCountyDaySummaryView],
                                             conn, queryUpdateTemp, probeSource.ref, fViewRecord, ec))
        probeSender.send(viewActor, partialQuery)
        probeSource.expectMsgClass(classOf[DBQuery])
        probeSource.reply(byCountyMonthResult)
        val actualMessage = probeSender.receiveOne(500 millis)
        actualMessage must_== TwitterDataStoreActor.mergeResult(byStateByDayResult, byCountyMonthResult)
      }
    }
    "ask the source directly if the summary level does not fit" in {
      val conn: AsterixConnection = null // it shall not be touched
      val viewActor = system.actorOf(Props(classOf[TwitterCountyDaySummaryView],
                                           conn, queryUpdateTemp, probeSource.ref, fViewRecord, ec))
      probeSender.send(viewActor, finerQuery)
      probeSource.expectMsgClass(classOf[DBQuery])
      probeSource.reply(byCountyMonthResult)
      val actualMessage = probeSender.receiveOne(500 millis)
      actualMessage must_== byCountyMonthResult
    }
    "return the view record to parent when receive the update msg" in {
      val conn: AsterixConnection = null // it shall not be touched
      val proxy = new TestProbe(system)
      val parent = system.actorOf(Props(new Actor {
        val viewActor = context.actorOf(Props(classOf[TwitterCountyDaySummaryView],
                                              conn, queryUpdateTemp, probeSource.ref, fViewRecord, ec))

        def receive = {
          case x if sender == viewActor => proxy.ref forward x
          case x => viewActor forward x
        }
      }))
      proxy.send(parent, ViewActor.UpdateViewMsg)
      val actualMsg = proxy.receiveOne(100 millis).asInstanceOf[ViewMetaRecord]
      // except the updateTime everything should be equal
      val unifyTime = new DateTime()
      actualMsg.copy(lastUpdateTime = unifyTime) must_== (viewRecord.copy(lastUpdateTime = unifyTime))
    }
  }

  "TwitterCountyDaySummaryView#generateAQL" should {
    "as expected" in {
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
                          |return { "key": string($c) , "count": sum(for $x in $t return $x.tweetCount) }
                          |
                          |)
                          |return $map;
                          |
                          |
                          |use dataverse twitter
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
                          |let $time := (
                          |for $t in $common
                          |
                          |group by $c := print-datetime(get-interval-start($t.timeBin), "YYYY-MM-DD") with $t
                          |return { "key" : $c, "count": sum(for $x in $t return $x.tweetCount)}
                          |
                          |)
                          |return $time
                          |
                          |
                          |use dataverse twitter
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
                          |return $hashtag
                          |""".stripMargin.trim)
    }

  }
}
