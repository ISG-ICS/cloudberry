package edu.uci.ics.cloudberry.noah.kafka

import java.util

import edu.uci.ics.cloudberry.noah.GeneralProducerKafka
import edu.uci.ics.cloudberry.noah.feed.Config
import org.apache.kafka.clients.consumer.{ConsumerRecords, KafkaConsumer}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.mockito._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.ws.ahc.AhcWSClient
import org.mockito.Mockito._

class TestKafka extends Specification with Mockito {

  "General Producer Kafka" should {
    "send data given to producer" in {
      val mockProducer = mock[KafkaProducer[String, String]]
      val argument = ArgumentCaptor.forClass(classOf[ProducerRecord[String, String]])
      val producerKafka = new GeneralProducerKafka(config = new Config)
      producerKafka.store("TestKafka", "testing...", mockProducer)
      Mockito.verify(mockProducer).send(argument.capture())
      "TestKafka" must_== (argument.getValue.topic())
      "testing..." must_== (argument.getValue.value())
      argument.getValue.isInstanceOf[ProducerRecord[String, String]] must(beTrue)
    }

  }

  "Asterix Consumer Kafka" should {

    val mockKafkaConsumer = mock[KafkaConsumer[String, String]]
    val mockClient = mock[AhcWSClient]
    val mockAsterix = mock[AsterixDataInsertion]
    val consumer = new AsterixConsumerKafka(config = new Config, mockClient)

    "subscribe in at least one topic" in {
      consumer.subscribe(mockKafkaConsumer, Config.Source.Zika)
      val argument = ArgumentCaptor.forClass(classOf[util.Collection[String]])
      Mockito.verify(mockKafkaConsumer).subscribe(argument.capture());
      argument.getValue.size() must(be_>(0))
    }

    "not call insert when record is empty" in {
      consumer.sendToAsterix(Config.Source.Zika, "", "", mockAsterix, ConsumerRecords.empty())
      Mockito.verify(mockAsterix, Mockito.times(0)).insertRecord(any, any, any, any) must_== (())
      Mockito.verify(mockAsterix, Mockito.times(0)).ingest(any, any) must_== (())
    }
  }

}
