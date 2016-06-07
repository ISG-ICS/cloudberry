package edu.uci.ics.cloudberry.noah.feed

import java.io.File

import edu.uci.ics.cloudberry.gnosis._
import edu.uci.ics.cloudberry.noah.adm.{UnknownPlaceException, Tweet}
import edu.uci.ics.cloudberry.util.Profile._
import twitter4j.{TwitterException, TwitterObjectFactory}

object TagTweet {
  var shapeMap = Seq( StateLevel -> "neo/public/data/state.json",
    CountyLevel -> "neo/public/data/county.json",
    CityLevel -> "neo/public/data/city.json").toMap

  val usGeoGnosis = profile("loading resource") {
    new USGeoGnosis(shapeMap.mapValues(new File(_)).toMap)
  }

  @throws[UnknownPlaceException]
  @throws[TwitterException]
  def tagOneTweet(ln: String): String = {
    val adm = Tweet.toADM(TwitterObjectFactory.createStatus(ln), usGeoGnosis)
    return adm
  }
}
