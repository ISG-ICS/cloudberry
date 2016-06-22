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
    @Option(required = true, name = "-ck", aliases = "--consumer-key", usage = "ConsumerKey for Twitter OAuth")
    private String consumerKey = null;

    @Option(required = true, name = "-cs", aliases = "--consumer-secret", usage = "Consumer Secret for Twitter OAuth")
    private String consumerSecret = null;

    @Option(required = true, name = "-tk", aliases = "--token", usage = "Token for Twitter OAuth")
    private String token = null;

    @Option(required = true, name = "-ts", aliases = "--token-secret", usage = "Token secret for Twitter OAuth")
    private String tokenSecret = null;

    @Option(name = "-tr", aliases = "--tracker", handler = TermArrayOptionHandler.class, usage = "Tracked terms, separated by comma.")
    private String[] trackTerms = new String[]{};

    @Option(name = "-tu", aliases = "--trackuser", handler = TermArrayOptionHandler.class, usage = "Tracked public users, separated by comma.")
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

    @Option(name = "-fo", aliases = "--fileonly", usage = "only store in a file, do not geotag nor ingest")
    private boolean isFileOnly = false;


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

    public boolean getIsFileOnly() {
        return isFileOnly;
    }

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
