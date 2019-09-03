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

    public void parse(String query) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(query);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            endDate = jsonNode.get("date").asText();
            option = Integer.parseInt(jsonNode.get("option").asText());
            lowerLongitude = Double.parseDouble(jsonNode.get("lowerLongitude").asText());
            upperLongitude = Double.parseDouble(jsonNode.get("upperLongitude").asText());
            lowerLatitude = Double.parseDouble(jsonNode.get("lowerLatitude").asText());
            upperLatitude = Double.parseDouble(jsonNode.get("upperLatitude").asText());
            clusteringAlgorithm = Integer.parseInt(jsonNode.get("clusteringAlgorithm").asText());
            timestamp = jsonNode.get("timestamp").asText();
            zoom = Integer.parseInt(jsonNode.get("zoom").asText());
            bundling = Integer.parseInt(jsonNode.get("bundling").asText());
            clustering = Integer.parseInt(jsonNode.get("clustering").asText());
            treeCutting = Integer.parseInt(jsonNode.get("treeCut").asText());
        } catch (NullPointerException ignored) {

        }
    }
}
