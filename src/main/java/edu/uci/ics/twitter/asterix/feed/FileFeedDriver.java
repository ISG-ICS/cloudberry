package edu.uci.ics.twitter.asterix.feed;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class FileFeedDriver {

    @Option(required = true, name = "-u", aliases = "--url", usage = "url of the feed adapter")
    private String adapterUrl;

    @Option(required = true, name = "-p", aliases = "--port", usage = "port of the feed socket")
    private int port;

    @Option(name = "-w", aliases = "--wait", usage = "waiting seconds per record, default 0")
    private int waitMillSecPerRecord = 0;

    @Option(name = "-c", aliases = "--count", usage = "maximum number to feed, default unlimited")
    private int maxCount = Integer.MAX_VALUE;

    @Argument
    private String sourceFilePath = null;

    public static void main(String[] args) {
        new FileFeedDriver().doMain(args);
    }

    private void doMain(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);

        try {
            parser.parseArgument(args);
            if (sourceFilePath == null) {
                throw new CmdLineException("No source file is given");
            }

            FileFeedSocketAdapterClient client = new FileFeedSocketAdapterClient(adapterUrl, port, sourceFilePath,
                    waitMillSecPerRecord, maxCount);
            try {
                client.initialize();
                client.ingest();
            } finally {
                client.finalize();
            }
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("usage [option] file");
            parser.printUsage(System.err);
        }
    }
}
