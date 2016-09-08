package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import edu.uci.ics.cloudberry.zion.actor.DataStoreManager.AskInfoAndViews
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, JSONParser, QueryPlanner}
import edu.uci.ics.cloudberry.zion.model.schema.Query
import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future}

/**
  * An Actor to serve the RESTFul request which will return one response for one request
  *
  * @param jsonParser  the parser to parse the JSON request
  * @param dataManager the dataStoreManager
  * @param planner     the queryPlanner to optimize an efficient query work load to solve the request
  * @param config      the configuration
  * @param ec          implicit executionContext
  */
class RESTFulBerryClient(val jsonParser: JSONParser, val dataManager: ActorRef, val planner: QueryPlanner, suggestView: Boolean, val config: Config)
                        (implicit val ec: ExecutionContext) extends Actor with ActorLogging {

  import RESTFulBerryClient._

  implicit val askTimeOut: Timeout = config.UserTimeOut

  override def receive: Receive = {
    case json: JsValue =>
      val query = jsonParser.parse(json)
      solveQuery(query, sender())
    case query: Query =>
      solveQuery(query, sender())
  }

  protected def solveQuery(query: Query, output: ActorRef) = {
    withDataSetInfo(query) {
      case seq if seq.isEmpty =>
        output ! NoSuchDataset(query.dataset)
      case infos: Seq[DataSetInfo] =>
        val (queries, merger) = planner.makePlan(query, infos.head, infos.tail)
        val fResponse = Future.traverse(queries) { subQuery =>
          dataManager ? subQuery
        }.map(seq => seq.map(_.asInstanceOf[JsValue]))

        fResponse.map { responses =>
          val r = merger(responses)
          output ! r
        }

        if (suggestView) {
          val newViews = planner.suggestNewView(query, infos.head, infos.tail)
          newViews.foreach(dataManager ! _)
        }
    }
  }

  protected def withDataSetInfo(query: Query)(block: Seq[DataSetInfo] => Unit): Unit = {
    //TODO replace the logic to visit a cache to alleviate dataManager's workload
    dataManager ? AskInfoAndViews(query.dataset) map {
      case seq: Seq[_] if seq.forall(_.isInstanceOf[DataSetInfo]) =>
        block(seq.map(_.asInstanceOf[DataSetInfo]))
    }
  }
}

object RESTFulBerryClient {

  def props(jsonParser: JSONParser, dataManagerRef: ActorRef, planner: QueryPlanner, suggestView: Boolean, config: Config)
           (implicit ec: ExecutionContext) = {
    Props(new RESTFulBerryClient(jsonParser, dataManagerRef, planner, suggestView, config))
  }

  case class NoSuchDataset(name: String)

}
