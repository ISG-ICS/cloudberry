package edu.uci.ics.cloudberry.zion.asterix

import java.util.concurrent.Executors

import edu.uci.ics.cloudberry.zion.actor.ViewMetaRecord
import edu.uci.ics.cloudberry.zion.model._
import org.joda.time.{Duration, _}
import play.api.libs.json.{JsArray, JsObject, Json}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait TestData {
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))

  DateTimeZone.setDefault(DateTimeZone.UTC)
  val startTime1 = new DateTime(2012, 1, 1, 0, 0)
  val interval1 = new Interval(startTime1, Weeks.weeks(1))

  val startTime2 = new DateTime(2016, 1, 1, 0, 0)
  val interval2 = new Interval(startTime2, Weeks.weeks(2))

  import TwitterDataStoreActor._

  val timePredicate1 = TimePredicate(FieldCreateAt, Seq[Interval](interval1))
  val timePredicate2 = TimePredicate(FieldCreateAt, Seq[Interval](interval1, interval2))

  val idPredicate = IdSetPredicate(FieldStateID, 1 to 50 toList)

  val keywordPredicate1 = KeywordPredicate(FieldKeyword, Seq("trump"))

  val keywordPredicate2 = KeywordPredicate(FieldKeyword, Seq("trump", "hilary"))

  val startTime = new DateTime(0)
  val lastVisitTime = new DateTime()
  val lastUpdateTime = new DateTime(lastVisitTime.minus(Duration.standardDays(1)))
  val visitTimes = 0
  val updateCycle = 30 minutes

  // Create by state, by day request
  val StateDaySummary = SummaryLevel(SpatialLevels.State, TimeLevels.Day)
  val byStateByDayQuery = new DBQuery(StateDaySummary, Seq(timePredicate1, idPredicate))
  val stateResult = Seq[KeyCountPair](KeyCountPair("1", 1), KeyCountPair("2", 2), KeyCountPair("3", 3))
  val dayResult = Seq[KeyCountPair](KeyCountPair("2012-01-01", 1), KeyCountPair("2012-01-02", 2))
  val hashTagResult = Seq[KeyCountPair](KeyCountPair("youShallPass", 100))
  val byStateByDayResult = SpatialTimeCount(stateResult, dayResult, hashTagResult)
  val byStateByDateMapAQL =
    s"""
       |
       |use dataverse twitter
       |let $$common := (
       |for $$t in dataset ds_tweet_
       |
       |where
       |
       |(get-interval-start($$t.timeBin) >= datetime("2012-01-01T00:00:00.000Z")
       |and get-interval-start($$t.timeBin) < datetime("2012-01-08T00:00:00.000Z"))
       |
       |
       |
       |for $$sid in [ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50 ]
       |where $$t.stateID = $$sid
       |
       |return $$t
       |)
       |
       |let $$map := (
       |for $$t in $$common
       |
       |group by $$c := $$t.stateID with $$t
       |return { "key": string($$c) , "count": sum(for $$x in $$t return $$x.tweetCount) }
       |
       |)
       |return $$map;
     """.stripMargin.trim
  val byStateByDayTimeAQL =
    s"""
       |use dataverse twitter
       |let $$common := (
       |for $$t in dataset ds_tweet_
       |
       |where
       |
        |(get-interval-start($$t.timeBin) >= datetime("2012-01-01T00:00:00.000Z")
       |and get-interval-start($$t.timeBin) < datetime("2012-01-08T00:00:00.000Z"))
       |
       |
       |
       |for $$sid in [ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50 ]
       |where $$t.stateID = $$sid
       |
       |return $$t
       |)
       |
       |let $$time := (
       |for $$t in $$common
       |
       |group by $$c := print-datetime(get-interval-start($$t.timeBin), "YYYY-MM-DD") with $$t
       |return { "key" : $$c, "count": sum(for $$x in $$t return $$x.tweetCount)}
       |
       |)
       |return $$time
     """.stripMargin.trim
  val byStateByDayHashtagAQL =
    s"""
       |use dataverse twitter
       |let $$common := (
       |for $$t in dataset ds_tweet_
       |
       |where
       |
       |(get-interval-start($$t.timeBin) >= datetime("2012-01-01T00:00:00.000Z")
       |and get-interval-start($$t.timeBin) < datetime("2012-01-08T00:00:00.000Z"))
       |
       |
       |
       |for $$sid in [ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50 ]
       |where $$t.stateID = $$sid
       |
       |return $$t
       |)
       |
       |let $$hashtag := (
       |for $$t in $$common
       |
       |for $$h in $$t.topHashTags
       |group by $$tag := $$h.tag with $$h
       |let $$c := sum(for $$x in $$h return $$x.count)
       |order by $$c desc
       |limit 50
       |return { "key": $$tag, "count" : $$c}
       |
       |)
       |return $$hashtag
     """.stripMargin.trim
  val byStateByDayAQLMap = Seq(byStateByDateMapAQL -> Seq(stateResult),
                               byStateByDayTimeAQL -> Seq(dayResult),
                               byStateByDayHashtagAQL -> Seq(hashTagResult)).toMap.mapValues(Json.toJson(_))

  // Create by county, by month request
  val CountyMonthSummary = SummaryLevel(SpatialLevels.County, TimeLevels.Month)
  val byCountyMonthQuery = new DBQuery(CountyMonthSummary, Seq(timePredicate1, idPredicate))
  val monthResult = Seq[KeyCountPair](KeyCountPair("2012-01", 1), KeyCountPair("2012-02", 2))
  val byCountyMonthResult = SpatialTimeCount(stateResult, monthResult, hashTagResult)
  val byCountyMonthMapAQL =
    s"""
       |use dataverse twitter
       |let $$common := (
       |for $$t in dataset ds_tweet_
       |
       |where
       |
       |(get-interval-start($$t.timeBin) >= datetime("2012-01-01T00:00:00.000Z")
       |and get-interval-start($$t.timeBin) < datetime("2012-01-08T00:00:00.000Z"))
       |
       |
       |
       |for $$sid in [ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50 ]
       |where $$t.countyID = $$sid
       |
       |return $$t
       |)
       |
       |let $$map := (
       |for $$t in $$common
       |
       |group by $$c := $$t.countyID with $$t
       |return { "key": string($$c) , "count": sum(for $$x in $$t return $$x.tweetCount) }
       |
       |)
       |return $$map;
       |""".stripMargin.trim
  val byCountyMonthTimeAQL =
    s"""
       |use dataverse twitter
       |let $$common := (
       |for $$t in dataset ds_tweet_
       |
       |where
       |
       |(get-interval-start($$t.timeBin) >= datetime("2012-01-01T00:00:00.000Z")
       |and get-interval-start($$t.timeBin) < datetime("2012-01-08T00:00:00.000Z"))
       |
       |
       |
       |for $$sid in [ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50 ]
       |where $$t.countyID = $$sid
       |
       |return $$t
       |)
       |
       |let $$time := (
       |for $$t in $$common
       |
       |group by $$c := print-datetime(get-interval-start($$t.timeBin), "YYYY-MM") with $$t
       |return { "key" : $$c, "count": sum(for $$x in $$t return $$x.tweetCount)}
       |
       |)
       |return $$time
     """.stripMargin.trim
  val byCountyMonthHashtagAQL =
    s"""
       |use dataverse twitter
       |let $$common := (
       |for $$t in dataset ds_tweet_
       |
       |where
       |
       |(get-interval-start($$t.timeBin) >= datetime("2012-01-01T00:00:00.000Z")
       |and get-interval-start($$t.timeBin) < datetime("2012-01-08T00:00:00.000Z"))
       |
       |
       |
       |for $$sid in [ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50 ]
       |where $$t.countyID = $$sid
       |
       |return $$t
       |)
       |
       |let $$hashtag := (
       |for $$t in $$common
       |
       |for $$h in $$t.topHashTags
       |group by $$tag := $$h.tag with $$h
       |let $$c := sum(for $$x in $$h return $$x.count)
       |order by $$c desc
       |limit 50
       |return { "key": $$tag, "count" : $$c}
       |
       |)
       |return $$hashtag
     """.stripMargin.trim

  val byCountyMonthAQLMap = Seq(byCountyMonthMapAQL -> Seq(stateResult),
                                byCountyMonthTimeAQL -> Seq(monthResult),
                                byCountyMonthHashtagAQL -> Seq(hashTagResult)).toMap.mapValues(Json.toJson(_))

  val emptyKeyCountResponse = Json.toJson(Seq(Seq.empty[KeyCountPair]))

  // Create a partially intersected time range db query
  val nowTime = new DateTime()
  val formatNow = AQLVisitor.TimeFormat.print(nowTime)
  val partialTime = TimePredicate(FieldCreateAt, Seq(new Interval(startTime1, nowTime)))
  val partialQuery = new DBQuery(StateDaySummary, Seq(partialTime))

  val partialQueryMapAQL =
    s"""
       |use dataverse twitter
       |let $$common := (
       |for $$t in dataset ds_tweet_
       |
       |where
       |
       |(get-interval-start($$t.timeBin) >= datetime("2012-01-01T00:00:00.000Z")
       |and get-interval-start($$t.timeBin) < datetime("$formatNow"))
       |
       |
       |return $$t
       |)
       |
       |let $$map := (
       |for $$t in $$common
       |
       |group by $$c := $$t.stateID with $$t
       |return { "key": string($$c) , "count": sum(for $$x in $$t return $$x.tweetCount) }
       |
       |)
       |return $$map;
     """.stripMargin.trim
  val partialQueryTimeAQL =
    s"""
       |use dataverse twitter
       |let $$common := (
       |for $$t in dataset ds_tweet_
       |
       |where
       |
       |(get-interval-start($$t.timeBin) >= datetime("2012-01-01T00:00:00.000Z")
       |and get-interval-start($$t.timeBin) < datetime("$formatNow"))
       |
       |
       |return $$t
       |)
       |
       |let $$time := (
       |for $$t in $$common
       |
       |group by $$c := print-datetime(get-interval-start($$t.timeBin), "YYYY-MM-DD") with $$t
       |return { "key" : $$c, "count": sum(for $$x in $$t return $$x.tweetCount)}
       |
       |)
       |return $$time
     """.stripMargin.trim
  val partialQueryHashtagAQL =
    s"""
       |use dataverse twitter
       |let $$common := (
       |for $$t in dataset ds_tweet_
       |
       |where
       |
       |(get-interval-start($$t.timeBin) >= datetime("2012-01-01T00:00:00.000Z")
       |and get-interval-start($$t.timeBin) < datetime("$formatNow"))
       |
       |
       |return $$t
       |)
       |
       |let $$hashtag := (
       |for $$t in $$common
       |
       |for $$h in $$t.topHashTags
       |group by $$tag := $$h.tag with $$h
       |let $$c := sum(for $$x in $$h return $$x.count)
       |order by $$c desc
       |limit 50
       |return { "key": $$tag, "count" : $$c}
       |
       |)
       |return $$hashtag
     """.stripMargin.trim

  val partialQueryAQL2JsonMap = Seq(partialQueryMapAQL -> Seq(stateResult),
                                    partialQueryTimeAQL -> Seq(dayResult),
                                    partialQueryHashtagAQL -> Seq(hashTagResult)).toMap.mapValues(Json.toJson(_))

  // Create a finner summary level db query
  val finerQuery = new DBQuery(SummaryLevel(SpatialLevels.City, TimeLevels.Second), Seq(timePredicate1))

  // Create a keyword query
  val keywordQuery1 = new DBQuery(StateDaySummary, Seq(keywordPredicate1, timePredicate1, idPredicate))
  val keywordQuery2 = new DBQuery(StateDaySummary, Seq(keywordPredicate2, timePredicate1, idPredicate))

  val viewMetaR1 = ViewMetaRecord("twitter", "twitter_", TwitterCountyDaySummaryView.SummaryLevel,
                                  new DateTime(0), new DateTime(5000), new DateTime(4000), 0, 1 hours)
  val viewMetaR2 = viewMetaR1.copy(viewKey = "twitter_trump")
  val viewMetaR3 = viewMetaR1.copy(viewKey = "twitter_rain", visitTimes = 20)
  val testRecords = Seq[ViewMetaRecord](viewMetaR1, viewMetaR2, viewMetaR3)
}
