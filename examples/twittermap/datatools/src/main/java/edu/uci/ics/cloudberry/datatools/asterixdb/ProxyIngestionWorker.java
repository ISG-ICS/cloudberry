package edu.uci.ics.cloudberry.datatools.asterixdb;

import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.net.URI;

/**
 * ProxyIngestionWorker
 *
 * ProxyIngestionWorker is a runnable class.
 * Once starts, it connects to given `from-proxy` url, and listens to the TwitterIngestionProxy websocket.
 *
 * @author Qiushi Bai, baiqiushi@gmail.com
 */
public class ProxyIngestionWorker implements Runnable{

    AsterixDBIngestionConfig config;
    WebSocketClient client;

    public ProxyIngestionWorker(AsterixDBIngestionConfig _config) {
        config = _config;
    }

    @Override
    public void run() {
        System.err.println("Proxy Ingestion Worker starts!");
        
        // start a Twitter ingestion proxy socket client
        client = new WebSocketClient();
        TwitterIngestionProxySocketClient socket = new TwitterIngestionProxySocketClient();
        try {
            client.start();
            URI fromProxyUri = new URI(config.getFromProxy());
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect(socket, fromProxyUri, request);

            System.err.println("Establish connection to the source proxy socket [" + config.getFromProxy() + "] succeeded!");
        }
        catch (Exception e) {
            System.err.println("Establish connection to the source proxy socket [" + config.getFromProxy() + "] failed!");
            e.printStackTrace();
        }

        while(true) {
            try {
                Thread.sleep(1000);
            }
            catch(Exception e) {
                e.printStackTrace(System.err);
            }
        }

        //System.err.println("Proxy Ingestion Worker ends!");
    }
    
    public void cleanUp() {
        // TODO - add any necessary cleanup code here.
    }
}
