package edu.uci.ics.cloudberry.guardian;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
    private static long preCount = 0;

    private long initialDelay;
    private long heartBeatRate;
    private String asterixDBQueryURL;
    private String cloudberryServerURL;
    private String twittermapURL;
    private String publisherEmail;
    private String publisherEmailPrefix;
    private String[] subscriberEmails;

    Guardian (GuardianConfig guardianConfig) {
        this.initialDelay = Integer.valueOf(guardianConfig.getGuardianConfig()
                .getOrDefault("initialDelay", GuardianConfig.DEFAULT_INITIAL_DELAY));
        this.heartBeatRate = Integer.valueOf(guardianConfig.getGuardianConfig()
                .getOrDefault("heartBeatRate", GuardianConfig.DEFAULT_HEART_BEAT_RATE));
        this.asterixDBQueryURL = guardianConfig.getAsterixdbConfig()
                .getOrDefault("queryURL", GuardianConfig.DEFAULT_ASTERIXDB_QUERY_URL);
        this.cloudberryServerURL = guardianConfig.getCloudberryConfig()
                .getOrDefault("queryURL", GuardianConfig.DEFAULT_CLOUDBERRY_QUERY_URL);
        this.twittermapURL = guardianConfig.getTwittermapConfig()
                .getOrDefault("url", GuardianConfig.DEFAULT_TWITTERMAP_URL);
        this.publisherEmail = guardianConfig.getNotificationConfig().getPublisherEmail();
        this.publisherEmailPrefix = guardianConfig.getNotificationConfig().getPublisherEmailPrefix();
        this.subscriberEmails = guardianConfig.getNotificationConfig().getSubscriberEmails();

        this.scheduler = Executors.newScheduledThreadPool(NUM_THREADS);
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(
                this, initialDelay, heartBeatRate, TimeUnit.SECONDS
        );
    }

    @Override public void run() {
        System.out.println("heart beat -- " + new Date());
        boolean success;

        // touch AsterixDB
        success = touchAsterixDB(asterixDBQueryURL);
        if (success) {
            System.out.println("[Good!] AsterixDB is alive.");
        }
        else {
            System.err.println("[Bad!] AsterixDB maybe down!");
            sendEmail("AsterixDB maybe down.", "Please check here: " + asterixDBQueryURL);
            return;
        }

        // touch Cloudberry
        success = touchCloudberry(this.cloudberryServerURL);
        if (success) {
            System.out.println("[Good!] Cloudberry is working properly.");
        }
        else {
            // emails are sent inside touchCloudberry function.
            return;
        }

        // touch Twittermap
        success = touchTwittermap(this.twittermapURL);
        if (success) {
            System.out.println("[Good!] Twittermap is accessible.");
        }
        else {
            System.err.println("[Bad!] Twittermap is NOT accessible!");
            sendEmail("Twittermap is NOT accessible.", "Please check here: " + twittermapURL);
        }
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
            //return;
            System.err.println("Use default config file path: ./guardian/guardian.yaml\n");
            configFilePath = "/Users/white/IdeaProjects/cloudberry/examples/twittermap/guardian/guardian.yaml";
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

            System.out.println("    [touchAsterixDB] touching AsterixDB ... ...");
            System.out.println("    [touchAsterixDB]    queryURL: " + queryURL);
            System.out.println("    [touchAsterixDB]    querySQL:" + querySQL);
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

            System.out.println("    [touchAsterixDB] responseCode = " + responseCode);

            if (responseCode != 200) {
                System.err.println("    [touchAsterixDB] failed! ==> HTTP responseCode = " + responseCode);
                return false;
            }

            // parse received data to JSON
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> jsonMap = mapper.readValue(con.getInputStream(), Map.class);
            String status = (String) jsonMap.get("status");

            System.out.println("    [touchAsterixDB] response Json = \n" + jsonMap);
            System.out.println("    [touchAsterixDB] status = " + status);

            if (status.equals("success")) {
                return true;
            }
            else {
                System.err.println("    [touchAsterixDB] failed! ==> query status = " + status);
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

    public boolean touchCloudberry(String cloudberryServerURL) {

        String queryJSON = "{\"dataset\":\"twitter.ds_tweet\",\"global\":{\"globalAggregate\":{\"field\":\"*\",\"apply\":{\"name\":\"count\"},\"as\":\"count\"}},\"estimable\":true}";

        System.out.println("    [touchCloudberry] touching Cloudberry ... ...");
        System.out.println("    [touchCloudberry]    queryURL: " + cloudberryServerURL);
        System.out.println("    [touchCloudberry]    queryJSON:" + queryJSON);
        final CloudberryWSClient cloudberryWSClient = new CloudberryWSClient(cloudberryServerURL);
        boolean success = cloudberryWSClient.connect();
        if (!success) {
            System.err.println("    [touchCloudberry] failed! ==> Can not establish connection.");
            sendEmail("Cannot connect to Cloudberry.",
                    "Can not establish connection to Cloudberry even after 5 trials: " + cloudberryServerURL);
            return false;
        }
        System.out.println("    [touchCloudberry] connect to server successfully...");
        String response = cloudberryWSClient.sendMessage(queryJSON, 5000);
        if (response == null) {
            System.err.println("    [touchCloudberry] failed! ==> response is null.");
            sendEmail("Cloudberry is not working properly.",
                    "Cloudberry returns null response: " + cloudberryServerURL);
            return false;
        }
        // parse received data to JSON
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> jsonMap = null;
        try {
            jsonMap = mapper.readValue(response, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("    [touchCloudberry] failed! ==> response JSON can not be parsed.");
            sendEmail("Cloudberry is not working properly.",
                    "Cloudberry response JSON can not be parsed: " + cloudberryServerURL);
            return false;
        }

        System.out.println("    [touchCloudberry] get response from cloudberry:");
        System.out.println("    [touchCloudberry] response Json = \n" + jsonMap);

        // Check whether the result count is changing
        int count = (int) jsonMap.get("count");
        if (count ==  preCount) {
            System.err.println("    [touchCloudberry] [Bad!] Twitter ingestion may stop or Cloudberry may malfunction.");
            sendEmail("Twitter ingestion may stop or Cloudberry may malfunction.",
                    "Twitter ingestion may stop or Cloudberry may malfunction. " +
                            "The total count of ds_tweet retrieved is: " + count +
                            "this time and " + preCount + " last time.");
            return false;
        }
        preCount = count;

        cloudberryWSClient.disconnect();
        return true;
    }

    public boolean touchTwittermap(String twittermapServerURL) {
        try {
            URL url = new URL(twittermapServerURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "*/*");
            con.setConnectTimeout(3000);
            con.setReadTimeout(15000);
            con.setDoOutput(true);

            // receive data
            int responseCode = con.getResponseCode();
            System.out.println("    [touchTwittermap] responseCode = " + responseCode);

            if (responseCode != 200) {
                System.err.println("    [touchTwittermap] failed! ==> HTTP responseCode = " + responseCode);
                return false;
            }

            return true;

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        } catch (ProtocolException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void sendEmail(String title, String content) {
        String subject = "[" + publisherEmailPrefix + "] " + title;
        System.out.println("Sending email:");
        System.out.println(" - subject: " + subject);
        System.out.println(" - content: " + content);
        for (int i = 0; i < subscriberEmails.length; i ++) {

            String to = subscriberEmails[i];
            String from = publisherEmail;

            // Get system properties
            Properties properties = System.getProperties();
            // Setup mail server
            properties.setProperty("mail.smtp.host", "localhost");
            // Get the default Session object.
            Session session = Session.getDefaultInstance(properties);

            try {
                // Create a default MimeMessage object.
                MimeMessage message = new MimeMessage(session);
                // Set From: header field of the header.
                message.setFrom(new InternetAddress(from));
                // Set To: header field of the header.
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
                // Set Subject: header field
                message.setSubject(subject);
                // Now set the actual message
                message.setText(content);
                // Send message
                Transport.send(message);
                System.out.println("to [" + to + "] successfully...");
            } catch (MessagingException mex) {
                System.out.println("to [" + to + "] failed...");
                mex.printStackTrace();
            }
        }
    }
}
