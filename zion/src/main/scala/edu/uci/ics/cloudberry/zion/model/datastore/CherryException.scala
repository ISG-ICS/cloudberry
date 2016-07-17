package edu.uci.ics.cloudberry.zion.model.datastore

/**
  * The principle of throwing the exception is to only throw it in the joint part so that
  * we will have unified place to handle the exceptions.
  *
  * @param msg
  * @param cause
  */
class CherryException(msg: String, cause: Throwable) extends RuntimeException(msg, cause) {
}

case class QueryParsingException(msg: String) extends CherryException(msg, null)

case class JsonRequestException(msg: String) extends CherryException(msg, null)
