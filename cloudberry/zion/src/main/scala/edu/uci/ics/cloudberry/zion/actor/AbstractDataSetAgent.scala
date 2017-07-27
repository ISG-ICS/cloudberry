package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging, Props, Stash}
import akka.pattern.pipe
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQLGenerator}
import edu.uci.ics.cloudberry.zion.model.schema._
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Abstract class of DataSetAgent.
  * It separate the query related read-only work with the maintenance workload which usually modify the dataset.
  * All maintenance workloads are queued so that it conducted one by one.
  *
  * @param dbName
  * @param queryParser
  * @param conn
  * @param config
  * @param ec
  */
abstract class AbstractDataSetAgent(val dbName: String,
                                    val schema: Schema,
                                    val queryParser: IQLGenerator,
                                    val conn: IDataConn,
                                    val config: Config)
                                   (implicit ec: ExecutionContext)
  extends Actor with Stash with ActorLogging {

  /**
    * Estimate the query by dataset's stats without visiting the underlying database.
    * If the query is not estimable return [[None]]
    *
    * @param query
    * @return [[JsValue]] if estimable, otherwise [[None]]
    */
  protected def estimate(query: Query): Option[JsValue]

  /**
    * Conduct maintenance related work.
    * The querying related logic has been covered by the [[AbstractDataSetAgent]].
    *
    * @return a message handle partial function.
    */
  protected def maintenanceWork: Receive

  override def receive: Receive = querying orElse {
    maintenanceWork
  }

  protected def querying: Receive = {
    case query: Query =>
      estimate(query) match {
        case Some(result) => sender() ! result
        case None =>
          //TODO should ask the MetaActor about other required schemas.
          conn.postQuery(queryParser.generate(query, Map(dbName -> schema))) pipeTo sender()
      }
  }

}

abstract class AbstractUpdatableDataSetAgent(override val dbName: String,
                                             override val schema: Schema,
                                             override val queryParser: IQLGenerator,
                                             override val conn: IDataConn,
                                             override val config: Config
                                            )
                                            (implicit ec: ExecutionContext)
  extends AbstractDataSetAgent(dbName, schema, queryParser, conn, config)(ec) {

  import AbstractUpdatableDataSetAgent.DoneUpdate

  protected def processUpdate(statement: String): Unit = {
    val curSender = sender()
    conn.postControl(statement).map { result =>
      curSender ! result
      self ! DoneUpdate
    }
    context.become(updatingDataSet)
  }

  protected def processUpdate(statements: Seq[String]): Unit = {
    val curSender = sender()
    Future.traverse(statements){ statement =>
      conn.postControl(statement)
    }.map { results =>
      if (results.forall(_ == true)) {
        curSender ! true
      } else {
        curSender ! false
      }
      self ! DoneUpdate
    }
    context.become(updatingDataSet)
  }

  protected def updatingDataSet: Receive = querying orElse {
    case DoneUpdate =>
      unstashAll()
      context.unbecome()
    case _ => stash()
  }
}

object AbstractUpdatableDataSetAgent {

  object DoneUpdate

}
