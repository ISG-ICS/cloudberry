package connection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Edge;
import models.Point;

import java.util.HashMap;

/**
 * this class is for GraphController to send responses to frontend
 */
public class Response {
    // option indicates the request type
    private int option;
    // the timestamp when opening the socket connection
    private String timestamp;
    // flag indicates whether the incremental query is finished
    private String flag;
    // the number of edges on the screen
    private int edgesCnt;
    // the number of reply tweets corresponding to a keyword
    private int repliesCnt;
    // the data of points or edges, for drawing graph in frontend
    private String data;
    // the number of edges on the screen that are not bundled
    private int isolatedEdgesCnt;
    // the end date of the current slice
    private String date;
    // the number of points (or points in clusters) on the screen
    private int pointsCnt;
    // the number of clusters on the screen
    private int clustersCnt;

    public int getOption() {
        return option;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getFlag() {
        return flag;
    }

    public int getEdgesCnt() {
        return edgesCnt;
    }

    public int getRepliesCnt() {
        return repliesCnt;
    }

    public String getData() {
        return data;
    }

    public int getIsolatedEdgesCnt() {
        return isolatedEdgesCnt;
    }

    public String getDate() {
        return date;
    }

    public int getPointsCnt() {
        return pointsCnt;
    }

    public int getClustersCnt() {
        return clustersCnt;
    }

    public void setClustersCnt(int clustersCnt) {
        this.clustersCnt = clustersCnt;
    }

    public void setPointsCnt(int pointsCnt) {
        this.pointsCnt = pointsCnt;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setOption(int option) {
        this.option = option;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public void setEdgesCnt(int edgesCnt) {
        this.edgesCnt = edgesCnt;
    }

    public void setRepliesCnt(int repliesCnt) {
        this.repliesCnt = repliesCnt;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setIsolatedEdgesCnt(int isolatedEdgesCnt) {
        this.isolatedEdgesCnt = isolatedEdgesCnt;
    }

    /**
     * set data as edges in JSON format
     * @param edges edges stored in HashMap
     */
    public void setEdges(HashMap<Edge, Integer> edges) {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (Edge edge : edges.keySet()) {
            ObjectNode lineNode = objectMapper.createObjectNode();
            lineNode.putArray("from").add(edge.getFromX()).add(edge.getFromY());
            lineNode.putArray("to").add(edge.getToX()).add(edge.getToY());
            lineNode.put("width", edges.get(edge));
            arrayNode.add(lineNode);
        }
        setData(arrayNode.toString());
    }

    /**
     * set data as points in JSON format
     * @param points points stored in HashMap
     */
    public void setPoints(HashMap<Point, Integer> points) {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (Point point : points.keySet()) {
            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.putArray("coordinates").add(point.getX()).add(point.getY());
            objectNode.put("size", points.get(point));
            arrayNode.add(objectNode);
        }
        setData(arrayNode.toString());
        clustersCnt = points.size();
    }
}
