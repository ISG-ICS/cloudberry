package edu.uci.ics.cloudberry.noah.feed;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.util.Random;

public class FileFeedDriver {

    @Option(required = true,
            name = "-u",
            aliases = "--url",
            usage = "URL(s) or IP(s) of the feed adapter - put a comma between two URLs or IPs to separate them")
    private String adapterUrl;

    @Option(required = true,
            name = "-p",
            aliases = "--port",
            usage = "port of the feed socket(s)")
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
        FeedSocketAdapterClient[] clients = null;
        String[] adapterUrls = null;
        int ingestionCount = 0;
        FeedSocketAdapterClient client = null;
        BufferedReader br = null;
        Random generator = null;
        boolean multipleClients = false;
        long startTime = System.currentTimeMillis();
        long elapsedTime = 0;
        long elapsedMinuteTime = 0;
        double ingestionSpeed = 0;

        try {
            parser.parseArgument(args);
            if (sourceFilePath == null || sourceFilePath.length() == 0) {
                System.err.println("Read from stdin");
            }

            // Multiple hosts? Then split them
            if (adapterUrl.contains(",")) {
                adapterUrls = adapterUrl.split(",");
                generator = new Random();
                multipleClients = true;
            } else {
                adapterUrls = new String[]{adapterUrl};
            }
            clients = new FeedSocketAdapterClient[adapterUrls.length];

            for (int i = 0; i < clients.length; i++) {
                clients[i] = new FeedSocketAdapterClient(adapterUrls[i], port,
                        batchSize, waitMillSecPerRecord, maxCount, i);
                clients[i].initialize();
            }

            InputStreamReader reader;
            if (sourceFilePath == null) {
                reader = new InputStreamReader(System.in);
            } else {
                reader = new FileReader(sourceFilePath);
            }

            br = new BufferedReader(reader);
            String nextRecord;
            int socketIndex = 0;
            while ((nextRecord = br.readLine()) != null) {
                socketIndex = multipleClients ? generator.nextInt(clients.length) : 0;
                clients[socketIndex].ingest(nextRecord);
                ingestionCount++;
                if (ingestionCount % 100000 == 0) {
                    elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
                    elapsedMinuteTime = elapsedTime / 60;
                    ingestionSpeed = (double) ingestionCount / elapsedTime;
                    System.err.println(">>> # of ingested records: " + ingestionCount + " Elapsed (s) : " +
                            elapsedTime + " (m) : " + elapsedMinuteTime + " record/sec : " + ingestionSpeed);
                }
            }
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("usage [option] filePath, write filePath as - to read from stdin");
            parser.printUsage(System.err);
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
            System.err.println("usage [option] filePath, write filePath as - to read from stdin");
            parser.printUsage(System.err);
        } catch(IOException e) {
            System.err.println(e.getMessage());
        } finally {
            try {
                br.close();
                elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
                elapsedMinuteTime = elapsedTime / 60;
                ingestionSpeed = (double) ingestionCount / elapsedTime;
                System.err.println(">>> Total # of ingested records: " + ingestionCount + " Elapsed (s) : " +
                        elapsedTime + " (m) : " + elapsedMinuteTime + " record/sec : " + ingestionSpeed);
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < clients.length; i++) {
                clients[i].finalize();
            }
        }
    }
}