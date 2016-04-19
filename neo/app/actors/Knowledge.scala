package actors

import edu.uci.ics.cloudberry.gnosis.USGeoGnosis._
import edu.uci.ics.cloudberry.gnosis._
import play.api.Environment
import utils.Profile

object Knowledge {

  val shapePathMap = Map[TypeLevel, String](StateLevel -> "public/data/state.shape.20m.json",
                                            CountyLevel -> "public/data/county.shape.20m.json",
                                            CityLevel -> "public/data/cities")
  val propPathMap = Map[TypeLevel, String](StateLevel -> "public/data/hierarchy/state.json",
                                           CountyLevel -> "public/data/hierarchy/county.json",
                                           CityLevel -> "public/data/hierarchy/cities.json")

  def buildUSKnowledge(playENV: Environment): USGeoGnosis = {
    Profile.profile("build us knowledge") {
      new USGeoGnosis(propPathMap.mapValues(playENV.getFile), shapePathMap.mapValues(playENV.getFile))
    }
  }
}

