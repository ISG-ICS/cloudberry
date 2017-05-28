package edu.uci.ics.cloudberry.gnosis

import java.io.File

import com.vividsolutions.jts.geom.{Coordinate, Envelope}
import edu.uci.ics.cloudberry.util.Rectangle

trait IGnosis {
  def levelShapeMap: Map[TypeLevel, IGeoIndex]

  protected def load(shapeMap: Map[TypeLevel, File]): Map[TypeLevel, IGeoIndex]

  def tagRectangle(level: TypeLevel, rectangle: Rectangle): Seq[IEntity] = {
    levelShapeMap.get(level).get.search(new Envelope(rectangle.swLog, rectangle.neLog, rectangle.swLat, rectangle.neLat))
  }

  def tagPoint(longitude: Double, latitude: Double): Option[IGeoTagInfo]
}
