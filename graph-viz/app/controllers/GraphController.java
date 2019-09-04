package controllers;

import clustering.IKmeans;
import clustering.Kmeans;
import clustering.Clustering;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import connection.Parser;
import connection.Response;
import edgeBundling.ForceBundling;
import models.*;
import play.mvc.*;
import actors.WebSocketActor;
import treeCut.TreeCut;
import utils.DatabaseUtils;
import utils.IncrementalQuery;
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
    private Kmeans kmeans;
    // Incremental edge data
    private HashMap<Edge, Integer> totalEdges = new HashMap<>();
    // total raw data points, to minimize the overhead of calculating total points from the clusters
    private List<Point> totalPoints = new ArrayList<>();
    // the batch points
    private List<Point> batchPoints = new ArrayList<>();
    private ObjectMapper objectMapper = new ObjectMapper();

    private Parser parser = new Parser();
    private Response response = new Response();

    private final int K = 17;
    // Size of resultSet

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
        if (query.isEmpty()) {
            return;
        }
        // Parse the request message with JSON structure
        parser.parse(query);
        // Option indicates the request type
        // 1: incremental data query
        // 2: point and cluster
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
        response.setOption(parser.getOption());
        response.setTimestamp(parser.getTimestamp());
        try {
            actor.returnData(objectMapper.writeValueAsString(response));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
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
            loadData(resultSet);
            if (parser.getClusteringAlgorithm() == 0) {
                loadHGC();
            } else if (parser.getClusteringAlgorithm() == 1) {
                if (kmeans == null) kmeans = new IKmeans(K);
                loadKmeans();
            } else if (parser.getClusteringAlgorithm() == 2) {
                if (kmeans == null) kmeans = new Kmeans(K);
                bindFields(PropertiesUtil.lastDate);
                state = DatabaseUtils.prepareStatement(parser.getQuery(), conn, PropertiesUtil.firstDate, PropertiesUtil.lastDate);
                loadData(state.executeQuery());
                loadKmeans();
            }
            resultSet.close();
        }
        state.close();
    }

    private void loadData(ResultSet resultSet) throws SQLException {
        batchPoints.clear();
        while (resultSet.next()) {
            Point from = new Point(resultSet.getDouble("from_longitude"), resultSet.getDouble("from_latitude"));
            Point to = new Point(resultSet.getDouble("to_longitude"), resultSet.getDouble("to_latitude"));
            Edge currentEdge = new Edge(from, to);
            putEdgeIntoMap(totalEdges, currentEdge, 1);
            totalPoints.add(from);
            totalPoints.add(to);
            batchPoints.add(from);
            batchPoints.add(to);
        }
    }

    private void loadKmeans() {
        if (kmeans instanceof IKmeans) {
            kmeans.execute(batchPoints);
        } else {
            kmeans.execute(totalPoints);
        }
    }

    private void loadHGC() {
        clustering.load(batchPoints);
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
        response.setDate(end);
        Calendar endCalendar = getCalendar(end);
        if (!endCalendar.before(PropertiesUtil.lastDateCalender)) {
            response.setFlag(finished);
        } else {
            response.setFlag(unfinished);
        }
    }

    /**
     * draw points
     */
    private void drawPoints() {
        HashMap<Point, Integer> pointsMap = new HashMap<>();
        if (parser.getClustering() == 0) {
            for (Point point : totalPoints) {
                putPointIntoMap(pointsMap, point, 1);
            }
        } else {
            if (parser.getClusteringAlgorithm() == 0) {
                ArrayList<Cluster> clusters = this.clustering.getClusters(new double[]{parser.getLowerLongitude(), parser.getLowerLatitude(), parser.getUpperLongitude(), parser.getUpperLatitude()}, parser.getZoom());
                for (Cluster cluster : clusters) {
                    putPointIntoMap(pointsMap, new Point(Clustering.xLng(cluster.getX()), Clustering.yLat(cluster.getY())), cluster.getNumPoints());
                }
            } else {
                pointsMap = kmeans.getClustersMap();
            }
        }
        response.setPoints(pointsMap);
        response.setPointsCnt(totalPoints.size());
        response.setRepliesCnt(getTotalEdgesSize());
    }

    /**
     * draw edges
     */
    private void drawEdges() {
        HashMap<Edge, Integer> edges = new HashMap<>();
        if (parser.getClustering() == 0) {
            edges = totalEdges;
        } else {
            if (parser.getClusteringAlgorithm() == 0) {
                HashSet<Edge> externalEdgeSet = new HashSet<>();
                HashSet<Cluster> externalCluster = new HashSet<>();
                HashSet<Cluster> internalCluster = new HashSet<>();
                generateEdgeSet(edges, externalEdgeSet, externalCluster, internalCluster);
                TreeCut treeCutInstance = new TreeCut();
                if (parser.getTreeCutting() == 1) {
                    treeCutInstance.execute(this.clustering, parser.getLowerLongitude(), parser.getUpperLongitude(), parser.getLowerLatitude(), parser.getUpperLatitude(), parser.getZoom(), edges, externalEdgeSet, externalCluster, internalCluster);
                }
            } else {
                for (Edge edge : totalEdges.keySet()) {
                    Edge e = new Edge(kmeans.getParent(edge.getFromPoint()), kmeans.getParent(edge.getToPoint()));
                    putEdgeIntoMap(edges, e, totalEdges.get(edge));
                }
            }
        }
        response.setEdgesCnt(edges.size());
        if (parser.getBundling() == 0) {
            noBundling(edges);
        } else {
            runFDEB(edges);
        }
        response.setRepliesCnt(getTotalEdgesSize());
    }

    /**
     * get the number of total edges
     * @return the number of total edges
     */
    private int getTotalEdgesSize() {
        int tot = 0;
        for (Edge edge : totalEdges.keySet()) {
            tot += totalEdges.get(edge);
        }
        return tot;
    }

    /**
     * prepares external egde set for tree cut.
     *
     * @param edges           the returning edge set, if tree cut is enabled, it contains only the internal edges.
     *                        Otherwise, it contains all the edges.
     * @param externalEdgeSet the returning external edge set
     * @param externalCluster outside cluster corresponding to edge set with only one node inside screen
     * @param internalCluster inside screen clusters
     */
    private void generateEdgeSet(HashMap<Edge, Integer> edges, HashSet<Edge> externalEdgeSet,
                                 HashSet<Cluster> externalCluster, HashSet<Cluster> internalCluster) {
        for (Edge edge : totalEdges.keySet()) {
            Cluster fromCluster = clustering.parentCluster(new Cluster(new Point(Clustering.lngX(edge.getFromX()), Clustering.latY(edge.getFromY()))), parser.getZoom());
            Cluster toCluster = clustering.parentCluster(new Cluster(new Point(Clustering.lngX(edge.getToX()), Clustering.latY(edge.getToY()))), parser.getZoom());
            double fromLongitude = Clustering.xLng(fromCluster.getX());
            double fromLatitude = Clustering.yLat(fromCluster.getY());
            double toLongitude = Clustering.xLng(toCluster.getX());
            double toLatitude = Clustering.yLat(toCluster.getY());
            boolean fromWithinRange = parser.getLowerLongitude() <= fromLongitude && fromLongitude <= parser.getUpperLongitude()
                    && parser.getLowerLatitude() <= fromLatitude && fromLatitude <= parser.getUpperLatitude();
            boolean toWithinRange = parser.getLowerLongitude() <= toLongitude && toLongitude <= parser.getUpperLongitude()
                    && parser.getLowerLatitude() <= toLatitude && toLatitude <= parser.getUpperLatitude();
            Edge e = new Edge(new Point(fromLongitude, fromLatitude), new Point(toLongitude, toLatitude));
            if (fromWithinRange && toWithinRange) {
                putEdgeIntoMap(edges, e, totalEdges.get(edge));
                internalCluster.add(fromCluster);
                internalCluster.add(toCluster);
            } else if (fromWithinRange || toWithinRange) {
                if (parser.getTreeCutting() == 0) {
                    putEdgeIntoMap(edges, e, totalEdges.get(edge));
                } else {
                    if (fromWithinRange) {
                        externalCluster.add(toCluster);
                    } else {
                        externalCluster.add(fromCluster);
                    }
                    externalEdgeSet.add(edge);
                }

            }
        }
    }

    /**
     * put edge into map
     * @param edges map of edges and width
     * @param edge edge
     * @param weight weight
     */
    private void putEdgeIntoMap(HashMap<Edge, Integer> edges, Edge edge, int weight) {
        if (edges.containsKey(edge)) {
            edges.put(edge, edges.get(edge) + weight);
        } else {
            edges.put(edge, weight);
        }
    }

    /**
     * put point into map
     * @param points map of points and width
     * @param point point
     * @param weight weight
     */
    private void putPointIntoMap(HashMap<Point, Integer> points, Point point, int weight) {
        if (points.containsKey(point)) {
            points.put(point, points.get(point) + weight);
        } else {
            points.put(point, weight);
        }
    }

    /**
     * run FDEB
     * @param edges input edges
     */
    private void runFDEB(HashMap<Edge, Integer> edges) {
        ArrayList<Edge> dataEdges = new ArrayList<>();
        ArrayList<Integer> closeEdgeList = new ArrayList<>();
        for (Map.Entry<Edge, Integer> entry : edges.entrySet()) {
            Edge edge = entry.getKey();
            // remove short edges
            if (Math.pow(edge.length(), 2) <= 0.001)
                continue;
            dataEdges.add(edge);
            closeEdgeList.add(entry.getValue());
        }
        ForceBundling forceBundling = new ForceBundling(dataEdges);
        forceBundling.setS(parser.getZoom());
        ArrayList<Path> pathResult = forceBundling.forceBundle();
        int isolatedEdgesCnt = forceBundling.getIsolatedEdgesCnt();
        HashMap<Edge, Integer> edgesData = new HashMap<>();
        for (int i = 0; i < pathResult.size(); i++) {
            Path path = pathResult.get(i);
            for (int j = 0; j < path.getPath().size() - 1; j++) {
                putEdgeIntoMap(edgesData, new Edge(path.getPath().get(j), path.getPath().get(j + 1)), closeEdgeList.get(i));
            }
        }
        response.setEdges(edgesData);
        response.setIsolatedEdgesCnt(isolatedEdgesCnt);
    }

    /**
     * show edges without bundling
     * @param edges input edges
     */
    private void noBundling(HashMap<Edge, Integer> edges) {
        response.setEdges(edges);
        response.setIsolatedEdgesCnt(edges.size());
    }
}
