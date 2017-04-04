package edu.uci.ics.cloudberry.util

// TODO can change the logging method by using Logger.info and get rid of self-defined Logging class
object Profile extends Logging {

  def profile[R](tag: String)(block: => R): R = {
    val tick = System.currentTimeMillis()
    val result = block
    val tock = System.currentTimeMillis()
    log.info(s"$tag time: ${tock - tick} ms")
    result
  }
}
