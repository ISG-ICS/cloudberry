package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, Props, Stash}
import edu.uci.ics.cloudberry.zion.actor.ViewActor.DoneInitializing
import edu.uci.ics.cloudberry.zion.api.{DBQuery, DataStore, DataStoreView, ViewMetaStore}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}


abstract class ViewsManager extends Actor with Stash {

  var viewMetaStore: ViewMetaStore = null

  def loadMetaStore: Future[ViewMetaStore]

  def flushMeta()

  override def preStart(): Unit = {
    loadMetaStore.onComplete {
      case Success(store) => viewMetaStore = store; self ! DoneInitializing
      case Failure(t) => "error happens: " + t.getMessage
    }
  }

  import ViewsManager._

  override def receive: Receive = initializing

  private def initializing: Receive = {
    case DoneInitializing =>
      unstashAll()
      context.become(routine)
    case msg => stash()
  }

  private def routine: Receive = {
    case (dataStore: DataStore, query: DBQuery) => {
      //TODO a little awkward implementation. we need the key before the view
      val key = viewMetaStore.getViewName(dataStore, query)
      context.child(key).getOrElse {
        context.actorOf(Props(new ViewActor(self, viewMetaStore.getOrCreateViewStore(dataStore, query))), key)
      } forward (query)
    }
    case flushMetaMsg => flushMeta()
  }


  // flush to persistent storage every 30 minutes.
  context.system.scheduler.schedule(30 minutes, 30 minutes, self, flushMetaMsg)

  override def postStop(): Unit = flushMeta()
}


object ViewsManager {

  object flushMetaMsg
}










