package edu.uci.ics.cloudberry.noah.feed;

import org.kohsuke.args4j.CmdLineException;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class TwitterFeedUsersTimelineDriver {

    public void run(Config config) throws IOException {

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setDebugEnabled(false)
                .setOAuthConsumerKey(config.getConsumerKey())
                .setOAuthConsumerSecret(config.getConsumerSecret())
                .setOAuthAccessToken(config.getToken())
                .setOAuthAccessTokenSecret(config.getTokenSecret())
                .setJSONStoreEnabled(true);

        try {
            TwitterFactory factory = new TwitterFactory(builder.build());
            Twitter twitter = factory.getInstance();
            String[] userNames = config.getTrackUsers();
            ResponseList<User> users = twitter.lookupUsers(userNames);
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