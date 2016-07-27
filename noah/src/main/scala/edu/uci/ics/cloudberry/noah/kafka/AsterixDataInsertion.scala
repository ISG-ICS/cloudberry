package edu.uci.ics.cloudberry.noah.kafka

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import edu.uci.ics.cloudberry.noah.feed._
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.kohsuke.args4j.CmdLineException
import play.api.libs.ws.ahc.AhcWSClient
import twitter4j.TwitterException

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global


class AsterixDataInsertion {

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

  def ingest(records: ConsumerRecords[String, String], config: Config) {
    val feedDriver: TwitterFeedStreamDriver = new TwitterFeedStreamDriver
    val socketAdapterClient = feedDriver.openSocket(config)
    try {
      for (record <- records) {
        val adm: String = TagTweet.tagOneTweet(record.value, false)
        socketAdapterClient.ingest(adm)
      }
    }
    catch {
      case e: TwitterException => {
        e.printStackTrace(System.err)
      }
      case e: CmdLineException => {
        e.printStackTrace(System.err)
      }
      case e: Exception => {
        e.printStackTrace(System.err)
      }
    } finally {
      if (socketAdapterClient != null) {
        socketAdapterClient.finalize
      }
    }
  }
}