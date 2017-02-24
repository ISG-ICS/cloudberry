package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging, Props, Stash}
import akka.pattern.pipe
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQLGenerator}
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.{DateTime, Duration}
import play.api.libs.json._

import scala.concurrent.ExecutionContext
import scala.util.Random

class BaseDataSetAgent(override val dbName: String,
                       override val schema: Schema,
                       override val queryParser: IQLGenerator,
                       override val conn: IDataConn,
                       override val config: Config)(implicit ec: ExecutionContext)
  extends AbstractDataSetAgent(dbName, schema, queryParser, conn, config)(ec) {

  import BaseDataSetAgent._

  var lastCount: Cardinality = UnInitialCount
  var ratePerSecond: Long = _

  /**
    * Ask for the initial stats when the Agent starts.
    * Stats including: minTimeStamp, maxTimeStamp, cardinality
    */
  override def preStart(): Unit = {
    val minTimeQuery = Query(dbName, globalAggr = Some(GlobalAggregateStatement(AggregateStatement(schema.timeField, Min, "min"))))
    val maxTimeQuery = Query(dbName, globalAggr = Some(GlobalAggregateStatement(AggregateStatement(schema.timeField, Max, "max"))))
    val cardinalityQuery = Query(dbName, globalAggr = Some(GlobalAggregateStatement(AggregateStatement("*", Count, "count"))))
    val future = for {
      minTime <- conn.postQuery(queryParser.generate(minTimeQuery, schema)).map(r => (r \\ "min").head.as[String])
      maxTime <- conn.postQuery(queryParser.generate(maxTimeQuery, schema)).map(r => (r \\ "max").head.as[String])
      cardinality <- conn.postQuery(queryParser.generate(cardinalityQuery, schema)).map(r => (r \\ "count").head.as[Long])
    } yield Cardinality(TimeField.TimeFormat.parseDateTime(minTime), TimeField.TimeFormat.parseDateTime(maxTime), cardinality)
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
      val seconds = new Duration(DateTime.now, lastCount.till).getStandardSeconds
      val count = lastCount.count + seconds * ratePerSecond + Random.nextInt(ratePerSecond.toInt)
      val tag = query.globalAggr.get.aggregate.as
      Some(Json.obj(tag -> JsNumber(count)))
    } else {
      None
    }
  }

  override protected def nonQueryingWorkLoad: Receive = {
    case newCount: Cardinality =>
      ratePerSecond = newCount.count / new Duration(newCount.till, newCount.from).getStandardSeconds
      if (lastCount == UnInitialCount) {
        lastCount = newCount
        context.system.scheduler.schedule(config.AgentCollectStatsInterval, config.AgentCollectStatsInterval, self, UpdateStats)
      } else {
        lastCount = Cardinality(lastCount.from, newCount.till, lastCount.count + newCount.count)
      }
    case UpdateStats =>
      collectStats(lastCount.till)
  }

  private def collectStats(start: DateTime): Unit = {
    val now = DateTime.now().minusSeconds(1)
    val filter = FilterStatement(schema.timeField, None, Relation.inRange, Seq(start, now).map(TimeField.TimeFormat.print))
    val aggr = GlobalAggregateStatement(AggregateStatement("*", Count, "count"))
    val queryCardinality = Query(dbName, filter = Seq(filter), globalAggr = Some(aggr))

    conn.postQuery(queryParser.generate(queryCardinality, schema))
      .map(r => Cardinality(start, now, (r \\ "count").head.as[Long]))
      .pipeTo(self)
  }

  //TODO extend the logic of using stats to solve more queries
  private def estimable(query: Query): Boolean = {
    if (query.isEstimable &&
      query.filter.isEmpty &&
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

object BaseDataSetAgent {

  object UpdateStats

  val UnInitialCount = Cardinality(DateTime.now(), DateTime.now(), -1)

  case class Cardinality(from: DateTime, till: DateTime, count: Long)

  def props(dbName: String, schema: Schema, queryParser: IQLGenerator, conn: IDataConn, config: Config)
           (implicit ec: ExecutionContext) =
    Props(new BaseDataSetAgent(dbName, schema, queryParser, conn, config))
}
