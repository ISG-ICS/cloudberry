package edu.uci.ics.cloudberry.noah.kafka;

import edu.uci.ics.cloudberry.noah.feed.CmdLineAux;
import edu.uci.ics.cloudberry.noah.feed.Config;
import org.kohsuke.args4j.CmdLineException;

public class ConsumerUserStream {
    public static void main(String[] args) {

        try {
            Config config = CmdLineAux.parseCmdLine(args);

            if (config.getKafkaServer().isEmpty() || config.getKafkaId().isEmpty()) {
                throw new CmdLineException("Should provide a server (hostname:port) and a consumer ID");
            }

            ConsumerKafka consumer = new ConsumerKafka();
            String[] topics = {"TwitterUserStream"};
            consumer.run(config.getKafkaServer(), config.getKafkaId(), topics);

        } catch (CmdLineException e) {
            e.printStackTrace(System.err);
        }
    }
}
