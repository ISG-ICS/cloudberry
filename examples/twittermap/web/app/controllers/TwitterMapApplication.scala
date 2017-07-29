package controllers

import java.io.{File, FileInputStream}
import javax.inject.{Inject, Singleton}

import model.Migration_20170428
import org.joda.time.DateTime
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsValue, Json, _}
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.{Configuration, Environment, Logger}

import scala.concurrent.Await
import scala.concurrent.duration._

@Singleton
class TwitterMapApplication @Inject()(val wsClient: WSClient,
                                      val config: Configuration,
                                      val environment: Environment) extends Controller {

  val USCityDataPath: String = config.getString("us.city.path").getOrElse("/public/data/city.sample.json")
  val cloudberryRegisterURL: String = config.getString("cloudberry.register").getOrElse("http://localhost:9000/admin/register")
  val cloudberryWS: String = config.getString("cloudberry.ws").getOrElse("ws://localhost:9000/ws")
  val sentimentEnabled: Boolean = config.getBoolean("sentimentEnabled").getOrElse(false)
  val sentimentUDF: String = config.getString("sentimentUDF").getOrElse("twitter.`snlp#getSentimentScore`(text)")
  val removeSearchBar: Boolean = config.getBoolean("removeSearchBar").getOrElse(false)
  val predefinedKeywords: Seq[String] = config.getStringSeq("predefinedKeywords").getOrElse(Seq())
  val startDate: String = config.getString("startDate").getOrElse("2015-11-22T00:00:00.000")
  val endDate : Option[String] = config.getString("endDate")
  val cities: List[JsValue] = TwitterMapApplication.loadCity(environment.getFile(USCityDataPath))

  val clientLogger = Logger("client")

  val register = Migration_20170428.migration.up(wsClient, cloudberryRegisterURL)
  Await.result(register, 1 minutes)

  def index = Action { request =>
    val remoteAddress = request.remoteAddress
    val userAgent = request.headers.get("user-agent").getOrElse("unknown")
    clientLogger.info(s"Connected: user_IP_address = $remoteAddress; user_agent = $userAgent")
    Ok(views.html.twittermap.index("TwitterMap", cloudberryWS, startDate, endDate, sentimentEnabled, sentimentUDF, removeSearchBar, predefinedKeywords, false))
  }

  def drugmap = Action {
    request =>
      val startDateDrugMap = "2017-05-01T00:00:00.000"
      val remoteAddress = request.remoteAddress
      val userAgent = request.headers.get("user-agent").getOrElse("unknown")
      clientLogger.info(s"Connected: user_IP_address = $remoteAddress; user_agent = $userAgent")
      Ok(views.html.twittermap.index("DrugMap", cloudberryWS, startDateDrugMap, endDate, false, sentimentUDF, true, Seq("drug"), true))
  }

  def tweet(id: String) = Action.async {
    val url = "https://api.twitter.com/1/statuses/oembed.json?id=" + id
    wsClient.url(url).get().map { response =>
      Ok(response.json)
    }
  }

  def getCity(neLat: Double, swLat: Double, neLng: Double, swLng: Double) = Action {
    Ok(TwitterMapApplication.findCity(neLat, swLat, neLng, swLng, cities))
  }

}

object TwitterMapApplication {
  val Features = "features"
  val Geometry = "geometry"
  val Type = "type"
  val Coordinates = "coordinates"
  val Polygon = "Polygon"
  val MultiPolygon = "MultiPolygon"
  val CentroidLatitude = "centroidLatitude"
  val CentroidLongitude = "centroidLongitude"

  val header = Json.parse("{\"type\": \"FeatureCollection\"}").as[JsObject]

