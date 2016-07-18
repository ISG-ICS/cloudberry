package edu.uci.ics.cloudberry.noah

import java.io.File
import java.util.Properties

import com.typesafe.config.ConfigFactory
import edu.uci.ics.cloudberry.noah.feed.Config
import org.apache.kafka.clients.producer._

/**
  * Created by Monique on 7/18/2016.
  */
object ProducerKafka {
  def store(topic: String, msg: String, config: Config) {

    val server = config.getKafkaServer();
    val file = new File(getClass().getClassLoader().getResource(config.getConfigFilename()).getFile())
    val conf = ConfigFactory.parseFile(file)
    val acks = conf.getString("acks")
    val batch = conf.getString("batch.size")
    val linger = conf.getString("linger.ms")
    val buffer = conf.getString("buffer.memory")
    val keySerial = conf.getString("key.serializer")
    val valueSerial = conf.getString("value.serializer")

    val props = new Properties();
    props.put("bootstrap.servers", server)
    props.put("acks", acks)
    props.put("batch.size", batch)
    props.put("linger.ms", linger)
    props.put("buffer.memory", buffer)
    props.put("key.serializer", keySerial)
    props.put("value.serializer", valueSerial)

    val producer = new KafkaProducer[String, String](props);
    val data = new ProducerRecord[String, String](topic, msg);
    producer.send(data);
    producer.close()
  }
}
