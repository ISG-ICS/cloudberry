package edu.uci.ics.cloudberry.noah.feed;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class FeedSocketAdapterClient {
    private OutputStream out = null;
    private int recordCount = 0;

    protected String adapterUrl;
    protected int port;
    protected int waitMillSecond;
    protected int batchSize;
    protected int maxCount;

    protected Socket socket;
    protected int socketNumber;

    public FeedSocketAdapterClient(String adapterUrl, int port, int batchSize,
                                   int waitMillSecPerRecord, int maxCount, int socketNumber) {
        this.adapterUrl = adapterUrl;
        this.port = port;
        this.maxCount = maxCount;
        this.waitMillSecond = waitMillSecPerRecord;
        this.batchSize = batchSize;
        this.socketNumber = socketNumber;
    }

    public void initialize() throws IOException {
        socket = new Socket(adapterUrl, port);
        out = socket.getOutputStream();
    }

    public void finalize() {
        try {
            out.close();
            socket.close();
            System.err.println("Socket " + socketNumber + " - # of total ingested records: " + recordCount);
        } catch (IOException e) {
            System.err.println("Problem in closing socket against host " + adapterUrl + " on the port " + port);
            e.printStackTrace();
        }
    }

    public void ingest(String record) throws IOException{
        recordCount++;
        if (recordCount % 10000 == 0) {
            System.err.println("Socket " + socketNumber + " - # of ingested records: " + recordCount);
        }
        byte[] b = record.replaceAll("\\s+", " ").getBytes();
        try {
            out.write(b);
            if (waitMillSecond >= 1 && recordCount % batchSize == 0) {
                Thread.currentThread().sleep(waitMillSecond);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
