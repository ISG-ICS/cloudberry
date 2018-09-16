package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import akka.pattern.ask
import akka.util.Timeout
import edu.uci.ics.cloudberry.zion.actor.DataStoreManager.{AskInfo, AskInfoAndViews}
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{IPostTransform, NoTransform}
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, JSONParser, QueryPlanner}
import edu.uci.ics.cloudberry.zion.model.schema.Query
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

/**
  * A reactive client which checks whether a query can be solved by the existed view.
  *
  */
class ViewStatusClient(val jsonParser: JSONParser,
                  val dataManager: ActorRef,
                  val planner: QueryPlanner,
                  val config: Config,
                  val out: ActorRef
                 )(implicit val ec: ExecutionContext) extends Actor with Stash with ActorLogging {

  import ViewStatusClient._

  // Timeout due to the configuration
  implicit val askTimeOut: Timeout = config.UserTimeOut

  // Handle the request when the client received message
  override def receive: Receive = {
    case json: JsValue =>
      handleRequest(json, NoTransform)
    case (json: JsValue, transform: IPostTransform) =>
      handleRequest(json, transform)
  }

  // Handle the Request in following step
  private def handleRequest(json: JsValue, transform: IPostTransform): Unit = {
    // 1. Use jsonParser to parse the json to get datasets name
    val datasets = jsonParser.getDatasets(json).toSeq

    // 2. Ask DataManager to get the schemaMap for the dataset, which can be used to parse json
    val fDataInfos = Future.traverse(datasets) { dataset =>
      dataManager ? AskInfo(dataset)
    }.map(seq => seq.map(_.asInstanceOf[Option[DataSetInfo]]))
    fDataInfos.foreach { seqInfos =>
      val schemaMap = seqInfos.zip(datasets).map {
        case (Some(info), _) =>
          info.name -> info.schema
        case (None, dataset) =>
          out ! noSuchDatasetJson(dataset)
          return
      }.toMap

      // 3. Parse the json progressively with schemaMap, get the queries
      val (queries, runOption) = jsonParser.parse(json, schemaMap)

      // 4. For each query, call "checkQuerySolvableByView"
      val futureResult = Future.traverse(queries)(q => checkQuerySolvableByView(q)).map(JsArray.apply)
      futureResult.map(result => (queries, result)).foreach {
        // 5. Return the result with queryID to frontend
        case (qs, r) => out ! transform.transform(r)
      }
    }
  }

  // Send DataManager AskInfoAndViews message, and then ask QueryPlanner to request view for query
  protected def checkQuerySolvableByView(query: Query): Future[JsValue] = {
    // get the necessary information about matched views from dataManager
    val fInfos = dataManager ? AskInfoAndViews(query.dataset) map {
      case seq: Seq[_] if seq.forall(_.isInstanceOf[DataSetInfo]) =>
        seq.map(_.asInstanceOf[DataSetInfo])
      case _ => Seq.empty
    }

    fInfos.flatMap {
      case seq if seq.isEmpty =>
        Future(JsBoolean(false))
      case infos: Seq[DataSetInfo] =>
        // give these information and query to QueryPlanner to request view for query
        val hasMatchedViews = planner.requestViewForQuery(query, infos.head, infos.tail)
        // If there are matched views, return json with true, otherwise return false
        if (hasMatchedViews) Future(JsBoolean(true))
        else Future(JsBoolean(false))
    }
  }
}

object ViewStatusClient {
  // New an object of ViewStatusClient
  def props(jsonParser: JSONParser, dataManager: ActorRef, planner: QueryPlanner, config: Config, out: ActorRef)
           (implicit ec: ExecutionContext) = {
    Props(new ViewStatusClient(jsonParser, dataManager, planner, config, out))
  }

  // The json format for the no such dataset error
  def noSuchDatasetJson(name: String): JsValue = {
    JsObject(Seq("error" -> JsString(s"Dataset $name does not exist")))
  }
}
