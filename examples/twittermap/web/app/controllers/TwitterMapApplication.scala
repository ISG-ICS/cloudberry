package controllers

import java.io.{File, FileInputStream, IOException}

import javax.inject.{Inject, Singleton}
import actor.TwitterMapPigeon
import akka.actor._
import akka.stream.Materializer
import model.{Migration_20170428, MySqlMigration_20170810, PostgreSqlMigration_20172829}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsValue, Json, _}
import play.api.libs.streams.ActorFlow
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.{Configuration, Environment, Logger}
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import twitter4j._
import websocket.WebSocketFactory
import java.net.{HttpURLConnection, URL, URLEncoder}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source
import scala.util.matching.Regex


@Singleton
class TwitterMapApplication @Inject()(val wsClient: WSClient,
                                      val config: Configuration,
                                      val environment: Environment)
                                     (implicit val materializer: Materializer,
                                      implicit val system: ActorSystem) extends Controller {

  val USCityDataPath: String = config.getString("us.city.path").getOrElse("/public/data/city.sample.json")
  val USCityPopDataPath: String = config.getString("us.citypop.path").getOrElse("/public/data/allCityPopulation.json")
  val cloudberryRegisterURL: String = config.getString("cloudberry.register").getOrElse("http://localhost:9000/admin/register")
  val cloudberryWS: String = config.getString("cloudberry.ws").getOrElse("ws://localhost:9000/ws")
  val cloudberryCheckQuerySolvableByView: String = config.getString("cloudberry.checkQuerySolvableByView").getOrElse("ws://localhost:9000/checkQuerySolvableByView")
  val sentimentEnabled: Boolean = config.getBoolean("sentimentEnabled").getOrElse(false)
  val sentimentUDF: String = config.getString("sentimentUDF").getOrElse("twitter.`snlp#getSentimentScore`(text)")
  val removeSearchBar: Boolean = config.getBoolean("removeSearchBar").getOrElse(false)
  val predefinedKeywords: Seq[String] = config.getStringSeq("predefinedKeywords").getOrElse(Seq())
  val startDate: String = config.getString("startDate").getOrElse("2015-11-22T00:00:00.000")
  val endDate : Option[String] = config.getString("endDate")
  val cities: List[JsValue] = TwitterMapApplication.loadCity(environment.getFile(USCityDataPath))
  val citiesPopulation: List[JsValue] = TwitterMapApplication.loadCityPop(environment.getFile(USCityPopDataPath))
  val cacheThreshold : Option[String] = config.getString("cacheThreshold")
  val querySliceMills: Option[String] = config.getString("querySliceMills")
  val heatmapSamplingDayRange: String = config.getString("heatmap.samplingDayRange").getOrElse("30")
  val heatmapSamplingLimit: String = config.getString("heatmap.samplingLimit").getOrElse("5000")
  val heatmapUnitIntensity: String = config.getString("heatmap.unitIntensity").getOrElse("50")
  val heatmapUnitRadius: String = config.getString("heatmap.unitRadius").getOrElse("15")
  val pinmapSamplingDayRange: String = config.getString("pinmap.samplingDayRange").getOrElse("30")
  val pinmapSamplingLimit: String = config.getString("pinmap.samplingLimit").getOrElse("5000")
  val defaultMapType: String = config.getString("defaultMapType").getOrElse("countmap")
  val liveTweetQueryInterval : Int = config.getInt("liveTweetQueryInterval").getOrElse(60)
  val liveTweetQueryOffset : Int = config.getInt("liveTweetQueryOffset").getOrElse(30)
  val liveTweetUpdateRate: Int = config.getInt("liveTweetUpdateRate").getOrElse(2)
  val liveTweetConsumerKey: String = config.getString("liveTweetConsumerKey").getOrElse(null)
  val liveTweetConsumerSecret: String = config.getString("liveTweetConsumerSecret").getOrElse(null)
  val liveTweetToken: String = config.getString("liveTweetToken").getOrElse(null)
  val liveTweetTokenSecret: String = config.getString("liveTweetTokenSecret").getOrElse(null)
  val enableLiveTweet : Boolean = config.getBoolean("enableLiveTweet").getOrElse(true)
  val useDirectSource : Boolean = config.getBoolean("useDirectSource").getOrElse(true)
  val webSocketFactory = new WebSocketFactory()
  val maxTextMessageSize: Int = config.getInt("maxTextMessageSize").getOrElse(5* 1024* 1024)
  val clientLogger = Logger("client")

  import TwitterMapApplication.DBType
  val sqlDB: DBType.Value = DBType.withName(config.getString("sqlDB").getOrElse("Default"))

  val register = sqlDB match {
    case DBType.MySQL => MySqlMigration_20170810.migration.up(wsClient, cloudberryRegisterURL)
    case DBType.PostgreSQL => PostgreSqlMigration_20172829.migration.up(wsClient, cloudberryRegisterURL)
    case _ => Migration_20170428.migration.up(wsClient, cloudberryRegisterURL)
  }
  Await.result(register, 1 minutes)

  def index = Action { request =>
    val remoteAddress = request.remoteAddress
    val userAgent = request.headers.get("user-agent").getOrElse("unknown")
    clientLogger.info(s"Connected: user_IP_address = $remoteAddress; user_agent = $userAgent")
    Ok(views.html.twittermap.index("TwitterMap", this, false))
  }

  def drugmap = Action {
    request =>
      val startDateDrugMap = "2017-05-01T00:00:00.000"
      val remoteAddress = request.remoteAddress
      val userAgent = request.headers.get("user-agent").getOrElse("unknown")
      clientLogger.info(s"Connected: user_IP_address = $remoteAddress; user_agent = $userAgent")
      Ok(views.html.twittermap.index("DrugMap", this, true))
  }

  def ws = WebSocket.accept[JsValue, JsValue] { request =>
    ActorFlow.actorRef { out =>
      TwitterMapPigeon.props(webSocketFactory, cloudberryWS, out, maxTextMessageSize)
    }
  }

  // A WebSocket that send query to Cloudberry, to check whether it is solvable by view
  def checkQuerySolvableByView = WebSocket.accept[JsValue, JsValue] { request =>
    ActorFlow.actorRef { out =>
      TwitterMapPigeon.props(webSocketFactory, cloudberryCheckQuerySolvableByView, out, maxTextMessageSize)
    }
  }


  object LiveTweetActor {
    def props(out: ActorRef) = Props(new LiveTweetActor(out))
  }

  class LiveTweetActor(out: ActorRef) extends Actor {
    override def receive = {
      case msg: JsValue =>
        var tweetArray = Json.arr()
        val queryWords = (msg \\ "keyword").head.as[String]

        //If useDirectSource set to false, we just use traditional search API to provide sample tweets
        if(!useDirectSource){
          val location = Array{(msg \\ "location").head.as[Array[Double]]}
          val locs = Array(Array(location.head(1),location.head(0)),Array(location.head(3),location.head(2)))
          val center = Array((locs(0)(0) + locs(1)(0))/2.00,(locs(0)(1)+locs(1)(1))/2.00)
          val centerLoc = new GeoLocation(center(1),center(0))
          val unit = Query.Unit.km
          /*
          Note: Twitter Search API does not support bounding box, the Geo information can only be specified
          By using center location and radius.
          Center location is calculated by the center point of bounding box(rectangle)
          Radius is calculated by the difference of latitude between left top corner and right bottom corner.
          Using latitude rather than longitude
          since the ratio between latitude to KM is almost constant
          But ratio between Longitude to KM varies largerly.
          ------------------
          |     * * *      |
          |    *     *     |
          |      * *       |
          ------------------
          */
          val radius = Math.abs(locs(0)(1) - locs(1)(1)) * 111
          val cb2 = new ConfigurationBuilder
          cb2.setDebugEnabled(true)
            .setOAuthConsumerKey(liveTweetConsumerKey)
            .setOAuthConsumerSecret(liveTweetConsumerSecret)
            .setOAuthAccessToken(liveTweetToken)
            .setOAuthAccessTokenSecret(liveTweetTokenSecret)
          val tf = new TwitterFactory(cb2.build)
          val twitterAPI = tf.getInstance
          val query = new twitter4j.Query
          var desiredTweetAmount = (liveTweetQueryInterval / liveTweetUpdateRate).toInt
          val resultType = twitter4j.Query.ResultType.recent
          query.setQuery(queryWords)
          query.setCount(desiredTweetAmount)
          query.setResultType(resultType)
          query.setGeoCode(centerLoc,radius,unit)
          val tweetsResult = twitterAPI.search(query).getTweets
          for(i <- 0 to tweetsResult.size()-1){
            val status = tweetsResult.get(i)
            if (status.isRetweet == false){
              tweetArray = tweetArray :+ (Json.obj("id" -> status.getId.toString))
            }

          }
        }
        else{
          //Thi url is used by twitter lastest timeline page to get the lastest tweets from their server
          val url = "https://twitter.com/i/search/timeline?f=tweets&vertical=news&q="+queryWords+"%20near%3A\"United%20States\"%20within%3A8000mi&l=en&src=typd"
          val connection = (new URL(url)).openConnection.asInstanceOf[HttpURLConnection]
          connection.setRequestMethod("GET")
          //This header must be added, otherwise we will get empty response
          connection.setRequestProperty("user-agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36")
          val inputStream = connection.getInputStream
          val pattern = new Regex("""dataitemid(\d+)""")
          var htmlContent = Source.fromInputStream(inputStream).mkString
          htmlContent = htmlContent.replaceAll("""[\p{Punct}&&[^.]]""", "")
          for(m <- pattern.findAllIn(htmlContent)) {
            val idString = m.replaceAll("dataitemid", "")
            tweetArray = tweetArray :+ (Json.obj("id" -> idString))
          }
        }

        out!tweetArray
      case msg:Any =>
        Logger.info("Invalid input for LiveTweet")
    }
  }

  object autoCompleteActor {
    def props(out: ActorRef) = Props(new autoCompleteActor(out))
  }

  class autoCompleteActor(out:ActorRef) extends Actor {
    override def receive = {
      case msg: JsValue =>
        try {
          val keyword = (msg \\ "keyword").head.as[String]
          val url = "https://twitter.com/i/search/typeahead.json?count=10&filters=true&q=" + URLEncoder.encode(keyword, "UTF-8") + "&result_type=topics"
          val connection = new URL(url).openConnection.asInstanceOf[HttpURLConnection]
          connection.setRequestMethod("GET")
          val inputStream = connection.getInputStream
          val content = Source.fromInputStream(inputStream).mkString
          if (inputStream != null) inputStream.close()
          out ! Json.toJson(content)
        }
        catch {
          case e: IOException => e.printStackTrace()
        }
      case _ =>
        Logger.info("Invalid input for autocomplete")
    }
  }


  /**
    * autoComplete is a callback function for twittermap's search box autocomplete
    *
    * @param query recieved from frontend request JsValue
    * @return A list of topics object in JsValue
    *
    */
  def autoComplete = WebSocket.accept[JsValue,JsValue]{ request =>
    ActorFlow.actorRef{
      out=>autoCompleteActor.props(out)
    }
  }


  /**
    * liveTweets is a callback function
    *
    * @param query recieved from frontend request JsValue
    * @return A list of tweet object in JsValue
    *
    */
  def liveTweets = WebSocket.accept[JsValue,JsValue] { request =>
    ActorFlow.actorRef{ out =>
      LiveTweetActor.props(out)
    }
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

  def getCityPop(cityIds: String) = Action {
    Ok(TwitterMapApplication.findCityPop(cityIds, citiesPopulation))
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

  def loadCityPop(file: File): List[JsValue] = {
    val stream = new FileInputStream(file)
    val json = Json.parse(stream)
    stream.close()
    (json).as[List[JsObject]]
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

  /** Find cities' population whose cityID are in cityIDs.
    *
    * @param cityIds List of cities in current boundary in String
    * @param citiesPopulation List of all cities' population data
    * @return List of cities population which centroids is in current boundary
    */
  def findCityPop(cityIds: String, citiesPopulation: List[JsValue]): JsArray = {
    val cityIdsList : List[Int] = cityIds.split(",").map(_.toInt).toList
    val incCityIdsList = cityIdsList.sortWith(_<_)
    sortedListBinarySearch(citiesPopulation, 0, 29833, incCityIdsList, 0, Json.arr())
  }

  /**
    * Find the population data data for each geoId in from the sorted geoIds list.
    *
    * @param sortedCityIds Sorted list of cities in current boundary
    * @param citiesPopulation List of all cities' population data
    * @return List of cities in sortedCityIds' population data
    */
  def sortedListBinarySearch(citiesPopulation: List[JsValue], startIndex: Int, endIndex: Int,
                             sortedCityIds: List[Int], sortedCityIndex: Int, resultArr: JsArray): JsArray = {
    if (startIndex == endIndex) {
        resultArr
    } else {
      val thisIndex = (startIndex + endIndex) / 2
      val thisCityId = (citiesPopulation(thisIndex) \ "cityID").as[Int]
      if (thisCityId > sortedCityIds(sortedCityIndex)) {
        sortedListBinarySearch(citiesPopulation, startIndex, thisIndex, sortedCityIds, sortedCityIndex, resultArr)
      } else if (thisCityId < sortedCityIds(sortedCityIndex)) {
        sortedListBinarySearch(citiesPopulation, thisIndex + 1, endIndex, sortedCityIds, sortedCityIndex, resultArr)
      } else { // Current cityId sortedCityIds(sortedCityIndex) is find in citiesPopulation
        val currPop : JsValue = JsObject(Seq("cityID" -> JsNumber(thisCityId),
                                             "population" -> JsNumber((citiesPopulation(thisIndex) \ "population").as[Int])
                                            ))
        if (sortedCityIndex < sortedCityIds.length - 1) {
          sortedListBinarySearch(citiesPopulation, thisIndex + 1, 29833, sortedCityIds, sortedCityIndex + 1, resultArr :+ currPop)
        } else {
          resultArr :+ currPop
        }
      }
    }
  }

  object DBType extends Enumeration {
    val MySQL, PostgreSQL, Default = Value
  }

}
