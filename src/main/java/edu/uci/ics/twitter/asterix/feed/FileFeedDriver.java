package edu.uci.ics.twitter.asterix.feed;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;

public class FileFeedDriver {

    @Option(required = true,
            name = "-u",
            aliases = "--url",
            usage = "url of the feed adapter")
    private String adapterUrl;

    @Option(required = true,
            name = "-p",
            aliases = "--port",
            usage = "port of the feed socket")
    private int port;

    @Option(name = "-w",
            aliases = "--wait",
            usage = "waiting milliseconds per record, default 0")
    private int waitMillSecPerRecord = 0;

    @Option(name = "-b",
            aliases = "--batch",
            usage = "batchsize per waiting periods, default 1")
    private int batchSize = 1;

    @Option(name = "-c",
            aliases = "--count",
            usage = "maximum number to feed, default unlimited")
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
            if (sourceFilePath == null || sourceFilePath.length() == 0) {
                System.err.println("Read from stdin");
            }

            InputStreamReader reader;
            if (sourceFilePath == null) {
                reader = new InputStreamReader(System.in);
            } else {
                reader = new FileReader(sourceFilePath);
            }

            FileFeedSocketAdapterClient client = new FileFeedSocketAdapterClient(adapterUrl, port, reader,
                    batchSize, waitMillSecPerRecord, maxCount);
            try {
                client.initialize();
                client.ingest();
            } finally {
                client.finalize();
            }
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("usage [option] filePath, write filePath as - to read from stdin");
            parser.printUsage(System.err);
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
            System.err.println("usage [option] filePath, write filePath as - to read from stdin");
            parser.printUsage(System.err);
        }
    }
}
