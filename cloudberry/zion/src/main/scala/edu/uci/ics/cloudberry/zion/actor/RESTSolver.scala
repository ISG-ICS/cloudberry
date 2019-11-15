package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.util.Timeout
import edu.uci.ics.cloudberry.zion.model.datastore.IPostTransform
import edu.uci.ics.cloudberry.zion.model.impl.QueryPlanner
import edu.uci.ics.cloudberry.zion.model.schema.Query
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

/**
  * The RESTFul Actor is responsible for processing the query in a RESTFul way, i.e., one request will get one response.
  * @param dataManager
  * @param planner
  * @param out
  */
class RESTSolver(val dataManager: ActorRef,
                 val planner: QueryPlanner,
                 val out: ActorRef
                )(implicit val ec: ExecutionContext, implicit val timeout: Timeout)
  extends Actor with IQuerySolver with ActorLogging {

  override def receive: Actor.Receive = {
    case (queries: Seq[Query], transform: IPostTransform) =>
      val futureResult = Future.traverse(queries)(q => solveAQuery(q)).map(JsArray.apply)
      futureResult.map(result => (queries, result)).foreach { case (qs, r) =>
        // handle average results
        val avgHandledResult = r.value.map(rows => QueryPlanner.handleAvg(rows.as[JsArray]))
        out ! transform.transform(Json.parse(Json.toJson(avgHandledResult).toString()))
        // suggest view to QueryPlanner
        qs.foreach(suggestViews)
      }
  }

}

