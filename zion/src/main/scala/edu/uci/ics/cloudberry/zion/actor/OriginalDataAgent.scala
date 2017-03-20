package edu.uci.ics.cloudberry.zion.actor

import akka.actor.Props
import akka.pattern.pipe
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQLGenerator}
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.{DateTime, Duration}
import play.api.libs.json._

import scala.concurrent.ExecutionContext

class OriginalDataAgent(override val dbName: String,
                        override val schema: Schema,
                        override val queryParser: IQLGenerator,
                        override val conn: IDataConn,
                        override val config: Config)(implicit ec: ExecutionContext)
  extends AbstractDataSetAgent(dbName, schema, queryParser, conn, config)(ec) {

  import OriginalDataAgent._

  val lastCount: Cardinality = UnInitialCardinality

  /**
    * Ask for the initial stats when the Agent starts.
    * Stats including: minTimeStamp, maxTimeStamp, cardinality
    */
  override def preStart(): Unit = {
    val minTimeQuery = Query(dbName, globalAggr = Some(GlobalAggregateStatement(AggregateStatement(schema.timeField, Min, Field.as(Min(schema.timeField), "min")))))
    val maxTimeQuery = Query(dbName, globalAggr = Some(GlobalAggregateStatement(AggregateStatement(schema.timeField, Max, Field.as(Max(schema.timeField), "max")))))
    val cardinalityQuery = Query(dbName, globalAggr = Some(GlobalAggregateStatement(AggregateStatement(schema.fieldMap("*"), Count, Field.as(Count(schema.fieldMap("*")), "count")))))
    val schemaMap = Map(dbName -> schema)
    val future = for {
      minTime <- conn.postQuery(queryParser.generate(minTimeQuery, schemaMap)).map(r => (r \\ "min").head.as[String])
      maxTime <- conn.postQuery(queryParser.generate(maxTimeQuery, schemaMap)).map(r => (r \\ "max").head.as[String])
      cardinality <- conn.postQuery(queryParser.generate(cardinalityQuery, schemaMap)).map(r => (r \\ "count").head.as[Long])
    } yield new Cardinality(TimeField.TimeFormat.parseDateTime(minTime), TimeField.TimeFormat.parseDateTime(maxTime), cardinality)
    future pipeTo self
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
      if (lastCount.count == UnInitialCount) {
        lastCount.reset(newCount.from, newCount.till, newCount.count)
        context.system.scheduler.schedule(config.AgentCollectStatsInterval, config.AgentCollectStatsInterval, self, UpdateStats)
      } else {
        lastCount.reset(lastCount.from, newCount.till, lastCount.count + newCount.count)
      }
    case UpdateStats =>
      collectStats(lastCount.till)
  }

  private def collectStats(start: DateTime): Unit = {
    val now = DateTime.now().minusMillis(1)
    val filter = FilterStatement(schema.timeField, None, Relation.inRange, Seq(start, now).map(TimeField.TimeFormat.print))
    val aggr = GlobalAggregateStatement(AggregateStatement(schema.fieldMap("*"), Count, Field.as(Count(schema.fieldMap("*")), "count")))
    val queryCardinality = Query(dbName, filter = Seq(filter), globalAggr = Some(aggr))

    conn.postQuery(queryParser.generate(queryCardinality, Map(dbName -> schema)))
      .map(r => new Cardinality(start, now, (r \\ "count").head.as[Long]))
      .pipeTo(self)
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

  val UnInitialCount = -1
  val UnInitialCardinality = new Cardinality(DateTime.now(), DateTime.now(), UnInitialCount)

  class Cardinality(var from: DateTime, var till: DateTime, var count: Long) {
    def reset(from: DateTime, till: DateTime, count: Long): Unit = {
      this.from = from
      this.till = till
      this.count = count
    }

    def ratePerSecond: Double = count.toDouble / new Duration(from, till).getStandardSeconds
  }

  def props(dbName: String, schema: Schema, queryParser: IQLGenerator, conn: IDataConn, config: Config)
           (implicit ec: ExecutionContext) =
    Props(new OriginalDataAgent(dbName, schema, queryParser, conn, config))
}
