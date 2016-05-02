package edu.uci.ics.cloudberry.zion.asterix

import edu.uci.ics.cloudberry.zion.api._
import org.joda.time.{DateTime, Interval}

import scala.concurrent.Future
import scala.concurrent.duration._

class TwitterKeywordView(val conn: AsterixConnection,
                         val source: TwitterDataStore,
                         val keyword: String,
                         val queryTemplate: DBQuery,
                         val startTime: DateTime,
                         var lastVisitTime: DateTime,
                         var lastUpdateTime: DateTime,
                         var visitTimes: Int,
                         val updateCycle: Duration = 1 hours
                        ) extends TwitterDataStore(conn) with AbstractTwitterView with SubSetView {

  import TwitterDataStore._

  override val name: String = source.name + "_" + keyword

  override def update(query: DBUpdateQuery): Future[Response] = ???

  override protected def usingViewOnly(query: DBQuery): Future[Response] = super[TwitterDataStore].query(query)

  override def query(query: DBQuery): Future[Response] = super[AbstractTwitterView].query(query)

  override protected def createSourceQuery(initQuery: DBQuery, unCovered: Seq[Interval]): DBQuery = {
    val newTimes = TimePredicate(FieldCreateAt, unCovered)
    val keywordPredicate = KeywordPredicate(FieldKeyword, Seq(this.keyword))
    initQuery.copy(predicates = Seq(newTimes, keywordPredicate))
  }
}
