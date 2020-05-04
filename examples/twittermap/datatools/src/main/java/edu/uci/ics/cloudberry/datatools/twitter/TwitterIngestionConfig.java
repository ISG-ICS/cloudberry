package edu.uci.ics.cloudberry.datatools.twitter;

import com.twitter.hbc.core.endpoint.Location;
import org.apache.commons.cli.*;

public class TwitterIngestionConfig {

    // authentication
    private String consumerKey = null;
    private String consumerSecret = null;
    private String token = null;
    private String tokenSecret = null;

    // filters
    private String[] trackKeywords = null;
    private Location[] trackLocations = null;

    // output
    private String filePrefix = null;
    private String outputPath = null;

    // proxy
    private int proxyPort;

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

    public String[] getTrackKeywords() {
        return trackKeywords;
    }

    public Location[] getTrackLocations() {
        return trackLocations;
    }

    public String getFilePrefix() {
        return filePrefix;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    /**
     * create a TwitterIngestionConfig object from parsing CLI arguments
     *
     * @param args
     * @return TwitterIngestionConfig object, or null if any exception.
     */
    public static TwitterIngestionConfig createFromCLIArgs(String[] args) {
        // define cli arguments options, consistent with members of this class
        final Options options = new Options();
        final Option consumerKeyOpt = Option.builder("ck")
                .longOpt("consumer-key")
                .desc("Consumer Key for Twitter OAuth")
                .type(String.class)
                .required()
                .hasArg()
                .build();
        final Option consumerSecretOpt = Option.builder("cs")
                .longOpt("consumer-secret")
                .desc("Consumer Secret for Twitter OAuth")
                .type(String.class)
                .required()
                .hasArg()
                .build();
        final Option tokenOpt = Option.builder("tk")
                .longOpt("token")
                .desc("Token for Twitter OAuth")
                .type(String.class)
                .required()
                .hasArg()
                .build();
        final Option tokenSecretOpt = Option.builder("ts")
                .longOpt("token-secret")
                .desc("Token secret for Twitter OAuth")
                .type(String.class)
                .required()
                .hasArg()
                .build();
        final Option trackOpt = Option.builder("tr")
                .longOpt("track")
                .desc("Keywords to track. Phrases of keywords are specified by a comma-separated list. \n" +
                        "See https://developer.twitter.com/en/docs/tweets/filter-realtime/guides/basic-stream-parameters for more information.")
                .type(String.class)
                .required(false)
                .numberOfArgs(400) // maximum 400 keywords
                .valueSeparator(',')
                .build();
        final Option locationOpt = Option.builder()
                .longOpt("locations")
                .desc("Specifies a set of bounding boxes to track. Each bounding box must be in format: southwest.longitude, southwest.latitude, northeast.longitude, northeast.latitude. \n" +
                        "Note: keywords filter (--track) and locations filter (--locations) are in OR logical relationship.")
                .type(Double.class)
                .required(false)
                .numberOfArgs(100) // 4 values for the bounding box
                .valueSeparator(',')
                .build();
        final Option filePrefixOpt = Option.builder("fp")
                .longOpt("file-prefix")
                .desc("Prefix for output gzip file names. (Default: Tweet)")
                .type(String.class)
                .required(false)
                .hasArg()
                .build();
        final Option outputPathOpt = Option.builder("op")
                .longOpt("output-path")
                .desc("Output path for output gzip files. (Default: ./)")
                .required(false)
                .hasArg()
                .build();
        final Option proxyPortOpt = Option.builder("pp")
                .longOpt("proxy-port")
                .desc("Port to which the proxy server will listen to, " +
                        "the proxy server outputs real time ingested tweets to any connected websocket client. " +
                        "(Default: 92617) Set -1 to disable this proxy server.")
                .type(Integer.class)
                .required(false)
                .hasArg()
                .build();
        options.addOption(consumerKeyOpt);
        options.addOption(consumerSecretOpt);
        options.addOption(tokenOpt);
        options.addOption(tokenSecretOpt);
        options.addOption(trackOpt);
        options.addOption(locationOpt);
        options.addOption(filePrefixOpt);
        options.addOption(outputPathOpt);
        options.addOption(proxyPortOpt);

        // parse args to generate a TwitterIngestionConfig object
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            TwitterIngestionConfig config = new TwitterIngestionConfig();
            // authentication
            config.consumerKey = cmd.getOptionValue("consumer-key");
            config.consumerSecret = cmd.getOptionValue("consumer-secret");
            config.token = cmd.getOptionValue("token");
            config.tokenSecret = cmd.getOptionValue("token-secret");
            // filters
            if (cmd.hasOption("track")) {
                config.trackKeywords = cmd.getOptionValues("track");
            }
            if (cmd.hasOption("locations")) {
                String[] lnglats = cmd.getOptionValues("locations");
                if (lnglats.length % 4 != 0) {
                    throw new ParseException("The number of values for each bounding box should be four!");
                }
                config.trackLocations = new Location[lnglats.length / 4];
                for (int i = 0; i < lnglats.length; i += 4) {
                    config.trackLocations[i / 4] = new Location(
                            new Location.Coordinate(Double.parseDouble(lnglats[i].trim()),
                                    Double.parseDouble(lnglats[i + 1].trim())),
                            new Location.Coordinate(Double.parseDouble(lnglats[i + 2].trim()),
                                    Double.parseDouble(lnglats[i + 3].trim())));
                }
            }
            if (config.getTrackKeywords() == null && config.getTrackLocations() == null) {
                throw new ParseException("Please provide at least one tracking keyword, or one location bounding box!");
            }
            // output
            config.filePrefix = cmd.getOptionValue("file-prefix", "Tweet");
            config.outputPath = cmd.getOptionValue("output-path", "./");
            // proxy
            config.proxyPort = Integer.parseInt(cmd.getOptionValue("proxy-port", "9088"));

            return config;

        } catch (ParseException e) {
            e.printStackTrace();
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(100, "TwitterIngestion", "",
                    options ,"Example: \n" +
                            "java -cp datatools-assembly-1.0-SNAPSHOT.jar \\ \n" +
                            "edu.uci.ics.cloudberry.datatools.twitter.TwitterIngestionServer \\ \n" +
                            "-ck your_own_consumer_key \\ \n" +
                            "-cs your_own_consumer_secret \\ \n" +
                            "-tk your_own_token \\ \n" +
                            "-ts your_own_token_secret \\ \n" +
                            "-tr=hurricane,tornado,storm \\ \n" +
                            "--locations=-170,30,-160,40,100,20,120,40 \\ \n" +
                            "-fp Twitter_hurricane \\ \n" +
                            "-op ./ \n" +
                            "To get your own authentication keys, " +
                            "visit: https://developer.twitter.com/en/docs/basics/authentication/oauth-1-0a");
        }

        return null;
    }
}
