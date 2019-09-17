package edu.uci.ics.cloudberry.guardian;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.*;


@ClientEndpoint
public class CloudberryWSClient implements Callable {
    private String serverURL = null;
    private int retryTimes = 0;
    private int retryDelay = 0;
    private int overallTimeout = 0;
    private Session userSession = null;
    private int countRetry = 0;
    private boolean syncWating = false;
    private String messageBuffer = null;

    public CloudberryWSClient(String serverURL, int retryTimes, int retryDelay, int overallTimeout) {
        this.serverURL = serverURL;
        this.retryTimes = retryTimes;
        this.retryDelay = retryDelay;
        this.overallTimeout = overallTimeout;
        this.countRetry = 0;
    }

    public Boolean call() {
        return this.connect();
    }

    public boolean establishConnection() {
        // use executor to run connect function (call function) in case the establishing is blocked for too long
        ExecutorService executor = Executors.newCachedThreadPool();
        Future<Boolean> future = executor.submit(this);
        Boolean success = false;
        try {
            success = future.get(overallTimeout, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            e.printStackTrace();
            System.err.println("        [CloudberryWSClient] establishing websocket connection timeout! (" + overallTimeout + " seconds)");
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.err.println("        [CloudberryWSClient] establishing websocket connection interrupted!");
            return false;
        } catch (ExecutionException e) {
            e.printStackTrace();
            System.err.println("        [CloudberryWSClient] establishing websocket connection failed!");
            return false;
        }

        return success;
    }

    private boolean connect() {

        if (countRetry > retryTimes) {
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
                System.out.println("        [CloudberryWSClient] will retry in " + retryDelay + " seconds.");
                TimeUnit.SECONDS.sleep(retryDelay);
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
            System.out.println("        [CloudberryWSClient] will retry in " + retryDelay + " seconds.");
            TimeUnit.SECONDS.sleep(retryDelay);
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
