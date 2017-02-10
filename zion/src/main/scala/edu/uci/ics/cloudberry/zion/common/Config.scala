package edu.uci.ics.cloudberry.zion.common

import play.api.Configuration
import play.api.Logger

import scala.concurrent.duration._

class Config(config: Configuration) {

  import Config._

  val AsterixURL = config.getString("asterixdb.url").getOrElse("testing")

  val AwaitInitial = config.getString("neo.timeout.initial").map(parseTimePair).getOrElse(10 minutes)

  val MaxFrameLengthForNeoWS = config.getString("neo.stream.max.frame.length").map(parseFrameLengthLimit).getOrElse(8 * MemorySize.MB)

  val UserTimeOut = config.getString("actor.user.timeout").map(parseTimePair).getOrElse(60 seconds)

  val ViewUpdateInterval = config.getString("view.update.interval").map(parseTimePair).getOrElse(60 minutes)

  val ViewMetaFlushInterval = config.getString("view.meta.flush.interval").map(parseTimePair).getOrElse(60 minutes)

  val DataManagerAppendViewTimeOut = config.getString("datamanager.timeout.appendview").map(parseTimePair).getOrElse(1 day)

  val FirstQueryTimeGap = config.getString("berry.firstquery.gap").map(parseTimePair).getOrElse(2 days)

  val MinTimeGap = config.getString("berry.query.gap").map(parseTimePair).getOrElse(1 day)
}

object MemorySize extends Enumeration {
  val KB: Int = 1024
  val MB: Int = 1024*1024
  val GB: Int = 1024*1024*1024
}

object Config {
  def parseTimePair(timeString: String): FiniteDuration = {
    val split = timeString.split("\\s+")
    FiniteDuration(split(0).toLong, split(1))
  }

  def parseFrameLengthLimit(memoryString: String): Int = {
    val size = memoryString.substring(memoryString.length-2)
    val num = memoryString.substring(0, memoryString.length-2).trim.toDouble
    val res = size match {
      case "KB" => (num * MemorySize.KB).toLong
      case "MB" => (num * MemorySize.MB).toLong
      case "GB" => (num * MemorySize.GB).toLong
      case _ =>
        Logger.logger.error("The neo.stream.max.frame.length in application.conf " +
          "accepts configuration only in KB, MB or GB. Use default value 8 MB instead")
        8 * MemorySize.MB
    }
    // Minimum 1KB, maximum 2GB
    if(res < 0){
      Logger.logger.error("The neo.stream.max.frame.length in application.conf accepts only positive numbers. " +
        "Use default value 8 MB instead")
      8 * MemorySize.MB
    } else if(res >= 0 && res < 1024){
      Logger.logger.error("The neo.stream.max.frame.length in application.conf is 1KB minimum, 2GB maximum. " +
        "Change the setting to 1KB.")
      1 * MemorySize.KB
    } else if(res >= 1024 && res <= Int.MaxValue){
      res.toInt
    } else if(res == Int.MaxValue.toLong + 1){
      Int.MaxValue
    } else{
      Logger.logger.error("The neo.stream.max.frame.length in application.conf is 1KB minimum, 2GB maximum. " +
        "Change the setting to 2GB.")
      Int.MaxValue
    }
  }

  val Default = new Config(Configuration.empty)
}
