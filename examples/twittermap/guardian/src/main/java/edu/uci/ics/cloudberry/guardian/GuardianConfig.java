package edu.uci.ics.cloudberry.guardian;

import java.util.Map;

public class GuardianConfig {
    public final static String DEFAULT_INITIAL_DELAY = "0";
    public final static String DEFAULT_HEART_BEAT_RATE = "3600";
    public final static String DEFAULT_ASTERIXDB_QUERY_URL = "http://localhost:19002/query/service";
    public final static String DEFAULT_CLOUDBERRY_QUERY_URL = "ws://localhost:9000/ws";
    public final static String DEFAULT_CLOUDBERRY_INGESTION_CHECK = "true";
    public final static String DEFAULT_TWITTERMAP_URL = "http://localhost:9001";

    private Map<String, String> guardianConfig;
    private Map<String, String> asterixdbConfig;
    private Map<String, String> cloudberryConfig;
    private Map<String, String> twittermapConfig;
    private NotificationConfig notificationConfig;

    public GuardianConfig() {}

    public GuardianConfig(Map<String, String> guardianConfig,
                          Map<String, String> asterixdbConfig,
                          Map<String, String> cloudberryConfig,
                          Map<String, String> twittermapConfig,
                          NotificationConfig notificationConfig) {
        this.guardianConfig = guardianConfig;
        this.asterixdbConfig = asterixdbConfig;
        this.cloudberryConfig = cloudberryConfig;
        this.twittermapConfig = twittermapConfig;
        this.notificationConfig = notificationConfig;
    }

    public Map<String, String> getGuardianConfig() {
        return guardianConfig;
    }

    public Map<String, String> getAsterixdbConfig() {
        return asterixdbConfig;
    }

    public Map<String, String> getCloudberryConfig() {
        return cloudberryConfig;
    }

    public Map<String, String> getTwittermapConfig() {
        return twittermapConfig;
    }

    public NotificationConfig getNotificationConfig() {
        return notificationConfig;
    }

    public boolean validate() {
        if (!notificationConfig.validate()) {
            return false;
        }
        System.out.println("Loading guardian config file successfully! ");
        System.out.println("We will be using the following configurations for the guardian service:");
        System.out.println("--------------------------------------------------");
        System.out.println("guardianConfig:");
        System.out.println("    initialDelay: " + guardianConfig.getOrDefault("initialDelay", DEFAULT_INITIAL_DELAY));
        System.out.println("    heartBeatRate: " + guardianConfig.getOrDefault("heartBeatRate", DEFAULT_HEART_BEAT_RATE));
        System.out.println("asterixdbConfig:");
        System.out.println("    queryURL: " + asterixdbConfig.getOrDefault("queryURL", DEFAULT_ASTERIXDB_QUERY_URL));
        System.out.println("cloudberryConfig:");
        System.out.println("    queryURL: " + cloudberryConfig.getOrDefault("queryURL", DEFAULT_CLOUDBERRY_QUERY_URL));
        System.out.println("    ingestionCheck: " + cloudberryConfig.getOrDefault("ingestionCheck", DEFAULT_CLOUDBERRY_INGESTION_CHECK));
        System.out.println("twittermapConfig:");
        System.out.println("    url: " + twittermapConfig.getOrDefault("queryURL", DEFAULT_TWITTERMAP_URL));
        notificationConfig.print();
        System.out.println("--------------------------------------------------");
        return true;
    }

    public void printTemplate() {
        System.out.println("Guardian config file template: (guardian.yaml):");
        System.out.println("--------------------------------------------------");
        System.out.println("guardianConfig:");
        System.out.println("    initialDelay: [integer] [required] [seconds to be delayed before the first heart beat] [default: " + DEFAULT_INITIAL_DELAY + "]");
        System.out.println("    heartBeatRate: [integer] [required] [seconds between each heart beat] [default: " + DEFAULT_HEART_BEAT_RATE + "]");
        System.out.println("asterixdbConfig:");
        System.out.println("    queryURL: [string] [required] [url for AsterixDB REST API query] [default: " + DEFAULT_ASTERIXDB_QUERY_URL + "]" );
        System.out.println("cloudberryConfig:");
        System.out.println("    queryURL: [string] [required] [url for Cloudberry REST API query] [default: " + DEFAULT_CLOUDBERRY_QUERY_URL + "]" );
        System.out.println("    ingestionCheck: [boolean] [required] [whether to check the count is increasing] [default: " + DEFAULT_CLOUDBERRY_INGESTION_CHECK + "]" );
        System.out.println("twittermapConfig:");
        System.out.println("    url: [string] [required] [url for Twittermap] [default: " + DEFAULT_TWITTERMAP_URL + "]" );
        notificationConfig.printTemplate();
        System.out.println("--------------------------------------------------");
    }

    public class NotificationConfig {
        private String publisherEmail;
        private String publisherEmailPrefix;
        private String[] subscriberEmails;

        public NotificationConfig(){

        }

        public void setPublisherEmail(String publisherEmail) {
            this.publisherEmail = publisherEmail;
        }

        public void setPublisherEmailPrefix(String publisherEmailPrefix) {
            this.publisherEmailPrefix = publisherEmailPrefix;
        }

        public void setSubscriberEmails(String[] subscriberEmails) {
            this.subscriberEmails = subscriberEmails;
        }

        public String getPublisherEmail() {
            return publisherEmail;
        }

        public String getPublisherEmailPrefix() {
            return publisherEmailPrefix;
        }

        public String[] getSubscriberEmails() {
            return subscriberEmails;
        }

        public boolean validate() {
            if (subscriberEmails == null || subscriberEmails.length < 1) {
                System.err.println("subscriberEmails must NOT be empty!");
                return false;
            }
            return true;
        }

        public void print() {
            System.out.println("notificationConfig:");
            System.out.println("    publisherEmail: " + publisherEmail);
            System.out.println("    publisherEmailPrefix: "+ publisherEmailPrefix);
            System.out.println("    subscriberEmails:");
            for (int i = 0; i < subscriberEmails.length; i ++) {
                System.out.println("      - " + subscriberEmails[i]);
            }
        }

        public void printTemplate() {
            System.out.println("notificationConfig:");
            System.out.println("    publisherEmail: abc@gmail.com");
            System.out.println("    publisherEmailPrefix: Production Server");
            System.out.println("    subscriberEmails:");
            System.out.println("      - efg@gmail.com");
            System.out.println("      - hij@gmail.com");
        }
    }
}
