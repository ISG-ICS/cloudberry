package edu.uci.ics.cloudberry.datatools.twitter.geotagger;

import com.fasterxml.jackson.core.JsonParser;
import edu.uci.ics.cloudberry.gnosis.USGeoGnosis;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.uci.ics.cloudberry.util.Rectangle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * TwitterGeoTagger
 *
 *  - (1) If used as an embedded API in other services,
 *        it provides a function tagOneTweet,
 *        which takes a Tweet String as input, along with a handle to a USGeoGnosis object,
 *        and outputs a geo_tagged Tweet Json Map (from jackson.databind.ObjectMapper).
 *
 *  - (2) If used as standalone process,
 *        it takes 4 arguments [state, county and city geo-json files, # of concurrent threads],
 *        and it reads pipelined Tweet String from stdin,
 *        and it outputs pipelined geo_tagged Tweet String into stdout.
 *
 * @author Qiushi Bai
 */
public class TwitterGeoTagger {

    public static boolean DEBUG = false;
    public static boolean SKIP_UNTAGGED = false;

    public static String GEO_TAG = "geo_tag";
    public static String STATE_ID = "stateID";
    public static String STATE_NAME = "stateName";
    public static String COUNTY_ID = "countyID";
    public static String COUNTY_NAME = "countyName";
    public static String CITY_ID = "cityID";
    public static String CITY_NAME = "cityName";

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        // parse command line arguments
        TwitterGeoTaggerConfig config = TwitterGeoTaggerConfig.createFromCLIArgs(args);
        // parsing exception or config invalid
        if (config == null) {
            return;
        }
        DEBUG = config.getDebug();
        SKIP_UNTAGGED = config.getSkipUntagged();

        // new a USGeoGnosis object shared by all threads of TwitterGeoTagger to use.
        USGeoGnosis usGeoGnosis = USGeoGnosisLoader.loadUSGeoGnosis(config.getStateJsonFile(),
                config.getCountyJsonFile(), config.getCityJsonFile());


        // create a thread pool with given thread number
        ExecutorService executorService = Executors.newFixedThreadPool(config.getThreadNumber());
        LinkedList<Future<?>> futures = new LinkedList<>();

        // use a buffer to store a batch of records for multiple threads to process
        Queue<String> buffer = new LinkedList<>();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        // keep reading lines into buffer
        while((line = bufferedReader.readLine()) != null) {
            buffer.add(line);
            // once enough lines are in buffer
            if (buffer.size() == config.getBufferSize()) {
                // submit a task to the executor service for each line in buffer
                while (!buffer.isEmpty()) {
                    String tweet = buffer.poll();
                    futures.add(executorService.submit(() -> {
                        printTagOneTweet(usGeoGnosis, tweet);
                    }));
                }
                // wait for all tasks are done.
                while(!futures.isEmpty()) {
                    Future future = futures.poll();
                    future.get();
                }
            }
        }
        // process the last lines in buffer
        while (!buffer.isEmpty()) {
            String tweet = buffer.poll();
            futures.add(executorService.submit(() -> {
                printTagOneTweet(usGeoGnosis, tweet);
            }));
        }
        // wait for all tasks are done.
        while(!futures.isEmpty()) {
            Future future = futures.poll();
            future.get();
        }
        executorService.shutdownNow();
    }

    public static Map<String, Object> tagOneTweet(USGeoGnosis usGeoGnosis, String tweetString) {
        Map<String, Object> tweetObject;

        // (1) parse tweet string to tweet object (Map<String, Object>)
        try {
            tweetObject = parseOneTweet(tweetString);
        } catch (IOException e) {
            System.err.println("Parse tweet string failed!");
            e.printStackTrace();
            return null;
        }

        // (2) add "geo_tag" object into tweet object
        //  - try text match first
        //  - then try exact point lookup
        //  - otherwise, no geo_tag information will be added, the tweet being as before
        if (!textMatchPlace(usGeoGnosis, tweetObject)) {
            exactPointLookup(usGeoGnosis, tweetObject);
        }

        return tweetObject;
    }

    public static boolean printTagOneTweet(USGeoGnosis usGeoGnosis, String tweetString) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> tweetObject;

            // (1) parse tweet string to tweet object (Map<String, Object>)
            tweetObject = mapper.readValue(tweetString, Map.class);

            boolean tagged = false;
            // (2) add "geo_tag" object into tweet object
            // (2.1) try text match first
            if (!tagged) {
                tagged = textMatchPlace(usGeoGnosis, tweetObject);
            }
            // (2.2) then try exact point lookup
            if (!tagged) {
                tagged = exactPointLookup(usGeoGnosis, tweetObject);
            }

            // print out the tweet if successfully tagged or SKIP_UNTAGGED flag is false
            if (tagged || !SKIP_UNTAGGED) {
                // (3) write tweet object back to string
                tweetString = mapper.writeValueAsString(tweetObject);

                // (4) print geo_tagged tweet to stdout
                System.out.println(tweetString);
            }

        } catch (Exception e) {
            if (DEBUG) {
                System.err.println("Geo-tag tweet failed!");
                System.err.println(tweetString);
                e.printStackTrace();
            }
            return false;
        }

        return true;
    }

    public static Map<String, Object> parseOneTweet(String tweetString) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(tweetString, Map.class);
    }

    public static Rectangle boundingBox2Rectangle(List<List<Double>> boundingBox) {

        // compatible with historical tweets, it might have 4 long-lat coordinates, or 2 long-lat coordinates
        if (boundingBox.size() != 4 || boundingBox.get(0).size() != 2) {
            throw new IllegalArgumentException("unknown bounding_box");
        }

        // get the min lat, min long, max lat and max long.
        double minLong = Double.MAX_VALUE;
        double maxLong = -Double.MAX_VALUE;
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;

        // boundingBox is in format: [[long0, lat0], [long1, lat1], ...]
        for (List<Double> longLat: boundingBox) {
            if (longLat.get(0) < minLong) {
                minLong = longLat.get(0);
            }
            if (longLat.get(0) > maxLong) {
                maxLong = longLat.get(0);
            }
            if (longLat.get(1) < minLat) {
                minLat = longLat.get(1);
            }
            if (longLat.get(1) > maxLat) {
                maxLat = longLat.get(1);
            }
        }

        // AsterixDB is unhappy with this kind of point "rectangular"
        if (minLong == maxLong && minLat == maxLat){
            minLong = maxLong - 0.0000001;
            minLat = maxLat - 0.0000001;
        }

        if (minLong > maxLong || minLat > maxLat) {
            throw new IllegalArgumentException(
                    "Not a good Rectangle: " +
                            "minLong:" + minLong +
                            ", minLat:" + minLat +
                            ", maxLong:" + maxLong +
                            ", maxLat:" + maxLat);
        }

        return new Rectangle(minLong, minLat, maxLong, maxLat);
    }

    public static void writeGeoTagToTweetObject(Map<String, Object> tweetObject, scala.Option<USGeoGnosis.USGeoTagInfo> geoTag) {
        Map<String, Object> geoTagObject = new HashMap<>();
        geoTagObject.put(STATE_ID, geoTag.get().stateID());
        geoTagObject.put(STATE_NAME, geoTag.get().stateName());
        if (!geoTag.get().countyID().isEmpty())
            geoTagObject.put(COUNTY_ID, geoTag.get().countyID().get());
        if (!geoTag.get().countyName().isEmpty())
            geoTagObject.put(COUNTY_NAME, geoTag.get().countyName().get());
        if (!geoTag.get().cityID().isEmpty())
            geoTagObject.put(CITY_ID, geoTag.get().cityID().get());
        if (!geoTag.get().cityName().isEmpty())
            geoTagObject.put(CITY_NAME, geoTag.get().cityName().get());

        tweetObject.put(GEO_TAG, geoTagObject);
    }

    public static boolean textMatchPlace(USGeoGnosis usGeoGnosis, Map<String, Object> tweetObject) {
        Map<String, Object> place = (Map<String, Object>) tweetObject.get("place");
        if (place == null) {
            return false;
        }
        String country = (String) place.get("country");
        if (!("United States").equals(country)) {
            return false;
        }
        scala.Option<USGeoGnosis.USGeoTagInfo> geoTag;
        String type = (String) place.get("place_type");
        switch (type) {
            case "country":
                return false;
            case "admin": // state level
                return false;
            case "city":
                String fullName = (String) place.get("full_name");
                int index = fullName.indexOf(',');
                if (index < 0) {
                    System.err.println("unknown neighborhood: " + fullName);
                    return false;
                }
                String stateAbbr = fullName.substring(index + 1).trim();
                String cityName = (String) place.get("name");
                geoTag = usGeoGnosis.tagCity(cityName, stateAbbr);
                break;
            case "neighborhood": // e.g. "The Las Vegas Strip, Paradise"
                fullName = (String) place.get("full_name");
                index = fullName.indexOf(',');
                if (index < 0) {
                    System.err.println("unknown neighborhood: " + fullName);
                    return false;
                }
                cityName = fullName.substring(index + 1).trim();
                Map<String, Object> boundingBox = (Map<String, Object>) place.get("bounding_box");
                List<List<List<Double>>> coordinates = (List<List<List<Double>>>) boundingBox.get("coordinates");
                geoTag = usGeoGnosis.tagNeighborhood(cityName,
                        boundingBox2Rectangle(coordinates.get(0)));
                break;
            case "poi": // a point
                // use the first point in bounding_box as the point to look up
                boundingBox = (Map<String, Object>) place.get("bounding_box");
                coordinates = (List<List<List<Double>>>) boundingBox.get("coordinates");
                double longitude = coordinates.get(0).get(0).get(0);
                double latitude = coordinates.get(0).get(0).get(1);
                geoTag = usGeoGnosis.tagPoint(longitude, latitude);
                break;
            default:
                System.err.println("unknown place type: " + type);
                return false;
        }

        if (geoTag == null || geoTag.isEmpty()) {
            return false;
        }

        // write geoTag to tweetObject
        writeGeoTagToTweetObject(tweetObject, geoTag);

        return true;
    }

    public static boolean exactPointLookup(USGeoGnosis usGeoGnosis, Map<String, Object> tweetObject) {
        Map<String, Object>  coordinates = (Map<String, Object> ) tweetObject.get("coordinates");
        if (coordinates == null || coordinates.isEmpty()) {
            return false;
        }
        List<Double> coordinate = (List<Double>) coordinates.get("coordinates");
        if (coordinate.size() != 2) {
            System.err.println("unknown coordinate: " + coordinate);
            return false;
        }

        scala.Option<USGeoGnosis.USGeoTagInfo> geoTag = usGeoGnosis.tagPoint(coordinate.get(0), coordinate.get(1));
        if (geoTag.isEmpty()) {
            return false;
        }

        writeGeoTagToTweetObject(tweetObject, geoTag);

        return true;
    }
}
