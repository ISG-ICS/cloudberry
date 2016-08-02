package edu.uci.ics.cloudberry.noah.kafka

import java.io.File
import java.util.Properties

import com.typesafe.config.ConfigFactory
import edu.uci.ics.cloudberry.noah.feed.Config.Source
import edu.uci.ics.cloudberry.noah.feed._
import org.apache.kafka.clients.consumer.{ConsumerRecords, KafkaConsumer}
import org.kohsuke.args4j.CmdLineException
import play.api.libs.ws.ahc.AhcWSClient

import scala.collection.JavaConversions._

/**
  * Created by Monique on 7/18/2016.
  */
class AsterixConsumerKafka(config: Config, wsClient: AhcWSClient) {

  def getProperties(): Properties = {
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

  def subscribe(consumer: KafkaConsumer[String, String], source: Source): Unit = {
    val topics: Array[String] = Array(config.getTopic(source))
    consumer.subscribe(topics.toSeq)
  }

  def sendToAsterix(source: Source, url: String, dataset: String, asterixDataInsertion: AsterixDataInsertion, records: ConsumerRecords[String, String]): Unit = {
    if (!records.isEmpty) {
      if (source == Config.Source.Zika) {
        asterixDataInsertion.ingest(records, config)
      }
      else {
        for (record <- records) {
          asterixDataInsertion.insertRecord(url, config.getDataverse, dataset, record.value)
        }
      }
    }
  }

  @throws[CmdLineException]
  def consume(source: Source, consumer: KafkaConsumer[String, String], timeout: Long) {
    subscribe(consumer, source)
    val asterixDataInsertion = new AsterixDataInsertion(wsClient)
    val dataset = config.getDataset(source)
    val url = s"${config.getAxServer}/aql"
    try {
      //TODO change while(true) logic to sleep(timeout) when there is no record available
      while (true) {
        val records: ConsumerRecords[String, String] = consumer.poll(timeout);
        sendToAsterix(source, url, dataset, asterixDataInsertion, records)
      }
    } catch {
      case e: Exception => {
        e.printStackTrace(System.err)
        }
    }
    }
}
