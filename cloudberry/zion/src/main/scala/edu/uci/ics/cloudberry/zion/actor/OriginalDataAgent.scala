package edu.uci.ics.cloudberry.zion.actor

import akka.actor.Props
import akka.pattern.pipe
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQLGenerator}
import edu.uci.ics.cloudberry.zion.model.impl.DataSetInfo
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.{DateTime, Duration, Interval}
import play.api.libs.json._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

class OriginalDataAgent(val dataSetInfo: DataSetInfo,
                        override val queryParser: IQLGenerator,
                        override val conn: IDataConn,
                        override val config: Config)(implicit ec: ExecutionContext)
  extends AbstractDataSetAgent(dataSetInfo.name, dataSetInfo.schema.asInstanceOf[Schema], queryParser, conn, config)(ec) {

  import OriginalDataAgent._

  val lastCount: Cardinality = Cardinality(
    dataSetInfo.dataInterval.getStart,
    dataSetInfo.dataInterval.getEnd,
    dataSetInfo.stats.cardinality
  )

  /**
    * When the Agent starts,
    *   1. Ask for the stats from maxTimeStamp till now,
    *   2. Start a scheduler to query for cardinality periodically.
    * Stats including: minTimeStamp, maxTimeStamp, cardinality.
    */
  override def preStart(): Unit = {
    context.system.scheduler.schedule(0 second, config.AgentCollectStatsInterval, self, UpdateStats)
  }

  /**
    * Estimate the result of the query w/o visiting DB
    * TODO collect more stats to solve more types of queries
    *
    * @param query
    * @return the estimated result. If it failed to estimate, return None
    */
  override protected def estimate(query: Query): Option[JsValue] = {
    if (estimable(query)) {
      val second = new Duration(lastCount.till, DateTime.now).getStandardSeconds
      val count = lastCount.count + second * lastCount.ratePerSecond
      val tag = query.globalAggr.get.aggregate.as
      Some(JsArray(Seq(Json.obj(tag.name -> JsNumber(count.toLong)))))
    } else {
      None
    }
  }

  override protected def maintenanceWork: Receive = {
    case newCount: Cardinality =>
      lastCount.reset(lastCount.from, newCount.till, lastCount.count + newCount.count, newCount.ratePerSecond)
      context.parent ! NewStats(dbName, newCount.count, new Interval(lastCount.from, newCount.till))
    case UpdateStats =>
      collectStats(lastCount.till)
  }

  private def collectStats(start: DateTime): Unit = {
    if (!dataSetInfo.schema.isInstanceOf[Schema]) {
      log.error("Cannot do aggregation query for lookup dataset " + dataSetInfo.schema.getTypeName)
      return
    }
    val temporalSchema = dataSetInfo.schema.asInstanceOf[Schema]
    val now = DateTime.now().minusMillis(1)
    val startFilter = FilterStatement(temporalSchema.timeField, None, Relation.>, Seq(start).map(TimeField.TimeFormat.print))
    val endFilter = FilterStatement(temporalSchema.timeField, None, Relation.<=, Seq(now).map(TimeField.TimeFormat.print))
    val aggr = GlobalAggregateStatement(AggregateStatement(temporalSchema.fieldMap("*"), Count, Field.as(Count(temporalSchema.fieldMap("*")), "count")))
    val queryCardinality = Query(dbName, filter = Seq(startFilter, endFilter), globalAggr = Some(aggr))
    conn.postQuery(queryParser.generate(queryCardinality, Map(dbName -> temporalSchema)))
      .map(r => Cardinality(start, now, (r \\ "count").head.as[Long])) pipeTo self
  }

  //TODO extend the logic of using stats to solve more queries
  private def estimable(query: Query): Boolean = {
    if (query.isEstimable &&
      query.groups.isEmpty &&
      query.lookup.isEmpty &&
      query.select.isEmpty &&
      query.unnest.isEmpty) {
      query.globalAggr.exists(g => g.aggregate.func.name == AggregateFunc.Count)
    } else {
      false
    }
  }
}

object OriginalDataAgent {

  object UpdateStats

  case class NewStats(dbName: String, additionalCount: Long, dataInterval: Interval)

  class Cardinality(var from: DateTime, var till: DateTime, var count: Long, var ratePerSecond: Int) {
    def reset(from: DateTime, till: DateTime, count: Long, rate: Int): Unit = {
      this.from = from
      this.till = till
      this.count = count
      this.ratePerSecond = rate
    }

    override def toString: String = s"Cardinality $count in [$from -- $till), rate: $ratePerSecond"
  }

  object Cardinality {
    def apply(from: DateTime, till: DateTime, count: Long): Cardinality = {
      val ratePerSecond = (count / new Duration(from, till).getStandardSeconds).toInt
      new Cardinality(from, till, count, ratePerSecond)
    }
  }

  def props(dataSetInfo: DataSetInfo, queryParser: IQLGenerator, conn: IDataConn, config: Config)
           (implicit ec: ExecutionContext) =
    Props(new OriginalDataAgent(dataSetInfo, queryParser, conn, config))
}
