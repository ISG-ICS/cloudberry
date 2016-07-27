package edu.uci.ics.cloudberry.noah.feed;

import edu.uci.ics.cloudberry.noah.GeneralProducerKafka;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.kohsuke.args4j.CmdLineException;
import twitter4j.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class TwitterHistoricalUsersTimelineDriver {

    public void run(Config config) throws IOException, CmdLineException {

        //Get historical user data
        try {
            GeneralProducerKafka producer = new GeneralProducerKafka(config);
            KafkaProducer<String, String> kafkaProducer = producer.createKafkaProducer();
            Twitter twitter = CmdLineAux.getTwitterInstance(config);
            ResponseList<User> users = CmdLineAux.getUsers(config, twitter);
            for (User user : users) {
                BufferedWriter bw = CmdLineAux.createWriter("Tweet_User_"+user.getName() + "_");
                try {
                    if (user.getStatus() != null) {
                        //Paging in order to get all the tweets in the user timeline. Default is only the last 20.
                        int pageNum = 0;
                        while (user.getStatusesCount() > pageNum * 100) {
                            pageNum++;
                            Paging page = new Paging(pageNum,100);
                            List<Status> statuses = twitter.getUserTimeline(user.getId(), page);
                            for (Status status : statuses) {
                                String statusJson = TwitterObjectFactory.getRawJSON(status);
                                bw.write(statusJson);
                                if (config.isStoreKafka()) {
                                    producer.store(config.getTopic(Config.Source.HistUser), statusJson, kafkaProducer);
                                }

                            }
                        }
                    }
                } finally {
                    bw.close();
                }
            }
        } catch (TwitterException te) {
            System.err.println("User not found");
            te.printStackTrace();
        }

    }

    public static void main(String[] args) throws IOException, CmdLineException {

        TwitterHistoricalUsersTimelineDriver userDriver = new TwitterHistoricalUsersTimelineDriver();
        Config config = CmdLineAux.parseCmdLine(args);

        try {
            if (config.getTrackUsers().length == 0) {
                throw new CmdLineException("Should provide at least one tracking user");
            }
        } catch (CmdLineException e) {
            System.err.println(e);
        }
        userDriver.run(config);
    }
}