package edu.uci.ics.cloudberry.gnosis

import com.vividsolutions.jts.geom.Geometry
import play.api.libs.json.{Json, JsObject}

sealed trait INYGeoJSONEntity extends IEntity{
  def geometry: Geometry

  def name: String

  def toJson: JsObject

  def toPropertyMap: Map[String, AnyRef]

}

case class NYBoroughEntity(boroCode: Int,
                           boroName: String,
                           geometry: Geometry
                          ) extends INYGeoJSONEntity {

  override def toJson: JsObject =
    Json.obj("boroCode" -> boroCode, "boroName" ->boroName);
  override val level: TypeLevel = BoroLevel

  override val parentLevel: TypeLevel = BoroLevel

  override val key: Long = boroCode.toLong
  override val parentKey: Long = boroCode.toLong

  override val name: String = boroName;

  override def toPropertyMap: Map[String, AnyRef] = Map[String, AnyRef](
    "boroCode" -> Int.box(boroCode),
    "boroName" -> boroName)
}

case class NYNeighborEntity(neighborID: Int,
                            neighborName: String,
                            boroCode: Int,
                            boroName: String,
                            geometry: Geometry
                           ) extends INYGeoJSONEntity {
  override def toJson: JsObject = Json.obj(
    "neighborID" ->neighborID, "neighborName" ->neighborName, "boroCode" -> boroCode, "boroName" -> boroName);
  override val level: TypeLevel = NeighborLevel

  override val parentLevel: TypeLevel = BoroLevel
  override val key: Long = neighborID.toLong
  override val parentKey: Long = boroCode.toLong

  override val name: String = neighborName

  override def toPropertyMap: Map[String, AnyRef] = Map[String, AnyRef](
    "neighborID" -> Int.box(neighborID),
    "neighborName" -> neighborName,
    "boroCode" -> Int.box(boroCode),
    "boroName" -> boroName
  )

}

object INYGeoJSONEntity {

  def apply(map: Map[String, AnyRef], geometry: Geometry): INYGeoJSONEntity = {
    map.get("NTAName") match {
      case Some(obj) => NYNeighborEntity(
        neighborID = map.get("OBJECTID").get.asInstanceOf[Int],
        neighborName = map.get("NTAName").get.asInstanceOf[String],
        boroCode = map.get("BoroCode").get.asInstanceOf[Int],
        boroName = map.get("BoroName").get.asInstanceOf[String],
        geometry
      )

      case None => NYBoroughEntity(
          boroCode = map.get("BoroCode").get.asInstanceOf[Int],
          boroName = map.get("BoroName").get.asInstanceOf[String],
          geometry
      )
    }
  }
}

