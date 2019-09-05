package connection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * this class is for GraphController to parse requests sent from frontend
 */
public class Parser {
    // option indicates the request type
    private int option;
    // the four coordinates corresponding to the current window
    private double lowerLongitude = 0;
    private double upperLongitude = 0;
    private double lowerLatitude = 0;
    private double upperLatitude = 0;
    // 0: display points, 1: display clusters
    private int clustering = 0;
    // 0: HGC, 1: IKmeans, 2: Kmeans
    private int clusteringAlgorithm = 0;
    // 0: no bundling, 1: do FDEB
    private int bundling = 0;
    // the timestamp when opening the socket
    private String timestamp = null;
    // the current zoom level
    private int zoom = 0;
    // 0: no treeCutting, 1: do treeCutting
    private int treeCutting = 0;
    // the query keyword
    private String query = "";
    // the end date of the last slice, also the start date of the current slice
    private String endDate = null;

    public int getOption() {
        return option;
    }

    public double getLowerLongitude() {
        return lowerLongitude;
    }

    public double getUpperLongitude() {
        return upperLongitude;
    }

    public double getLowerLatitude() {
        return lowerLatitude;
    }

    public double getUpperLatitude() {
        return upperLatitude;
    }

    public int getClustering() {
        return clustering;
    }

    public int getClusteringAlgorithm() {
        return clusteringAlgorithm;
    }

    public int getBundling() {
        return bundling;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getZoom() {
        return zoom;
    }

    public int getTreeCutting() {
        return treeCutting;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getQuery() {
        return query;
    }

    /**
     * parse the JSON sent from frontend into variables in backend
     * @param query the JSON sent from frontend
     */
    public void parse(String query) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(query);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (jsonNode != null) {
            if (jsonNode.has("date")) {
                endDate = jsonNode.get("date").asText();
            }
            if (jsonNode.has("query")) {
                this.query = jsonNode.get("query").asText();
            }
            if (jsonNode.has("timestamp")) {
                timestamp = jsonNode.get("timestamp").asText();
            }
            if (jsonNode.has("option")) {
                option = Integer.parseInt(jsonNode.get("option").asText());
            }
            if (jsonNode.has("clusteringAlgorithm")) {
                clusteringAlgorithm = Integer.parseInt(jsonNode.get("clusteringAlgorithm").asText());
            }
            if (jsonNode.has("lowerLongitude"))
                lowerLongitude = Double.parseDouble(jsonNode.get("lowerLongitude").asText());
            if (jsonNode.has("upperLongitude")) {
                upperLongitude = Double.parseDouble(jsonNode.get("upperLongitude").asText());
            }
            if (jsonNode.has("lowerLatitude")) {
                lowerLatitude = Double.parseDouble(jsonNode.get("lowerLatitude").asText());
            }
            if (jsonNode.has("upperLatitude")) {
                upperLatitude = Double.parseDouble(jsonNode.get("upperLatitude").asText());
            }
            if (jsonNode.has("zoom"))
                zoom = Integer.parseInt(jsonNode.get("zoom").asText());
            if (jsonNode.has("bundling"))
                bundling = Integer.parseInt(jsonNode.get("bundling").asText());
            if (jsonNode.has("clustering"))
                clustering = Integer.parseInt(jsonNode.get("clustering").asText());
            if (jsonNode.has("treeCut"))
                treeCutting = Integer.parseInt(jsonNode.get("treeCut").asText());
        }
    }
}
