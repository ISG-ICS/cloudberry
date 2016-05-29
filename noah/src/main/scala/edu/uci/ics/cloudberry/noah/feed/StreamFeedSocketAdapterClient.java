package edu.uci.ics.cloudberry.noah.feed;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class StreamFeedSocketAdapterClient {

    private final int waitMillSecond;
    private String adapterUrl;
    private int port;
    private Socket socket;
    private int batchSize;
    private int maxCount;
    private OutputStream out = null;
    private int recordCount = 0;

    public StreamFeedSocketAdapterClient(String adapterUrl, int port, int batchSize,
                                       int waitMillSecPerRecord, int maxCount) {
        this.adapterUrl = adapterUrl;
        this.port = port;
        this.maxCount = maxCount;
        this.waitMillSecond = waitMillSecPerRecord;
        this.batchSize = batchSize;
    }

    public void initialize() {
        try {
            socket = new Socket(adapterUrl, port);
            out = socket.getOutputStream();
            System.out.println("adapterUrl: " + this.adapterUrl + ":" + Integer.toString(port));
            System.out.println("maxCount: " + maxCount);
            System.out.println("wait: " + waitMillSecond);
            System.out.println("batchSize: " + batchSize);
        } catch (IOException e) {
            System.err.println("Problem in creating socket against host " + adapterUrl + " on the port " + port);
            e.printStackTrace();
        }
    }

    public void finalize() {
        try {
            out.close();
            socket.close();
        } catch (IOException e) {
            System.err.println("Problem in closing socket against host " + adapterUrl + " on the port " + port);
            e.printStackTrace();
        }
    }

    public void ingest(String record) {
        recordCount++;
        System.out.println("send record: " + recordCount);
        byte[] b = record.replaceAll("\\s+", " ").getBytes();
        try {
            out.write(b);
            if (waitMillSecond >= 1 && recordCount % batchSize == 0) {
                Thread.currentThread().sleep(waitMillSecond);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}