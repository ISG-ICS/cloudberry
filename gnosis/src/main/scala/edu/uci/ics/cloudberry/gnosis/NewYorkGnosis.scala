package edu.uci.ics.cloudberry.gnosis

import java.io.{File, FilenameFilter}

import com.vividsolutions.jts.geom.{Coordinate, Envelope, Geometry}
import play.api.libs.json.{JsObject, Json, Writes}

class NewYorkGnosis(levelGeoPathMap: Map[TypeLevel, File]) extends IGnosis{

  import NewYorkGnosis._

  val levelShapeMap = load(levelGeoPathMap)

  override def load(shapeMap: Map[TypeLevel, File]): Map[TypeLevel, NYGeoJSONIndex] = {
    NYLevels.map(level => {
      val index = new NYGeoJSONIndex()
      loadShape(shapeMap.get(level).get, index)(INYGeoJSONEntity.apply)
      level -> index
    }).toMap
  }

  lazy val neighbors: Seq[NYNeighborEntity] = {
    levelShapeMap.get(NeighborLevel).get.entities.map(_.asInstanceOf[NYNeighborEntity])
  }

  lazy val neighborShapes: IGeoIndex = levelShapeMap.get(NeighborLevel).get

  // used in geo tag
  override def tagPoint(longitude: Double, latitude: Double): Option[NYGeoTagInfo] = {
    val box = new Envelope(new Coordinate(longitude, latitude))
    neighborShapes.search(box).headOption.map(entity => NYGeoTagInfo(entity.asInstanceOf[NYNeighborEntity]))
  }

}

object NewYorkGnosis {

  case class NYGeoTagInfo(neighborID: Int, neighborName: String,
                          boroCode: Int, boroName: String) extends IGeoTagInfo{
    override def toString: String = Json.toJson(this).asInstanceOf[JsObject].toString()
  }

  object NYGeoTagInfo {
    implicit val writer: Writes[NYGeoTagInfo] = Json.writes[NYGeoTagInfo]

    def apply(entity: NYNeighborEntity): NYGeoTagInfo = {
      entity match {
        case neighbor: NYNeighborEntity => NYGeoTagInfo(boroCode = neighbor.boroCode, boroName = neighbor.boroName,
          neighborID = neighbor.neighborID, neighborName = neighbor.neighborName)
      }
    }
  }

  def loadShape(file: File, index: NYGeoJSONIndex)(builder: (Map[String, AnyRef], Geometry) => INYGeoJSONEntity): Unit = {
    if (file.isDirectory) {
      file.listFiles(new FilenameFilter {
        override def accept(dir: File, name: String): Boolean = name.endsWith(".json")
      }).foreach { file =>
        loadShape(file, index)(builder)
      }
    } else {
      val textJson = loadSmallJSONFile(file)
      index.loadShape(textJson)(builder)
    }
  }
}
