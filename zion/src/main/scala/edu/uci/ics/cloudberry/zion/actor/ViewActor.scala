package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorRef,Stash}
import edu.uci.ics.cloudberry.zion.api.{DBQuery, DataStoreView}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class ViewActor(viewsManager: ActorRef, fViewStore: Future[DataStoreView]) extends Actor with Stash{
  var viewStore: DataStoreView = null

  import ViewActor._

  override def preStart(): Unit = {
    fViewStore.onComplete {
      case Success(store) => viewStore = store; self ! DoneInitializing
      case Failure(t) => "error happens: " + t.getMessage
    }
  }

  override def receive: Receive = initializing

  private def initializing: Receive = {
    case DoneInitializing =>
      unstashAll()
      context.become(routine)
    case msg => stash()
  }

  private def routine: Receive = {
    case query: DBQuery => {
      val thisSender = sender()
      viewStore.query(query).map(thisSender ! _)
    }
    case updateViewMsg => updateView()
    case updated => // update viewStores
  }

  private def updateView() = ???

  context.system.scheduler.schedule(30 minutes, 30 minutes, self, updateViewMsg)
}

object ViewActor {

  object DoneInitializing

  object updateViewMsg

}