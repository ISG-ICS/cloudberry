package edu.uci.ics.cloudberry.zion.common

import play.api.Configuration

import scala.concurrent.duration._

class Config(config: Configuration) {

  import Config._

  val AsterixURL = config.getString("asterixdb.url").getOrElse("testing")

  val MySqlURL = config.getString("mysqldb.url").getOrElse("testing")

  val OracleURL = config.getString("oracledb.url").getOrElse("testing")

  val PostgreSqlURL = config.getString("postgresqldb.url").getOrElse("testing")

  val AsterixLang = config.getString("asterixdb.lang").getOrElse("sqlpp").toLowerCase

  val UserTimeOut = config.getString("actor.user.timeout").map(parseTimePair).getOrElse(60 seconds)

  val ViewUpdateInterval = config.getString("view.update.interval").map(parseTimePair).getOrElse(60 minutes)

  val ViewMetaFlushInterval = config.getString("view.meta.flush.interval").map(parseTimePair).getOrElse(60 minutes)

  val DataManagerAppendViewTimeOut = config.getString("datamanager.timeout.appendview").map(parseTimePair).getOrElse(1 day)

  val FirstQueryTimeGap = config.getString("berry.firstquery.gap").map(parseTimePair).getOrElse(2 days)

  val MinTimeGap = config.getString("berry.query.gap").map(parseTimePair).getOrElse(1 day)

  val AgentCollectStatsInterval: FiniteDuration = config.getString("agent.collect.stats.interval").map(parseTimePair).getOrElse(4 hours)
}

object Config {
  def parseTimePair(timeString: String): FiniteDuration = {
    val split = timeString.split("\\s+")
    FiniteDuration(split(0).toLong, split(1))
  }

  val Default = new Config(Configuration.empty)
}
