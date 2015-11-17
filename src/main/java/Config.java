import java.util.ArrayList;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

import com.twitter.hbc.core.endpoint.Location;

public class Config {

    @Option(name = "-ck", aliases = "--consumer-key", usage = "ConsumerKey for Twitter OAuth")
    private String consumerKey = "";

    @Option(name = "-cs", aliases = "--consumer-secret", usage = "Consumer Secret for Twitter OAuth")
    private String consumerSecret = "";

    @Option(name = "-tk", aliases = "--token", usage = "Token for Twitter OAuth")
    private String token = "";

    @Option(name = "-ts", aliases = "--token-secret", usage = "Token secret for Twitter OAuth")
    private String tokenSecret = "";

    @Option(name = "-tr", aliases = "--tracker", handler = TermArrayOptionHandler.class, usage = "Tracked terms, separated by comma.")
    private String[] trackTerms = new String[] {};

    @Option(name = "-loc", aliases = "--location", handler = LocationListOptionHandler.class, usage = "location rectangular, southwest.lon, southwest.lat, northeast.lon, northeast.lat")
    private Location[] trackLocation = new Location[] {};

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
            }
            Setter s = this.setter;
            s.addValue(terms.toArray(new String[terms.size()]));
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
            s.addValue(locations.toArray(new Location[locations.size()]));
            return counter;
        }

        @Override
        public String getDefaultMetaVariable() {
            return "Location";
        }
    }
}
