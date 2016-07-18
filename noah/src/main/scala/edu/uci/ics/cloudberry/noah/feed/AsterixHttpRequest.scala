package edu.uci.ics.cloudberry.noah.feed

import java.net.URLEncoder

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.ws.ahc.AhcWSClient
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.ExecutionContext.Implicits.global

object AsterixHttpRequest {

  def insertDB(server: String, dataverse:String, dataset:String, record:String){
    var adm = TagTweet.tagOneTweet(record, false)
    adm = URLEncoder.encode(adm, "UTF-8")
    val url = server + "/update?statements=use dataverse "+dataverse+"; insert into dataset "+dataset+"("+adm+");"
    httpRequest(url)
  }

  def httpRequest(url:String){

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    val wsClient = AhcWSClient()
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