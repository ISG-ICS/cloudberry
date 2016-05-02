package edu.uci.ics.cloudberry.zion.asterix

import edu.uci.ics.cloudberry.zion.api._
import org.joda.time.{DateTime, Interval}

import scala.concurrent.Future

trait AbstractTwitterView {
  def source: TwitterDataStore

  def startTime: DateTime

  def lastUpdateTime: DateTime

  import TwitterDataStore._

  def mergeResult(viewResponse: SpatialTimeCount, sourceResponse: SpatialTimeCount): SpatialTimeCount = {
    SpatialTimeCount(viewResponse.map ++ sourceResponse.map,
                     viewResponse.time ++ sourceResponse.time,
                     viewResponse.hashtag ++ sourceResponse.hashtag)
  }

  protected def usingViewOnly(query: DBQuery): Future[Response]

  protected def createSourceQuery(initQuery: DBQuery, unCovered: Seq[Interval]): DBQuery

  def query(query: DBQuery): Future[Response] = {
    val requiredTime = query.predicates.find(_.isInstanceOf[TimePredicate]).map(_.asInstanceOf[TimePredicate].intervals).get
    val unCovered = getTimeRangeDifference(new Interval(startTime, lastUpdateTime), requiredTime)
    val fViewResponse = usingViewOnly(query)
    if (unCovered.isEmpty) {
      return fViewResponse
    } else {
      val sourceQuery = createSourceQuery(query, unCovered)
      val fSource = source.query(sourceQuery)
      for {
        viewResponse <- fViewResponse.mapTo[SpatialTimeCount]
        sourceResponse <- fSource.mapTo[SpatialTimeCount]
      } yield mergeResult(viewResponse, sourceResponse)
    }
  }

}
