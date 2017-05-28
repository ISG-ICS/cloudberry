package edu.uci.ics.cloudberry.noah.kafka

import edu.uci.ics.cloudberry.noah.feed.{CmdLineAux, Config}

/**
  * Created by Monique on 7/18/2016.
  */

object ConsumerHistUsersTimeline extends GeneralTopicConsumer{
  def main(args: Array[String]) {
    runMain(args, Config.Source.HistUser)
  }
}