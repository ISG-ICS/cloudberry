package controllers;

import clustering.IKmeans;
import clustering.Kmeans;
import clustering.Clustering;
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
    private ObjectMapper objectMapper = new ObjectMapper();
    private ObjectNode dataNode;
    private Parser parser = new Parser();
    // Size of resultSet
    private int resultSetSize = 0;

    /**
     * Dispatcher for the request message.
     *
     * @param query received query message
     * @param actor WebSocket actor to return response.
     */
    public void dispatcher(String query, WebSocketActor actor) {
        // Heartbeat package handler
        // WebSocket will automatically close after several seconds
        // To keep the state, maintain WebSocket connection is a must
        if (query.equals("")) {
            return;
        }
        dataNode = objectMapper.createObjectNode();
        // Parse the request message with JSON structure
        parser.parse(query);
        // Option indicates the request type
        // 1: incremental data query
        // 2: point and drawPoints
        // 3: edge and bundled edge and tree cut
        // others: invalid
        switch (parser.getOption()) {
            case 0:
                IncrementalQuery incrementalQuery = new IncrementalQuery();
                incrementalQuery.readProperties(parser.getEndDate());
                try {
                    doQuery(incrementalQuery.getStart(), incrementalQuery.getEnd());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;
            case 1:
                drawPoints();
                break;
            case 2:
                drawEdges();
                break;
            default:
                System.err.println("Internal error: no option included");
                break;
        }
        dataNode.put("option", parser.getOption());
        dataNode.put("timestamp", parser.getTimestamp());
        actor.returnData(dataNode.toString());
    }

    private void doQuery(String start, String end) throws SQLException {
        Connection conn = DatabaseUtils.getConnection();
        PreparedStatement state = DatabaseUtils.prepareStatement(parser.getQuery(), conn, start, end);
        ResultSet resultSet = null;
        try {
            resultSet = state.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        bindFields(end);
        if (resultSet != null) {
            if (parser.getClusteringAlgorithm() == 0) {
                loadHGC(resultSet);
            } else if (parser.getClusteringAlgorithm() == 1) {
                loadIKmeans(resultSet);
            } else if (parser.getClusteringAlgorithm() == 2) {
                bindFields(PropertiesUtil.lastDate);
                state = DatabaseUtils.prepareStatement(parser.getQuery(), conn, PropertiesUtil.firstDate, PropertiesUtil.lastDate);
                resultSet = state.executeQuery();
                loadKmeans(resultSet);
            }
            resultSet.close();
        }
        state.close();
    }

    private List<Point> getKmeansData(ResultSet resultSet) throws SQLException {
        List<Point> points = new ArrayList<>();
        while (resultSet.next()) {
            resultSetSize++;
            double fromLongitude = resultSet.getDouble("from_longitude");
            double fromLatitude = resultSet.getDouble("from_latitude");
            double toLongitude = resultSet.getDouble("to_longitude");
            double toLatitude = resultSet.getDouble("to_latitude");
            Edge currentEdge = new Edge(fromLongitude, fromLatitude, toLongitude, toLatitude);
            if (edgeSet.contains(currentEdge)) continue;
            points.add(new Point(fromLongitude, fromLatitude));
            points.add(new Point(toLongitude, toLatitude));
            edgeSet.add(currentEdge);
        }
        return points;
    }

    private void loadKmeans(ResultSet resultSet) throws SQLException {
        List<Point> points = getKmeansData(resultSet);
        if (kmeans == null) {
            kmeans = new Kmeans(17);
        }
        kmeans.execute(points);
    }

    private void loadIKmeans(ResultSet resultSet) throws SQLException {
        List<Point> points = getKmeansData(resultSet);
        if (iKmeans == null) {
            iKmeans = new IKmeans(17);
            iKmeans.setDataSet(points);
            iKmeans.updateK();
            if (iKmeans.getDataSetLength() != 0) {
                iKmeans.init();
            }
        }
        iKmeans.execute(points);
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

    private void bindFields(String end) {
        dataNode.put("date", end);
        Calendar endCalendar = getCalendar(end);
        if (!endCalendar.before(PropertiesUtil.lastDateCalender)) {
            System.out.println(finished + end);
            dataNode.put("flag", finished);
        } else {
            System.out.println(unfinished + end);
            dataNode.put("flag", unfinished);
        }
    }

    private void drawPoints() {
        int pointsCnt;
        int clustersCnt;
        ArrayNode arrayNode = objectMapper.createArrayNode();
        if (parser.getClusteringAlgorithm() == 0) {
            ArrayList<Cluster> points = this.clustering.getClusters(new double[]{parser.getLowerLongitude(), parser.getLowerLatitude(), parser.getUpperLongitude(), parser.getUpperLatitude()}, 18);
            ArrayList<Cluster> clusters = this.clustering.getClusters(new double[]{parser.getLowerLongitude(), parser.getLowerLatitude(), parser.getUpperLongitude(), parser.getUpperLatitude()}, parser.getZoom());
            pointsCnt = points.size();
            clustersCnt = clusters.size();
            for (Cluster cluster : clusters) {
                ObjectNode objectNode = objectMapper.createObjectNode();
                objectNode.putArray("coordinates").add(Clustering.xLng(cluster.getX())).add(Clustering.yLat(cluster.getY()));
                objectNode.put("size", cluster.getNumPoints());
                arrayNode.add(objectNode);
            }
        } else if (parser.getClusteringAlgorithm() == 1) {
            if (parser.getClustering() == 0) {
                pointsCnt = iKmeans.getPointsCnt();
                clustersCnt = pointsCnt;
                for (int i = 0; i < iKmeans.getK(); i++) {
                    for (int j = 0; j < iKmeans.getAllClusters().get(i).size(); j++) {
                        ObjectNode objectNode = objectMapper.createObjectNode();
                        objectNode.putArray("coordinates").add(iKmeans.getAllClusters().get(i).get(j).getX()).add(iKmeans.getAllClusters().get(i).get(j).getY());
                        objectNode.put("size", 1);
                        arrayNode.add(objectNode);
                    }
                }
            } else {
                pointsCnt = iKmeans.getPointsCnt();
                clustersCnt = iKmeans.getK();
                for (int i = 0; i < iKmeans.getK(); i++) {
                    ObjectNode objectNode = objectMapper.createObjectNode();
                    objectNode.putArray("coordinates").add(iKmeans.getCenters().get(i).getX()).add(iKmeans.getCenters().get(i).getY());
                    objectNode.put("size", iKmeans.getAllClusters().get(i).size());
                    arrayNode.add(objectNode);
                }
            }
        } else {
            if (parser.getClustering() == 0) {
                pointsCnt = kmeans.getDataSetLength();
                clustersCnt = pointsCnt;
                for (int i = 0; i < kmeans.getDataSetLength(); i++) {
                    ObjectNode objectNode = objectMapper.createObjectNode();
                    objectNode.putArray("coordinates").add(kmeans.getDataSet().get(i).getX()).add(kmeans.getDataSet().get(i).getY());
                    objectNode.put("size", 1);
                    arrayNode.add(objectNode);
                }
            } else {
                pointsCnt = kmeans.getDataSetLength();
                clustersCnt = kmeans.getK();
                for (int i = 0; i < kmeans.getK(); i++) {
                    ObjectNode objectNode = objectMapper.createObjectNode();
                    objectNode.putArray("coordinates").add(kmeans.getCenters().get(i).getX()).add(kmeans.getCenters().get(i).getY());
                    objectNode.put("size", kmeans.getClusters().get(i).size());
                    arrayNode.add(objectNode);
                }
            }
        }
        dataNode.put("data", arrayNode.toString());
        dataNode.put("repliesCnt", resultSetSize);
        dataNode.put("pointsCnt", pointsCnt);
        dataNode.put("clustersCnt", clustersCnt);
    }

    private void drawEdges() {
        int edgesCnt;
        int repliesCnt = resultSetSize;
        if (parser.getClusteringAlgorithm() == 0) {
            HashMap<Edge, Integer> edges = new HashMap<>();
            if (parser.getClustering() == 0) {
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
                generateExternalEdgeSet(parser.getLowerLongitude(), parser.getLowerLatitude(), parser.getUpperLongitude(), parser.getUpperLatitude(), parser.getZoom(), edges, externalEdgeSet, externalCluster, internalCluster);
                TreeCut treeCutInstance = new TreeCut();
                System.out.println(parser.getTreeCutting());
                if (parser.getTreeCutting() == 1) {
                    treeCutInstance.treeCut(this.clustering, parser.getLowerLongitude(), parser.getLowerLatitude(), parser.getUpperLongitude(), parser.getUpperLatitude(), parser.getZoom(), edges, externalEdgeSet, externalCluster, internalCluster);
                } else {
                    treeCutInstance.nonTreeCut(this.clustering, parser.getZoom(), edges, externalEdgeSet);
                }
            }
            edgesCnt = edges.size();
            dataNode.put("edgesCnt", edgesCnt);
            if (parser.getBundling() == 0) {
                noBundling(edges);
            } else {
                runFDEB(parser.getZoom(), edges);
            }
        } else if (parser.getClusteringAlgorithm() == 1) {
            getKmeansEdges(parser.getZoom(), parser.getBundling(), parser.getBundling(), iKmeans.getParents(), iKmeans.getCenters());
        } else {
            getKmeansEdges(parser.getZoom(), parser.getBundling(), parser.getClustering(), kmeans.getParents(), kmeans.getCenters());
        }
        dataNode.put("repliesCnt", repliesCnt);
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


    private void getKmeansEdges(int zoom, int bundling, int clustering, HashMap<models.Point, Integer> parents, ArrayList<Point> center) {
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
                Edge e = new Edge(center.get(fromCluster).getX(), center.get(fromCluster).getY(),
                        center.get(toCluster).getX(), center.get(toCluster).getY()
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
