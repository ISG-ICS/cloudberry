package edu.uci.ics.cloudberry.zion.model.datastore

class CherryException(msg: String, cause: Throwable) extends RuntimeException(msg, cause) {

}

case class QueryParsingException(msg: String) extends CherryException(msg, null)
