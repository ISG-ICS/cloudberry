package edu.uci.ics.cloudberry.guardian;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
public final class Guardian {

    private final static int NUM_THREADS = 1;
    private final ScheduledExecutorService scheduler;
    private long initialDelay = 0;
    private long heartBeatRate = 3600;

    Guardian (long initialDelay, long heartBeatRate) {
        this.initialDelay = initialDelay;
        this.heartBeatRate = heartBeatRate;
        this.scheduler = Executors.newScheduledThreadPool(NUM_THREADS);
    }

    public void start() {
        Runnable heartBeatTask = new HeartBeatTask();
        ScheduledFuture<?> soundAlarmFuture = scheduler.scheduleWithFixedDelay(
                heartBeatTask, initialDelay, heartBeatRate, TimeUnit.SECONDS
        );
    }

    private static final class HeartBeatTask implements Runnable {
        @Override public void run() {
            count ++;
            System.out.println("heart beat -- " + count);
            boolean success = Guardian.touchAsterixDB("http://ipubmed2.ics.uci.edu:19002/query/service",
                    "select count(*) from berry.meta;");
            System.out.println("touch AsterixDB: " + success);
        }
        private int count;
    }

    public static void main(String[] args) {
        System.out.println("Guardian started ...");
        Guardian guardian = new Guardian(0, 3);
        guardian.start();
    }

    public static boolean touchAsterixDB(String queryUrl, String querySQL) {
        try {

            System.out.println("touch AsterixDB at " + queryUrl + " ... ...");

            // prepare post data
            byte[] postData = ("statement=" + querySQL).getBytes(StandardCharsets.UTF_8);
            int postDataLength = postData.length;

            // build http post connection
            URL url = new URL(queryUrl);
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

            if (status.equals("success")){
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
