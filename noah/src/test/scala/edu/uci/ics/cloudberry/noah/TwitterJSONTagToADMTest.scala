package edu.uci.ics.cloudberry.noah

import java.io.File

import edu.uci.ics.cloudberry.gnosis._
import edu.uci.ics.cloudberry.util.Profile._
import org.scalatest.{FlatSpec, Matchers}

class TwitterJSONTagToADMTest extends FlatSpec with Matchers {
  "USGeoGnosis" should "tag the us json file" in {
    val shapeMap = Map.apply(StateLevel -> new File("neo/public/data/state.json"),
                             CountyLevel -> new File("neo/public/data/county.json"),
                             CityLevel -> new File("neo/public/data/city.json"))
    val usGeoGnosis = profile("load shapes")(new USGeoGnosis(shapeMap))
    for (ln <- scala.io.Source.fromURL(getClass.getResource("/sample.json")).getLines()) {
      TwitterJSONTagToADM.tagOneTweet(ln, usGeoGnosis)
    }
  }

}
