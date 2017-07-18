package edu.uci.ics.cloudberry.noah

import java.io.File

import edu.uci.ics.cloudberry.gnosis._
import org.scalatest.{FlatSpec, Matchers}
import edu.uci.ics.cloudberry.util.Profile._

class TwitterJSONTagToADMTest extends FlatSpec with Matchers {
  "USGeoGnosis" should "tag the us json file" in {
    val shapeMap = Map.apply(StateLevel -> new File("web/public/data/state.json"),
                             CountyLevel -> new File("web/public/data/county.json"),
                             CityLevel -> new File("web/public/data/city.json"))
    val usGeoGnosis = profile("load shapes")(new USGeoGnosis(shapeMap))
    for (ln <- scala.io.Source.fromURL(getClass.getResource("/sample.json")).getLines()) {
      TwitterJSONTagToADM.tagOneTweet(ln, usGeoGnosis)
    }
  }

}
