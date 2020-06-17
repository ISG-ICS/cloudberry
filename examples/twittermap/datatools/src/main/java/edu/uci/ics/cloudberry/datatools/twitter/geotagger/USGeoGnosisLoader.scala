package edu.uci.ics.cloudberry.datatools.twitter.geotagger

import edu.uci.ics.cloudberry.gnosis._
import java.io.File

/**
  * USGeoGnosisLoader
  *  - Help load a Scala USGeoGnosis Object with given 3 geo-json file paths,
  *  and the returned USGeoGnosis object can be used in Java
  *
  * @author: Qiushi Bai
  */
object USGeoGnosisLoader {
  def loadUSGeoGnosis(stateJsonFile: String, countyJsonFile: String, cityJsonFile: String): USGeoGnosis = {
    val shapeMap = Seq( StateLevel -> stateJsonFile,
      CountyLevel -> countyJsonFile,
      CityLevel -> cityJsonFile).toMap
    val usGeoGnosis = new USGeoGnosis(shapeMap.mapValues(new File(_)))
    usGeoGnosis
  }
}
