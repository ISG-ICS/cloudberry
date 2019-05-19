package edu.uci.ics.cloudberry.guardian;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Guardian
 *
 * Monitor services stack of Twittermap Application in a heart beat manner (default 3600s)
 *  - AsterixDB
 *    - Send query `SELECT COUNT(*) FROM berry.meta`
 *  - Cloudberry
 *    - Send query `{"dataset":"twitter.ds_tweet",
 *                    "global":{"globalAggregate":{"field":"*","apply":{"name":"count"},"as":"count"}
 *                             },"estimable":true
 *                  }`
 *  - Twittermap
 *    - Send http GET request to Twittermap
 */
public final class Guardian implements Runnable {

    private final static int NUM_THREADS = 1;
    private final ScheduledExecutorService scheduler;

    private long initialDelay;
    private long heartBeatRate;
    private String asterixDBQueryURL;

    Guardian (GuardianConfig guardianConfig) {
        this.initialDelay = Integer.valueOf(guardianConfig.getGuardianConfig()
                .getOrDefault("initialDelay", GuardianConfig.DEFAULT_INITIAL_DELAY));
        this.heartBeatRate = Integer.valueOf(guardianConfig.getGuardianConfig()
                .getOrDefault("heartBeatRate", GuardianConfig.DEFAULT_HEART_BEAT_RATE));
        this.asterixDBQueryURL = guardianConfig.getAsterixdbConfig()
                .getOrDefault("queryURL", GuardianConfig.DEFAUTL_ASTERIXDB_QUERY_URL);

        this.scheduler = Executors.newScheduledThreadPool(NUM_THREADS);
    }

    public void start() {
        ScheduledFuture<?> soundAlarmFuture = scheduler.scheduleWithFixedDelay(
                this, initialDelay, heartBeatRate, TimeUnit.SECONDS
        );
    }

    @Override public void run() {
        System.out.println("heart beat -- " + new Date());
        // touch AsterixDB
        boolean success = touchAsterixDB(asterixDBQueryURL);
        System.out.println("touch AsterixDB: " + success);
    }

    public static void main(String[] args) {
        GuardianConfig config = null;

        // parse arguments
        String configFilePath = null;
        for (int i = 0; i < args.length; i ++) {
            switch (args[i].toLowerCase()) {
                case "--config" :
                case "-c" :
                    try {
                        configFilePath = args[i + 1];
                    }
                    catch (ArrayIndexOutOfBoundsException e) {
                        System.err.println("Config file path should follow -c [--config].");
                        return;
                    }
            }
        }

        if (configFilePath == null) {
            System.err.println("Please indicate config file path.\nUsage: --config [file] or -c [file].\n");
            return;
        }

        // load config file
        try {
            config = loadGuardianConfig(configFilePath);
        }
        catch (IOException e) {
            e.printStackTrace();
            System.err.println("Config file: [" + configFilePath + "] does not exist.");
            return;
        }

        if (!config.validate()) {
            System.err.println("The config file provided is not valid, please refer to the following template:");
            config.printTemplate();
            return;
        }

        Guardian guardian = new Guardian(config);
        guardian.start();
        System.out.println("Guardian started ...");
    }

    public static GuardianConfig loadGuardianConfig(String configFilePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        GuardianConfig config = mapper.readValue(new File(configFilePath), GuardianConfig.class);
        return config;
    }

    public boolean touchAsterixDB(String queryURL) {
        try {
            String querySQL = "select count(*) from berry.meta;";

            System.out.println("[" + new Date() + "]");
            System.out.println("touch AsterixDB ... ...");
            System.out.println("    queryURL: " + queryURL);
            System.out.println("    querySQL:" + querySQL);
            // prepare post data
            byte[] postData = ("statement=" + querySQL).getBytes(StandardCharsets.UTF_8);
            int postDataLength = postData.length;

            // build http post connection
            URL url = new URL(queryURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("Accept", "*/*");
            con.setRequestProperty("charset", "utf-8");
            con.setRequestProperty("Content-Length", Integer.toString(postDataLength));
            con.setConnectTimeout(3000);
            con.setReadTimeout(15000);
            con.setDoOutput(true);

            // send post data
            DataOutputStream out = new DataOutputStream(con.getOutputStream());
            out.write(postData);
            out.flush();
            out.close();

            // receive data
            int responseCode = con.getResponseCode();

            System.out.println("responseCode = " + responseCode);

            if (responseCode != 200) {
                return false;
            }

            // parse received data to JSON
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> jsonMap = mapper.readValue(con.getInputStream(), Map.class);
            String status = (String) jsonMap.get("status");

            System.out.println("status = " + status);

            if (status.equals("success")) {
                return true;
            }
            else {
                return false;
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
