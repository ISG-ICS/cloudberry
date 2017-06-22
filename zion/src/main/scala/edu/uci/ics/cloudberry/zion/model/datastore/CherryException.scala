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

case class QueryInitException(msg: String) extends CherryException(msg, null)

class QueryParsingException(msg: String) extends CherryException(msg, null)

//TODO change to BADRequest response
case class JsonRequestException(msg: String) extends CherryException(msg, null)

case class FieldNotFound(fieldName: String) extends QueryParsingException(s"cannot find field $fieldName")

case class CollectStatsException(msg: String) extends CherryException(msg, null)
