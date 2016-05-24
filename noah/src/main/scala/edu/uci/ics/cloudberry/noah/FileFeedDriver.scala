package edu.uci.ics.cloudberry.noah

import java.io.FileReader
import scala.collection.mutable

object FileFeedDriver {

  val paraMap = mutable.Map.empty[String, String]
  val usage =
    """
      |Usage:
      | -src: path of source file
      | -url: url of the feed adapter
      | -p: port of the feed socket
      | -w: waiting milliseconds per record, default 500
      | -b: batchsize per waiting periods, default 50
      | -c: maximum number to feed, default unlimited
    """.stripMargin

  paraMap += "waitMillSecPerRecord" -> "500"
  paraMap += "batchSize" -> "50"
  paraMap += "maxCount" -> Int.MaxValue.toString

  def parseOption(list: List[String]) {
    list match {
      case Nil =>
      case "-h" :: tail => System.err.println(usage); System.exit(0)
      case "-src" :: value :: tail => paraMap += "srcPath" -> value; parseOption(tail)
      case "-url" :: value :: tail => paraMap += "adapterUrl" -> value; parseOption(tail)
      case "-p" :: value :: tail => paraMap += "port" -> value; parseOption(tail)
      case "-w" :: value :: tail => paraMap("waitMillSecPerRecord") = value; parseOption(tail)
      case "-b" :: value :: tail => paraMap("batchSize") = value; parseOption(tail)
      case "-c" :: value :: tail => paraMap("maxCount") = value; parseOption(tail)
      case option :: tail => System.err.println("unknown option:" + option); System.err.println(usage); System.exit(1);
    }
  }

  def main(args: Array[String]): Unit = {
    parseOption(args.toList)

    val reader = new FileReader(paraMap("srcPath"))
    val client: FileFeedSocketAdapterClient = new FileFeedSocketAdapterClient(
      paraMap("adapterUrl"),
      paraMap("port").toInt,
      reader,
      paraMap("batchSize").toInt,
      paraMap("waitMillSecPerRecord").toInt,
      paraMap("maxCount").toInt
    )
    try {
        client.initialize
        client.ingest
    } finally {
        client.finalize
    }
  }
}

