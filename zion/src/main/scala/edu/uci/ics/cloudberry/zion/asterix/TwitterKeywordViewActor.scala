package edu.uci.ics.cloudberry.zion.asterix

import akka.actor.{ActorRef, Props}
import edu.uci.ics.cloudberry.zion.actor.{ViewActor, ViewMetaRecord}
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model._
import org.joda.time.{DateTime, Interval}
import play.api.libs.ws.WSResponse

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class TwitterKeywordViewActor(val conn: AsterixConnection,
                              val queryTemplate: DBQuery,
                              val keyword: String,
                              override val sourceActor: ActorRef,
                              fViewStore: Future[ViewMetaRecord],
                              config: Config
                             )(implicit ec: ExecutionContext)
  extends ViewActor(sourceActor, fViewStore, config) {

  import TwitterDataStoreActor._

  override def createSourceQuery(initQuery: DBQuery, unCovered: Seq[Interval]): DBQuery = {
    val newTimes = TimePredicate(FieldCreateAt, unCovered)
    val others = initQuery.predicates.filter(p => !p.isInstanceOf[TimePredicate])
    new DBQuery(initQuery.summaryLevel, others :+ newTimes)
  }

  override def mergeResult(viewResponse: Response, sourceResponse: Response): Response = {
    viewResponse match {
      case v: SpatialTimeCount =>
        val viewCount = viewResponse.asInstanceOf[SpatialTimeCount]
        val sourceCount = sourceResponse.asInstanceOf[SpatialTimeCount]
        TwitterDataStoreActor.mergeResult(viewCount, sourceCount)
      case v: SampleList =>
        v
    }
  }

  override def updateView(from: DateTime, to: DateTime): Future[Unit] = {
    val aql = TwitterViewsManagerActor.generateKeywordUpdateAQL(sourceName, key, keyword, from, to)
    conn.postUpdate(aql).map[Unit] { succeed: Boolean =>
      if (!succeed) {
        throw UpdateFailedDBException(key + ", keyword is:" + keyword)
      }
    }
  }

  override def askViewOnly(query: DBQuery): Future[Response] = {
    val originKeywordP = query.predicates.find(_.isInstanceOf[KeywordPredicate]).map(_.asInstanceOf[KeywordPredicate]).get
    val keywords = originKeywordP.keywords.filterNot(_ == keyword)
    val newPred =
      if (keywords.length == 0) {
        query.predicates.filterNot(_.isInstanceOf[KeywordPredicate])
      } else {
        query.predicates.filterNot(_.isInstanceOf[KeywordPredicate]) :+ KeywordPredicate(originKeywordP.fieldName, keywords)
      }

    query match {
      case q: SampleQuery =>
        conn.postQuery(generateSampleAQL(key, q.copy(predicates = newPred))).map(handleSampleResponse)
      case q: DBQuery =>
        askAsterixAndGetAllResponse(conn, key, new DBQuery(q.summaryLevel, newPred))
    }
  }

  override def updateInterval: FiniteDuration = config.ViewUpdateInterval
}

object TwitterKeywordViewActor {
}
