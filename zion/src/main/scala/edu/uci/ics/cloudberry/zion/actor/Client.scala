package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.actor.DataStoreManager.AskInfoMsg
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, JSONParser, QueryPlanner}
import edu.uci.ics.cloudberry.zion.model.schema.Query
import play.api.libs.json.{JsArray, JsValue}

import scala.concurrent.{ExecutionContext, Future}

class Client(val out: Option[ActorRef], val jsonParser: JSONParser, val dataManager: ActorRef, val planner: QueryPlanner, val config: Config)
            (implicit val ec: ExecutionContext) extends Actor {

  import Client._

  implicit val askTimeOut: Timeout = config.UserTimeOut

  override def receive: Receive = {
    case json: JsValue =>
      val query = jsonParser.parse(json)
      solveQuery(query, sender())
    case query: Query =>
      solveQuery(query, sender())
  }

  private def solveQuery(query: Query, curSender: ActorRef) = {
    withDataSetInfo(query) {
      case seq if seq.isEmpty =>
        out.getOrElse(curSender) ! NoSuchDataset(query.dataset)
      case infos: Seq[DataSetInfo] =>
        val queries = planner.makePlan(query, infos.head, infos.tail)
        val fResponse = Future.traverse(queries) { query =>
          dataManager ? query
        }.map(seq => seq.map(_.asInstanceOf[JsValue]))

        fResponse.map { responses =>
          out.getOrElse(curSender) ! mergeResponse(responses)
        }

        val newViews = planner.suggestNewView(query, infos.head, infos.tail)
        newViews.foreach(dataManager ! _)
    }
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

  def props(out: ActorRef, jsonParser: JSONParser, dataManagerRef: ActorRef, planner: QueryPlanner, config: Config)
           (implicit ec: ExecutionContext) = {
    Props(new Client(Some(out), jsonParser, dataManagerRef, planner, config))
  }

  def props(jsonParser: JSONParser, dataManagerRef: ActorRef, planner: QueryPlanner, config: Config)
           (implicit ec: ExecutionContext) = {
    Props(new Client(None, jsonParser, dataManagerRef, planner, config))
  }

  def mergeResponse(responses: TraversableOnce[JsValue]): JsArray = {
    val builder = Seq.newBuilder[JsValue]
    responses.foreach(jsValue => builder ++= jsValue.asInstanceOf[JsArray].value)
    JsArray(builder.result())
  }

  case class NoSuchDataset(name: String)

}
