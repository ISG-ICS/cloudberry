package edu.uci.ics.cloudberry.noah.kafka;

import edu.uci.ics.cloudberry.noah.feed.CmdLineAux;
import edu.uci.ics.cloudberry.noah.feed.Config;
import org.kohsuke.args4j.CmdLineException;

public class ConsumerHistUsersTimeline {
    public static void main(String[] args) {

        try {
            Config config = CmdLineAux.parseCmdLine(args);

            if (config.getKafkaServer().isEmpty() || config.getKafkaId().isEmpty()) {
                throw new CmdLineException("Should provide a server (hostname:port) and a consumer ID");
            }

            ConsumerKafka consumer = new ConsumerKafka();
            String[] topics = {config.getTopicHistUsers()};
            consumer.run(config, topics, "ds_hist_users_timeline");

        } catch (CmdLineException e) {
            e.printStackTrace(System.err);
        }
    }
}
