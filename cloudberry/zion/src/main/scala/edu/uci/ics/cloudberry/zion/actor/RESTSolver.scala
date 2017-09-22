package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging, ActorRef}
import edu.uci.ics.cloudberry.zion.model.datastore.IPostTransform
import edu.uci.ics.cloudberry.zion.model.impl.QueryPlanner
import edu.uci.ics.cloudberry.zion.model.schema.Query
import play.api.libs.json.JsArray

import scala.concurrent.{ExecutionContext, Future}

class RESTSolver(val dataManager: ActorRef,
                 val planner: QueryPlanner,
                 val out: ActorRef
                )(implicit val ec: ExecutionContext) extends Actor with IQuerySolver with ActorLogging {

  override def receive: Actor.Receive = {
    case (queries: Seq[Query], transform: IPostTransform) =>
      val futureResult = Future.traverse(queries)(q => solveAQuery(q)).map(JsArray.apply)
      futureResult.foreach { r =>
        out ! transform.transform(r)
      }
  }

}

