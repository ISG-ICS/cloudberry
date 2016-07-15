package edu.uci.ics.cloudberry.noah.feed

import java.net.URLEncoder

import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.ExecutionContext.Implicits.global

object AsterixHttpRequest {

  def insertDB(dataverse:String, dataset:String, record:String){
    var adm = TagTweetGeotagNotRequired.tagOneTweet(record)
    adm = URLEncoder.encode(adm, "UTF-8")

    val url = "http://kiwi.ics.uci.edu:19002/update?statements=use dataverse "+dataverse+"; insert into dataset "+dataset+"("+adm+");"
    httpRequest(url)
  }

  def httpRequest(url:String){
    val wsClient = NingWSClient()
    wsClient
      .url(url)
      .withHeaders("Cache-Control" -> "no-cache")
      .get()
      .map { wsResponse =>
        if (!(200 to 299).contains(wsResponse.status)) {
          println(s"Received unexpected status ${wsResponse.status} : ${wsResponse.body}")
        } else {
          println("OK! Record successfully inserted")
        }
        wsClient.close()
      }
  }
}