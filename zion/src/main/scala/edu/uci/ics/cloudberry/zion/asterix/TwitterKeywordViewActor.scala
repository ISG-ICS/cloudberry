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
    val keywordPredicate = KeywordPredicate(FieldKeyword, Seq(this.keyword))
    val others = initQuery.predicates.filter(p => !p.isInstanceOf[TimePredicate] && !p.isInstanceOf[KeywordPredicate])
    new DBQuery(initQuery.summaryLevel, others :+ newTimes :+ keywordPredicate)
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
    conn.post(aql).map[Unit] { response: WSResponse =>
      if (response.status != 200) {
        throw UpdateFailedDBException(response.body)
      }
    }
  }

  override def askViewOnly(query: DBQuery): Future[Response] = {
    query match {
      case q: SampleQuery =>
        conn.post(generateSampleAQL(key, q)).map(handleSampleResponse)
      case q: DBQuery =>
        conn.post(generateAQL(key, q)).map(handleAllInOneWSResponse)
    }
  }

  override def updateInterval: FiniteDuration = config.ViewUpdateInterval
}

object TwitterKeywordViewActor {
}
