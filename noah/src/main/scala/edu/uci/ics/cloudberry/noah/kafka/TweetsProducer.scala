package edu.uci.ics.cloudberry.noah.kafka

import java.io.BufferedWriter
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import com.twitter.hbc.ClientBuilder
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint
import com.twitter.hbc.core.processor.StringDelimitedProcessor
import com.twitter.hbc.core.{Client, Constants}
import com.twitter.hbc.httpclient.auth.{Authentication, OAuth1}
import edu.uci.ics.cloudberry.noah.GeneralProducerKafka
import edu.uci.ics.cloudberry.noah.feed.{CmdLineAux, Config}
import org.apache.kafka.clients.producer.KafkaProducer
import org.kohsuke.args4j.CmdLineException

import scala.collection.JavaConverters._
import play.api.Logger

import scala.util.{Failure, Success, Try}

object TweetsProducer {
  def main(args: Array[String]) {
    val tweetsProducer: TweetsProducer = new TweetsProducer
    try {
      val config: Config = CmdLineAux.parseCmdLine(args)
      val queue: BlockingQueue[String] = new LinkedBlockingQueue[String](10000)
      val twitterClient = Try(tweetsProducer.connectTwitter(config, queue))
      Runtime.getRuntime.addShutdownHook(new Thread() {
        override def run {
          twitterClient match {
            case Success(tc) => if (!tc.isDone) tc.stop else Logger.info("Twitter client has been stopped")
            case Failure(tc) => Logger.warn("No twitter client when shut down")
          }
        }
      })
      if (config.getTrackTerms.length == 0 && config.getTrackLocation.length == 0) {
        throw new CmdLineException("Should provide at least one tracking word, or one location boundary")
      }

      val generalProducerKafka: GeneralProducerKafka = new GeneralProducerKafka(config)
      val kafkaProducer: KafkaProducer[String, String] = generalProducerKafka.createKafkaProducer

      tweetsProducer.run(config, twitterClient.get, generalProducerKafka, kafkaProducer, queue)

    } catch {
      case e: Exception => {
        e.printStackTrace
      }
    }
  }
}

class TweetsProducer {

  def connectTwitter(config: Config, queue: BlockingQueue[String]): Client = {
    val endpoint: StatusesFilterEndpoint = new StatusesFilterEndpoint
    if (config.getTrackTerms.length != 0) {
      Logger.info("set track terms are: ")
      for (term <- config.getTrackTerms) {
        Logger.info(term)
      }
      endpoint.trackTerms(config.getTrackTerms.toList.asJava)
    }
    if (config.getTrackLocation.length != 0) {
      Logger.info("set track locations are:")
      for (location <- config.getTrackLocation) {
        Logger.info(location.toString)
      }
      endpoint.locations(config.getTrackLocation.toList.asJava)
    }
    val auth: Authentication = new OAuth1(config.getConsumerKey, config.getConsumerSecret, config.getToken, config.getTokenSecret)
    val twitterClient: Client = new ClientBuilder()
      .hosts(Constants.STREAM_HOST)
      .endpoint(endpoint)
      .authentication(auth)
      .processor(new StringDelimitedProcessor(queue))
      .build

    twitterClient.connect
    twitterClient
  }

  def run(config: Config, twitterClient: Client, generalProducerKafka: GeneralProducerKafka, kafkaProducer: KafkaProducer[String, String], queue: BlockingQueue[String]) {

    val bw: Option[BufferedWriter] = if (config.getKfkOnly) None else Some(CmdLineAux.createWriter("Tweet_"))
    while (!twitterClient.isDone) {
      val msg: String = queue.take
      if (! config.getKfkOnly ) {
        bw.map(_.write(msg))
      }
      generalProducerKafka.store(config.getKfkTopic, msg, kafkaProducer)
    }
  }
}