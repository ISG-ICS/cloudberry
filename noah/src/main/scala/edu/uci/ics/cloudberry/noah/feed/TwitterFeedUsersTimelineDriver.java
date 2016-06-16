package edu.uci.ics.cloudberry.noah.feed;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class TwitterFeedUsersTimelineDriver {

    public void run(Config config) throws IOException {

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setDebugEnabled(true)
                .setOAuthConsumerKey(config.getConsumerKey())
                .setOAuthConsumerSecret(config.getConsumerSecret())
                .setOAuthAccessToken(config.getToken())
                .setOAuthAccessTokenSecret(config.getTokenSecret())
                .setJSONStoreEnabled(true);

        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Date now = new Date();
        String strDate = sdfDate.format(now);

        String fileName = "Tweet_Users_" + strDate + ".gz";
        GZIPOutputStream zip = new GZIPOutputStream(
                new FileOutputStream(new File(fileName)));
        BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(zip, "UTF-8"));

        try {
            TwitterFactory factory = new TwitterFactory(builder.build());
            Twitter twitter = factory.getInstance();
            String[] userNames = config.getTrackUsers();
            ResponseList<User> users = twitter.lookupUsers(userNames);
            for (User user : users) {
                if (user.getStatus() != null) {
                    //Paging in order to get all the tweets in the user timeline. Default is only the last 20.
                    int pageNum = 0;
                    while (user.getStatusesCount() > pageNum * 100) {
                        pageNum++;
                        Paging page = new Paging(pageNum, 100);
                        List<Status> statuses = twitter.getUserTimeline(user.getId(), page);
                        for (Status status : statuses) {
                            String statusJson = TwitterObjectFactory.getRawJSON(status);
                            bw.write(statusJson);
                        }
                    }

                }
            }
        } catch (TwitterException te) {
            System.err.println("Twitter get user timeline Exception");
        } finally {
            bw.close();
        }
    }

    public static void main(String[] args) throws IOException {
        TwitterFeedUsersTimelineDriver userDriver = new TwitterFeedUsersTimelineDriver();
        Config config = new Config();
        CmdLineParser parser = new CmdLineParser(config);
        try {
            parser.parseArgument(args);
            if (config.getTrackUsers().length == 0) {
                throw new CmdLineException("Should provide at least one tracking user");
            }
        } catch (CmdLineException e) {
            System.err.println(e);
            parser.printUsage(System.err);
        }
        userDriver.run(config);
    }
}
