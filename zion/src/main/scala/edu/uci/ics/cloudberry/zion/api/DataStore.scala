package edu.uci.ics.cloudberry.zion.api

import scala.concurrent.Future

trait DataStore {
  def name: String

  def query(query: DBQuery): Future[Response]

  def update(query: DBUpdateQuery): Future[Response]
}

trait Response {

}
