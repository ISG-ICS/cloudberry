package controllers;

import clustering.IKmeans;
import clustering.Kmeans;
import clustering.Clustering;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edgeBundling.ForceBundling;
import models.*;
import play.mvc.*;
import actors.WebSocketActor;
import treeCut.TreeCut;
import utils.DatabaseUtils;
import utils.PropertiesUtil;

import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.util.*;

/**
 * This controller contains an action to handle HTTP requests to the
 * application's home page.
 */

public class GraphController extends Controller {

    // Indicates the sending process is completed
    private static final String finished = "Y";
    // Indicates the sending process is not completed
    private static final String unfinished = "N";
    // hierarchical structure for HGC algorithm
    private Clustering clustering = new Clustering(0, 17);
    private IKmeans iKmeans;
    private Kmeans kmeans;
    // Incremental edge data
    private Set<Edge> edgeSet = new HashSet<>();
    private WebSocketActor webSocketActor;
    private ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode dataNode;
    Parser parser = new Parser();
    // Size of resultSet
    private int resultSetSize = 0;

    /**
     * Dispatcher for the request message.
     *
     * @param query received query message
     * @param actor WebSocket actor to return response.
     */
    public void dispatcher(String query, WebSocketActor actor) {
        webSocketActor = actor;
        // Heartbeat package handler
        // WebSocket will automatically close after several seconds
        // To keep the state, maintain WebSocket connection is a must
        if (query.equals("")) {
            return;
        }
        dataNode = objectMapper.createObjectNode();
        // Parse the request message with JSON structure
        JsonNode jsonNode = null;
        // Option indicates the request type
        // 1: incremental data query
        // 2: point and cluster
        // 3: edge and bundled edge and tree cut
        // others: invalid
        int option = -1;
        String endDate = null;
        try {
            jsonNode = objectMapper.readTree(query);
            option = Integer.parseInt(jsonNode.get("option").asText());
            if (jsonNode.has("date")) {
                endDate = jsonNode.get("date").asText();
            }
        } catch (IOException e) {
            System.err.println("Invalid Request received.");
            e.printStackTrace();
        }
        double lowerLongitude = 0;
        double upperLongitude = 0;
        double lowerLatitude = 0;
        double upperLatitude = 0;
        int clustering = 0;
        int clusteringAlgorithm = 0;
        int bundling = 0;
        String timestamp = null;
        int zoom = 0;
        int treeCutting = 0;
        try {
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

        switch (option) {
            case 0:
                IncrementalQuery incrementalQuery = new IncrementalQuery();
                incrementalQuery.readProperties(endDate);
                try {
                    doIncrementalQuery(query, incrementalQuery.getStart(), incrementalQuery.getEnd());
                } catch (SQLException | ParseException e) {
                    e.printStackTrace();
                }
                break;
            case 1:
                cluster(lowerLongitude, upperLongitude, lowerLatitude, upperLatitude, clustering, clusteringAlgorithm, timestamp, zoom);
                break;
            case 2:
                edgeCluster(lowerLongitude, upperLongitude, lowerLatitude, upperLatitude, clusteringAlgorithm, timestamp, zoom, bundling, clustering, treeCutting);
                break;
            default:
                System.err.println("Internal error: no option included");
                break;
        }
    }

    //TODO change to doQuery
    private void doIncrementalQuery(String query, String start, String end)
            throws SQLException, ParseException {
        Connection conn = DatabaseUtils.getConnection();
        JsonNode jsonNode;
        int clusteringAlgorithm = -1;
        String timestamp = null;
        try {
            jsonNode = objectMapper.readTree(query);
            clusteringAlgorithm = Integer.parseInt(jsonNode.get("clusteringAlgorithm").asText());
            timestamp = jsonNode.get("timestamp").asText();
            query = jsonNode.get("query").asText();
        } catch (IOException e) {
            e.printStackTrace();
        }
        PreparedStatement state = DatabaseUtils.prepareStatement(query, conn, end, start);
        ResultSet resultSet = null;
        try {
            resultSet = state.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        bindFields(timestamp, end);
        loadCluster(query, clusteringAlgorithm, timestamp, conn, state, resultSet);
    }

    private void loadCluster(String query, int clusteringAlgorithm, String timestamp,
                             Connection conn, PreparedStatement state, ResultSet resultSet) throws ParseException, SQLException {
        String start;
        String end;
        if (clusteringAlgorithm == 0) {
            loadHGC(resultSet);
        } else if (clusteringAlgorithm == 1) {
            loadIKmeans(resultSet);
        } else if (clusteringAlgorithm == 2) {
            start = PropertiesUtil.firstDate;
            end = PropertiesUtil.lastDate;
            bindFields(timestamp, end);
            state = DatabaseUtils.prepareStatement(query, conn, end, start);
            resultSet = state.executeQuery();
            loadKmeans(resultSet);
        }
        dataNode.put("option", 0);
        webSocketActor.returnData(dataNode.toString());
        resultSet.close();
        state.close();
    }

    private List<double[]> getKmeansData(ResultSet resultSet) throws SQLException {
        List<double[]> data = new ArrayList<>();
        while (resultSet.next()) {
            resultSetSize++;
            double fromLongitude = resultSet.getDouble("from_longitude");
            double fromLatitude = resultSet.getDouble("from_latitude");
            double toLongitude = resultSet.getDouble("to_longitude");
            double toLatitude = resultSet.getDouble("to_latitude");
            Edge currentEdge = new Edge(fromLongitude, fromLatitude, toLongitude, toLatitude);
            if (edgeSet.contains(currentEdge)) continue;
            data.add(new double[]{fromLongitude, fromLatitude});
            data.add(new double[]{toLongitude, toLatitude});
            edgeSet.add(currentEdge);
        }
        return data;
    }

    private void loadKmeans(ResultSet resultSet) throws SQLException {
        List<double[]> data = getKmeansData(resultSet);
        if (kmeans == null) {
            kmeans = new Kmeans(17);
        }
        kmeans.execute(data);
    }

    private void loadIKmeans(ResultSet resultSet) throws SQLException {
        List<double[]> data = getKmeansData(resultSet);
        if (iKmeans == null) {
            iKmeans = new IKmeans(17);
            iKmeans.setDataSet(data);
            iKmeans.init();
        }
        iKmeans.execute(data);
    }

    private void loadHGC(ResultSet resultSet) throws SQLException {
        ArrayList<Cluster> points = new ArrayList<>();
        while (resultSet.next()) {
            resultSetSize++;
            double fromLongitude = resultSet.getDouble("from_longitude");
            double fromLatitude = resultSet.getDouble("from_latitude");
            double toLongitude = resultSet.getDouble("to_longitude");
            double toLatitude = resultSet.getDouble("to_latitude");
            Edge currentEdge = new Edge(fromLongitude, fromLatitude, toLongitude, toLatitude);
            if (edgeSet.contains(currentEdge)) continue;
            points.add(new Cluster(fromLongitude, fromLatitude));
            points.add(new Cluster(toLongitude, toLatitude));
            edgeSet.add(currentEdge);
        }
        clustering.load(points);
    }

    private static Calendar getCalendar(String date) {
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(PropertiesUtil.dateFormat.parse(date));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return calendar;
    }

    private void bindFields(String timestamp, String end) {
        dataNode.put("date", end);
        dataNode.put("timestamp", timestamp);
        Calendar endCalendar = getCalendar(end);
        if (!endCalendar.before(PropertiesUtil.lastDateCalender)) {
            System.out.println(finished + end);
            dataNode.put("flag", finished);
        } else {
            System.out.println(unfinished + end);
            dataNode.put("flag", unfinished);
        }
    }

    public void cluster(double lowerLongitude, double upperLongitude, double lowerLatitude, double upperLatitude, int clustering, int clusteringAlgorithm, String timestamp, int zoom) {
        int pointsCnt = 0;
        int clustersCnt = 0;
        int repliesCnt = resultSetSize;
        ArrayNode arrayNode = objectMapper.createArrayNode();
        if (clusteringAlgorithm == 0) {
            ArrayList<Cluster> points = this.clustering.getClusters(new double[]{lowerLongitude, lowerLatitude, upperLongitude, upperLatitude}, 18);
            ArrayList<Cluster> clusters = this.clustering.getClusters(new double[]{lowerLongitude, lowerLatitude, upperLongitude, upperLatitude}, zoom);
            pointsCnt = points.size();
            clustersCnt = clusters.size();
            for (Cluster cluster : clusters) {
                ObjectNode objectNode = objectMapper.createObjectNode();
                objectNode.putArray("coordinates").add(Clustering.xLng(cluster.getX())).add(Clustering.yLat(cluster.getY()));
                objectNode.put("size", cluster.getNumPoints());
                arrayNode.add(objectNode);
            }
        } else if (clusteringAlgorithm == 1) {
            if (clustering == 0) {
                pointsCnt = iKmeans.getPointsCnt();
                clustersCnt = pointsCnt;
                for (int i = 0; i < iKmeans.getK(); i++) {
                    for (int j = 0; j < iKmeans.getAllClusters().get(i).size(); j++) {
                        ObjectNode objectNode = objectMapper.createObjectNode();
                        objectNode.putArray("coordinates").add(iKmeans.getAllClusters().get(i).get(j)[0]).add(iKmeans.getAllClusters().get(i).get(j)[1]);
                        objectNode.put("size", 1);
                        arrayNode.add(objectNode);
                    }
                }
            } else {
                pointsCnt = iKmeans.getPointsCnt();
                clustersCnt = iKmeans.getK();
                for (int i = 0; i < iKmeans.getK(); i++) {
                    ObjectNode objectNode = objectMapper.createObjectNode();
                    objectNode.putArray("coordinates").add(iKmeans.getCenters().get(i)[0]).add(iKmeans.getCenters().get(i)[1]);
                    objectNode.put("size", iKmeans.getAllClusters().get(i).size());
                    arrayNode.add(objectNode);
                }
            }
        } else if (clusteringAlgorithm == 2) {
            if (clustering == 0) {
                pointsCnt = kmeans.getDataSetLength();
                clustersCnt = pointsCnt;
                for (int i = 0; i < kmeans.getDataSetLength(); i++) {
                    ObjectNode objectNode = objectMapper.createObjectNode();
                    objectNode.putArray("coordinates").add(kmeans.getDataSet().get(i)[0]).add(kmeans.getDataSet().get(i)[1]);
                    objectNode.put("size", 1);
                    arrayNode.add(objectNode);
                }
            } else {
                pointsCnt = kmeans.getDataSetLength();
                clustersCnt = kmeans.getK();
                for (int i = 0; i < kmeans.getK(); i++) {
                    ObjectNode objectNode = objectMapper.createObjectNode();
                    objectNode.putArray("coordinates").add(kmeans.getCenters().get(i)[0]).add(kmeans.getCenters().get(i)[1]);
                    objectNode.put("size", kmeans.getClusters().get(i).size());
                    arrayNode.add(objectNode);
                }
            }
        }
        dataNode.put("option", 1);
        dataNode.put("data", arrayNode.toString());
        dataNode.put("timestamp", timestamp);
        dataNode.put("repliesCnt", repliesCnt);
        dataNode.put("pointsCnt", pointsCnt);
        dataNode.put("clustersCnt", clustersCnt);
        webSocketActor.returnData(dataNode.toString());
    }

    private void edgeCluster(double lowerLongitude, double upperLongitude, double lowerLatitude, double upperLatitude, int clusteringAlgorithm, String timestamp, int zoom, int bundling, int clustering, int treeCutting) {
        int edgesCnt;
        int repliesCnt = resultSetSize;
        if (clusteringAlgorithm == 0) {
            HashMap<Edge, Integer> edges = new HashMap<>();
            if (clustering == 0) {
                for (Edge edge : edgeSet) {
                    if (edges.containsKey(edge)) {
                        edges.put(edge, edges.get(edge) + 1);
                    } else {
                        edges.put(edge, 1);
                    }
                }
            } else {
                HashSet<Edge> externalEdgeSet = new HashSet<>();
                HashSet<Cluster> externalCluster = new HashSet<>();
                HashSet<Cluster> internalCluster = new HashSet<>();
                generateExternalEdgeSet(lowerLongitude, upperLongitude, lowerLatitude, upperLatitude, zoom, edges, externalEdgeSet, externalCluster, internalCluster);
                TreeCut treeCutInstance = new TreeCut();
                if (treeCutting == 1) {
                    treeCutInstance.treeCut(this.clustering, lowerLongitude, upperLongitude, lowerLatitude, upperLatitude, zoom, edges, externalEdgeSet, externalCluster, internalCluster);
                } else {
                    treeCutInstance.nonTreeCut(this.clustering, zoom, edges, externalEdgeSet);
                }
            }
            edgesCnt = edges.size();
            dataNode.put("edgesCnt", edgesCnt);
            if (bundling == 0) {
                noBundling(edges);
            } else {
                runFDEB(zoom, edges);
            }
        } else if (clusteringAlgorithm == 1) {
            getKmeansEdges(zoom, bundling, clustering, iKmeans.getParents(), iKmeans.getCenters());
        } else if (clusteringAlgorithm == 2) {
            getKmeansEdges(zoom, bundling, clustering, kmeans.getParents(), kmeans.getCenters());
        }
        dataNode.put("repliesCnt", repliesCnt);
        dataNode.put("option", 2);
        dataNode.put("timestamp", timestamp);
        webSocketActor.returnData(dataNode.toString());
    }

    private void generateExternalEdgeSet(double lowerLongitude, double upperLongitude, double lowerLatitude, double upperLatitude, int zoom,
                                         HashMap<Edge, Integer> edges, HashSet<Edge> externalEdgeSet,
                                         HashSet<Cluster> externalCluster, HashSet<Cluster> internalCluster) {
        for (Edge edge : edgeSet) {
            Cluster fromCluster = clustering.parentCluster(new Cluster(Clustering.lngX(edge.getFromX()), Clustering.latY(edge.getFromY())), zoom);
            Cluster toCluster = clustering.parentCluster(new Cluster(Clustering.lngX(edge.getToX()), Clustering.latY(edge.getToY())), zoom);
            double fromLongitude = Clustering.xLng(fromCluster.getX());
            double fromLatitude = Clustering.yLat(fromCluster.getY());
            double toLongitude = Clustering.xLng(toCluster.getX());
            double toLatitude = Clustering.yLat(toCluster.getY());
            boolean fromWithinRange = lowerLongitude <= fromLongitude && fromLongitude <= upperLongitude
                    && lowerLatitude <= fromLatitude && fromLatitude <= upperLatitude;
            boolean toWithinRange = lowerLongitude <= toLongitude && toLongitude <= upperLongitude
                    && lowerLatitude <= toLatitude && toLatitude <= upperLatitude;
            if (fromWithinRange && toWithinRange) {
                Edge e = new Edge(fromLongitude, fromLatitude, toLongitude, toLatitude);
                if (edges.containsKey(e)) {
                    edges.put(e, edges.get(e) + 1);
                } else {
                    edges.put(e, 1);
                }
                internalCluster.add(fromCluster);
                internalCluster.add(toCluster);
            } else if (fromWithinRange || toWithinRange) {
                if (fromWithinRange) {
                    externalCluster.add(toCluster);
                } else {
                    externalCluster.add(fromCluster);
                }
                externalEdgeSet.add(edge);
            }
        }
    }


    private void getKmeansEdges(int zoom, int bundling, int clustering, HashMap<models.Point, Integer> parents, ArrayList<double[]> center) {
        HashMap<Edge, Integer> edges = new HashMap<>();
        if (clustering == 0) {
            for (Edge edge : edgeSet) {
                if (edges.containsKey(edge)) {
                    edges.put(edge, edges.get(edge) + 1);
                } else {
                    edges.put(edge, 1);
                }
            }
        } else {
            for (Edge edge : edgeSet) {
                double fromLongitude = edge.getFromX();
                double fromLatitude = edge.getFromY();
                double toLongitude = edge.getToX();
                double toLatitude = edge.getToY();
                models.Point fromPoint = new models.Point(fromLongitude, fromLatitude);
                models.Point toPoint = new models.Point(toLongitude, toLatitude);
                int fromCluster = parents.get(fromPoint);
                int toCluster = parents.get(toPoint);
                Edge e = new Edge(center.get(fromCluster)[0], center.get(fromCluster)[1],
                        center.get(toCluster)[0], center.get(toCluster)[1]
                );
                if (edges.containsKey(e)) {
                    edges.put(e, edges.get(e) + 1);
                } else {
                    edges.put(e, 1);
                }
            }
        }
        dataNode.put("edgesCnt", edges.size());
        if (bundling == 0) {
            noBundling(edges);
        } else {
            runFDEB(zoom, edges);
        }
    }

    private void runFDEB(int zoom, HashMap<Edge, Integer> edges) {
        ArrayList<Point> dataNodes = new ArrayList<>();
        ArrayList<EdgeVector> dataEdges = new ArrayList<>();
        ArrayList<Integer> closeEdgeList = new ArrayList<>();
        for (Map.Entry<Edge, Integer> entry : edges.entrySet()) {
            Edge edge = entry.getKey();
            if (Math.pow(edge.length(), 2) <= 0.001)
                continue;
            dataNodes.add(new Point(edge.getFromX(), edge.getFromY()));
            dataNodes.add(new Point(edge.getToX(), edge.getToY()));
            dataEdges.add(new EdgeVector(dataNodes.size() - 2, dataNodes.size() - 1));
            closeEdgeList.add(entry.getValue());
        }
        ForceBundling forceBundling = new ForceBundling(dataNodes, dataEdges);
        forceBundling.setS(zoom);
        ArrayList<Path> pathResult = forceBundling.forceBundle();
        int isolatedEdgesCnt = forceBundling.getIsolatedEdgesCnt();
        ArrayNode pathJson = objectMapper.createArrayNode();
        int edgeNum = 0;
        for (Path path : pathResult) {
            for (int j = 0; j < path.getPath().size() - 1; j++) {
                ObjectNode lineNode = objectMapper.createObjectNode();
                ArrayNode fromArray = objectMapper.createArrayNode();
                fromArray.add(path.getPath().get(j).getX());
                fromArray.add(path.getPath().get(j).getY());
                ArrayNode toArray = objectMapper.createArrayNode();
                toArray.add(path.getPath().get(j + 1).getX());
                toArray.add(path.getPath().get(j + 1).getY());
                lineNode.putArray("from").addAll(fromArray);
                lineNode.putArray("to").addAll(toArray);
                lineNode.put("width", closeEdgeList.get(edgeNum));
                pathJson.add(lineNode);
            }
            edgeNum++;
        }
        dataNode.put("data", pathJson.toString());
        dataNode.put("isolatedEdgesCnt", isolatedEdgesCnt);
    }

    private void noBundling(HashMap<Edge, Integer> edges) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (Map.Entry<Edge, Integer> entry : edges.entrySet()) {
            ObjectNode lineNode = objectMapper.createObjectNode();
            lineNode.putArray("from").add(entry.getKey().getFromX()).add(entry.getKey().getFromY());
            lineNode.putArray("to").add(entry.getKey().getToX()).add(entry.getKey().getToY());
            lineNode.put("width", entry.getValue());
            arrayNode.add(lineNode);
        }
        dataNode.put("data", arrayNode.toString());
        dataNode.put("isolatedEdgesCnt", edges.size());
    }
}
