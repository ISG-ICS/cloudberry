package edu.uci.ics.cloudberry.gnosis

import com.vividsolutions.jts.geom.Geometry
import edu.uci.ics.cloudberry.gnosis.USAnnotationHelper.{CityProp, CountyProp}
import play.api.libs.json.{JsObject, Json}
import USGeoGnosis._

sealed trait IUSGeoJSONEntity extends IGeoJSONEntity {
  def geometry: Geometry

  def toJson: JsObject

  def stateID: Int

  def name: String

  def LSAD: String

  def area: Double
}

case class USStateEntity(geoID: String,
                         stateID: Int,
                         name: String,
                         LSAD: String,
                         area: Double,
                         geometry: Geometry
                        ) extends IUSGeoJSONEntity {
  override def toJson: JsObject =
    Json.obj("geoID" -> geoID, "stateID" -> stateID, "name" -> name, "LSAD" -> LSAD, "area" -> area)

  override def level: TypeLevel = StateLevel

  override def parentLevel: TypeLevel = StateLevel

  override def key: Long = stateID.toLong

  override def parentKey: Long = stateID.toLong
}

case class USCountyEntity(geoID: String,
                          stateID: Int,
                          stateName: String,
                          countyID: Int,
                          name: String,
                          LSAD: String,
                          area: Double,
                          geometry: Geometry) extends IUSGeoJSONEntity {
  override def toJson: JsObject =
    Json.obj("geoID" -> geoID, "stateID" -> stateID, "countyID" -> countyID, "name" -> name, "LSAD" -> LSAD, "area" -> area)

  override def level: TypeLevel = CountyLevel

  override def parentLevel: TypeLevel = StateLevel

  override def key: Long = stateID * 1000l + countyID

  override def parentKey: Long = stateID
}

case class USCityEntity(geoID: String,
                        stateID: Int,
                        stateName: String,
                        countyID: Int,
                        countyName: String,
                        cityID: Int,
                        name: String,
                        LSAD: String,
                        landArea: Double,
                        waterArea: Double,
                        geometry: Geometry
                       ) extends IUSGeoJSONEntity {
  val area = landArea + waterArea

  override def toJson: JsObject =
    Json.obj("geoID" -> geoID, "stateID" -> stateID, "cityID" -> cityID, "name" -> name, "LSAD" -> LSAD, "area" -> area)

  override def level: TypeLevel = CityLevel

  override def parentLevel: TypeLevel = if (countyID != 0) CountyLevel else StateLevel

  override def key: Long = cityID

  override def parentKey: Long = if (countyID != 0) stateID * 1000l + countyID else stateID.toLong
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
  val Unknown = "unknown"

  def apply(props: Seq[USAnnotationHelper.HelperProp], map: Map[String, AnyRef], geometry: Geometry): IUSGeoJSONEntity = {
    map.get(PLACEEFP) match {
      case Some(obj) =>
        import USCityEntity._
        val cityID = map.get(CITYID).get.asInstanceOf[String].toInt
        val cityPropOpt = props.map(_.asInstanceOf[CityProp]).find(_.cityID == cityID)
        val countyID = cityPropOpt.map(_.countyID).getOrElse(0)
        val countyName: String = cityPropOpt.map(_.countyName).getOrElse(Unknown)
        val stateName = cityPropOpt.map(_.stateName).getOrElse(Unknown)
        USCityEntity(
          geoID = map.get(GEOID).get.asInstanceOf[String],
          stateID = map.get(STATEFP).get.asInstanceOf[String].toInt,
          stateName = stateName,
          countyID = countyID,
          countyName = countyName,
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
            val geoID = map.get(GEO_ID).get.asInstanceOf[String]
            val countyPropOpt = props.map(_.asInstanceOf[CountyProp]).find(_.geoID == geoID)
            val stateName = countyPropOpt.map(_.stateName).getOrElse(Unknown)
            USCountyEntity(
              geoID = geoID,
              stateID = map.get(STATE).get.asInstanceOf[String].toInt,
              stateName = stateName,
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

