package edu.uci.ics.cloudberry.zion.model.datastore

import edu.uci.ics.cloudberry.zion.model.schema.{Query, Schema}

import scala.concurrent.Future

trait IDataStore {
  def name: String = schema.dataset

  def schema: Schema

  def query(query: String): Future[IResponse]
}

