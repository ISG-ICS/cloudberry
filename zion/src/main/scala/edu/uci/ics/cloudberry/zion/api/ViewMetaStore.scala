package edu.uci.ics.cloudberry.zion.api

import scala.concurrent.Future

/**
  * This meta store should be singleton. And it should be accessed only by [[edu.uci.ics.cloudberry.zion.actor.ViewsManager]]
  * We should not need the Synchronized data structure since it should be used within one actor.
  */
trait ViewMetaStore {

  def getViewName(source: DataStore, query: DBQuery): String

  def getOrCreateViewStore(source: DataStore, query: DBQuery): Future[DataStoreView]
}


