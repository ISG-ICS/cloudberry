package edu.uci.ics.cloudberry.noah

import edu.uci.ics.cloudberry.gnosis.USGeoGnosis._
import edu.uci.ics.cloudberry.gnosis.{TypeLevel, USGeoGnosis}
import edu.uci.ics.cloudberry.noah.adm.Tweet
import edu.uci.ics.cloudberry.util.Profile._
import twitter4j.TwitterObjectFactory

import scala.collection.mutable

object TwitterJSONTagToADM {
  // TODO reserve all the fields and just replace the date and geo location to different part.
  // and only change it to the accept asterix json format, then we can use the feed in Asterix to transfer the format
  // without to much effort.
  val propMap = mutable.Map.empty[TypeLevel, String]
  val shapeMap = mutable.Map.empty[TypeLevel, String]

  val usage =
    """
      |Usage: USHierarchyBuilder -state /path/to/state.json -county /path/to/county.json -city /path/to/city.json
      | -helpstate /path/to/stateprop.json -helpcouty /path/to/countyprop.json -helpcicy /path/to/cityprop.json
      |It will read the status from stdIn, geoTag city/county/state information, and then convert it to ADM format
    """.stripMargin

  def parseOption(list: List[String]) {
    list match {
      case Nil =>
      case "-h" :: tail => System.err.println(usage); System.exit(0)
      case "-state" :: value :: tail => shapeMap += StateLevel -> value; parseOption(tail)
      case "-county" :: value :: tail => shapeMap += CountyLevel -> value; parseOption(tail)
      case "-city" :: value :: tail => shapeMap += CityLevel -> value; parseOption(tail)
      case "-helpstate" :: value :: tail => propMap += StateLevel -> value; parseOption(tail)
      case "-helpcounty" :: value :: tail => propMap += CountyLevel -> value; parseOption(tail)
      case "-helpcity" :: value :: tail => propMap += CityLevel -> value; parseOption(tail)
      case option :: tail => System.err.println("unknown option:" + option); System.err.println(usage); System.exit(1);
    }
  }

  def tagOneTweet(ln: String, usGeoGnosis: USGeoGnosis) = {
    try {
      println(Tweet.toADM(TwitterObjectFactory.createStatus(ln), usGeoGnosis))
    } catch {
      case e: Throwable => {
        e.printStackTrace(System.err)
        System.err.println(ln)
      }
    }
  }

  def main(args: Array[String]): Unit = {
    parseOption(args.toList)
    val usGeoGnosis = profile("loading resource") {
      new USGeoGnosis(propMap.toMap, shapeMap.toMap)
    }
    for (ln <- scala.io.Source.stdin.getLines()) {
      tagOneTweet(ln, usGeoGnosis)
    }
  }
}
