package edu.uci.ics.cloudberry.noah.feed

import java.io.{EOFException, File}

import edu.uci.ics.cloudberry.noah.GeneralProducerKafka
import org.apache.kafka.clients.producer.KafkaProducer
import org.kohsuke.args4j.CmdLineException

object FileProducer {
  def main(args: Array[String]) {
    val fileProducer: FileProducer = new FileProducer
    try {
      val config: Config = CmdLineAux.parseCmdLine(args)
      fileProducer.run(config)
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

class FileProducer {
  private var generalProducerKafka: GeneralProducerKafka = null
  private var kafkaProducer: KafkaProducer[String, String] = null

  def load(filePath: String, config: Config): Unit = {
    val file: File = new File(filePath)

    println(filePath)
    if (file.isDirectory) {
      file.listFiles().foreach { file =>
        load(filePath + "/" + file.getName, config)
      }
    } else if (filePath.endsWith(".gz")){
      val br = CmdLineAux.createGZipReader(filePath)
      var str: String = null
      try {
        while ((str = br.readLine() )!= null) {
          println(str)
          generalProducerKafka.store(config.getKfkTopic, str, kafkaProducer)
        }
      } catch {
        case e: EOFException => {}
      } finally {
        br.close()
      }
    }
  }
  def run(config: Config) {
      generalProducerKafka = new GeneralProducerKafka(config)
      kafkaProducer = generalProducerKafka.createKafkaProducer

      load(config.getFilePath, config)

      kafkaProducer.close
  }
}