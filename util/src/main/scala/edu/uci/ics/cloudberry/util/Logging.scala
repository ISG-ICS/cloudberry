package edu.uci.ics.cloudberry.util

import play.api.Logger

// TODO: Need this?
trait Logging {
  val log = Logger(this.getClass)
}
