package edu.uci.ics.cloudberry.noah.feed;

import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.kohsuke.args4j.CmdLineException;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TwitterFeedUsersTimelineDriver {

    public void run(Config config) throws IOException {
        List<Long> usersID = new ArrayList<Long>();

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setDebugEnabled(false)
                .setOAuthConsumerKey(config.getConsumerKey())
                .setOAuthConsumerSecret(config.getConsumerSecret())
                .setOAuthAccessToken(config.getToken())
                .setOAuthAccessTokenSecret(config.getTokenSecret())
                .setJSONStoreEnabled(true);
        //Get historical user data
        try {
            TwitterFactory factory = new TwitterFactory(builder.build());
            Twitter twitter = factory.getInstance();
            String[] userNames = config.getTrackUsers();
            ResponseList<User> users = twitter.lookupUsers(userNames);
            for (User user : users) {
                usersID.add(user.getId());
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
                            }
                        }
                    }
                } finally {
                    bw.close();
                }
            }
        } catch (TwitterException te) {
            System.err.println("Twitter get user timeline Exception");
        }

        //User Stream
        Client twitterClient;
        boolean isConnected = false;
        BlockingQueue<String> queue = new LinkedBlockingQueue<String>(10000);
        StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
        if (config.getTrackUsers().length != 0) {
            endpoint.followings(usersID);
        }
        Authentication auth = new OAuth1(config.getConsumerKey(), config.getConsumerSecret(), config.getToken(),
                config.getTokenSecret());

        // Create a new BasicClient. By default gzip is enabled.
        twitterClient = new ClientBuilder()
                .hosts(Constants.STREAM_HOST)
                .endpoint(endpoint)
                .authentication(auth)
                .processor(new StringDelimitedProcessor(queue))
                .build();

        // Establish a connection
        BufferedWriter bw = CmdLineAux.createWriter("Tweet_UserStream_");
        try {
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
            }
        } finally {
            bw.close();
            twitterClient.stop();
        }

    }

    public static void main(String[] args) throws IOException {

        TwitterFeedUsersTimelineDriver userDriver = new TwitterFeedUsersTimelineDriver();
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