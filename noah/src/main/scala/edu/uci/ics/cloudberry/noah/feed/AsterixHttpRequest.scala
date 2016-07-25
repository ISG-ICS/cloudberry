package edu.uci.ics.cloudberry.noah.feed

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.ExecutionContext.Implicits.global

object AsterixHttpRequest {

  def createClient(): AhcWSClient = {
    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    AhcWSClient()
  }

  def insertRecord(url: String, dataverse: String, dataset: String, record: String, wsClient: AhcWSClient) {
    val adm = TagTweet.tagOneTweet(record, false)
    val aql = s"use dataverse $dataverse; insert into dataset $dataset($adm);"
    httpRequest(url, aql, wsClient)
  }

  def httpRequest(url: String, aql: String, wsClient: AhcWSClient) {
    wsClient
      .url(url)
      .post(aql)
      .map { wsResponse =>
        if (!(200 to 299).contains(wsResponse.status)) {
          System.err.println(s"Received unexpected status ${wsResponse.status} : ${wsResponse.body}")
        } else {
          println("OK! Record successfully inserted")
        }
      }
  }

  def close(wsClient: AhcWSClient): Unit = {
    wsClient.close()
  }
}