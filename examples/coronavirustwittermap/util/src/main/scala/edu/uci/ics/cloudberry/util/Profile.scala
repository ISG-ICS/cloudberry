package edu.uci.ics.cloudberry.util


object Profile extends Logging {

  def profile[R](tag: String)(block: => R): R = {
    val tick = System.currentTimeMillis()
    val result = block
    val tock = System.currentTimeMillis()
    log.info(s"$tag time: ${tock - tick} ms")
    result
  }
}
