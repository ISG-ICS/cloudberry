import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.Location;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

public class FilterStreamDriver {

    public static void run(Config config)
            throws InterruptedException {
        BlockingQueue<String> queue = new LinkedBlockingQueue<String>(10000);
        StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
        // add some track terms

        if (config.getTrackTerms().length != 0) {
            System.out.print("set track terms are: ");
            for (String term : config.getTrackTerms()) {
                System.out.print(term);
                System.out.print(" ");
            }
            System.out.println();
            endpoint.trackTerms(Lists.newArrayList(config.getTrackTerms()));
        }
        if (config.getTrackLocation().length != 0) {

            System.out.print("set track locations are:");
            for (Location location : config.getTrackLocation()) {
                System.out.print(location);
                System.out.print(" ");
            }
            System.out.println();

            endpoint.locations(Lists.<Location>newArrayList(config.getTrackLocation()));
        }

        Authentication auth = new OAuth1(config.getConsumerKey(), config.getConsumerSecret(), config.getToken(),
                config.getTokenSecret());

        // Create a new BasicClient. By default gzip is enabled.
        Client client = new ClientBuilder()
                .hosts(Constants.STREAM_HOST)
                .endpoint(endpoint)
                .authentication(auth)
                .processor(new StringDelimitedProcessor(queue))
                .build();

        // Establish a connection
        try {
            client.connect();

            // Do whatever needs to be done with messages
            String msg = queue.take();
            System.out.println(msg);
        } finally {
            client.stop();
        }

    }

    public static void main(String[] args) {
        try {
            Config config = new Config();
            CmdLineParser parser = new CmdLineParser(config);
            try {
                parser.parseArgument(args);

                if (config.getTrackTerms().length == 0 && config.getTrackLocation().length == 0) {
                    throw new CmdLineException("Should provide at list on tracking word, or one location boundary");
                }
            } catch (CmdLineException e) {
                System.err.print(e);
                System.err.println();
                parser.printUsage(System.err);
                System.err.println();
            }
            FilterStreamDriver.run(config);
        } catch (InterruptedException e) {
            System.out.println(e);
        }
    }
}
