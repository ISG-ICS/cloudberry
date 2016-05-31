package edu.uci.ics.cloudberry.noah.feed;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.Location;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import edu.uci.ics.cloudberry.noah.adm.UnknownPlaceException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import twitter4j.TwitterException;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPOutputStream;

public class TwitterFeedStreamDriver {

    Client twitterClient;
    volatile boolean isConnected = false;

    StreamFeedSocketAdapterClient socketAdapterClient;

    public void run(Config config)
            throws InterruptedException, IOException {
        BlockingQueue<String> queue = new LinkedBlockingQueue<String>(10000);
        StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
        // add some track terms

        if (config.getTrackTerms().length != 0) {
            System.err.print("set track terms are: ");
            for (String term : config.getTrackTerms()) {
                System.err.print(term);
                System.err.print(" ");
            }
            System.err.println();
            endpoint.trackTerms(Lists.newArrayList(config.getTrackTerms()));
        }
        if (config.getTrackLocation().length != 0) {

            System.err.print("set track locations are:");
            for (Location location : config.getTrackLocation()) {
                System.err.print(location);
                System.err.print(" ");
            }
            System.err.println();

            endpoint.locations(Lists.<Location>newArrayList(config.getTrackLocation()));
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

        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Date now = new Date();
        String strDate = sdfDate.format(now);

        String fileName = "Tweet_" + strDate + ".gz";
        GZIPOutputStream zip = new GZIPOutputStream(
                    new FileOutputStream(new File(fileName)));
        BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(zip, "UTF-8"));

        // Establish a connection
        try {
            twitterClient.connect();
            isConnected = true;
            // Do whatever needs to be done with messages
            while (true) {
                String msg = queue.take();
                bw.write(msg);
                try {
                    String adm = tagTweet.tagOneTweet(msg);
                    socketAdapterClient.ingest(adm);
                } catch (UnknownPlaceException e) {

                } catch(TwitterException e) {

                }
            }
        } finally {
            bw.close();
            twitterClient.stop();
        }
    }

    public void openSocket(Config config) throws IOException{
        String adapterUrl = config.getAdapterUrl();
        int port = config.getPort();
        int batchSize = config.getBatchSize();
        int waitMillSecPerRecord = config.getWaitMillSecPerRecord();
        int maxCount = config.getMaxCount();
        socketAdapterClient = new StreamFeedSocketAdapterClient(adapterUrl, port,
                batchSize, waitMillSecPerRecord, maxCount);
        socketAdapterClient.initialize();
    }

    public static void main(String[] args) throws IOException{
        TwitterFeedStreamDriver feedDriver = new TwitterFeedStreamDriver();
        try {
            Config config = new Config();
            CmdLineParser parser = new CmdLineParser(config);
            try {
                parser.parseArgument(args);
                if (config.getTrackTerms().length == 0 && config.getTrackLocation().length == 0) {
                    throw new CmdLineException("Should provide at list one tracking word, or one location boundary");
                }
            } catch (CmdLineException e) {
                System.err.println(e);
                parser.printUsage(System.err);
            }
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    if (feedDriver.twitterClient != null && feedDriver.isConnected) {
                        feedDriver.twitterClient.stop();
                    }
                }
            });
            feedDriver.openSocket(config);
            feedDriver.run(config);
        } catch (InterruptedException e) {
            System.err.println(e);
        } finally {
            feedDriver.socketAdapterClient.finalize();
        }
    }
}