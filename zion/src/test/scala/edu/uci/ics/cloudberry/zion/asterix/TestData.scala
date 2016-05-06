package edu.uci.ics.cloudberry.zion.asterix

import edu.uci.ics.cloudberry.zion.model.{IdSetPredicate, KeywordPredicate, TimePredicate}
import org.joda.time.{DateTime, Interval, Weeks}

trait TestData {
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
}
