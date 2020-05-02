package edu.uci.ics.cloudberry.datatools.twitter;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class TwitterIngestionServer {

    public static StringBuffer tweet;

    /**
     * Parse command line arguments into a TwitterIngestionDriverConfig object.
     *
     * @param args
     * @return TwitterIngestionDriverConfig, null if exception in parsing or config invalid.
     */
    public TwitterIngestionConfig parseCmdLine(String[] args) {
        TwitterIngestionConfig config = new TwitterIngestionConfig();
        CmdLineParser parser = new CmdLineParser(config);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            e.printStackTrace();
            parser.printUsage(System.err);
            return null;
        }
        // validate config
        if (config.getTrackKeywords().length == 0 && config.getTrackLocation().length == 0) {
            System.err.println("Please provide at least one tracking keyword, or one location bounding box!");
            return null;
        }
        return config;
    }

    public static void main(String[] args) {
        TwitterIngestionServer twitterIngestionServer = new TwitterIngestionServer();

        // parse command line arguments
        TwitterIngestionConfig config = twitterIngestionServer.parseCmdLine(args);
        // parsing exception or config invalid
        if (config == null) {
            return;
        }

        // start Twitter Ingestion Worker
        TwitterIngestionWorker twitterIngestionWorker = new TwitterIngestionWorker(config, tweet);
        Thread twitterIngestionWorkerThread = new Thread(twitterIngestionWorker);
        twitterIngestionWorkerThread.start();

        // clean up
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (twitterIngestionWorker != null) {
                twitterIngestionWorker.cleanUp();
            }
        }));
    }
}