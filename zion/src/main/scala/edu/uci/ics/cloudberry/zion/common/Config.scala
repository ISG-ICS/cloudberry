package edu.uci.ics.cloudberry.zion.common

import play.api.Configuration

import scala.concurrent.duration._

class Config(config: Configuration) {
  def parseTimePair(timeString: String) = {
    val split = timeString.split("\\s+")
    FiniteDuration(split(0).toLong, split(1))
  }

  val AsterixURL = config.getString("asterixdb.url").getOrElse("testing")

  val AwaitInitial = config.getString("neo.timeout.initial").map(parseTimePair).getOrElse(10 minutes)

  val UserTimeOut = config.getString("actor.user.timeout").map(parseTimePair).getOrElse(5 seconds)

  val CacheTimeOut = config.getString("actor.cache.timeout").map(parseTimePair).getOrElse(1 minutes)

  val ViewTimeOut = config.getString("actor.view.timeout").map(parseTimePair).getOrElse(1 minutes)

  val ViewUpdateInterval = config.getString("view.update.interval").map(parseTimePair).getOrElse(30 minutes)

  val ViewMetaFlushInterval = config.getString("view.meta.flush.interval").map(parseTimePair).getOrElse(30 minutes)
}
