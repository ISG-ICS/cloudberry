    package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class Parser {
    private int option;
    private double lowerLongitude = 0;
    private double upperLongitude = 0;
    private double lowerLatitude = 0;
    private double upperLatitude = 0;
    private int clustering = 0;
    private int clusteringAlgorithm = 0;
    private int bundling = 0;
    private String timestamp = null;
    private int zoom = 0;
    private int treeCutting = 0;
    private String query = "";

    private String endDate = null;

    int getOption() {
        return option;
    }

    double getLowerLongitude() {
        return lowerLongitude;
    }

    double getUpperLongitude() {
        return upperLongitude;
    }

    double getLowerLatitude() {
        return lowerLatitude;
    }

    double getUpperLatitude() {
        return upperLatitude;
    }

    int getClustering() {
        return clustering;
    }

    int getClusteringAlgorithm() {
        return clusteringAlgorithm;
    }

    int getBundling() {
        return bundling;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getZoom() {
        return zoom;
    }

    int getTreeCutting() {
        return treeCutting;
    }

    String getEndDate() {
        return endDate;
    }

    public String getQuery() {
        return query;
    }

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
