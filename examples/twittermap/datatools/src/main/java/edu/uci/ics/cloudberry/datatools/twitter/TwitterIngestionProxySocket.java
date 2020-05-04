package edu.uci.ics.cloudberry.datatools.twitter;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

@WebSocket
public class TwitterIngestionProxySocket {

    int sessionId;

    @OnWebSocketConnect
    public void onWebSocketConnect(Session sess)
    {
        System.out.println("Socket Connected: " + sess);
        sessionId = TwitterIngestionServer.subscribe(sess.getRemote());
    }

    @OnWebSocketMessage
    public void onWebSocketText(String message)
    {
        System.out.println("Received TEXT message: " + message);
    }

    @OnWebSocketClose
    public void onWebSocketClose(int statusCode, String reason)
    {
        System.out.println("Socket Closed: [" + statusCode + "] " + reason);
        TwitterIngestionServer.unsubscribe(sessionId);
    }

    @OnWebSocketError
    public void onWebSocketError(Throwable cause)
    {
        cause.printStackTrace(System.err);
        TwitterIngestionServer.unsubscribe(sessionId);
    }
}
