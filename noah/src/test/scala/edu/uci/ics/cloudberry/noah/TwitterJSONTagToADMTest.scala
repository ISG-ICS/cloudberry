package edu.uci.ics.cloudberry.noah

import edu.uci.ics.cloudberry.gnosis.USGeoGnosis
import org.scalatest.{FlatSpec, Matchers}
import USGeoGnosis._
import edu.uci.ics.cloudberry.util.Profile._

class TwitterJSONTagToADMTest extends FlatSpec with Matchers {
  "USGeoGnosis" should "tag the us json file" in {
    val shapeMap = Map.apply(StateLevel -> "neo/public/data/state.shape.20m.json",
                             CountyLevel -> "neo/public/data/county.shape.20m.json",
                             CityLevel -> "neo/public/data/cities")
    val propMap = Map.apply(StateLevel -> "neo/public/data/hierarchy/state.json",
                            CountyLevel -> "neo/public/data/hierarchy/county.json",
                            CityLevel -> "neo/public/data/hierarchy/cities.json"
    )
    val usGeoGnosis = profile("load shapes")(new USGeoGnosis(propMap, shapeMap))
    for (ln <- scala.io.Source.fromURL(getClass.getResource("/sample.json")).getLines()) {
      TwitterJSONTagToADM.tagOneTweet(ln, usGeoGnosis)
    }
  }

}
