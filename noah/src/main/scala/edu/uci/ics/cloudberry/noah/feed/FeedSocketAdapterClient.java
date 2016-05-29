package edu.uci.ics.cloudberry.noah.feed;

import java.io.IOException;
import java.net.Socket;

public abstract class FeedSocketAdapterClient {
    protected String adapterUrl;
    protected int port;
    protected int waitMillSecond;
    protected int batchSize;
    protected int maxCount;

    protected Socket socket;

    public abstract void initialize() throws IOException;

    public abstract void finalize();


}
