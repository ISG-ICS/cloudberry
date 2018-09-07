package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import akka.pattern.ask
import akka.util.Timeout
import edu.uci.ics.cloudberry.zion.actor.DataStoreManager.{AskInfo, AskInfoAndViews}
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{IPostTransform}
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, JSONParser, QueryPlanner}
import edu.uci.ics.cloudberry.zion.model.schema.Query
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

/**
  * A reactive client which checks whether a query can be solved by existed view.
  *
  */
class ViewStatusClient(val jsonParser: JSONParser,
                  val dataManager: ActorRef,
                  val planner: QueryPlanner,
                  val config: Config,
                  val out: ActorRef
                 )(implicit val ec: ExecutionContext) extends Actor with Stash with ActorLogging {

  import ViewStatusClient._

  implicit val askTimeOut: Timeout = config.UserTimeOut

  override def receive: Receive = {
    case json: JsValue =>
      handleRequest(json)
    case (json: JsValue, transform: IPostTransform) =>
      handleRequest(json)
  }

  private def handleRequest(json: JsValue): Unit = {
    val datasets = jsonParser.getDatasets(json).toSeq
    val queryID = jsonParser.getQueryID(json)
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

      val (queries, runOption) = jsonParser.parse(json, schemaMap)
      val futureResult = Future.traverse(queries)(q => checkQuerySolvableByView(q)).map(JsArray.apply)
      futureResult.map(result => (queries, result)).foreach {
        case (qs, r) => out ! r.append(JsObject(Seq("queryID" -> JsNumber(queryID))))
      }
    }
  }

  protected def checkQuerySolvableByView(query: Query): Future[JsValue] = {
    val fInfos = dataManager ? AskInfoAndViews(query.dataset) map {
      case seq: Seq[_] if seq.forall(_.isInstanceOf[DataSetInfo]) =>
        seq.map(_.asInstanceOf[DataSetInfo])
      case _ => Seq.empty
    }

    fInfos.flatMap {
      case seq if seq.isEmpty =>
        Future(resultJson(false))
      case infos: Seq[DataSetInfo] =>
        val hasMatchedViews = planner.requestViewForQuery(query, infos.head, infos.tail)
        if (hasMatchedViews) Future(resultJson(true))
        else  Future(resultJson(false))
    }
  }
}

object ViewStatusClient {
  def props(jsonParser: JSONParser, dataManager: ActorRef, planner: QueryPlanner, config: Config, out: ActorRef)
           (implicit ec: ExecutionContext) = {
    Props(new ViewStatusClient(jsonParser, dataManager, planner, config, out))
  }

  def resultJson(result: Boolean): JsValue = {
    JsObject(Seq("isQuerySolvableByView" -> JsBoolean(result)))
  }

  def noSuchDatasetJson(name: String): JsValue = {
    JsObject(Seq("error" -> JsString(s"Dataset $name does not exist")))
  }
}
