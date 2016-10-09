package edu.uci.ics.cloudberry.noah.feed

import com.twitter.hbc.ClientBuilder
import com.twitter.hbc.core.Client
import com.twitter.hbc.core.Constants
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint
import com.twitter.hbc.core.processor.StringDelimitedProcessor
import com.twitter.hbc.httpclient.auth.Authentication
import com.twitter.hbc.httpclient.auth.OAuth1
import edu.uci.ics.cloudberry.noah.GeneralProducerKafka
import org.apache.kafka.clients.producer.KafkaProducer
import org.kohsuke.args4j.CmdLineException
import java.io.{BufferedWriter, IOException}
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import scala.collection.JavaConverters._


object TweetsProducer {
  @throws(classOf[IOException])
  def main(args: Array[String]) {
    val tweetsProducer: TweetsProducer = new TweetsProducer
    try {
      val config: Config = CmdLineAux.parseCmdLine(args)
      Runtime.getRuntime.addShutdownHook(new Thread() {
        override def run {
          if (tweetsProducer.twitterClient != null && tweetsProducer.isConnected) {
            tweetsProducer.twitterClient.stop
          }
        }
      })
      if (config.getTrackTerms.length == 0 && config.getTrackLocation.length == 0) {
        throw new CmdLineException("Should provide at least one tracking word, or one location boundary")
      }
      tweetsProducer.run(config)
    }
    catch {
      case e: CmdLineException => {
        e.printStackTrace(System.err)
      }
      case e: Exception => {
        e.printStackTrace(System.err)
      }
    }
  }
}

class TweetsProducer {
  private[feed] var twitterClient: Client = null
  @volatile
  private[feed] var isConnected: Boolean = false

  def run(config: Config) {
    val queue: BlockingQueue[String] = new LinkedBlockingQueue[String](10000)
    val endpoint: StatusesFilterEndpoint = new StatusesFilterEndpoint
    if (config.getTrackTerms.length != 0) {
      System.err.print("set track terms are: ")
      for (term <- config.getTrackTerms) {
        System.err.print(term)
        System.err.print(" ")
      }
      System.err.println
      endpoint.trackTerms(config.getTrackTerms.toList.asJava)
    }
    if (config.getTrackLocation.length != 0) {
      System.err.print("set track locations are:")
      for (location <- config.getTrackLocation) {
        System.err.print(location)
        System.err.print(" ")
      }
      System.err.println
      endpoint.locations(config.getTrackLocation.toList.asJava)
    }
    val auth: Authentication = new OAuth1(config.getConsumerKey, config.getConsumerSecret, config.getToken, config.getTokenSecret)
    twitterClient = new ClientBuilder().hosts(Constants.STREAM_HOST).endpoint(endpoint).authentication(auth).processor(new StringDelimitedProcessor(queue)).build
    val generalProducerKafka: GeneralProducerKafka = new GeneralProducerKafka(config)
    val kafkaProducer: KafkaProducer[String, String] = generalProducerKafka.createKafkaProducer
    val br: BufferedWriter = CmdLineAux.createWriter("Tweet_");
    try {
      twitterClient.connect
      isConnected = true
      while (!twitterClient.isDone) {
        val msg: String = queue.take
        br.write(msg)
        generalProducerKafka.store(config.getKfkTopic, msg, kafkaProducer)
      }
    }
    catch {
      case e: Exception => {
        e.printStackTrace(System.err)
      }
    } finally {
      twitterClient.stop
      br.close()
      kafkaProducer.close
    }
  }
}