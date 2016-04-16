package edu.uci.ics.cloudberry.gnosis

import java.io.File

import edu.uci.ics.cloudberry.gnosis.USAnnotationHelper.{CityProp, CountyProp, StateProp}
import play.api.libs.json.{JsArray, Json}

private class USGeoGnosis(levelPropPathMap: Map[TypeLevel, String], levelGeoPathMap: Map[TypeLevel, String]) {

  import USGeoGnosis._

  val levelShapeMap: Map[TypeLevel, USGeoJSONIndex] = load(levelPropPathMap, levelGeoPathMap)

  private def load(propMap: Map[TypeLevel, String], shapeMap: Map[TypeLevel, String]): Map[TypeLevel, USGeoJSONIndex] = {
    OrderedLevels.map(level => {
      val index = new USGeoJSONIndex()
      val jsArrays = Json.parse(loadSmallJSONFile(propMap.get(level).get)).asInstanceOf[JsArray].value
      index.loadShape(shapeMap.get(level).get, level match {
        case StateLevel => jsArrays.map(_.as[StateProp])
        case CountyLevel => jsArrays.map(_.as[CountyProp])
        case CityLevel => jsArrays.map(_.as[CityProp])
      })
      level -> index
    }).toMap
  }

  def tagNeighborhood(cityName: String, rectangle: Rectangle): USGeoTagInfo = ???
}

object USGeoGnosis {

  case class USGeoTagInfo(stateID: Int, stateName: String,
                          countyID: Int, countyName: String,
                          cityID: Int, cityName: String)

  def loadShape(filePath: String, index: USGeoJSONIndex, prop: Seq[USAnnotationHelper.HelperProp]) {
    val file = new File(filePath)
    if (file.isDirectory) {
      file.list.filter(_.endsWith(".json")).foreach { fileName =>
        loadShape(file.getAbsolutePath + File.separator + fileName, index, prop)
      }
    } else {
      val textJson = loadSmallJSONFile(filePath)
      index.loadShape(textJson, prop)
    }
  }

  val StateLevel: TypeLevel = 1
  val CountyLevel: TypeLevel = 2
  val CityLevel: TypeLevel = 3

  val OrderedLevels: Seq[TypeLevel] = Seq(StateLevel, CountyLevel, CityLevel)

  val StateAbbr2FullNameMap: Map[String, String] = Map(
    "AL" -> "Alabama",
    "AK" -> "Alaska",
    "AS" -> "American Samoa",
    "AZ" -> "Arizona",
    "AR" -> "Arkansas",
    "CA" -> "California",
    "CO" -> "Colorado",
    "CT" -> "Connecticut",
    "DE" -> "Delaware",
    "DC" -> "District Of Columbia",
    "FM" -> "Federated States Of Micronesia",
    "FL" -> "Florida",
    "GA" -> "Georgia",
    "GU" -> "Guam",
    "HI" -> "Hawaii",
    "ID" -> "Idaho",
    "IL" -> "Illinois",
    "IN" -> "Indiana",
    "IA" -> "Iowa",
    "KS" -> "Kansas",
    "KY" -> "Kentucky",
    "LA" -> "Louisiana",
    "ME" -> "Maine",
    "MH" -> "Marshall Islands",
    "MD" -> "Maryland",
    "MA" -> "Massachusetts",
    "MI" -> "Michigan",
    "MN" -> "Minnesota",
    "MS" -> "Mississippi",
    "MO" -> "Missouri",
    "MT" -> "Montana",
    "NE" -> "Nebraska",
    "NV" -> "Nevada",
    "NH" -> "New Hampshire",
    "NJ" -> "New Jersey",
    "NM" -> "New Mexico",
    "NY" -> "New York",
    "NC" -> "North Carolina",
    "ND" -> "North Dakota",
    "MP" -> "Northern Mariana Islands",
    "OH" -> "Ohio",
    "OK" -> "Oklahoma",
    "OR" -> "Oregon",
    "PW" -> "Palau",
    "PA" -> "Pennsylvania",
    "PR" -> "Puerto Rico",
    "RI" -> "Rhode Island",
    "SC" -> "South Carolina",
    "SD" -> "South Dakota",
    "TN" -> "Tennessee",
    "TX" -> "Texas",
    "UT" -> "Utah",
    "VT" -> "Vermont",
    "VI" -> "Virgin Islands",
    "VA" -> "Virginia",
    "WA" -> "Washington",
    "WV" -> "West Virginia",
    "WI" -> "Wisconsin",
    "WY" -> "Wyoming"
  )

  val StateFullName2AbbrMap: Map[String, String] = StateAbbr2FullNameMap.map(_.swap)
}
