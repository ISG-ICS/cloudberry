package edu.uci.ics.cloudberry.guardian;

import java.util.Map;

public class GuardianConfig {
    public final static String DEFAULT_INITIAL_DELAY = "0";
    public final static String DEFAULT_HEART_BEAT_RATE = "3600";
    public final static String DEFAUTL_ASTERIXDB_QUERY_URL = "http://localhost:19002/query/service";

    private Map<String, String> guardianConfig;
    private Map<String, String> asterixdbConfig;

    public GuardianConfig() {}

    public GuardianConfig(Map<String, String> guardianConfig, Map<String, String> asterixdbConfig) {
        this.guardianConfig = guardianConfig;
        this.asterixdbConfig = asterixdbConfig;
    }

    public Map<String, String> getGuardianConfig() {
        return guardianConfig;
    }

    public Map<String, String> getAsterixdbConfig() {
        return asterixdbConfig;
    }

    public boolean validate() {
        System.out.println("Loading guardian config file successfully! ");
        System.out.println("We will be using the following configurations for the guardian service:");
        System.out.println("--------------------------------------------------");
        System.out.println("guardianConfig:");
        System.out.println("    initialDelay: " + guardianConfig.getOrDefault("initialDelay", DEFAULT_INITIAL_DELAY));
        System.out.println("    heartBeatRate: " + guardianConfig.getOrDefault("heartBeatRate", DEFAULT_HEART_BEAT_RATE));
        System.out.println("asterixdbConfig:");
        System.out.println("    queryURL: " + asterixdbConfig.getOrDefault("queryURL", DEFAUTL_ASTERIXDB_QUERY_URL));
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
        System.out.println("    queryURL: [string] [required] [url for AsterixDB REST API query] [default: " + DEFAUTL_ASTERIXDB_QUERY_URL + "]" );
        System.out.println("--------------------------------------------------");
    }
}
