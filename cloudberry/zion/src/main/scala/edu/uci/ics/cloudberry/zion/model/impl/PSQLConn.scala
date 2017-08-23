package edu.uci.ics.cloudberry.zion.model.impl

import scala.concurrent.ExecutionContext

class PSQLConn(url: String)(implicit ec: ExecutionContext) extends SQLConn(url) {}
