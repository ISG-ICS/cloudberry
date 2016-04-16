package edu.uci.ics.cloudberry.gnosis

import play.api.libs.json._
import play.api.libs.functional.syntax._


class USAnnotationHelper(levelMap: Map[TypeLevel, String]) {

  import USAnnotationHelper._
  import USGeoGnosis._

  val stateProps: Seq[StateProp] = loadJSON(levelMap.get(StateLevel).get).map(_.as[StateProp])
  val countyProp: Seq[CountyProp] = loadJSON(levelMap.get(CountyLevel).get).map(_.as[CountyProp])
  val cityProp: Seq[CityProp] = loadJSON(levelMap.get(CityLevel).get).map(_.as[CityProp])

  private def loadJSON(jsonText: String): Seq[JsValue] = Json.parse(jsonText).asInstanceOf[JsArray].value

}

object USAnnotationHelper {
  sealed trait HelperProp

  case class StateProp(val geoID: String,
                       val stateID: Int,
                       val name: String) extends HelperProp

  case class CountyProp(val geoID: String,
                        val stateID: Int,
                        val stateName: String,
                        val countyID: Int,
                        val name: String) extends HelperProp

  case class CityProp(val geoID: String,
                      val stateID: Int,
                      val stateName: String,
                      val countyID: Int,
                      val countyName: String,
                      val cityID: Int,
                      val name: String) extends HelperProp

  implicit val stateReader: Reads[StateProp] = (
    (JsPath \ "geoID").read[String] and
      (JsPath \ "stateID").read[Int] and
      (JsPath \ "name").read[String]) (StateProp.apply _)

  implicit val countyReader: Reads[CountyProp] = (
    (JsPath \ "geoID").read[String] and
      (JsPath \ "stateID").read[Int] and
      (JsPath \ "stateName").read[String] and
      (JsPath \ "countyID").read[Int] and
      (JsPath \ "name").read[String]) (CountyProp.apply _)

  implicit val citiReader: Reads[CityProp] = (
    (JsPath \ "geoID").read[String] and
      (JsPath \ "stateID").read[Int] and
      (JsPath \ "stateName").read[String] and
      (JsPath \ "countyID").read[Int] and
      (JsPath \ "countyName").read[String] and
      (JsPath \ "cityID").read[Int] and
      (JsPath \ "name").read[String]) (CityProp.apply _)

}
