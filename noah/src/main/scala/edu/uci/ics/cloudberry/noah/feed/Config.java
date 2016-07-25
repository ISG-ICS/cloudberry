package edu.uci.ics.cloudberry.noah.feed;

import com.twitter.hbc.core.endpoint.Location;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

import java.util.ArrayList;

public class Config {
    @Option(name = "-ck", aliases = "--consumer-key", usage = "ConsumerKey for Twitter OAuth")
    private String consumerKey = null;

    @Option(name = "-cs", aliases = "--consumer-secret", usage = "Consumer Secret for Twitter OAuth")
    private String consumerSecret = null;

    @Option(name = "-tk", aliases = "--token", usage = "Token for Twitter OAuth")
    private String token = null;

    @Option(name = "-ts", aliases = "--token-secret", usage = "Token secret for Twitter OAuth")
    private String tokenSecret = null;

    @Option(name = "-tr", aliases = "--tracker", handler = TermArrayOptionHandler.class, usage = "Tracked terms, separated by comma.")
    private String[] trackTerms = new String[]{};

    @Option(name = "-tu", aliases = "--track-user", handler = TermArrayOptionHandler.class, usage = "Tracked public users, by username, separated by comma.")
    private String[] trackUsers = new String[]{};

    @Option(name = "-loc", aliases = "--location", handler = LocationListOptionHandler.class, usage = "location rectangular, southwest.lon, southwest.lat, northeast.lon, northeast.lat")
    private Location[] trackLocation = new Location[]{};

    @Option(name = "-u", aliases = "--url", usage = "url of the feed adapter")
    private String adapterUrl;

    @Option(name = "-p", aliases = "--port", usage = "port of the feed socket")
    private int port;

    @Option(name = "-w", aliases = "--wait", usage = "waiting milliseconds per record, default 500")
    private int waitMillSecPerRecord = 500;

    @Option(name = "-b", aliases = "--batch", usage = "batchsize per waiting periods, default 50")
    private int batchSize = 50;

    @Option(name = "-c", aliases = "--count", usage = "maximum number to feed, default unlimited")
    private int maxCount = Integer.MAX_VALUE;

    @Option(name = "-fo", aliases = "--file-only", usage = "only store in a file, do not geotag nor ingest")
    private boolean isFileOnly = false;

    @Option(name = "-kaf", aliases = "--kafka", usage = "send data to Kafka Cluster")
    private boolean storeKafka = false;

    @Option(name = "-ks", aliases = "--kafka-server", usage = "hostname:port used to start the connection with KafkaCluster")
    private String kafkaServer = "";

    @Option(name = "-kid", aliases = "--kafka-consumer-id", usage = "Id of the consumer for Kafka")
    private String kafkaId = "";

    @Option(name = "-tpzs", aliases = "--topic-zika-streaming", usage = "Topic name on Kafka for Zika Streaming")
    private String topicZikaStream = "TwitterZikaStreaming";

    @Option(name = "-tpus", aliases = "--topic-user-stream", usage = "Topic name on Kafka for Users Stream")
    private String topicUserStream = "TwitterUserStream";

    @Option(name = "-tpht", aliases = "--topic-hist-users", usage = "Topic name on Kafka for Historical Users Timeline")
    private String topicHistUsers = "TwitterHistUsersTimeline";

    @Option(name = "-kcf", aliases = "--kafka-config-filename", usage = "file with configuration for Kafka Consumer and Producer")
    private String configFilename = "kafka/kafka.conf";

    @Option(name = "-axs", aliases = "--asterix-server", usage = "server:port for AsterixDB requests")
    private String axServer = "http://kiwi.ics.uci.edu:19002";

    @Option(name = "-dv", aliases = "--dataverse-zika-twitter", usage = "Dataverse name for zika related tweets")
    private String dataverse = "twitter_zika";

    @Option(name = "-uds", aliases = "--users-dataset", usage = "Dataset name for zika related tweets from specific users ")
    private String usersDataset = "ds_users_tweet";

