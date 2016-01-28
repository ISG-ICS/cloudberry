/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package edu.uci.ics.twitter.asterix.feed;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class FileFeedSocketAdapterClient {

    private final int waitMillSecond;
    private String adapterUrl;
    private int port;
    private Socket socket;
    private String sourceFilePath;
    private int batchSize;
    private int maxCount;
    private OutputStream out = null;

    public FileFeedSocketAdapterClient(String adapterUrl, int port, String sourceFilePath, int batchSize,
            int waitMillSecPerRecord, int maxCount) {
        this.adapterUrl = adapterUrl;
        this.port = port;
        this.sourceFilePath = sourceFilePath;
        this.maxCount = maxCount;
        this.waitMillSecond = waitMillSecPerRecord;
        this.batchSize = batchSize;
    }

    public void initialize() {
        try {
            socket = new Socket(adapterUrl, port);
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
            br = new BufferedReader(new FileReader(sourceFilePath));
            String nextRecord;
            byte[] b;
            byte[] newLineBytes = "\n".getBytes();

            while ((nextRecord = br.readLine()) != null) {
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
