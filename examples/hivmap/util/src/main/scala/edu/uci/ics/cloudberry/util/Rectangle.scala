package edu.uci.ics.cloudberry.util

import play.api.libs.json.Json

case class Rectangle(swLog: Double, swLat: Double, neLog: Double, neLat: Double)

object Rectangle {
  implicit val rectangularFormat = Json.format[Rectangle]
}

