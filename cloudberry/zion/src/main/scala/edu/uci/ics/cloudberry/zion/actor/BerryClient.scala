package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import akka.pattern.ask
import akka.util.Timeout
import edu.uci.ics.cloudberry.zion.actor.DataStoreManager.AskInfo
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{IPostTransform, JsonRequestException, NoTransform}
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, JSONParser, QueryPlanner}
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

/**
  * A reactive client which will continuously feed the result back to user
  * One user should only attach to one ReactiveClient
  *
  * TODO: merge the multiple times AskViewsInfos
  */
class BerryClient(val jsonParser: JSONParser,
                  val dataManager: ActorRef,
                  val planner: QueryPlanner,
                  val config: Config,
                  val out: ActorRef
                 )(implicit val ec: ExecutionContext) extends Actor with Stash with ActorLogging {

  import BerryClient._

  implicit val askTimeOut: Timeout = config.UserTimeOut

  //One RESTFul solver is enough to solve the RESTFul request.
  private val restfulSolver: ActorRef = context.actorOf(Props(new RESTSolver(dataManager, planner, out)))

  override def receive: Receive = {
    case json: JsValue =>
      handleRequest(json, NoTransform)
    case (json: JsValue, transform: IPostTransform) =>
      handleRequest(json, transform)
  }

  private def handleRequest(json: JsValue, transform: IPostTransform): Unit = {
    val datasets = jsonParser.getDatasets(json).toSeq
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
      if (runOption.sliceMills <= 0) {
        restfulSolver ! (queries, transform)
      } else {
        val paceMS = runOption.sliceMills
        val resultSizeLimit = runOption.limit
        val mapInfos = seqInfos.map(_.get).map(info => info.name -> info).toMap

        if (resultSizeLimit.nonEmpty && queries.size > 1) {
          // TODO send error messages to user
          throw JsonRequestException("Batch Requests cannot contain \"limit\" field")
        }
        //Each streaming request need a specific actor to handle the request.
        //TODO Ultimately, clients can run multiple streaming request simultaneously.
        //     They can also cancel or reset a specific request.
        //     Right now, we are just allow one streaming request at once, the later one will stop the previous running request.
        val child = context.child("stream").getOrElse(
          context.actorOf(Props(new ProgressiveSolver(dataManager, planner, config, out)), "stream")
        )
        child ! ProgressiveSolver.Cancel // Cancel ongoing slicing work if any
        child ! ProgressiveSolver.SlicingRequest(paceMS, resultSizeLimit, queries, mapInfos, transform)
      }
    }
  }
}

object BerryClient {

  val Done = Json.obj("key" -> JsString("done"))

  def props(jsonParser: JSONParser, dataManager: ActorRef, planner: QueryPlanner, config: Config, out: ActorRef)
           (implicit ec: ExecutionContext) = {
    Props(new BerryClient(jsonParser, dataManager, planner, config, out))
  }

  def noSuchDatasetJson(name: String): JsValue = {
    JsObject(Seq("error" -> JsString(s"Dataset $name does not exist")))
  }
}
