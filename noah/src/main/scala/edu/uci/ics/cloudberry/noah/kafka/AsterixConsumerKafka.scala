package edu.uci.ics.cloudberry.noah.kafka

import java.io.File
import java.util.Properties

import com.typesafe.config.ConfigFactory
import edu.uci.ics.cloudberry.noah.feed.Config.Source
import edu.uci.ics.cloudberry.noah.feed._
import org.apache.kafka.clients.consumer.{ConsumerRecords, KafkaConsumer}
import org.kohsuke.args4j.CmdLineException
import play.api.libs.ws.ahc.AhcWSClient
import twitter4j.TwitterException

import scala.collection.JavaConversions._

/**
  * Created by Monique on 7/18/2016.
  */
class AsterixConsumerKafka(config: Config) {

  private def getProperties(): Properties = {
    val server = config.getKafkaServer
    val groupId = config.getKafkaId
    val filename = config.getConfigFilename
    val props: Properties = new Properties
    val file = new File(getClass().getClassLoader().getResource(filename).getFile())
    val conf = ConfigFactory.parseFile(file)
    val autoCommit = conf.getString("enable.auto.commit")
    val commitInterval = conf.getString("auto.commit.interval.ms")
    val timeout = conf.getString("session.timeout.ms")
    val reset = conf.getString("auto.offset.reset")
    val keySerial = conf.getString("key.deserializer")
    val valueSerial = conf.getString("value.deserializer")
    val poolTimeout = conf.getString("poll.ms")

    props.put("bootstrap.servers", server)
    props.put("group.id", groupId)
    props.put("enable.auto.commit", autoCommit)
    props.put("auto.commit.interval.ms", commitInterval)
    props.put("session.timeout.ms", timeout)
    props.put("auto.offset.reset", reset)
    props.put("key.deserializer", keySerial)
    props.put("value.deserializer", valueSerial)
    props.put("poll.ms", poolTimeout)
    return props
  }

  private def ingest(records: ConsumerRecords[String, String]) {
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

  @throws[CmdLineException]
  def consume(source: Source) {
    val props = getProperties
    val consumer = new KafkaConsumer[String, String](props)
    val topics: Array[String] = Array(config.getTopic(source))
    consumer.subscribe(topics.toSeq)
    val dataset = config.getDataset(source)

    val wsClient: AhcWSClient = AsterixHttpRequest.createClient()
    while (true) {
      val records: ConsumerRecords[String, String] = consumer.poll(props.getProperty("poll.ms").toLong)

      if (source == Config.Source.Zika) {
        ingest(records)
      }
      else {
        for (record <- records) {
         AsterixHttpRequest.insertRecord(config.getAxServer, config.getDataverse, dataset, record.value, wsClient)
        }
      }
    }
    AsterixHttpRequest.close(wsClient)
    }
}
