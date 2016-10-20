package edu.uci.ics.cloudberry.noah.kafka

import java.io.{EOFException, File}

import edu.uci.ics.cloudberry.noah.GeneralProducerKafka
import edu.uci.ics.cloudberry.noah.feed.{CmdLineAux, Config}
import org.apache.kafka.clients.producer.KafkaProducer
import org.kohsuke.args4j.CmdLineException
import play.api.Logger

object FileProducer {
  def main(args: Array[String]) {
    val fileProducer: FileProducer = new FileProducer

    var kafkaProducer: KafkaProducer[String, String] = null
    try {
      val config: Config = CmdLineAux.parseCmdLine(args)
      val generalProducerKafka = new GeneralProducerKafka(config)
      kafkaProducer = generalProducerKafka.createKafkaProducer
      fileProducer.run(config, generalProducerKafka, kafkaProducer)
    } catch {
      case e: Exception => {
        e.printStackTrace
      }
    } finally {
      if (kafkaProducer != null)
        kafkaProducer.close
    }
  }
}

class FileProducer {
  def load(filePath: String, topic: String, generalProducerKafka: GeneralProducerKafka, kafkaProducer: KafkaProducer[String, String]): Unit = {
    val file: File = new File(filePath)
    if (file.isDirectory) {
      file.listFiles().foreach { file =>
        load(filePath + "/" + file.getName, topic, generalProducerKafka, kafkaProducer)
      }
    } else if (filePath.endsWith(".gz")){
      Logger.info("Loading file " + filePath + " ...... ")
      val br = CmdLineAux.createGZipReader(filePath)
      try {
        var str = br.readLine()
        while ( str != null) {
          generalProducerKafka.store(topic, str, kafkaProducer)
          str = br.readLine()
        }
      } catch {
        case e: EOFException => {}
      } finally {
        br.close()
      }
    } else {
      Logger.info("Ingored file " + filePath)
    }
  }
  def run(config: Config, generalProducerKafka: GeneralProducerKafka, kafkaProducer: KafkaProducer[String, String]) {
      load(config.getFilePath, config.getKfkTopic, generalProducerKafka, kafkaProducer)
  }
}