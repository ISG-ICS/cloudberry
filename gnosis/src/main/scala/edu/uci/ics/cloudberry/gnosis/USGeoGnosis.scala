package edu.uci.ics.cloudberry.gnosis

import java.io.File

import com.vividsolutions.jts.geom.{Coordinate, Envelope}
import edu.uci.ics.cloudberry.gnosis.USAnnotationHelper.{CityProp, CountyProp, StateProp}
import play.api.libs.json.{JsArray, JsObject, Json, Writes}

class USGeoGnosis(levelPropPathMap: Map[TypeLevel, String], levelGeoPathMap: Map[TypeLevel, String]) {

  import USGeoGnosis._

  val levelShapeMap: Map[TypeLevel, USGeoJSONIndex] = load(levelPropPathMap, levelGeoPathMap)

  private def load(propMap: Map[TypeLevel, String], shapeMap: Map[TypeLevel, String]): Map[TypeLevel, USGeoJSONIndex] = {
    OrderedLevels.map(level => {
      val index = new USGeoJSONIndex()
      val jsArrays = Json.parse(loadSmallJSONFile(propMap.get(level).get)).asInstanceOf[JsArray].value
      loadShape(shapeMap.get(level).get, index, level match {
        case StateLevel => jsArrays.map(_.as[StateProp])
        case CountyLevel => jsArrays.map(_.as[CountyProp])
        case CityLevel => jsArrays.map(_.as[CityProp])
      })
      level -> index
    }).toMap
  }

  lazy val states: Seq[USStateEntity] = {
    levelShapeMap.get(StateLevel).get.entities.map(_.asInstanceOf[USStateEntity])
  }

  lazy val counties: Seq[USCountyEntity] = {
    levelShapeMap.get(CountyLevel).get.entities.map(_.asInstanceOf[USCountyEntity])
  }

  lazy val cities: Seq[USCityEntity] = {
    levelShapeMap.get(CityLevel).get.entities.map(_.asInstanceOf[USCityEntity])
  }

  lazy val stateShapes: IGeoIndex = levelShapeMap.get(StateLevel).get
  lazy val countyShapes: IGeoIndex = levelShapeMap.get(CountyLevel).get
  lazy val cityShapes: IGeoIndex = levelShapeMap.get(CityLevel).get

  def tagNeighborhood(cityName: String, rectangle: Rectangle): Option[USGeoTagInfo] = {
    val box = new Envelope(rectangle.swLog, rectangle.neLog, rectangle.swLat, rectangle.neLat)
    cities.find(city => city.name == cityName && city.geometry.getEnvelopeInternal.covers(box)).map(USGeoTagInfo(_))
  }

  def tagPoint(longitude: Double, latitude: Double): Option[USGeoTagInfo] = {
    val box = new Envelope(new Coordinate(longitude, latitude))
    val cityOpt = cityShapes.search(box).headOption.map(entity => USGeoTagInfo(entity.asInstanceOf[USCityEntity]))
    if (cityOpt.isDefined) cityOpt
    countyShapes.search(box).headOption.map(entity => USGeoTagInfo(entity.asInstanceOf[USCountyEntity]))
  }

  def tagCity(cityName: String, stateAbbr: String): Option[USGeoTagInfo] = {
    cities.find(city => city.name == cityName &&
      city.stateName == StateAbbr2FullNameMap.get(stateAbbr).getOrElse("")).map(USGeoTagInfo(_))
  }
}

object USGeoGnosis {

  case class USGeoTagInfo(stateID: Int, stateName: String,
                          countyID: Int, countyName: String,
                          cityID: Int, cityName: String) {
    override def toString: String = Json.toJson(this).asInstanceOf[JsObject].toString()
  }

  object USGeoTagInfo {
    implicit val writer: Writes[USGeoTagInfo] = Json.writes[USGeoTagInfo]

    def apply(city: USCityEntity): USGeoTagInfo = {
      USGeoTagInfo(city.stateID, city.stateName, city.countyID, city.countyName, city.cityID, city.name)
    }

    def apply(county: USCountyEntity): USGeoTagInfo = {
      USGeoTagInfo(stateID = county.stateID, stateName = county.stateName,
                   countyID = county.countyID, countyName = county.name,
                   cityID = 0, cityName = "")
    }
  }

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
