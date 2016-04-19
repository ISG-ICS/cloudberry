package edu.uci.ics.cloudberry.gnosis

import com.vividsolutions.jts.geom.Envelope
import play.api.libs.json.Json

case class Rectangle(swLog: Double, swLat: Double, neLog: Double, neLat: Double) {
  def getEnvelopInternal: Envelope = new Envelope(swLog, neLog, swLat, neLat)
}

object Rectangle {
  implicit val rectangularFormat = Json.format[Rectangle]
}

