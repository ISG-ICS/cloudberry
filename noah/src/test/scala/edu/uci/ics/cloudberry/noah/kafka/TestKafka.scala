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

  "FileProducer" should {
    "read from a .gz file and send records to kafka" in {
      val mockConfig = mock[Config]
      val fileProducer = new FileProducer
      when (mockConfig.getFilePath).thenReturn("./noah/src/test/resources")
      when (mockConfig.getKfkTopic).thenReturn("")
      val generalProducerKafka = mock[GeneralProducerKafka]
      val kafkaProducer = mock[KafkaProducer[String, String]]
      fileProducer.run(mockConfig, generalProducerKafka, kafkaProducer)

      val argument = ArgumentCaptor.forClass(classOf[String])
      Mockito.verify(generalProducerKafka, times(3)).store(any, argument.capture(), any)

      val result = argument.getAllValues
      result should have size 3
      result.get(0) must_== "{\"create_at\":datetime(\"2016-10-04T17:43:24.000\"),\"id\":int64(\"783467591641751553\"),\"text\":\"Andd im here to see youuuuu\\u2764\\uFE0F https://t.co/60HOUreDsW\",\"in_reply_to_status\":int64(\"-1\"),\"in_reply_to_user\":int64(\"-1\"),\"favorite_count\":int64(\"0\"),\"retweet_count\":int64(\"0\"),\"lang\":\"en\",\"is_retweet\":false,\"place\":{\"country\":\"United States\",\"country_code\":\"United States\",\"full_name\":\"Manhattan, NY\",\"id\":\"01a9a39529b27f36\",\"name\":\"Manhattan\",\"place_type\":\"city\",\"bounding_box\":rectangle(\"-74.026675,40.683935 -73.910408,40.877483\")},\"geo_tag\":{\"stateID\":36,\"stateName\":\"New York\",\"countyID\":36061,\"countyName\":\"New York\",\"cityID\":36061,\"cityName\":\"Manhattan\"},\"user\":{\"id\":int64(\"238642932\"),\"name\":\"Snap\\u00A9hat: Heytasia\",\"screen_name\":\"ohheytasia\",\"lang\":\"en\",\"location\":\"Manhattan, NY\",\"create_at\":date(\"2011-01-15\"),\"description\":\"Owner of FAIRYDUST \\uD83C\\uDF02\",\"followers_count\":324,\"friends_count\":263,\"statues_count\":8119}}"
      result.get(1) must_== "{\"create_at\":datetime(\"2016-10-04T17:43:25.000\"),\"id\":int64(\"783467594972033025\"),\"text\":\"Been a bad couple days for this. https://t.co/0ZiH0RyQgc\",\"in_reply_to_status\":int64(\"-1\"),\"in_reply_to_user\":int64(\"-1\"),\"favorite_count\":int64(\"0\"),\"retweet_count\":int64(\"0\"),\"lang\":\"en\",\"is_retweet\":false,\"place\":{\"country\":\"United States\",\"country_code\":\"United States\",\"full_name\":\"Brooklyn, NY\",\"id\":\"011add077f4d2da3\",\"name\":\"Brooklyn\",\"place_type\":\"city\",\"bounding_box\":rectangle(\"-74.041878,40.570842 -73.855673,40.739434\")},\"geo_tag\":{\"stateID\":36,\"stateName\":\"New York\",\"countyID\":36047,\"countyName\":\"Kings\",\"cityID\":36047,\"cityName\":\"Brooklyn\"},\"user\":{\"id\":int64(\"52253803\"),\"name\":\"Ralph D. Russo\",\"screen_name\":\"ralphDrussoAP\",\"lang\":\"en\",\"location\":\"Brooklyn, usually.\",\"create_at\":date(\"2009-06-29\"),\"description\":\"Associated Press College Football Writer since 2005. I like to tweet about the Mets and sports in general, but college football butters my bread.\",\"followers_count\":24054,\"friends_count\":1095,\"statues_count\":51324}}"
      result.get(2) must_== "{\"create_at\":datetime(\"2016-10-04T17:43:24.000\"),\"id\":int64(\"783467591641752034\"),\"text\":\"Andd im here to see youuuuu\\u2764\\uFE0F https://t.co/60HOUreDsW\",\"in_reply_to_status\":int64(\"-1\"),\"in_reply_to_user\":int64(\"-1\"),\"favorite_count\":int64(\"0\"),\"retweet_count\":int64(\"0\"),\"lang\":\"en\",\"is_retweet\":false,\"place\":{\"country\":\"United States\",\"country_code\":\"United States\",\"full_name\":\"Manhattan, NY\",\"id\":\"01a9a39529b27f36\",\"name\":\"Manhattan\",\"place_type\":\"city\",\"bounding_box\":rectangle(\"-74.026675,40.683935 -73.910408,40.877483\")},\"geo_tag\":{\"stateID\":36,\"stateName\":\"New York\",\"countyID\":36061,\"countyName\":\"New York\",\"cityID\":36061,\"cityName\":\"Manhattan\"},\"user\":{\"id\":int64(\"238642932\"),\"name\":\"Snap\\u00A9hat: Heytasia\",\"screen_name\":\"ohheytasia\",\"lang\":\"en\",\"location\":\"Manhattan, NY\",\"create_at\":date(\"2011-01-15\"),\"description\":\"Owner of FAIRYDUST \\uD83C\\uDF02\",\"followers_count\":324,\"friends_count\":263,\"statues_count\":8119}}"

    }
  }
}
