package edu.uci.ics.cloudberry.noah

import java.io.File
import java.util.concurrent.Executors

import edu.uci.ics.cloudberry.gnosis._
import edu.uci.ics.cloudberry.noah.adm.Tweet
import edu.uci.ics.cloudberry.util.Profile._
import twitter4j.TwitterObjectFactory

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, ExecutionContext, Future}

object TwitterJSONTagToADM {
  // TODO reserve all the fields and just replace the date and geo location to different part.
  // and only change it to the accept asterix json format, then we can use the feed in Asterix to transfer the format
  // without to much effort.
  val shapeMap = mutable.Map.empty[TypeLevel, String]
  var threadNumber = 2
  var isDebug = false

  val usage =
    """
      |Usage: USHierarchyBuilder -state /path/to/state.json -county /path/to/county.json -city /path/to/city.json
      |It will read the status from stdIn, geoTag city/county/state information, and then convert it to ADM format
    """.stripMargin

  def parseOption(list: List[String]) {
    list match {
      case Nil =>
      case "-h" :: tail => System.err.println(usage); System.exit(0)
      case "-state" :: value :: tail => shapeMap += StateLevel -> value; parseOption(tail)
      case "-county" :: value :: tail => shapeMap += CountyLevel -> value; parseOption(tail)
      case "-city" :: value :: tail => shapeMap += CityLevel -> value; parseOption(tail)
      case "-thread" :: value :: tail => threadNumber = value.toInt; parseOption(tail)
      case "-debug" :: value :: tail => isDebug = true; parseOption(tail)
      case option :: tail => System.err.println("unknown option:" + option); System.err.println(usage); System.exit(1);
    }
  }

  def tagOneTweet(ln: String, usGeoGnosis: USGeoGnosis) = {
    try {
      val adm = Tweet.toADM(TwitterObjectFactory.createStatus(ln), usGeoGnosis, true)
      if (adm.length > 0) println(adm)
    } catch {
      case e: Throwable => {
        if (isDebug) {
          e.printStackTrace(System.err)
          System.err.println(ln)
        }
      }
    }
  }

  //TODO make a parallel version of this one
  def main(args: Array[String]): Unit = {
    parseOption(args.toList)
    val usGeoGnosis = profile("loading resource") {
      new USGeoGnosis(shapeMap.mapValues(new File(_)).toMap)
    }

    val thpool = Executors.newFixedThreadPool(threadNumber)
    implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(thpool)

    val buffer = new ArrayBuffer[String](threadNumber)

    for (ln <- scala.io.Source.stdin.getLines()) {
      buffer += ln
      if (buffer.size == threadNumber) {
        val f = Future.traverse(buffer) { tw => Future(tagOneTweet(tw, usGeoGnosis)) }
        Await.result(f, scala.concurrent.duration.Duration.Inf)
        buffer.clear()
      }
    }
    buffer.foreach(tagOneTweet(_, usGeoGnosis))
    thpool.shutdownNow()
  }
}
