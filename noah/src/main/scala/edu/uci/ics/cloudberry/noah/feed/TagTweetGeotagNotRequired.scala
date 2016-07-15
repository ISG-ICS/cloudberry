package edu.uci.ics.cloudberry.noah.feed

import java.io.File

import edu.uci.ics.cloudberry.gnosis._
import edu.uci.ics.cloudberry.noah.adm.TweetGeotagNotRequired
import edu.uci.ics.cloudberry.util.Profile._
import twitter4j.{TwitterException, TwitterObjectFactory}

object TagTweetGeotagNotRequired {
  var shapeMap = Seq( StateLevel -> "neo/public/data/state.json",
    CountyLevel -> "neo/public/data/county.json",
    CityLevel -> "neo/public/data/city.json").toMap

  val usGeoGnosis = profile("loading resource") {
    new USGeoGnosis(shapeMap.mapValues(new File(_)).toMap)
  }

  @throws[TwitterException]
  def tagOneTweet(ln: String): String = {
    val adm = TweetGeotagNotRequired.toADM(TwitterObjectFactory.createStatus(ln), usGeoGnosis)
    return adm
  }
}
