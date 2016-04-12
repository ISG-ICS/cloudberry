package edu.uci.ics.cloudberry.oracle

import play.api.libs.json._
import play.api.libs.functional.syntax._

trait IRelationResolver {

  def getChildren(entity: IEntity): Seq[IEntity]

  def getParent(entity: IEntity): Option[IEntity]
}

class USGeoRelationResolver(stateJson: String, countJson: String, cityJson: String) extends IRelationResolver {

  import USGeoRelationResolver._

  val stateProps: Seq[StateProp] = loadJSON(stateJson).map(_.as[StateProp])
  val countyProp: Seq[CountyProp] = loadJSON(countJson).map(_.as[CountyProp])
  val cityProp: Seq[CityProp] = loadJSON(cityJson).map(_.as[CityProp])

  private def loadJSON(jsonText: String): Seq[JsValue] = Json.parse(jsonText).asInstanceOf[JsArray].value

  override def getChildren(entity: IEntity): Seq[IGeoJSONEntity] = {
    //TODO optimize it to operate binary search
    entity.level match {
      case StateLevel => countyProp.filter(_.parentKey == entity.key)
      case CountyLevel => cityProp.filter(_.parentKey == entity.key)
      case CityLevel => Seq.empty[IGeoJSONEntity]
      case _ => throw new UnknownEntityException(entity)
    }
  }

  override def getParent(entity: IEntity): Option[IGeoJSONEntity] = {
    entity.level match {
      case StateLevel => None
      case CountyLevel => stateProps.find(_.key == entity.parentKey)
      case CityLevel => countyProp.find(_.key == entity.parentKey)
    }
  }
}

object USGeoRelationResolver {
  val StateLevel = 1
  val CountyLevel = 2
  val CityLevel = 3

  sealed trait USHierarchyProp extends IGeoJSONEntity

  class StateProp(val geoID: String, val stateID: Int, val name: String) extends USHierarchyProp {
    val level = StateLevel
    val key = stateID.toLong
    val parentKey = 0l
  }

  object StateProp {
    def apply(geoID: String, stateID: Int, name: String): StateProp = new StateProp(geoID, stateID, name)
  }

  class CountyProp(val geoID: String, val stateID: Int, val countyID: Int, val name: String) extends USHierarchyProp {
    val level = CountyLevel
    val key = stateID.toLong * 1000 + countyID
    val parentKey = stateID.toLong
  }

  object CountyProp {
    def apply(geoID: String, stateID: Int, countyID: Int, name: String): CountyProp =
      new CountyProp(geoID, stateID, countyID, name)
  }

  class CityProp(val geoID: String, val stateID: Int, val countyID: Int, val cityID: Int, val name: String) extends USHierarchyProp {
    val level = CityLevel
    val key = cityID.toLong
    val parentKey = stateID.toLong * 1000 + countyID
  }

  object CityProp {
    def apply(geoID: String, stateID: Int, countyID: Int, cityID: Int, name: String): CityProp =
      new CityProp(geoID, stateID, countyID, cityID, name)
  }

  implicit val stateReader: Reads[StateProp] = (
    (JsPath \ "geoID").read[String] and
      (JsPath \ "stateID").read[Int] and
      (JsPath \ "name").read[String]) (StateProp.apply _)

  implicit val countyReader: Reads[CountyProp] = (
    (JsPath \ "geoID").read[String] and
      (JsPath \ "stateID").read[Int] and
      (JsPath \ "countyID").read[Int] and
      (JsPath \ "name").read[String]) (CountyProp.apply _)

  implicit val citiReader: Reads[CityProp] = (
    (JsPath \ "geoID").read[String] and
      (JsPath \ "stateID").read[Int] and
      (JsPath \ "countyID").read[Int] and
      (JsPath \ "cityID").read[Int] and
      (JsPath \ "name").read[String]) (CityProp.apply _)

}