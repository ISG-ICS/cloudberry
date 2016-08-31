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

/**
  * An Actor to serve the RESTFul request which will return one response for one request
  *
  * @param out         the specific output actor replying to
  * @param jsonParser  the parser to parse the JSON request
  * @param dataManager the dataStoreManager
  * @param planner     the queryPlanner to optimize an efficient query work load to solve the request
  * @param config      the configuration
  * @param ec          implicit executionContext
  */
class RESTFulBerryClient(val out: Option[ActorRef], val jsonParser: JSONParser, val dataManager: ActorRef, val planner: QueryPlanner, val config: Config)
                        (implicit val ec: ExecutionContext) extends Actor {

  import RESTFulBerryClient._

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
        val (queries, merger) = planner.makePlan(query, infos.head, infos.tail)
        val fResponse = Future.traverse(queries) { query =>
          dataManager ? query
        }.map(seq => seq.map(_.asInstanceOf[JsValue]))

        fResponse.map { responses =>
          val r = merger(responses)
          out.getOrElse(curSender) ! r
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

object RESTFulBerryClient {

  def props(out: ActorRef, jsonParser: JSONParser, dataManagerRef: ActorRef, planner: QueryPlanner, config: Config)
           (implicit ec: ExecutionContext) = {
    Props(new RESTFulBerryClient(Some(out), jsonParser, dataManagerRef, planner, config))
  }

  def props(jsonParser: JSONParser, dataManagerRef: ActorRef, planner: QueryPlanner, config: Config)
           (implicit ec: ExecutionContext) = {
    Props(new RESTFulBerryClient(None, jsonParser, dataManagerRef, planner, config))
  }

  case class NoSuchDataset(name: String)

}
