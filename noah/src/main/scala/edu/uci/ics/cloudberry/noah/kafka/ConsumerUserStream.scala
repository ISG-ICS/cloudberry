package edu.uci.ics.cloudberry.noah.kafka

import edu.uci.ics.cloudberry.noah.feed.{CmdLineAux, Config}
import org.kohsuke.args4j.CmdLineException

/**
  * Created by Monique on 7/18/2016.
  */

object ConsumerUserStream extends GeneralTopicConsumer{
  def main(args: Array[String]) {
    runMain(args, Config.Source.User)
  }
}
