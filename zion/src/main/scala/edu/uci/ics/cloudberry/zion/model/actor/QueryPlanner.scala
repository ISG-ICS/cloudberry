package edu.uci.ics.cloudberry.zion.model.actor

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import edu.uci.ics.cloudberry.zion.model.actor.DataManager.AskInfoMsg
import edu.uci.ics.cloudberry.zion.model.datastore.IResponse
import edu.uci.ics.cloudberry.zion.model.schema.{IQuery, Query}

import scala.concurrent.Future

class QueryPlanner(dataManager: ActorRef) extends Actor {

  import QueryPlanner._

  override def receive: Receive = {
    case query: Query =>

      val curSender = sender()

      withDataSetInfo(query) {
        case Seq.empty =>
          curSender ! NoSuchDataset(query.dataset)
        case infos: Seq[DataSetInfo] =>
          val queries = makePlan(query, infos.head, infos.tail)
          val fResponse = Future.traverse(queries) { query =>
            dataManager ? query
          }.map(seq => seq.map(_.asInstanceOf[IResponse]))

          fResponse.map(curSender ! mergeResponse(_))
      }
    case _ =>
  }

  protected def withDataSetInfo(query: Query)(block: Seq[DataSetInfo] => Unit): Unit = {
    dataManager ? AskInfoMsg(query.dataset) map {
      case seq: Seq[DataSetInfo] =>
        block(seq)
    }
  }
}

object QueryPlanner {

  def mergeResponse(responses: TraversableOnce[IResponse]): IResponse = ???

  def makePlan(query: Query, source: DataSetInfo, views: Seq[DataSetInfo]): Seq[IQuery] = ???

  case class NoSuchDataset(name: String)

}
