package edu.uci.ics.cloudberry.util

import play.api.Logger

object Profile {

  def profile[R](tag: String)(block: => R) : R = {
    val tick = System.currentTimeMillis()
    val result = block
    val tock = System.currentTimeMillis()
    Logger.logger.info(s"$tag time: ${tock - tick} ms" )
    result
  }
}
