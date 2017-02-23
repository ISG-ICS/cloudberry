package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging, Props, Stash}
import akka.pattern.pipe
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQLGenerator}
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.{DateTime, Duration}

import scala.concurrent.ExecutionContext
import scala.util.Random

class DataSetAgent(val dbName: String, val schema: Schema, val queryParser: IQLGenerator, val conn: IDataConn, val config: Config)
                  (implicit ec: ExecutionContext)
  extends Actor with Stash with ActorLogging {

  import DataSetAgent._

  var lastCount: Cardinality = UnInitialCount
  var ratePerSecond: Long = _

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

  override def receive: Receive = querying orElse {
    case append: AppendView =>
      process(queryParser.generate(append, schema))
    case upsert: UpsertRecord =>
      process(queryParser.generate(upsert, schema))
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

  private def querying: Receive = {
    case estQuery: EstimateQuery =>
      if (estimable(estQuery.query)) {
        sender() ! estimate(estQuery.query)
      } else {
        conn.postQuery(queryParser.generate(estQuery.query, schema)) pipeTo sender()
      }
    case query: Query =>
      conn.postQuery(queryParser.generate(query, schema)) pipeTo sender()
  }

  private def process(statement: String): Unit = {
    val curSender = sender()
    conn.postControl(statement).map { result =>
      curSender ! result
      self ! DataSetAgent.DoneUpdating
    }
    context.become(updating)
  }

  private def updating: Receive = querying orElse {
    case DataSetAgent.DoneUpdating =>
      unstashAll()
      context.unbecome()
    case _ => stash()
  }

  //TODO extend the logic to all sorts of stats related aggregation functions
  private def estimable(query: Query): Boolean = {
    if (query.filter.isEmpty && query.groups.isEmpty && query.lookup.isEmpty && query.select.isEmpty && query.unnest.isEmpty) {
      query.globalAggr.exists(g => g.aggregate.func.name == AggregateFunc.Count)
    } else {
      false
    }
  }

  private def estimate(query: Query): Long = {
    val seconds = new Duration(DateTime.now, lastCount.till).getStandardSeconds
    lastCount.count + seconds * ratePerSecond + Random.nextInt(ratePerSecond.toInt)
  }

  private def collectStats(start: DateTime): Unit = {
    val now = DateTime.now().minusSeconds(1)
    val filter = FilterStatement(schema.timeField, None, Relation.inRange, Seq(start, now).map(TimeField.TimeFormat.print))
    val aggr = GlobalAggregateStatement(AggregateStatement("*", Count, "count"))
    val queryCardinality = Query(dbName, filter = Seq(filter), globalAggr = Some(aggr))

    conn
      .postQuery(queryParser.generate(queryCardinality, schema))
      .map(r => Cardinality(start, now, (r \\ "count").head.as[Long])) pipeTo self
  }

}

object DataSetAgent {

  object DoneUpdating

  object UpdateStats

  val UnInitialCount = Cardinality(DateTime.now(), DateTime.now(), -1)

  case class Cardinality(from: DateTime, till: DateTime, count: Long)

  def props(dbName: String, schema: Schema, queryParser: IQLGenerator, conn: IDataConn, config: Config)(implicit ec: ExecutionContext) =
    Props(new DataSetAgent(dbName, schema, queryParser, conn, config))
}
