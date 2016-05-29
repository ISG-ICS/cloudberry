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
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TwitterFeedStreamDriver {

    static Client twitterClient;
    volatile static boolean isConnected = false;

    static StreamFeedSocketAdapterClient socketAdapterClient;

    public static void run(Config config)
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

        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        Date now = new Date();
        String strDate = sdfDate.format(now);

        String fileName = "Tweet_" + strDate + ".txt";
        File fout = new File(fileName);
        FileOutputStream fos = new FileOutputStream(fout);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));


        // Establish a connection
        try {
            twitterClient.connect();

            isConnected = true;
            // Do whatever needs to be done with messages
            while (true) {
                String msg = queue.take();
                String adm = tagTweet.tagOneTweet(msg) + "\n";
                if ((adm.length() > 0) && (!adm.equals("\n"))) {
                    socketAdapterClient.ingest(adm);
                    bw.write(adm);
                }
            }
        } finally {
            bw.close();
            twitterClient.stop();
        }

    }

    public static void openSocket(Config config) {
        String adapterUrl = config.getAdapterUrl();
        Integer port = config.getPort();
        Integer batchSize = config.getBatchSize();
        Integer waitMillSecPerRecord = config.getWaitMillSecPerRecord();
        Integer maxCount = config.getMaxCount();
        socketAdapterClient = new StreamFeedSocketAdapterClient(adapterUrl, port,
                batchSize, waitMillSecPerRecord, maxCount);
        socketAdapterClient.initialize();
    }

    public static void main(String[] args) {
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
                    if (twitterClient != null && isConnected) {
                        twitterClient.stop();
                    }
                }
            });
            openSocket(config);
            TwitterFeedStreamDriver.run(config);
        } catch (InterruptedException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            socketAdapterClient.finalize();
        }
    }
}