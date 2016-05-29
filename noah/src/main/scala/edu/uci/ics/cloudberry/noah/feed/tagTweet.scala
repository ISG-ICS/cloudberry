package edu.uci.ics.cloudberry.noah.feed

import java.io.File

import edu.uci.ics.cloudberry.gnosis._
import edu.uci.ics.cloudberry.noah.adm.Tweet
import edu.uci.ics.cloudberry.util.Profile._
import twitter4j.TwitterObjectFactory

import scala.collection.mutable

object tagTweet {
  val shapeMap = mutable.Map.empty[TypeLevel, String]

  shapeMap += StateLevel -> "neo/public/data/state.json"
  shapeMap += CountyLevel -> "neo/public/data/county.json"
  shapeMap += CityLevel -> "neo/public/data/city.json"

  val usGeoGnosis = profile("loading resource") {
    new USGeoGnosis(shapeMap.mapValues(new File(_)).toMap)
  }

  def tagOneTweet(ln: String): String = {
    try {
      val adm = Tweet.toADM(TwitterObjectFactory.createStatus(ln), usGeoGnosis)
      return adm
    } catch {
      case e: Throwable => {
        e.printStackTrace(System.err)
        System.err.println(ln)
      }
    }
    return ""
  }
}
