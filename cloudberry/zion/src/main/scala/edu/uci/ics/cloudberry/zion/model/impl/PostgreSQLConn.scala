package edu.uci.ics.cloudberry.zion.model.impl

import scala.concurrent.ExecutionContext

class PostgreSQLConn(url: String)(implicit ec: ExecutionContext) extends MySQLConn(url) {}
