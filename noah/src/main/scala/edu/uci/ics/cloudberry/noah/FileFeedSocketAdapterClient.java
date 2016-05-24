package edu.uci.ics.cloudberry.noah;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class FileFeedSocketAdapterClient {

    private final int waitMillSecond;
    private String adapterUrl;
    private int port;
    private Socket socket;
    private InputStreamReader inputReader;
    private int batchSize;
    private int maxCount;
    private OutputStream out = null;

    public FileFeedSocketAdapterClient(String adapterUrl, int port, InputStreamReader reader, int batchSize,
                                       int waitMillSecPerRecord, int maxCount) {
        this.adapterUrl = adapterUrl;
        this.port = port;
        this.inputReader = reader;
        this.maxCount = maxCount;
        this.waitMillSecond = waitMillSecPerRecord;
        this.batchSize = batchSize;
    }

    public void initialize() {
        try {
            socket = new Socket(adapterUrl, port);
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
            socket.close();
        } catch (IOException e) {
            System.err.println("Problem in closing socket against host " + adapterUrl + " on the port " + port);
            e.printStackTrace();
        }
    }

    public void ingest() {
        int recordCount = 0;
        BufferedReader br = null;
        try {
            out = socket.getOutputStream();
            br = new BufferedReader(inputReader);
            String nextRecord;
            byte[] b;
            byte[] newLineBytes = "\n".getBytes();

            while ((nextRecord = br.readLine()) != null) {
                System.out.println(nextRecord);
                b = nextRecord.replaceAll("\\s+", " ").getBytes();
                if (waitMillSecond >= 1 && recordCount % batchSize == 0) {
                    Thread.currentThread().sleep(waitMillSecond);
                }
                out.write(b);
                out.write(newLineBytes);
                recordCount++;
                if (recordCount % 100000 == 0) {
                    System.err.println("send " + recordCount);
                }
                if (recordCount == maxCount) {
                    break;
                }
            }
            System.err.println("send " + recordCount);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}