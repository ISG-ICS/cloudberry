package edu.uci.ics.cloudberry.datatools.asterixdb;

import org.apache.commons.cli.*;

public class AsterixDBIngestionConfig {

    // source
    private String fromProxy = null;

    // geo-json files, only required if fromProxy is not null
    String stateJsonFile = null;
    String countyJsonFile = null;
    String cityJsonFile = null;

    // target AsterixDB
    private String host = null;
    private int port;

    public String getFromProxy() {
        return fromProxy;
    }

    public String getStateJsonFile() {
        return stateJsonFile;
    }

    public String getCountyJsonFile() {
        return countyJsonFile;
    }

    public String getCityJsonFile() {
        return cityJsonFile;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public static AsterixDBIngestionConfig createFromCLIArgs(String[] args) {
        // define cli arguments options, consistent with members of this class
        final Options options = new Options();
        final Option fromProxyOpt = Option.builder("fp")
                .longOpt("from-proxy")
                .desc("URL of the source twitter ingestion proxy, if not given, it will ingest from stdin.")
                .type(String.class)
                .required(false)
                .hasArg()
                .build();
        final Option stateOpt = Option.builder("state")
                .longOpt("state-json-file")
                .desc("State Json file for geographical information of states in the U.S.")
                .type(String.class)
                .required(false)
                .hasArg()
                .build();
        final Option countyOpt = Option.builder("county")
                .longOpt("county-json-file")
                .desc("County Json file for geographical information of counties in the U.S.")
                .type(String.class)
                .required(false)
                .hasArg()
                .build();
        final Option cityOpt = Option.builder("city")
                .longOpt("city-json-file")
                .desc("City Json file for geographical information of cities in the U.S.")
                .type(String.class)
                .required(false)
                .hasArg()
                .build();
        final Option hostOpt = Option.builder("h")
                .longOpt("host")
                .desc("Domain name or IP address of the target AsterixDB host.")
                .type(String.class)
                .required()
                .hasArg()
                .build();
        final Option portOpt = Option.builder("p")
                .longOpt("port")
                .desc("Port of target AsterixDB socket feed.")
                .type(Integer.class)
                .required()
                .hasArg()
                .build();
        options.addOption(fromProxyOpt);
        options.addOption(stateOpt);
        options.addOption(countyOpt);
        options.addOption(cityOpt);
        options.addOption(hostOpt);
        options.addOption(portOpt);

        // parse args to generate a TwitterIngestionConfig object
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            AsterixDBIngestionConfig config = new AsterixDBIngestionConfig();

            // source
            if (cmd.hasOption("from-proxy")) {
                config.fromProxy = cmd.getOptionValue("from-proxy");
                if (!cmd.hasOption("state-json-file") || !cmd.hasOption("county-json-file") || !cmd.hasOption("city-json-file")) {
                    throw new ParseException("If from-proxy is given, state, county, city json files arguments are required.");
                }
                config.stateJsonFile = cmd.getOptionValue("state-json-file");
                config.countyJsonFile = cmd.getOptionValue("county-json-file");
                config.cityJsonFile = cmd.getOptionValue("city-json-file");
            }

            // target AsterixDB
            config.host = cmd.getOptionValue("host");
            config.port = Integer.parseInt(cmd.getOptionValue("port"));

            return config;

        } catch (ParseException e) {
            e.printStackTrace();
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(100, "AsterixDBIngestionDriver", "",
                    options ,"Example: \n" +
                            "java -cp datatools-assembly-1.0-SNAPSHOT.jar \\ \n" +
                            "edu.uci.ics.cloudberry.datatools.asterixdb.AsterixDBIngestionDriver \\ \n" +
                            "-fp ws://localhost:9088/proxy \\ \n" +
                            "-state web/public/data/state.json \\ \n" +
                            "-county web/public/data/county.json \\ \n" +
                            "-city web/public/data/city.json \\ \n" +
                            "-h localhost \\ \n" +
                            "-p 10001 \\ \n");
        }

        return null;
    }
}
