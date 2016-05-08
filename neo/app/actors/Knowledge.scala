package actors

import edu.uci.ics.cloudberry.gnosis._
import play.api.Environment
import utils.Profile

//TODO nothing is thread-safe, make this knowledge as an Actor
object Knowledge {

  val shapePathMap = Map[TypeLevel, String](StateLevel -> "public/data/state.json",
                                            CountyLevel -> "public/data/county.json",
                                            CityLevel -> "public/data/city.sample.json")
//TODO speedup the debugging for now.       CityLevel -> "public/data/city.json")

  def buildUSKnowledge(playENV: Environment): USGeoGnosis = {
    Profile.profile("build us knowledge") {
      new USGeoGnosis(shapePathMap.mapValues(playENV.getFile))
    }
  }
}

