package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging, Props, Stash}
import akka.pattern.pipe
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQLGenerator}
import edu.uci.ics.cloudberry.zion.model.schema._
import play.api.libs.json._

import scala.concurrent.ExecutionContext

abstract class AbstractDataSetAgent(val dbName: String,
                                    val schema: Schema,
                                    val queryParser: IQLGenerator,
                                    val conn: IDataConn,
                                    val config: Config)
                                   (implicit ec: ExecutionContext)
  extends Actor with Stash with ActorLogging {

  protected def estimate(query: Query): Option[JsValue]

  protected def nonQueryingWorkLoad: Receive

  override def receive: Receive = querying orElse {
    nonQueryingWorkLoad
  }

  protected def querying: Receive = {
    case query: Query =>
      estimate(query) match {
        case Some(result) => sender() ! result
        case None =>
          conn.postQuery(queryParser.generate(query, schema)) pipeTo sender()
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
