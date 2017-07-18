package edu.uci.ics.cloudberry.noah.feed;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CmdLineAux {

    public static Config parseCmdLine(String[] args) {
        Config config = new Config();
        CmdLineParser parser = new CmdLineParser(config);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e);
            parser.printUsage(System.err);
        }
        return config;
    }

    public static BufferedWriter createWriter(String fileName) throws IOException {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Date now = new Date();
        String strDate = sdfDate.format(now);
        fileName += strDate + ".gz";
        GZIPOutputStream zip = new GZIPOutputStream(
                new FileOutputStream(new File(fileName)));
        BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(zip, "UTF-8"));
        return bw;
    }

    public static BufferedReader createGZipReader(String fileName) throws IOException {
        GZIPInputStream input = new GZIPInputStream(
                new FileInputStream(new File(fileName)));
        BufferedReader br = new BufferedReader(
                new InputStreamReader(input, "UTF-8"));
        return br;
    }

    public static ResponseList<User> getUsers(Config config, Twitter twitter) throws CmdLineException {

        ResponseList<User> users = null;
        try {
            users = twitter.lookupUsers(config.getTrackUsers());
        } catch (TwitterException ex) {
            throw new CmdLineException("No user was found, please check the username(s) provided");
        }
        return users;
    }

    public static Twitter getTwitterInstance(Config config) {
        ConfigurationBuilder builder = new ConfigurationBuilder();

        builder.setDebugEnabled(true)
                .setOAuthConsumerKey(config.getConsumerKey())
                .setOAuthConsumerSecret(config.getConsumerSecret())
                .setOAuthAccessToken(config.getToken())
                .setOAuthAccessTokenSecret(config.getTokenSecret())
                .setJSONStoreEnabled(true);

        TwitterFactory factory = new TwitterFactory(builder.build());
        Twitter twitter = factory.getInstance();
        return twitter;
    }
}
