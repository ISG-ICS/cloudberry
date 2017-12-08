package edu.uci.ics.cloudberry.zion.actor

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import edu.uci.ics.cloudberry.zion.actor.DataStoreManager.AskInfoAndViews
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, QueryPlanner}
import edu.uci.ics.cloudberry.zion.model.schema.Query
import play.api.libs.json.{JsObject, JsString, JsValue}

import scala.concurrent.{ExecutionContext, Future}

trait IQuerySolver {
  def dataManager: ActorRef

  def planner: QueryPlanner

  protected def solveAQuery(query: Query)(implicit timeout: Timeout, execution: ExecutionContext): Future[JsValue] = {
    val fInfos = dataManager ? AskInfoAndViews(query.dataset) map {
      case seq: Seq[_] if seq.forall(_.isInstanceOf[DataSetInfo]) =>
        seq.map(_.asInstanceOf[DataSetInfo])
      case _ => Seq.empty
    }

    fInfos.flatMap {
      case seq if seq.isEmpty =>
        Future(IQuerySolver.noSuchDatasetJson(query.dataset))
      case infos: Seq[DataSetInfo] =>
        val (queries, merger) = planner.makePlan(query, infos.head, infos.tail)
        val fResponse = Future.traverse(queries) { subQuery =>
          dataManager ? subQuery
        }.map(seq => seq.map(_.asInstanceOf[JsValue]))

        fResponse.map { responses => merger(responses) }
    }
  }

  protected def suggestViews(query: Query)(implicit timeout: Timeout, executionContext: ExecutionContext): Unit = {
    dataManager ? AskInfoAndViews(query.dataset) map {
      case seq: Seq[_] if seq.forall(_.isInstanceOf[DataSetInfo]) => {
        val infos = seq.map(_.asInstanceOf[DataSetInfo])
        val newViews = planner.suggestNewView(query, infos.head, infos.tail)
        newViews.foreach(dataManager ! _)
      }
    }
  }

}

object IQuerySolver {
  def noSuchDatasetJson(name: String): JsValue = {
    JsObject(Seq("error" -> JsString(s"Dataset $name does not exist")))
  }
}
