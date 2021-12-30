package edu.uci.ics.cloudberry.datatools.asterixdb;

import edu.uci.ics.cloudberry.datatools.twitter.geotagger.TwitterGeoTagger;
import edu.uci.ics.cloudberry.datatools.twitter.geotagger.USGeoGnosisLoader;
import edu.uci.ics.cloudberry.gnosis.USGeoGnosis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * AsterixDBIngestionDriver
 *
 * operates in the following two modes:
 *
 *  - (1) If given `from-proxy` url argument,
 *        it listens to the TwitterIngestionProxy websocket,
 *        and for each tweet received, it geotags, transforms and
 *        then ingests the tweet to the target AsterixDB.
 *
 *  - (2) If not given `from-proxy` url argument,
 *        it reads tweets from stdin line by line,
 *        and for each tweet received, it transforms and
 *        then ingests the tweet to the target AsterixDB.
 */
public class AsterixDBIngestionDriver {

    public static ProxyIngestionWorker proxyIngestionWorker;
    public static AsterixDBFeedClient asterixDBFeedClient;
    public static AsterixDBAdapter asterixDBAdapter;
    public static USGeoGnosis usGeoGnosis;
    public static long startTime;
    public static long counter;

    /**
     * tag and ingest one tweet
     *
     *  - (1) use TwitterGeoTagger to geotag the tweet
     *  - (2) use AsterixDBAdapter to transform a tweet from JSON to AsterixDB object (reducing columns at the same time)
     *  - (3) use AsterixDBFeedClient to ingest the tweet to the target AsterixDB
     *
     * @param tweet
     */
    public static void tagAndIngestOneTweet(String tweet) {
        Map<String, Object> tweetObject = TwitterGeoTagger.tagOneTweet(usGeoGnosis, tweet);
        try {
            String tweetADBObject = asterixDBAdapter.transform(tweetObject);
            asterixDBFeedClient.ingest(tweetADBObject);
        }
        catch (Exception e) {
            System.err.println("processOneTweet failed!");
            e.printStackTrace();
        }
    }

    /**
     * ingest one tweet
     *
     *  - (1) use AsterixDBAdapter to transform a tweet from JSON to AsterixDB object (reducing columns at the same time)
     *  - (2) use AsterixDBFeedClient to ingest the tweet to the target AsterixDB
     *
     * @param tweet
     */
    public static void ingestOneTweet(String tweet) {
        try {
            String tweetADBObject = asterixDBAdapter.transform(tweet);
            asterixDBFeedClient.ingest(tweetADBObject);
        }
        catch (Exception e) {
            System.err.println("ingestOneTweet failed!");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // parse command line arguments
        AsterixDBIngestionConfig config = AsterixDBIngestionConfig.createFromCLIArgs(args);
        // parsing exception or config invalid
        if (config == null) {
            return;
        }

        // start an AsterixDB feed client to the target AsterixDB
        asterixDBFeedClient = new AsterixDBFeedClient(config.getHost(), config.getPort());
        try {
            asterixDBFeedClient.initialize();
        } catch (IOException e) {
            System.err.println("Establish connection to the target AsterixDB Feed [" + config.getHost() + ": " + config.getPort() + "] failed!");
            e.printStackTrace();
            return;
        }

        // initialize AsterixDB adapter
        if (config.getAdapterName().equalsIgnoreCase("twittermap")) {
            asterixDBAdapter = new AsterixDBAdapterForTwitterMap();
        }
        else {
            asterixDBAdapter = new AsterixDBAdapterForTwitter();
        }

        // mode (1) - from Twitter ingestion proxy
        if (config.getFromProxy() != null) {

            // create a USGeoGnosis object for the use of TwitterGeoTagger
            try {
                usGeoGnosis = USGeoGnosisLoader.loadUSGeoGnosis(config.getStateJsonFile(),
                        config.getCountyJsonFile(), config.getCityJsonFile());
            } catch (Exception e) {
                System.err.println("Load USGeoGnosis failed!");
                System.err.println("Please check the 3 GeoJson files for state, county and city are available.");
                e.printStackTrace();
            }

            // start a Proxy Ingestion Worker to use a TwitterIngestProxySocketClient to get tweets from the Proxy Server
            proxyIngestionWorker = new ProxyIngestionWorker(config);
            Thread proxyIngestionWorkerThread = new Thread(proxyIngestionWorker);
            // clean up when shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (proxyIngestionWorker != null) {
                    proxyIngestionWorker.cleanUp();
                }
            }));
            proxyIngestionWorkerThread.start();
            try {
                proxyIngestionWorkerThread.join();
            }
            catch(Exception e) {
                e.printStackTrace(System.err);
            }

            // WebSocketClient client = new WebSocketClient();
            // TwitterIngestionProxySocketClient socket = new TwitterIngestionProxySocketClient();
            // try {
            //     client.start();
            //     URI fromProxyUri = new URI(config.getFromProxy());
            //     ClientUpgradeRequest request = new ClientUpgradeRequest();
            //     client.connect(socket, fromProxyUri, request);
            // }
            // catch (Exception e) {
            //     System.err.println("Establish connection to the source proxy socket [" + config.getFromProxy() + "] failed!");
            //     e.printStackTrace();
            // }
        }
        // mode (2) - from stdin
        else {
            counter = 0;
            startTime = System.currentTimeMillis();
            try {
                InputStreamReader reader = new InputStreamReader(System.in);
                BufferedReader bufferedReader = new BufferedReader(reader);
                String tweet;
                while ((tweet = bufferedReader.readLine()) != null) {
                    ingestOneTweet(tweet);
                    counter++;
                    if (counter % 100000 == 0) {
                        printStats();
                    }
                }
                System.err.println(">>> Ingestion done! <<<");
                printStats();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void printStats() {
        long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;;
        long elapsedMinutes = elapsedSeconds / 60;
        double rate = (double) counter / elapsedSeconds;
        System.err.println(">>> # of ingested records: " + counter + " Elapsed (s) : " +
                elapsedSeconds + " (m) : " + elapsedMinutes + " record/sec : " + rate);
    }
}
