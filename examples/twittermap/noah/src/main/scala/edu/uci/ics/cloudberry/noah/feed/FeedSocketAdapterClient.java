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

    public FeedSocketAdapterClient(String adapterUrl, int port, int batchSize,
                                         int waitMillSecPerRecord, int maxCount) {
        this.adapterUrl = adapterUrl;
        this.port = port;
        this.maxCount = maxCount;
        this.waitMillSecond = waitMillSecPerRecord;
        this.batchSize = batchSize;
    }

    public void initialize() throws IOException {
        socket = new Socket(adapterUrl, port);
        out = socket.getOutputStream();
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

    public void ingest(String record) throws IOException{
        recordCount++;
        if (recordCount % 5000 == 0) {
            System.err.println("send record: " + recordCount);
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
