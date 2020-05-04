package edu.uci.ics.cloudberry.datatools.twitter;

import org.eclipse.jetty.http.pathmap.ServletPathSpec;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TwitterIngestionServer {

    // for websocket clients to subscribe to realtime ingestion proxy
    public static ConcurrentHashMap<Integer, RemoteEndpoint> subscribers = new ConcurrentHashMap<>();

    public static int subscribe(RemoteEndpoint subscriber) {
        int sessionId = subscribers.size() + 1;
        subscribers.put(sessionId, subscriber);
        return sessionId;
    }

    public static void unsubscribe(int sessionId) {
        subscribers.remove(sessionId);
    }

    public static void publish(String tweet) {
        for (Map.Entry<Integer, RemoteEndpoint> subscriber: subscribers.entrySet() ) {
            subscriber.getValue().sendStringByFuture(tweet);
        }
    }

    // for http clients to get statistics of TwitterIngestionWorker
    public static TwitterIngestionWorker twitterIngestionWorker;

    public static String getStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"startTime\": \"" + twitterIngestionWorker.getStartTime() + "\"");
        sb.append(",");
        sb.append("\"counter\": \"" + twitterIngestionWorker.getCounter() + "\"");
        sb.append(",");
        sb.append("\"averageRate\": \"" + twitterIngestionWorker.getAverageRate() + "\"");
        sb.append(",");
        sb.append("\"instantRate\": \"" + twitterIngestionWorker.getInstantRate() + "\"");
        sb.append("}");
        return sb.toString();
    }

    /**
     * TwitterIngestionProxySocketCreator
     *
     * only used for jetty server websocket filter setup
     */
    public static class TwitterIngestionProxySocketCreator implements WebSocketCreator
    {
        @Override
        public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp)
        {
            return new TwitterIngestionProxySocket();
        }
    }

    public static void main(String[] args) {

        // parse command line arguments
        TwitterIngestionConfig config = TwitterIngestionConfig.createFromCLIArgs(args);
        // parsing exception or config invalid
        if (config == null) {
            return;
        }

        // start Twitter Ingestion Worker
        twitterIngestionWorker = new TwitterIngestionWorker(config);
        Thread twitterIngestionWorkerThread = new Thread(twitterIngestionWorker);
        // clean up when shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (twitterIngestionWorker != null) {
                twitterIngestionWorker.cleanUp();
            }
        }));
        twitterIngestionWorkerThread.start();

        // start Twitter Ingestion Proxy
        if (config.getProxyPort() > 0) {
            try {
                Server server = new Server();
                ServerConnector connector = new ServerConnector(server);
                connector.setPort(config.getProxyPort());
                server.addConnector(connector);

                URL webRootLocation = TwitterIngestionServer.class.getResource("/webroot/index.html");
                if (webRootLocation == null)
                {
                    throw new IllegalStateException("Unable to determine webroot URL location");
                }

                URI webRootUri = URI.create(webRootLocation.toURI().toASCIIString().replaceFirst("/index.html$","/"));

                // Setup the basic application "context" for this application at "/"
                ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
                context.setContextPath("/");
                context.setBaseResource(Resource.newResource(webRootUri));
                context.setWelcomeFiles(new String[]{"index.html"});
                context.getMimeTypes().addMimeMapping("txt","text/plain;charset=utf-8");
                server.setHandler(context);

                // Setup websocket filter to map TwitterIngestionProxySocket listen to "/proxy"
                WebSocketUpgradeFilter wsfilter = WebSocketUpgradeFilter.configure(context);
                wsfilter.getFactory().getPolicy().setIdleTimeout(5000);
                wsfilter.addMapping(new ServletPathSpec("/proxy"), new TwitterIngestionProxySocketCreator());

                // Setup stats http servlet
                context.addServlet(TwitterIngestionStats.class, "/stats");

                // Setup default servlet to serve the static files
                ServletHolder staticFileHolder = new ServletHolder("default", DefaultServlet.class);
                staticFileHolder.setInitParameter("dirAllowed","true");
                context.addServlet(staticFileHolder,"/");

                System.err.println("Twitter Ingestion Proxy Server started!");
                String ip = InetAddress.getLocalHost().getHostAddress();
                System.err.println(" - Visit http://" + ip + ":" + config.getProxyPort() + " to access the admin console.");
                System.err.println(" - Build Websocket client connecting to ws://" + ip + ":" + config.getProxyPort() + "/proxy to receive realtime stream of this ingestion.");

                // start jetty http server
                server.start();
                server.join();
            } catch (Throwable t) {
                t.printStackTrace(System.err);
            }
        }

        System.err.println("main function exit...");
    }
}