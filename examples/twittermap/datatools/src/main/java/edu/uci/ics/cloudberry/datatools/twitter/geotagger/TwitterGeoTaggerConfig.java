package edu.uci.ics.cloudberry.datatools.twitter.geotagger;

import org.apache.commons.cli.*;

public class TwitterGeoTaggerConfig {
    String stateJsonFile = null;
    String countyJsonFile = null;
    String cityJsonFile = null;
    int threadNumber = 0;
    int bufferSize = 0;
    boolean debug = false;

    public String getStateJsonFile() {
        return stateJsonFile;
    }

    public String getCountyJsonFile() {
        return countyJsonFile;
    }

    public String getCityJsonFile() {
        return cityJsonFile;
    }

    public int getThreadNumber() {
        return threadNumber;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public boolean getDebug() {
        return debug;
    }

    /**
     * create a TwitterGeoTaggerConfig object from parsing CLI arguments
     *
     * @param args
     * @return TwitterGeoTaggerConfig object, or null if any exception.
     */
    public static TwitterGeoTaggerConfig createFromCLIArgs(String[] args) {
        // define cli arguments options, consistent with members of this class
        final Options options = new Options();
        final Option consumerKeyOpt = Option.builder("state")
                .longOpt("state-json-file")
                .desc("State Json file for geographical information of states in the U.S.")
                .type(String.class)
                .required()
                .hasArg()
                .build();
        final Option consumerSecretOpt = Option.builder("county")
                .longOpt("county-json-file")
                .desc("County Json file for geographical information of counties in the U.S.")
                .type(String.class)
                .required()
                .hasArg()
                .build();
        final Option tokenOpt = Option.builder("city")
                .longOpt("city-json-file")
                .desc("City Json file for geographical information of cities in the U.S.")
                .type(String.class)
                .required()
                .hasArg()
                .build();
        final Option threadOpt = Option.builder("thread")
                .longOpt("thread-number")
                .desc("Number of threads to parallelize this TwitterGeoTagger process. (Default: 1)")
                .type(Integer.class)
                .required(false)
                .hasArg()
                .build();
        final Option bufferOpt = Option.builder("buffer")
                .longOpt("buffer-size")
                .desc("Size of buffer to synchronize all threads - finish geo-tagging all tweets in the buffer and then continue. (Default: 100)")
                .type(Integer.class)
                .required(false)
                .hasArg()
                .build();
        final Option debugOpt = Option.builder("debug")
                .longOpt("enable-debug-mode")
                .desc("Enable debug mode. (Default: false)")
                .type(Boolean.class)
                .required(false)
                .hasArg(false)
                .build();
        options.addOption(consumerKeyOpt);
        options.addOption(consumerSecretOpt);
        options.addOption(tokenOpt);
        options.addOption(threadOpt);
        options.addOption(bufferOpt);
        options.addOption(debugOpt);

        // parse args to generate a TwitterIngestionConfig object
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            TwitterGeoTaggerConfig config = new TwitterGeoTaggerConfig();
            config.stateJsonFile = cmd.getOptionValue("state-json-file");
            config.countyJsonFile = cmd.getOptionValue("county-json-file");
            config.cityJsonFile = cmd.getOptionValue("city-json-file");
            config.threadNumber = Integer.parseInt(cmd.getOptionValue("thread-number", "1"));
            config.bufferSize = Integer.parseInt(cmd.getOptionValue("buffer-size", "100"));
            config.debug = cmd.hasOption("debug");
            return config;

        } catch (ParseException e) {
            e.printStackTrace();
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(100, "TwitterIngestion", "",
                    options ,"Example: \n" +
                            "java -cp datatools-assembly-1.0-SNAPSHOT.jar \\ \n" +
                            "edu.uci.ics.cloudberry.datatools.twitter.TwitterGeoTagger \\ \n" +
                            "-state web/public/data/state.json \\ \n" +
                            "-county web/public/data/county.json \\ \n" +
                            "-city web/public/data/city.json \\ \n" +
                            "-thread 2 \\ \n" +
                            "-buffer 100 \\ \n");
        }

        return null;
    }
}
