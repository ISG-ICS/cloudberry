package edu.uci.ics.cloudberry.noah.kafka

import java.io.{EOFException, File}

import edu.uci.ics.cloudberry.noah.GeneralProducerKafka
import edu.uci.ics.cloudberry.noah.feed.{CmdLineAux, Config}
import org.apache.kafka.clients.producer.KafkaProducer
import play.api.Logger
import scala.util.{Failure, Success, Try}

object FileProducer {
  def main(args: Array[String]) {
    val fileProducer: FileProducer = new FileProducer

    var kafkaProducer = None: Option[KafkaProducer[String, String]]
    try {
      val config: Config = CmdLineAux.parseCmdLine(args)
      val generalProducerKafka = new GeneralProducerKafka(config)
      kafkaProducer = Some(generalProducerKafka.createKafkaProducer)
      fileProducer.run(config, generalProducerKafka, kafkaProducer.get)
    } catch {
      case e: Exception => {
        e.printStackTrace
      }
    } finally {
      Try (kafkaProducer.get) match {
        case Success(k) => k.close
        case Failure(k) => Logger.warn("No KafkaProducer found when closing")
      }
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
        val stream = Stream.continually(br.readLine()).takeWhile(Option(_) != None)
        stream.foreach (generalProducerKafka.store(topic, _, kafkaProducer))
        Logger.info("Loaded " + stream.size + " records into kafka")
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
    val path = getClass.getResource(config.getFilePath).getPath

    if (config.getKfkTopic == None)
      throw new Error("No kafka topic specified")

    load(path, config.getKfkTopic, generalProducerKafka, kafkaProducer)
  }
}