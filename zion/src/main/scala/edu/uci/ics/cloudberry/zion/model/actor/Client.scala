package edu.uci.ics.cloudberry.zion.model.actor

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import edu.uci.ics.cloudberry.zion.model.actor.DataManager.AskInfoMsg
import edu.uci.ics.cloudberry.zion.model.datastore.QueryResponse
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, QueryOptimizer}
import edu.uci.ics.cloudberry.zion.model.schema.Query
import play.api.libs.json.{JsArray, JsValue}

import scala.concurrent.{ExecutionContext, Future}

class Client(dataManager: ActorRef, optimizer: QueryOptimizer)
            (implicit val askTimeOut: Timeout, implicit val ec: ExecutionContext) extends Actor {

  import Client._

  override def receive: Receive = {
    case query: Query =>

      val curSender = sender()

      withDataSetInfo(query) {
        case seq if seq.isEmpty =>
          curSender ! NoSuchDataset(query.dataset)
        case infos: Seq[DataSetInfo] =>
          val queries = optimizer.makePlan(query, infos.head, infos.tail)
          val fResponse = Future.traverse(queries) { query =>
            dataManager ? query
          }.map(seq => seq.map(_.asInstanceOf[QueryResponse]))

          fResponse.map(curSender ! mergeResponse(_))

          val newViews = optimizer.suggestNewView(query, infos.head, infos.tail)
          newViews.foreach(dataManager ! _)
      }
    case _ =>
  }

  protected def withDataSetInfo(query: Query)(block: Seq[DataSetInfo] => Unit): Unit = {
    //TODO replace the logic to visit a cache to alleviate dataManager's workload
    dataManager ? AskInfoMsg(query.dataset) map {
      case seq: Seq[_] if seq.forall(_.isInstanceOf[DataSetInfo]) =>
        block(seq.map(_.asInstanceOf[DataSetInfo]))
    }
  }
}

object Client {

  def mergeResponse(responses: TraversableOnce[QueryResponse]): QueryResponse = {
    val jsValue = responses.foldLeft(List[JsValue]())((merge, response) => merge ++ response.jsArray.value)
    QueryResponse(JsArray(jsValue))
  }

  case class NoSuchDataset(name: String)

}
