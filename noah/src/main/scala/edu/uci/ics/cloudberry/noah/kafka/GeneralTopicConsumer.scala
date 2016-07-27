package edu.uci.ics.cloudberry.noah.kafka

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import edu.uci.ics.cloudberry.noah.feed.Config.Source
import edu.uci.ics.cloudberry.noah.feed.{CmdLineAux, Config}
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.kohsuke.args4j.CmdLineException
import play.api.libs.ws.ahc.AhcWSClient

/**
  * Created by Monique on 7/21/2016.
  */

class GeneralTopicConsumer {

 private def createClient(): AhcWSClient = {
    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    AhcWSClient()
  }

  private def closeClient(wsClient:AhcWSClient): Unit = {
    wsClient.close()
  }

  def runMain(args: Array[String], source:Source): Unit ={
    val wsClient = createClient()
    try {
      val config: Config = CmdLineAux.parseCmdLine(args)

      if (config.getKafkaServer.isEmpty || config.getKafkaId.isEmpty || config.getAxServer.isEmpty) {
        throw new CmdLineException("Should provide a server for both kafka and asterixDB(hostname:port) and a consumer ID")
      }
      val consumer = new AsterixConsumerKafka(config, wsClient)
      val props = consumer.getProperties()
      val kafkaConsumer = new KafkaConsumer[String, String](props)
      consumer.consume(source, kafkaConsumer, props.getProperty("poll.ms").toLong)
    }
    catch {
      case e: CmdLineException => {
        e.printStackTrace(System.err)
      }
    }finally {
      closeClient(wsClient)
    }
  }
}
