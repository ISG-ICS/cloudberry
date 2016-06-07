package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging}
import edu.uci.ics.cloudberry.zion.model.{DBQuery, DBSyncQuery, Response}

import scala.concurrent.{ExecutionContext, Future}

abstract class DataStoreActor(val name: String)(implicit ec: ExecutionContext) extends Actor with ActorLogging{

  def query(query: DBQuery): Future[Response]

  override def receive: Receive = {
    case query: DBQuery => {
      log.info(self + " get query " + query + " from : " + sender())
      val querySender = sender()
      this.query(query).map(querySender ! _)
    }
  }
}