  def loadCity(file: File): List[JsValue] = {
    val stream = new FileInputStream(file)
    val json = Json.parse(stream)
    stream.close()
    val features = (json \ Features).as[List[JsObject]]
    val newValues = features.map { thisValue =>
      (thisValue \ Geometry \ Type).as[String] match {
        case Polygon => {
          val coordinates = (thisValue \ Geometry \ Coordinates).as[JsArray].apply(0).as[List[List[Double]]]
          val (minLong, maxLong, minLat, maxLat) = coordinates.foldLeft(180.0, -180.0, 180.0, -180.0) {
            case (((minLong, maxLong, minLat, maxLat)), e) =>
              (math.min(minLong, e(0)), math.max(maxLong, e(0)), math.min(minLat, e(1)), math.max(minLat, e(1)))
          }
          val thisLong = (minLong + maxLong) / 2
          val thisLat = (minLat + maxLat) / 2
          thisValue + (CentroidLongitude -> Json.toJson(thisLong)) + (CentroidLatitude -> Json.toJson(thisLat))
        }
        case MultiPolygon => {
          val allCoordinates = (thisValue \ Geometry \ Coordinates).as[JsArray]
          val coordinatesBuilder = List.newBuilder[List[Double]]
          for (coordinate <- allCoordinates.value) {
            val rawCoordinate = coordinate.as[JsArray]
            val realCoordinate = rawCoordinate.apply(0).as[List[List[Double]]]
            realCoordinate.map(x => coordinatesBuilder += x)
          }
          val coordinates = coordinatesBuilder.result()
          val (minLong, maxLong, minLat, maxLat) = coordinates.foldLeft(180.0, -180.0, 180.0, -180.0) {
            case (((minLong, maxLong, minLat, maxLat)), e) =>
              (math.min(minLong, e(0)), math.max(maxLong, e(0)), math.min(minLat, e(1)), math.max(minLat, e(1)))
          }
          val thisLong = (minLong + maxLong) / 2
          val thisLat = (minLat + maxLat) / 2
          thisValue + (CentroidLongitude -> Json.toJson(thisLong)) + (CentroidLatitude -> Json.toJson(thisLat))
        }
        case _ => {
          throw new IllegalArgumentException("Unidentified geometry type in city.json");
        }
      }
    }
    newValues.sortWith((x, y) => (x \ CentroidLongitude).as[Double] < (y \ CentroidLongitude).as[Double])
  }

  /** Use binary search twice to find two breakpoints (startIndex and endIndex) to take out all cities whose longitude are in the range,
    * then scan those cities one by one for latitude.
    *
    * @param neLat  Latitude of the NorthEast point of the boundary
    * @param swLat  Latitude of the SouthWest point of the boundary
    * @param neLng  Latitude of the NorthEast point of the boundary
    * @param swLng  Latitude of the SouthWest point of the boundary
    * @param cities List of all cities
    * @return List of cities which centroids is in current boundary
    */
  def findCity(neLat: Double, swLat: Double, neLng: Double, swLng: Double, cities: List[JsValue]): JsValue = {
    val startIndex = binarySearch(cities, 0, cities.size, swLng)
    val endIndex = binarySearch(cities, 0, cities.size, neLng)

    if (startIndex == -1) {
      //no cities found
      Json.toJson(header)
    } else {
      val citiesWithinBoundary = cities.slice(startIndex, endIndex).filter { city =>
        (city \ CentroidLatitude).as[Double] <= neLat && (city \ CentroidLatitude).as[Double] >= swLat.toDouble
      }
      val response = header + (Features -> Json.toJson(citiesWithinBoundary))
      Json.toJson(response)
    }
  }

  /**
    * Use binary search to find the twitterMapIndex in cities to insert the target Longitude
    *
    * @param targetLng the target Longitude
    * @return the twitterMapIndex
    */
  def binarySearch(cities: List[JsValue], startIndex: Int, endIndex: Int, targetLng: Double): Int = {
    if (startIndex == endIndex) {
      startIndex
    } else {
      val thisIndex = (startIndex + endIndex) / 2
      val thisCity = cities(thisIndex)
      val centroidLongitude = (thisCity \ CentroidLongitude).as[Double]
      if (centroidLongitude > targetLng) {
        binarySearch(cities, startIndex, thisIndex, targetLng)
      } else if (centroidLongitude < targetLng) {
        binarySearch(cities, thisIndex + 1, endIndex, targetLng)
      } else {
        thisIndex
      }
    }
  }


}
