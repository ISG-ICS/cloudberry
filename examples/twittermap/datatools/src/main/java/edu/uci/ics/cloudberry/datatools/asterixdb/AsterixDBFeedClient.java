package edu.uci.ics.cloudberry.datatools.asterixdb;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class AsterixDBFeedClient {
    private OutputStream out = null;
    private int counter = 0;
    private long startTime = 0;

    protected String host;
    protected int port;

    protected Socket socket;

    public AsterixDBFeedClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void initialize() throws IOException {
        socket = new Socket(host, port);
        out = socket.getOutputStream();
    }

    public void finalize() {
        try {
            out.close();
            socket.close();
            System.err.println("Socket feed to AsterixDB at " + host + ":" + port + " - Total # of ingested records: " + counter);
        } catch (IOException e) {
            System.err.println("Problem in closing socket against host " + host + " on the port " + port);
            e.printStackTrace();
        }
    }

    public void ingest(String record) throws IOException {
        // initialize timer when ingest the first record
        if (counter == 0) startTime = System.currentTimeMillis();

        counter ++;
        // output statistics for every 10,000 records ingested
        if (counter % 10000 == 0) {
            System.err.println("Socket feed to AsterixDB at " + host + ":" + port + " - # of ingested records: " + counter);
            long endTime = System.currentTimeMillis();
            double rate = counter * 1000.0 / (endTime - startTime);
            System.err.println("Average ingestion rate: " + rate + " records/second");
        }
        byte[] b = record.replaceAll("\\s+", " ").getBytes();
        out.write(b);
    }
}
