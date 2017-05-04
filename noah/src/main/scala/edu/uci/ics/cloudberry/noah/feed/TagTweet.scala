package edu.uci.ics.cloudberry.noah.feed

import java.io.File

import edu.uci.ics.cloudberry.gnosis._
import edu.uci.ics.cloudberry.noah.adm.{UnknownPlaceException, Tweet}
import edu.uci.ics.cloudberry.util.Profile._
import twitter4j.{TwitterException, TwitterObjectFactory}

object TagTweet {
  val shapeMap = Seq( StateLevel -> "twittermap/public/data/state.json",
    CountyLevel -> "twittermap/public/data/county.json",
    CityLevel -> "twittermap/public/data/city.json").toMap

  val usGeoGnosis = profile("loading resource") {
    new USGeoGnosis(shapeMap.mapValues(new File(_)).toMap)
  }

  @throws[UnknownPlaceException]
  @throws[TwitterException]
  def tagOneTweet(ln: String, requireGeoField: Boolean): String = {
    val adm = Tweet.toADM(TwitterObjectFactory.createStatus(ln), usGeoGnosis, requireGeoField)
    return adm
  }
}
