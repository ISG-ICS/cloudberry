package edu.uci.ics.cloudberry.zion.model

class DBException(message: String, cause: Throwable = null) extends RuntimeException(message, cause) {}

case class UpdateFailedDBException(responseBody: String) extends DBException(responseBody)
