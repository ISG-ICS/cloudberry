package edu.uci.ics.cloudberry.gnosis

import com.vividsolutions.jts.geom.Geometry
import play.api.libs.json.{JsObject, Json}

sealed trait IUSGeoJSONEntity extends IGeoJSONEntity {
  def geometry: Geometry

  def stateID: Int

  def name: String

  def area: Double

  def toJson: JsObject

  /**
    * Used to output the GeoJson format
    *
    * @return
    */
  def toPropertyMap: Map[String, AnyRef]

}

case class USStateEntity(geoID: String,
                         stateID: Int,
                         name: String,
                         area: Double,
                         geometry: Geometry
                        ) extends IUSGeoJSONEntity {
  override def toJson: JsObject =
    Json.obj("geoID" -> geoID, "stateID" -> stateID, "name" -> name, "area" -> area)

  override val level: TypeLevel = StateLevel

  override val parentLevel: TypeLevel = StateLevel

  override val key: Long = stateID.toLong

  override val parentKey: Long = stateID.toLong

  /**
    * Used to output the GeoJson format
    *
    * @return
    */
  override def toPropertyMap: Map[String, AnyRef] = Map[String, AnyRef](
    "geoID" -> geoID,
    "stateID" -> Int.box(stateID),
    "name" -> name,
    "area" -> Double.box(area))
}

case class USCountyEntity(geoID: String,
                          stateID: Int,
                          stateName: String,
                          countyID: Int,
                          name: String,
                          area: Double,
                          geometry: Geometry) extends IUSGeoJSONEntity {
  override def toJson: JsObject =
    Json.obj("geoID" -> geoID, "stateID" -> stateID, "countyID" -> countyID, "name" -> name, "area" -> area)

  override val level: TypeLevel = CountyLevel

  override val parentLevel: TypeLevel = StateLevel

  override val key: Long = countyID

  override val parentKey: Long = stateID

  override def toPropertyMap: Map[String, AnyRef] = Map[String, AnyRef](
    "geoID" -> geoID,
    "stateID" -> Int.box(stateID),
    "stateName" -> stateName,
    "countyID" -> Int.box(countyID),
    "name" -> name,
    "area" -> Double.box(area))
}

case class USCityEntity(geoID: String,
                        stateID: Int,
                        stateName: String,
                        countyID: Int,
                        countyName: String,
                        cityID: Int,
                        name: String,
                        landArea: Double,
                        waterArea: Double,
                        geometry: Geometry
                       ) extends IUSGeoJSONEntity {
  val area = landArea + waterArea

  override def toJson: JsObject =
    Json.obj("geoID" -> geoID, "stateID" -> stateID, "cityID" -> cityID, "name" -> name, "area" -> area)

  override val level: TypeLevel = CityLevel

  override val parentLevel: TypeLevel = if (countyID != 0) CountyLevel else StateLevel

  override val key: Long = cityID

  override val parentKey: Long = if (countyID != 0) countyID.toLong else stateID.toLong


  override def toPropertyMap: Map[String, AnyRef] = Map[String, AnyRef](
    "geoID" -> geoID,
    "stateID" -> Int.box(stateID),
    "stateName" -> stateName,
    "countyID" -> Int.box(countyID),
    "countyName" -> countyName,
    "cityID" -> Int.box(cityID),
    "name" -> name,
    "landArea" -> Double.box(landArea),
    "waterArea" -> Double.box(waterArea))
}

object IUSGeoJSONEntity {

  def apply(map: Map[String, AnyRef], geometry: Geometry): IUSGeoJSONEntity = {
    map.get("cityID") match {
      case Some(obj) =>
        USCityEntity(
          geoID = map.get("geoID").get.asInstanceOf[String],
          stateID = map.get("stateID").get.asInstanceOf[Int],
          stateName = map.get("stateName").get.asInstanceOf[String],
          countyID = map.get("countyID").get.asInstanceOf[Int],
          countyName = map.get("countyName").get.asInstanceOf[String],
          cityID = map.get("cityID").get.asInstanceOf[Int],
          name = map.get("name").get.asInstanceOf[String],
          landArea = map.get("landArea").get.asInstanceOf[Double],
          waterArea = map.get("waterArea").get.asInstanceOf[Double],
          geometry
        )
      case None =>
        map.get("countyID") match {
          case Some(obj) =>
            USCountyEntity(
              geoID = map.get("geoID").get.asInstanceOf[String],
              stateID = map.get("stateID").get.asInstanceOf[Int],
              stateName = map.get("stateName").get.asInstanceOf[String],
              countyID = map.get("countyID").get.asInstanceOf[Int],
              name = map.get("name").get.asInstanceOf[String],
              area = map.get("area").get.asInstanceOf[Double],
              geometry
            )
          case None => USStateEntity(
            geoID = map.get("geoID").get.asInstanceOf[String],
            stateID = map.get("stateID").get.asInstanceOf[Int],
            name = map.get("name").get.asInstanceOf[String],
            area = map.get("area").get.asInstanceOf[Double],
            geometry
          )
        }
    }
  }
}

