package edu.uci.ics.cloudberry.util

import play.api.Logger

trait Logging {
  val log = Logger(this.getClass)
}
