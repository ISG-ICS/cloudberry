package edu.uci.ics.cloudberry.noah.kafka

import java.util.concurrent.{BlockingQueue, LinkedBlockingDeque}

import com.twitter.hbc.core.Client
import edu.uci.ics.cloudberry.noah.GeneralProducerKafka
import edu.uci.ics.cloudberry.noah.feed.Config
import org.apache.kafka.clients.producer.KafkaProducer
import org.mockito._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.mockito.Mockito._
import com.twitter.hbc.core.endpoint.Location

class TestProducer extends Specification with Mockito {

  "FileProducer" should {
    "read from a .gz file and send records to kafka" in {
      val generalProducerKafka = mock[GeneralProducerKafka]
      val kafkaProducer = mock[KafkaProducer[String, String]]

      val mockConfig = mock[Config]
      val fileProducer = new FileProducer
      when (mockConfig.getFilePath).thenReturn("/Tweet")
      when (mockConfig.getKfkTopic).thenReturn("")
      fileProducer.run(mockConfig, generalProducerKafka, kafkaProducer)

      val argument = ArgumentCaptor.forClass(classOf[String])
      Mockito.verify(generalProducerKafka, times(3)).store(any, argument.capture(), any)

      val result = argument.getAllValues
      result should have size 3
      result.get(0) must_== "test1"
      result.get(1) must_== "test2"
      result.get(2) must_== "test3"
    }
  }

  "TweetsProducer" should {
    val tweetsProducer = new TweetsProducer
    val mockConfig = mock[Config]
    when (mockConfig.getTrackTerms).thenReturn(Array("trump"))

    val loc: Array[Location] = new Array(0)
    when (mockConfig.getTrackLocation).thenReturn(loc)

 /**
  * Need to add your consumer key, consumer secret, token, token secret in the following code to run this test.
  *
    when (mockConfig.getConsumerKey).thenReturn("ck")
    when (mockConfig.getConsumerSecret).thenReturn("cs")
    when (mockConfig.getToken).thenReturn("tk")
    when (mockConfig.getTokenSecret).thenReturn("ts")

    "create a Twitter Client and set up connection" in {
      val queue:BlockingQueue[String] = new LinkedBlockingDeque[String]()
      val client = tweetsProducer.connectTwitter(mockConfig, queue)
      client.isDone must_== false
      queue.take.isEmpty must_== false
    }
  */

    "send records to kafka" in {
      val generalProducerKafka = mock[GeneralProducerKafka]
      val kafkaProducer = mock[KafkaProducer[String, String]]
      val queue = mock[BlockingQueue[String]]
      val mockTwitterClient = mock[Client]
      when (mockTwitterClient.isDone).thenReturn(false, true)
      when (queue.take).thenReturn("test")
      when (mockConfig.getKfkOnly).thenReturn(true)
      tweetsProducer.run(mockConfig, mockTwitterClient, generalProducerKafka, kafkaProducer, queue)

      val argument = ArgumentCaptor.forClass(classOf[String])
      Mockito.verify(generalProducerKafka).store(any, argument.capture(), any)

      val result = argument.getAllValues
      result should have size 1
      result.get(0) must_== "test"
    }
  }

}
