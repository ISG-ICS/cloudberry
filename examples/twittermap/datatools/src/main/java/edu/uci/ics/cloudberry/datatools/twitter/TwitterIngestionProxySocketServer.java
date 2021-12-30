package edu.uci.ics.cloudberry.datatools.twitter;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

@WebSocket(maxTextMessageSize = 64 * 1024) // For one tweet, we believe it's safe to set max text message size as 64KB
public class TwitterIngestionProxySocketServer {

    int sessionId;

    @OnWebSocketConnect
    public void onConnect(Session session)
    {
        System.err.println("Socket Connected: " + session);
        sessionId = TwitterIngestionServer.subscribe(session.getRemote());
    }

    @OnWebSocketMessage
    public void onMessage(String message)
    {
        System.err.println("Received TEXT message: " + message);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason)
    {
        System.err.println("Socket Closed: [" + statusCode + "] " + reason);
        TwitterIngestionServer.unsubscribe(sessionId);
    }

    @OnWebSocketError
    public void onError(Throwable cause)
    {
        System.err.println("Socket error:");
        cause.printStackTrace(System.err);
        TwitterIngestionServer.unsubscribe(sessionId);
    }
}
