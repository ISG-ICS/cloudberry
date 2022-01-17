package edu.uci.ics.cloudberry.datatools.asterixdb;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

@WebSocket(maxTextMessageSize = 64 * 1024) // For one tweet, we believe it's safe to set max text message size as 64KB
public class TwitterIngestionProxySocketClient {

    private Session session;

    @OnWebSocketClose
    public void onClose(int statusCode, String reason)
    {
        System.err.println("Socket Closed: [" + statusCode + "] " + reason);
    }

    @OnWebSocketConnect
    public void onConnect(Session session)
    {
        System.err.println("Socket Connected: " + session);
        this.session = session;
    }

    @OnWebSocketMessage
    public void onMessage(String message)
    {
        AsterixDBIngestionDriver.tagAndIngestOneTweet(message);
    }

    @OnWebSocketError
    public void onError(Throwable cause)
    {
        System.err.println("Socket error:");
        cause.printStackTrace(System.err);
    }
}
