package edu.uci.ics.cloudberry.noah.feed

import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import java.io.File

object AsterixHttpRequest {

  def createTwitterDS(ddlFilename:String){
    val f = new File(getClass.getClassLoader.getResource(ddlFilename).getPath)
    val source = Source.fromFile(f)
    val lines = try source.mkString finally source.close()
    val query = "http://kiwi.ics.uci.edu:19002/ddl?ddl=" + lines

    httpRequest(query)

  }
  def queryDB(dataverse:String, dataset:String){
    val url = "http://kiwi.ics.uci.edu:19002/query?query=use dataverse "+dataverse+"; for $l in dataset('"+dataset+"') return $l;"
    httpRequest(url)
  }

  def insertDB(dataverse:String, dataset:String, record:String){
    val adm = TagTweetNoGeoTagException.tagOneTweet(record)
    println("ADM = "+ adm)
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
          println(url)
          println(s"Received unexpected status ${wsResponse.status} : ${wsResponse.body}")
        }
        wsClient.close()
      }
  }

  def main(args: Array[String]): Unit = {
    //createTwitterDS("twitter/zika/ddl.aql")
    //insertDB("company","Employee", "{\"id\":65255,\"name\":\"Jia\"}")
    queryDB("twitter_test","ds_users_test")
  }
}