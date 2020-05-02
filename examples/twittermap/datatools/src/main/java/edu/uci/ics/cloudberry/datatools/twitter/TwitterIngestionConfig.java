package edu.uci.ics.cloudberry.datatools.twitter;

import com.twitter.hbc.core.endpoint.Location;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

import java.util.ArrayList;

public class TwitterIngestionConfig {
    @Option(name = "-ck", aliases = "--consumer-key", usage = "Consumer Key for Twitter OAuth")
    private String consumerKey = null;

    @Option(name = "-cs", aliases = "--consumer-secret", usage = "Consumer Secret for Twitter OAuth")
    private String consumerSecret = null;

    @Option(name = "-tk", aliases = "--token", usage = "Token for Twitter OAuth")
    private String token = null;

    @Option(name = "-ts", aliases = "--token-secret", usage = "Token secret for Twitter OAuth")
    private String tokenSecret = null;

    @Option(name = "-tr", aliases = "--track", handler = KeywordListOptionHandler.class,
            usage = "Keywords to track. Phrases of keywords are specified by a comma-separated list. " +
                    "See https://developer.twitter.com/en/docs/tweets/filter-realtime/guides/basic-stream-parameters for more information.")
    private String[] trackKeywords = new String[]{};

    @Option(name = "-loc", aliases = "--location", handler = LocationListOptionHandler.class,
            usage = "Specifies one bounding box to track. southwest.longitude, southwest.latitude, northeast.longitude, northeast.latitude. " +
                    "Note: keywords filter (--track) and location filter (--location) are in OR logical relationship.")
    private Location[] trackLocation = new Location[]{};

    @Option(name = "-as", aliases = "--asterixdb-socket", usage = "host:port for AsterixDB feed socket.")
    private String asterixdbSocket = null;

    @Option(name = "-fp", aliases = "--file-prefix", usage = "prefix for ingested twitter data file names.")
    private String filePrefix = "Tweet";

    @Option(name = "-op", aliases = "--output-path", usage = "output path for ingested twitter data files.")
    private String outputPath = "./";

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

    public String[] getTrackKeywords() {
        return trackKeywords;
    }

    public String getAsterixdbSocket() {
        return asterixdbSocket;
    }

    public String getFilePrefix() {
        return filePrefix;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public static class KeywordListOptionHandler extends OptionHandler<String[]> {

        public KeywordListOptionHandler(CmdLineParser parser, OptionDef option,
                                        Setter<? super String[]> setter) {
            super(parser, option, setter);
        }

        @Override
        public int parseArguments(Parameters params) throws CmdLineException {
            int counter = 0;
            ArrayList<String> keywords = new ArrayList<>();
            while (true) {
                String param;
                try {
                    param = params.getParameter(counter);
                } catch (CmdLineException e) {
                    e.printStackTrace();
                    break;
                }
                if (param.startsWith("-")) {
                    break;
                }
                for (String str : param.split(",")) {
                    if (str.trim().length() > 0) {
                        keywords.add(str.trim());
                    }
                }
                counter ++;
            }
            Setter s = this.setter;
            for (String keyword : keywords)
                s.addValue(keyword);
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
            ArrayList<Location> locations = new ArrayList<>();
            while (true) {
                String param;
                try {
                    param = params.getParameter(counter);
                } catch (CmdLineException e) {
                    e.printStackTrace();
                    break;
                }
                if (param.startsWith("-") && param.length() > 1 && !Character.isDigit(param.charAt(1))) {
                    break;
                }

                String[] points = param.split(",");
                if (points.length % 4 != 0) {
                    throw new CmdLineException("The number of values for bounding box should be four");
                }
                for (int i = 0; i < points.length; i += 4) {
                    locations.add(new Location(
                            new Location.Coordinate(Double.parseDouble(points[i].trim()),
                                    Double.parseDouble(points[i + 1].trim())),
                            new Location.Coordinate(Double.parseDouble(points[i + 2].trim()),
                                    Double.parseDouble(points[i + 3].trim()))));
                }
                counter ++;
            }

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
