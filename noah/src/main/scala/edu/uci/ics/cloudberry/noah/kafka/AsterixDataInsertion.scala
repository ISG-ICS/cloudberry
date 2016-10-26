package edu.uci.ics.cloudberry.noah.kafka

import edu.uci.ics.cloudberry.noah.feed._
import org.apache.kafka.clients.consumer.ConsumerRecords
import play.api.libs.ws.ahc.AhcWSClient

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global


class AsterixDataInsertion(wsClient: AhcWSClient){

  def insertRecord(url: String, dataverse: String, dataset: String, record: String) {
    val adm = TagTweet.tagOneTweet(record, false)
    val aql = s"use dataverse $dataverse; insert into dataset $dataset($adm);"
    httpRequest(url, aql)
  }

  private def httpRequest(url: String, aql: String) {
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

  def ingest(records: ConsumerRecords[String, String], config: Config) {
    val feedDriver: TwitterFeedStreamDriver = new TwitterFeedStreamDriver
    val socketAdapterClient = feedDriver.openSocket(config)
    try {
      for (record <- records) {
        val adm: String = TagTweet.tagOneTweet(record.value, false)
        if (adm.length() > 0)
          socketAdapterClient.ingest(adm)
      }
    }
    catch {
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