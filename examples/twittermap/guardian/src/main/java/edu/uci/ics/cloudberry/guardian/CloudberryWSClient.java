package edu.uci.ics.cloudberry.guardian;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;


@ClientEndpoint
public class CloudberryWSClient {
    private String serverURL = null;
    private Session userSession = null;
    private int countRetry = 0;
    private boolean syncWating = false;
    private String messageBuffer = null;

    public CloudberryWSClient(String serverURL) {
        this.serverURL = serverURL;
        this.countRetry = 0;
    }

    public boolean connect() {

        if (countRetry > 5) {
            return false;
        }

        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            System.out.println("        [CloudberryWSClient] connecting to cloudberry [" + this.serverURL + "]......");
            container.connectToServer(this, new URI(this.serverURL));
            System.out.println("        [CloudberryWSClient] server connected.");
            return true;
        } catch (DeploymentException | IOException e) {
            System.out.println("        [CloudberryWSClient] exception:");
            e.printStackTrace();
            try {
                System.out.println("        [CloudberryWSClient] will retry in 3 seconds.");
                TimeUnit.SECONDS.sleep(3);
                this.countRetry ++;
                return connect();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
                return false;
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
            System.err.println("        [CloudberryWSClient] the serverURL provided is not correct!");
            return false;
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.err.println("        [CloudberryWSClient] establishing websocket connection failed!");
            return false;
        }
    }

    public void disconnect() {
        System.out.println("        [CloudberryWSClient] disconnecting from cloudberry ......");
        if (this.userSession != null) {
            try {
                this.userSession.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("        [CloudberryWSClient] server disconnected.");
    }

    @OnOpen
    public void onOpen(Session userSession) {
        this.userSession = userSession;
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        this.userSession = null;
    }

    @OnMessage
    public void onMessage(String message) {
        this.messageBuffer = message;
        this.syncWating = false;
    }

    @OnError
    public void onError(Throwable t) {
        this.userSession = null;
        try {
            System.out.println("        [CloudberryWSClient] will retry in 3 seconds.");
            TimeUnit.SECONDS.sleep(3);
            connect();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
    }

    public String sendMessage(String message, int timeout) {
        try {
            this.syncWating = true;
            this.userSession.getBasicRemote().sendText(message);
            int stopWatch = timeout;
            while(this.syncWating) {
                if (stopWatch <= 0) {
                    System.err.println("        [CloudberryWSClient] sendMessage " + timeout + "ms time out!");
                    return null;
                }
                Thread.sleep(500);
                stopWatch -= 500;
            }
            return this.messageBuffer;

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.err.println("        [CloudberryWSClient] sendMessage Exception.");
            return null;
        }
    }
}
