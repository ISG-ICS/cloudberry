package edu.uci.ics.cloudberry.zion.asterix

import akka.actor.{Actor, ActorRef, Props}
import akka.testkit.TestProbe
import edu.uci.ics.cloudberry.zion.actor._
import edu.uci.ics.cloudberry.zion.model._
import org.joda.time.{DateTime, Duration, Period}
import org.specs2.matcher.MatchResult
import org.specs2.mutable.SpecificationLike
import play.api.libs.json.{JsArray, JsObject, JsValue}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class TwitterCountyDaySummaryViewTest extends TestkitExample with SpecificationLike with MockConnClientOld with TestData {

  import TwitterCountyDaySummaryView._

  //It's usually safer to run the tests sequentially for Actors
  sequential

  val queryUpdateTemp: DBQuery = new DBQuery(SummaryLevel, Seq.empty)
  val viewRecord = ViewMetaRecord("twitter", "ds_tweet_", SummaryLevel, startTime, lastVisitTime, lastUpdateTime, visitTimes, updateCycle)
  val fViewRecord = Future(viewRecord)

  "TwitterCountyDaySummaryView" should {

    def runSummaryView(dbQuery: DBQuery, aql2json: Map[String,JsValue], result: SpatialTimeCount): MatchResult[Any] = {

      val probeSender = new TestProbe(system)
      val probeSource = new TestProbe(system)

      withQueryAQLConn(aql2json) { conn =>
        val viewActor = system.actorOf(Props(classOf[TwitterCountyDaySummaryView],
                                             conn, queryUpdateTemp, probeSource.ref, fViewRecord, cloudberryConfig, ec))
        probeSender.send(viewActor, dbQuery)
        // for some reason, the first Actor request is very slow.
        val actualMessage = probeSender.receiveOne(5 seconds)
        probeSource.expectNoMsg()
        actualMessage must_== result
      }
    }

    "answer the state summary query by aggregate those counties" in {
      runSummaryView(byStateByDayQuery, byStateByDayAQLMap, byStateByDayResult)
    }
    "answer the month summary query by aggregate those days " in {
      runSummaryView(byCountyMonthQuery, byCountyMonthAQLMap, byCountyMonthResult)
    }
    "split the query to ask the source if can not answer by view only" in {

      val probeSender = new TestProbe(system)
      val probeSource = new TestProbe(system)
      withQueryAQLConn(partialQueryAQL2JsonMap) { conn =>
        val viewActor = system.actorOf(Props(classOf[TwitterCountyDaySummaryView],
                                             conn, queryUpdateTemp, probeSource.ref, fViewRecord, cloudberryConfig, ec))
        probeSender.send(viewActor, partialQuery)
        probeSource.expectMsgClass(classOf[DBQuery])
        probeSource.reply(byCountyMonthResult)
        val actualMessage = probeSender.receiveOne(500 millis)
        actualMessage must_== TwitterDataStoreActor.mergeResult(byStateByDayResult, byCountyMonthResult)
      }
    }
    "ask the source directly if the summary level does not fit" in {

      val probeSender = new TestProbe(system)
      val probeSource = new TestProbe(system)
      val conn: AsterixConnection = null // it shall not be touched
      val viewActor = system.actorOf(Props(classOf[TwitterCountyDaySummaryView],
                                           conn, queryUpdateTemp, probeSource.ref, fViewRecord, cloudberryConfig, ec))
      probeSender.send(viewActor, finerQuery)
      probeSource.expectMsgClass(classOf[DBQuery])
      probeSource.reply(byCountyMonthResult)
      val actualMessage = probeSender.receiveOne(500 millis)
      actualMessage must_== byCountyMonthResult
    }
    "update the views if receives the update msg" in {

      val probeSource = new TestProbe(system)
      withSucceedUpdateAQLConn { conn =>
        val proxy = new TestProbe(system)
        val parent = system.actorOf(Props(new Actor {
          val viewActor = context.actorOf(Props(classOf[TwitterCountyDaySummaryView],
                                                conn, queryUpdateTemp, probeSource.ref, fViewRecord, cloudberryConfig, ec))

          def receive = {
            case x if sender == viewActor => proxy.ref forward x
            case x => viewActor forward x
          }
        }))

        proxy.send(parent, ViewActor.UpdateViewMsg)
        val newViewRecord = proxy.receiveOne(200 millis).asInstanceOf[ViewMetaRecord]
        new Period(newViewRecord.lastUpdateTime.getMillis, DateTime.now.getMillis).getMillis must be_<(300)
      }
    }
  }

  "TwitterCountyDaySummaryView#generateAQL" should {
    "as expected" in {
      val dbQuery = new DBQuery(new SummaryLevel(SpatialLevels.State, TimeLevels.Day), Seq(idPredicate, keywordPredicate2, timePredicate2))
      val name = "ds_tweet_"
      TwitterCountyDaySummaryView.generateByMapAQL(name, dbQuery).trim must_==
        """
          |use dataverse twitter
          |let $common := (
          |for $t in dataset ds_tweet_
          |
          |for $sid in [ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50 ]
          |where $t.stateID = $sid
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
          |group by $c := $t.stateID with $t
          |return { "key": string($c) , "count": sum(for $x in $t return $x.tweetCount) }
          |
          |)
          |return $map;
          | """.stripMargin.trim

      TwitterCountyDaySummaryView.generateByTimeAQL(name, dbQuery).trim must_== (
        """
          |use dataverse twitter
          |let $common := (
          |for $t in dataset ds_tweet_
          |
          |for $sid in [ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50 ]
          |where $t.stateID = $sid
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
          | """.stripMargin.trim)

      TwitterCountyDaySummaryView.generateByHashtagAQL(name, dbQuery).trim must_== (
        """
          |use dataverse twitter
          |let $common := (
          |for $t in dataset ds_tweet_
          |
          |for $sid in [ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50 ]
          |where $t.stateID = $sid
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
          | """.stripMargin.trim)
    }

  }
}
