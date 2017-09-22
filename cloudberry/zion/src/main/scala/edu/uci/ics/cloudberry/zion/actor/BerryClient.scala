package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import edu.uci.ics.cloudberry.zion.actor.DataStoreManager.AskInfo
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{IPostTransform, NoTransform}
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, JSONParser, QueryPlanner}
import org.joda.time.DateTime
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

/**
  * A reactive client which will continuously feed the result back to user
  * One user should only attach to one ReactiveClient
  *
  * TODO: a better design should be a reception actor that directs the slicing query and the entire query to different
  * workers(actors).
  * TODO: merge the multiple times AskViewsInfos
  */
class BerryClient(val jsonParser: JSONParser,
                  val dataManager: ActorRef,
                  val planner: QueryPlanner,
                  val config: Config,
                  val out: ActorRef
                 )(implicit val ec: ExecutionContext,
                   implicit val materializer: Materializer) extends Actor with Stash with ActorLogging {


  import BerryClient._

  implicit val askTimeOut: Timeout = config.UserTimeOut

  private val restSolver : ActorRef = context.actorOf(Props(new RESTSolver(dataManager, planner, out)))
  private val streamSolver : ActorRef = context.actorOf(Props(new StreamingSolver(dataManager, planner, config, out)))

  override def receive: Receive = {
    case json: JsValue =>
      handleNewRequest(json, NoTransform)
    case (json: JsValue, transform: IPostTransform) =>
      handleNewRequest(json, transform)
  }

  private def handleNewRequest(json: JsValue, transform: IPostTransform): Unit = {
    val key = DateTime.now()
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
        restSolver ! (queries, transform)
      } else {
        val targetMillis = runOption.sliceMills
        val mapInfos = seqInfos.map(_.get).map(info => info.name -> info).toMap
        streamSolver ! StreamingSolver.SlicingRequest(targetMillis, queries, mapInfos, transform)
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
