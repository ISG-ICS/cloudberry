package edu.uci.ics.cloudberry.noah.feed;

import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import edu.uci.ics.cloudberry.noah.GeneralProducerKafka;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.kohsuke.args4j.CmdLineException;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.User;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TwitterUserStreamDriver {

    private List<Long> getUsersID(Config config) throws CmdLineException {

        Twitter twitter = CmdLineAux.getTwitterInstance(config);
        ResponseList<User> users = CmdLineAux.getUsers(config, twitter);
        List<Long> usersID = new ArrayList<Long>();
        for (User user : users) {
            usersID.add(user.getId());
        }
        return usersID;
    }

    public void run(Config config) throws IOException, CmdLineException {

        List<Long> usersID = getUsersID(config);

        boolean isConnected = false;
        BlockingQueue<String> queue = new LinkedBlockingQueue<String>(10000);
        StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
        if (!usersID.isEmpty()) {
            endpoint.followings(usersID);
        }
        Authentication auth = new OAuth1(config.getConsumerKey(), config.getConsumerSecret(), config.getToken(),
                config.getTokenSecret());

        // Create a new BasicClient. By default gzip is enabled.
        Client twitterClient = new ClientBuilder()
                .hosts(Constants.STREAM_HOST)
                .endpoint(endpoint)
                .authentication(auth)
                .processor(new StringDelimitedProcessor(queue))
                .build();

        // Create a gz file to store live tweets from the list of users provided
        BufferedWriter bw = CmdLineAux.createWriter("Tweet_Zika_Users_");
        try {
            GeneralProducerKafka producer = new GeneralProducerKafka(config);
            KafkaProducer<String, String> kafkaProducer = producer.createKafkaProducer();
            twitterClient.connect();
            isConnected = true;
            // Do whatever needs to be done with messages
            while (!twitterClient.isDone()) {
                String msg = null;
                try {
                    msg = queue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                bw.write(msg);
                if (config.isStoreKafka()) {
                    producer.store(config.getTopic(Config.Source.User), msg, kafkaProducer);
            }
            }
        } finally {
            bw.close();
            twitterClient.stop();
        }
    }

    public static void main(String[] args) throws IOException, CmdLineException {

        TwitterUserStreamDriver userDriver = new TwitterUserStreamDriver();
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
