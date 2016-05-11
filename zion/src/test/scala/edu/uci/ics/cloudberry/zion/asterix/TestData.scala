package edu.uci.ics.cloudberry.zion.asterix

import edu.uci.ics.cloudberry.zion.model._
import org.joda.time.{DateTime, Duration, Interval, Weeks}
import play.api.libs.json.{JsArray, JsObject, Json}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait TestData {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val startTime1 = new DateTime(2012, 1, 1, 0, 0)
  val interval1 = new Interval(startTime1, Weeks.weeks(1))

  val startTime2 = new DateTime(2016, 1, 1, 0, 0)
  val interval2 = new Interval(startTime2, Weeks.weeks(2))

  import TwitterDataStoreActor._

  val timePredicate1 = TimePredicate(FieldCreateAt, Seq[Interval](interval1))
  val timePredicate2 = TimePredicate(FieldCreateAt, Seq[Interval](interval1, interval2))

  val idPredicate = IdSetPredicate(FieldStateID, Seq(1, 2, 3))

  val keywordPredicate1 = KeywordPredicate(FieldKeyword, Seq("trump"))

  val keywordPredicate2 = KeywordPredicate(FieldKeyword, Seq("trump", "hilary"))

  val startTime = new DateTime(0)
  val lastVisitTime = new DateTime()
  val lastUpdateTime = new DateTime(lastVisitTime.minus(Duration.standardDays(1)))
  val visitTimes = 0
  val updateCycle = 30 minutes

  // Create by state, by day request
  val StateDaySummary = SummaryLevel(SpatialLevels.State, TimeLevels.Day)
  val byStateByDayQuery = DBQuery(StateDaySummary, Seq(timePredicate1, idPredicate))
  val stateResult = Seq[KeyCountPair](KeyCountPair("1", 1), KeyCountPair("2", 2), KeyCountPair("3", 3))
  val dayResult = Seq[KeyCountPair](KeyCountPair("2012-01-01", 1), KeyCountPair("2012-01-02", 2))
  val hashTagResult = Seq[KeyCountPair](KeyCountPair("youShallPass", 100))
  val byStateByDayResult = SpatialTimeCount(stateResult, dayResult, hashTagResult)
  val byStateByDayResponse = JsArray(Seq(stateResult, dayResult, hashTagResult).map(Json.toJson(_)))

  // Create by county, by month request
  val CountyMonthSummary = SummaryLevel(SpatialLevels.County, TimeLevels.Month)
  val byCountyMonthQuery = DBQuery(CountyMonthSummary, Seq(timePredicate1, idPredicate))
  val monthResult = Seq[KeyCountPair](KeyCountPair("2012-01", 1), KeyCountPair("2012--02", 2))
  val byCountyMonthResult = SpatialTimeCount(stateResult, monthResult, hashTagResult)
  val byCountyMonthResponse = JsArray(Seq(stateResult, monthResult, hashTagResult).map(Json.toJson(_)))

  // Create a partially intersected time range db query
  val partialTime = TimePredicate(FieldCreateAt, Seq(new Interval(startTime1, new DateTime())))
  val partialQuery = DBQuery(StateDaySummary, Seq(partialTime))

  // Create a finner summary level db query
  val finerQuery = DBQuery(SummaryLevel(SpatialLevels.City, TimeLevels.Second), Seq(timePredicate1))

  // Create a keyword query
  val keywordQuery = DBQuery(StateDaySummary, Seq(keywordPredicate1, timePredicate1, idPredicate))
}
