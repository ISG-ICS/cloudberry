package edu.uci.ics.cloudberry.noah.feed

import java.io.File

import edu.uci.ics.cloudberry.gnosis._
import edu.uci.ics.cloudberry.noah.adm.{UnknownPlaceException, Tweet}
import edu.uci.ics.cloudberry.util.Profile._
import twitter4j.{TwitterException, TwitterObjectFactory}

object TagTweet {
  val shapeMap = Seq( StateLevel -> "web/public/data/state.json",
    CountyLevel -> "web/public/data/county.json",
    CityLevel -> "web/public/data/city.json").toMap

  val usGeoGnosis = profile("loading resource") {
    new USGeoGnosis(shapeMap.mapValues(new File(_)).toMap)
  }

  @throws[UnknownPlaceException]
  @throws[TwitterException]
  def tagOneTweet(ln: String, requireGeoField: Boolean): String = {
    val adm = Tweet.toADM(ln, usGeoGnosis, requireGeoField)
    return adm
  }
}
