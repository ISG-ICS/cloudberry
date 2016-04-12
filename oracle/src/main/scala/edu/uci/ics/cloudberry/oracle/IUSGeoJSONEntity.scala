package edu.uci.ics.cloudberry.oracle

import com.vividsolutions.jts.geom.Geometry
import play.api.libs.json.{JsObject, Json}
import USGeoRelationResolver._

sealed trait IUSGeoJSONEntity extends IGeoJSONEntity {
  def geometry: Geometry

  def toJson: JsObject

  def stateID: Int

  def name: String

  def LSAD: String

  def area: Double
}

case class USStateEntity(override val geoID: String,
                         override val stateID: Int,
                         override val name: String,
                         LSAD: String,
                         area: Double,
                         geometry: Geometry
                        ) extends StateProp(geoID, stateID, name) with IUSGeoJSONEntity {
  override def toJson: JsObject =
    Json.obj("geoID" -> geoID, "stateID" -> stateID, "name" -> name, "LSAD" -> LSAD, "area" -> area)
}

case class USCountyEntity(override val geoID: String,
                          override val stateID: Int,
                          override val countyID: Int,
                          override val name: String,
                          LSAD: String,
                          area: Double,
                          geometry: Geometry) extends CountyProp(geoID, stateID, countyID, name) with IUSGeoJSONEntity {
  override def toJson: JsObject =
    Json.obj("geoID" -> geoID, "stateID" -> stateID, "countyID" -> countyID, "name" -> name, "LSAD" -> LSAD, "area" -> area)
}

case class USCityEntity(override val geoID: String,
                        override val stateID: Int,
                        override val countyID: Int,
                        override val cityID: Int,
                        override val name: String,
                        LSAD: String,
                        landArea: Double,
                        waterArea: Double,
                        geometry: Geometry
                       ) extends CityProp(geoID, stateID, countyID, cityID, name) with IUSGeoJSONEntity {
  val area = landArea + waterArea

  override def toJson: JsObject =
    Json.obj("geoID" -> geoID, "stateID" -> stateID, "cityID" -> cityID, "name" -> name, "LSAD" -> LSAD, "area" -> area)
}

object USCityEntity {
  val STATEFP = "STATEFP"
  val PLACEEFP = "PLACEEFP"
  val GEOID = "AFFGEOID"
  val CITYID = "GEOID"
  val ALADN = "ALAND"
  val AWATER = "AWATER"
}

object IUSGeoJSONEntity {
  val GEO_ID = "GEO_ID"
  val STATE = "STATE"
  val NAME = "NAME"
  val LSAD = "LSAD"
  val AREA = "CENSUSAREA"
  val COUNTY = "COUNTY"
  val PLACEEFP = "PLACEFP"

  def apply(props: Seq[USHierarchyProp], map: Map[String, AnyRef], geometry: Geometry): IUSGeoJSONEntity = {
    map.get(PLACEEFP) match {
      case Some(obj) =>
        import USCityEntity._
        val cityID = map.get(CITYID).get.asInstanceOf[String].toInt
        val countyID = props.map(_.asInstanceOf[CityProp]).find(_.cityID == cityID).map(_.countyID).getOrElse(0)
        USCityEntity(
          geoID = map.get(GEOID).get.asInstanceOf[String],
          stateID = map.get(STATEFP).get.asInstanceOf[String].toInt,
          countyID = countyID,
          cityID = cityID,
          name = map.get(NAME).get.asInstanceOf[String],
          LSAD = map.get(LSAD).get.asInstanceOf[String],
          landArea = map.get(ALADN).get.asInstanceOf[Double],
          waterArea = map.get(AWATER).get.asInstanceOf[Double],
          geometry
        )
      case None =>
        map.get(COUNTY) match {
          case Some(obj) =>
            USCountyEntity(
              geoID = map.get(GEO_ID).get.asInstanceOf[String],
              stateID = map.get(STATE).get.asInstanceOf[String].toInt,
              countyID = map.get(COUNTY).get.asInstanceOf[String].toInt,
              name = map.get(NAME).get.asInstanceOf[String],
              LSAD = map.get(LSAD).get.asInstanceOf[String],
              area = map.get(AREA).get.asInstanceOf[Double],
              geometry
            )
          case None => USStateEntity(
            geoID = map.get(GEO_ID).get.asInstanceOf[String],
            stateID = map.get(STATE).get.asInstanceOf[String].toInt,
            name = map.get(NAME).get.asInstanceOf[String],
            LSAD = map.get(LSAD).get.asInstanceOf[String],
            area = map.get(AREA).get.asInstanceOf[Double],
            geometry
          )
        }
    }
  }
}

