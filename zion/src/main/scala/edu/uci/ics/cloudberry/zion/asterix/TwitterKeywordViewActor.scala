package edu.uci.ics.cloudberry.zion.asterix

import akka.actor.{ActorRef, Props}
import edu.uci.ics.cloudberry.zion.actor.{ViewActor, ViewMetaRecord}
import edu.uci.ics.cloudberry.zion.model._
import org.joda.time.Interval

import scala.concurrent.{ExecutionContext, Future}

class TwitterKeywordViewActor(val conn: AsterixConnection,
                              val queryTemplate: DBQuery,
                              val keyword: String,
                              override val sourceActor: ActorRef,
                              fViewStore: Future[ViewMetaRecord])(implicit ec: ExecutionContext) extends ViewActor(sourceActor, fViewStore) {

  import TwitterDataStoreActor._

  override def createSourceQuery(initQuery: DBQuery, unCovered: Seq[Interval]): DBQuery = {
    val newTimes = TimePredicate(FieldCreateAt, unCovered)
    val keywordPredicate = KeywordPredicate(FieldKeyword, Seq(this.keyword))
    initQuery.copy(predicates = Seq(newTimes, keywordPredicate))
  }

  override def mergeResult(viewResponse: Response, sourceResponse: Response): Response = {
    val viewCount = viewResponse.asInstanceOf[SpatialTimeCount]
    val sourceCount = sourceResponse.asInstanceOf[SpatialTimeCount]
    TwitterDataStoreActor.mergeResult(viewCount, sourceCount)
  }

  override def updateView(): Future[Unit] = ???

  override def askViewOnly(query: DBQuery): Future[Response] = {
    conn.post(generateAQL(key, query)).map(handleAllInOneWSResponse)
  }

}

object TwitterKeywordViewActor {
}
