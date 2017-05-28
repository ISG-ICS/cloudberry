package edu.uci.ics.cloudberry.noah.kafka

import edu.uci.ics.cloudberry.noah.feed.{CmdLineAux, Config}
import org.kohsuke.args4j.CmdLineException



object ConsumerZikaStreaming extends GeneralTopicConsumer{
  def main(args: Array[String]) {
    runMain(args, Config.Source.Zika)
  }
}
