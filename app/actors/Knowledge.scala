package actors

import com.esri.core.geometry.Polygon

object Knowledge {
  def loadFromDB = {}

  def geoTag(polygon: Polygon, level: Int): Seq[String] = {
    return Seq("CA", "NV", "AZ")
  }

  def decompose(entity: String): Seq[String] = {
    return Seq("CA-LA", "CA-Orange County")
  }

  // dataset information
  // dataverse
  // field to indexed on . etc.
}

