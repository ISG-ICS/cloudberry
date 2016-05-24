package edu.uci.ics.cloudberry.gnosis

import java.io.{File, FilenameFilter}

import com.vividsolutions.jts.geom.{Coordinate, Envelope, Geometry}
import edu.uci.ics.cloudberry.util.Rectangle
import play.api.libs.json.{JsObject, Json, Writes}

import scala.collection.mutable

class USGeoGnosis(levelGeoPathMap: Map[TypeLevel, File]) extends IGnosis{

  import USGeoGnosis._

  val levelShapeMap: Map[TypeLevel, USGeoJSONIndex] = load(levelGeoPathMap)

  override def load(shapeMap: Map[TypeLevel, File]): Map[TypeLevel, USGeoJSONIndex] = {
    OrderedLevels.map(level => {
      val index = new USGeoJSONIndex()
      loadShape(shapeMap.get(level).get, index)(IUSGeoJSONEntity.apply)
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

  lazy val cityByNameList: Map[String, List[USCityEntity]] = {
    val map = mutable.Map.empty[String, List[USCityEntity]]
    cities.foreach(city => map += (city.name -> (city :: (map.getOrElse(city.name, Nil)))))
    map.toMap
  }

  lazy val stateShapes: IGeoIndex = levelShapeMap.get(StateLevel).get
  lazy val countyShapes: IGeoIndex = levelShapeMap.get(CountyLevel).get
  lazy val cityShapes: IGeoIndex = levelShapeMap.get(CityLevel).get

  // used in geo tag
  def tagNeighborhood(cityName: String, rectangle: Rectangle): Option[USGeoTagInfo] = {
    val box = new Envelope(rectangle.swLog, rectangle.neLog, rectangle.swLat, rectangle.neLat)
    cityByNameList.get(cityName).flatMap(list => list.find(_.geometry.getEnvelopeInternal.covers(box)).map(USGeoTagInfo(_)))
  }

  // used in geo tag
  override def tagPoint(longitude: Double, latitude: Double): Option[USGeoTagInfo] = {
    val box = new Envelope(new Coordinate(longitude, latitude))
    val cityOpt = cityShapes.search(box).headOption.map(entity => USGeoTagInfo(entity.asInstanceOf[USCityEntity]))
    if (cityOpt.isDefined) return cityOpt
    countyShapes.search(box).headOption.map(entity => USGeoTagInfo(entity.asInstanceOf[USCountyEntity]))
  }

  // used in geo tag
  def tagCity(cityName: String, stateAbbr: String): Option[USGeoTagInfo] = {
    cityByNameList.get(cityName).flatMap(
      list => list.find(_.stateName == StateAbbr2FullNameMap.get(stateAbbr).getOrElse("")).map(USGeoTagInfo(_)))
  }
}

object USGeoGnosis {

  case class USGeoTagInfo(stateID: Int, stateName: String,
                          countyID: Option[Int], countyName: Option[String],
                          cityID: Option[Int], cityName: Option[String]) extends IGeoTagInfo{
    override def toString: String = Json.toJson(this).asInstanceOf[JsObject].toString()
  }

  object USGeoTagInfo {
    implicit val writer: Writes[USGeoTagInfo] = Json.writes[USGeoTagInfo]

    def apply(entity: IUSGeoJSONEntity): USGeoTagInfo = {
      entity match {
        case state: USStateEntity => USGeoTagInfo(stateID = state.stateID, stateName = state.name,
                                                  countyID = None, countyName = None, cityID = None, cityName = None)
        case county: USCountyEntity => USGeoTagInfo(stateID = county.stateID, stateName = county.stateName,
                                                    countyID = Some(county.countyID), countyName = Some(county.name),
                                                    cityID = None, cityName = None)
        case city: USCityEntity => USGeoTagInfo(city.stateID, city.stateName, Some(city.countyID), Some(city.countyName),
                                                Some(city.cityID), Some(city.name))
      }
    }
  }

  def loadShape(file: File, index: USGeoJSONIndex)(builder: (Map[String, AnyRef], Geometry) => IUSGeoJSONEntity): Unit = {
    if (file.isDirectory) {
      file.listFiles(new FilenameFilter {
        override def accept(dir: File, name: String): Boolean = name.endsWith(".json")
      }).foreach { file =>
        loadShape(file, index)(builder)
      }
    } else {
      val textJson = loadSmallJSONFile(file)
      index.loadShape(textJson)(builder)
    }
  }

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
