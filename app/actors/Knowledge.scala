package actors

import com.esri.core.geometry.Polygon
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

object Knowledge {
  def loadFromDB = Future {}

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