    @Option(name = "-zds", aliases = "--zika-dataset", usage = "Dataset name for streaming zika related tweets")
    private String zikaStreamDataset = "ds_zika_streaming";

    public String getConsumerKey() {
        return consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public String getToken() {
        return token;
    }

    public String getTokenSecret() {
        return tokenSecret;
    }

    public Location[] getTrackLocation() {
        return trackLocation;
    }

    public String[] getTrackTerms() {
        return trackTerms;
    }

    public String[] getTrackUsers() {
        return trackUsers;
    }

    public String getAdapterUrl() {
        return adapterUrl;
    }

    public int getPort() {
        return port;
    }

    public int getWaitMillSecPerRecord() {
        return waitMillSecPerRecord;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public String getKafkaServer() { return kafkaServer; }

    public String getKafkaId() { return kafkaId; }

    public boolean isFileOnly() {
        return isFileOnly;
    }

    public boolean isStoreKafka() {
        return storeKafka;
    }

    public enum Source {
        Zika, User, HistUser
    }
    public String getTopic(Source source){
        switch (source){
            case Zika: return topicZikaStream;
            case User: return topicUserStream;
            case HistUser: return topicHistUsers;
            default: return null;
        }
    }

    public String getDataset(Source source){
        switch (source){
            case Zika: return zikaStreamDataset;
            case User:
            case HistUser: return usersDataset;
            default: return null;
        }
    }

    public String  getConfigFilename() { return configFilename; }

    public String getAxServer() { return axServer; }

    public String getDataverse() { return dataverse; }

    public static class TermArrayOptionHandler extends OptionHandler<String[]> {

        public TermArrayOptionHandler(CmdLineParser parser, OptionDef option,
                                      Setter<? super String[]> setter) {
            super(parser, option, setter);
        }

        @Override
        public int parseArguments(Parameters params) throws CmdLineException {
            int counter = 0;
            ArrayList<String> terms = new ArrayList<String>();
            while (true) {
                String param;
                try {
                    param = params.getParameter(counter);
                } catch (CmdLineException ex) {
                    ex.printStackTrace();
                    System.out.println("track term exception");
                    break;
                }
                if (param.startsWith("-")) {
                    break;
                }
                for (String str : param.split(",")) {
                    if (str.trim().length() > 0) {
                        terms.add(str.trim());
                    }
                }
                counter++;
            }
            Setter s = this.setter;
            for (String term : terms)
                s.addValue(term);
            return counter;

        }

        @Override
        public String getDefaultMetaVariable() {
            return "String[]";
        }
    }

    public static class LocationListOptionHandler extends OptionHandler<Location[]> {

        public LocationListOptionHandler(CmdLineParser parser, OptionDef option,
                                         Setter<? super Location[]> setter) {
            super(parser, option, setter);
        }

        @Override
        public int parseArguments(Parameters params) throws CmdLineException {
            int counter = 0;
            ArrayList<Location> locations = new ArrayList<Location>();
            while (true) {
                String param;
                try {
                    param = params.getParameter(counter);
                } catch (CmdLineException ex) {
                    break;
                }
                if (param.startsWith("-") && param.length() > 1 && !Character.isDigit(param.charAt(1))) {
                    break;
                }

                String[] points = param.split(",");
                if (points.length % 4 != 0) {
                    throw new CmdLineException("The number of point for one rectangular should be four");
                }
                for (int i = 0; i < points.length; i += 4) {
                    locations.add(new Location(
                            new Location.Coordinate(Double.parseDouble(points[i].trim()),
                                    Double.parseDouble(points[i + 1].trim())),
                            new Location.Coordinate(Double.parseDouble(points[i + 2].trim()),
                                    Double.parseDouble(points[i + 3].trim()))));
                }
                counter++;
            }//while true

            Setter s = this.setter;
            for (Location loc : locations) {
                s.addValue(loc);
            }
            return counter;
        }

        @Override
        public String getDefaultMetaVariable() {
            return "Location";
        }
    }
}
