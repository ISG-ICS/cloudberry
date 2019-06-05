package edu.uci.ics.cloudberry.guardian;

import java.util.Map;

public class GuardianConfig {
    public final static String DEFAULT_INITIAL_DELAY = "0";
    public final static String DEFAULT_HEART_BEAT_RATE = "3600";
    public final static String DEFAULT_ASTERIXDB_QUERY_URL = "http://localhost:19002/query/service";
    public final static String DEFAULT_CLOUDBERRY_QUERY_URL = "ws://localhost:9000/ws";
    public final static String DEFAULT_TWITTERMAP_URL = "http://localhost:9001";

    private Map<String, String> guardianConfig;
    private Map<String, String> asterixdbConfig;
    private Map<String, String> cloudberryConfig;
    private Map<String, String> twittermapConfig;

    public GuardianConfig() {}

    public GuardianConfig(Map<String, String> guardianConfig,
                          Map<String, String> asterixdbConfig,
                          Map<String, String> cloudberryConfig,
                          Map<String, String> twittermapConfig) {
        this.guardianConfig = guardianConfig;
        this.asterixdbConfig = asterixdbConfig;
        this.cloudberryConfig = cloudberryConfig;
        this.twittermapConfig = twittermapConfig;
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

    public boolean validate() {
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
        System.out.println("twittermapConfig:");
        System.out.println("    url: " + twittermapConfig.getOrDefault("queryURL", DEFAULT_TWITTERMAP_URL));
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
        System.out.println("twittermapConfig:");
        System.out.println("    url: [string] [required] [url for Twittermap] [default: " + DEFAULT_TWITTERMAP_URL + "]" );
        System.out.println("--------------------------------------------------");
    }
}
