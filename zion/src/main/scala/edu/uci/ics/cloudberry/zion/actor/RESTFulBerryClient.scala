package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import edu.uci.ics.cloudberry.zion.actor.DataStoreManager.AskInfoAndViews
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, JSONParser, QueryPlanner}
import edu.uci.ics.cloudberry.zion.model.schema.Query
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue}

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

  implicit val askTimeOut: Timeout = config.UserTimeOut

  override def receive: Receive = {
    case json: JsValue =>
      val (queries, _) = jsonParser.parse(json)
      Future.sequence(queries.map(solveAQuery)).map(JsArray.apply) pipeTo sender
    case queries: Seq[Query] =>
      Future.sequence(queries.map(solveAQuery)).map(JsArray.apply) pipeTo sender
    case query: Query =>
      solveAQuery(query) pipeTo sender
  }

  protected def solveAQuery(query: Query): Future[JsValue] = {
    val fInfos = dataManager ? AskInfoAndViews(query.dataset) map {
      case seq: Seq[_] if seq.forall(_.isInstanceOf[DataSetInfo]) =>
        seq.map(_.asInstanceOf[DataSetInfo])
      case _ => Seq.empty
    }

    fInfos.flatMap {
      case seq if seq.isEmpty =>
        Future(RESTFulBerryClient.noSuchDatasetJson(query.dataset))
      case infos: Seq[DataSetInfo] =>
        val (queries, merger) = planner.makePlan(query, infos.head, infos.tail)
        val fResponse = Future.traverse(queries) { subQuery =>
          dataManager ? subQuery
        }.map(seq => seq.map(_.asInstanceOf[JsValue]))

        if (suggestView) {
          val newViews = planner.suggestNewView(query, infos.head, infos.tail)
          newViews.foreach(dataManager ! _)
        }
        fResponse.map { responses => merger(responses) }
    }
  }
}

object RESTFulBerryClient {

  def props(jsonParser: JSONParser, dataManagerRef: ActorRef, planner: QueryPlanner, suggestView: Boolean, config: Config)
           (implicit ec: ExecutionContext) = {
    Props(new RESTFulBerryClient(jsonParser, dataManagerRef, planner, suggestView, config))
  }

  def noSuchDatasetJson(name: String): JsValue = {
    JsObject(Seq("error" -> JsString(s"Dataset $name does not exist")))
  }
}
