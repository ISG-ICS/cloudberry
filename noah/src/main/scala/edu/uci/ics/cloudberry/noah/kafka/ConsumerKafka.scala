package edu.uci.ics.cloudberry.noah.kafka

import java.io.File
import java.util.Properties

import com.typesafe.config.ConfigFactory
import edu.uci.ics.cloudberry.noah.feed.{AsterixHttpRequest, Config, TagTweet, TwitterFeedStreamDriver}
import org.apache.kafka.clients.consumer.{ConsumerRecords, KafkaConsumer}
import org.kohsuke.args4j.CmdLineException
import twitter4j.TwitterException

import scala.collection.JavaConversions._

/**
  * Created by Monique on 7/18/2016.
  */
object ConsumerKafka {

  private def getProperties(server: String, groupId: String, filename: String): Properties = {

    val props: Properties = new Properties
    val file = new File(getClass().getClassLoader().getResource(filename).getFile())
    val conf = ConfigFactory.parseFile(file)
    val autoCommit = conf.getString("enable.auto.commit")
    val commitInterval = conf.getString("auto.commit.interval.ms")
    val timeout = conf.getString("session.timeout.ms")
    val reset = conf.getString("auto.offset.reset")
    val keySerial = conf.getString("key.deserializer")
    val valueSerial = conf.getString("value.deserializer")

    props.put("bootstrap.servers", server)
    props.put("group.id", groupId)
    props.put("enable.auto.commit", autoCommit)
    props.put("auto.commit.interval.ms", commitInterval)
    props.put("session.timeout.ms", timeout)
    props.put("auto.offset.reset", reset)
    props.put("key.deserializer", keySerial)
    props.put("value.deserializer", valueSerial)
    return props
  }

  private def ingest(config: Config, records: ConsumerRecords[String, String]) {
    val feedDriver: TwitterFeedStreamDriver = new TwitterFeedStreamDriver
    try {
      feedDriver.openSocket(config)
      try {
        for (record <- records) {
          val adm: String = TagTweet.tagOneTweet(record.value, false)
          feedDriver.socketAdapterClient.ingest(adm)
        }
      }
      catch {
        case e: TwitterException => {
          e.printStackTrace(System.err)
        }
      }
    }
    catch {
      case e: CmdLineException => {
        e.printStackTrace(System.err)
      }
      case e: Exception => {
        e.printStackTrace(System.err)
      }
    } finally {
      if (feedDriver.socketAdapterClient != null) {
        feedDriver.socketAdapterClient.finalize
      }
    }
  }

  @throws[CmdLineException]
  def run(config: Config, topics: Array[String], dataset: String) {
    val server: String = config.getKafkaServer
    val groupId: String = config.getKafkaId
    val props: Properties = this.getProperties(server, groupId, config.getConfigFilename)
    val consumer: KafkaConsumer[String, String] = new KafkaConsumer[String, String](props)
    consumer.subscribe(topics.toSeq)
    while (true) {
      val records: ConsumerRecords[String, String] = consumer.poll(100)
      if (dataset == "ds_zika_streaming") {
        ingest(config, records)
      }
      else {
        for (record <- records) {
          AsterixHttpRequest.insertDB(config.getAxServer, "twitter_zika", dataset, record.value)
        }
      }
    }
  }
}
