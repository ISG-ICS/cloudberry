package controllers;

import Utils.PropertiesUtil;
import actors.BundleActor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.mvc.*;

import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This controller contains an action to handle HTTP requests to the
 * application's home page.
 */

public class GraphController extends Controller {

    /**
     * An action that renders an HTML page with a welcome message. The configuration
     * in the <code>routes</code> file means that this method will be called when
     * the application receives a <code>GET</code> request with a path of
     * <code>/</code>.
     */

//    private Hashtable<Integer, Edge> edges;
    private ArrayList<Edge> allEdges;
    // configuration properties
    private Properties configProps;
    // indicates the sending process is completed
    private final String finished = "Y";
    // indicates the sending process is not completed
    private final String unfinished = "N";
    // indicates the sending process is changed to real incremental edge bundling
    private final String afterEdgeLimit = "C";
    // the object that contains previous return information
    private BundlingAlgorithmReturn bar;
    private ArrayList<Edge> centerEdges;
    final int edgeLimit = 1000;
    private boolean beforeEdgeLimit = true;

    public void getData(String query, BundleActor bundleActor) {
        // parse the json and initialize the variables
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(query);
        } catch (IOException e) {
            e.printStackTrace();
        }
        double lowerLongitude = Double.parseDouble(jsonNode.get("lowerLongitude").asText());
        double upperLongitude = Double.parseDouble(jsonNode.get("upperLongitude").asText());
        double lowerLatitude = Double.parseDouble(jsonNode.get("lowerLatitude").asText());
        double upperLatitude = Double.parseDouble(jsonNode.get("upperLatitude").asText());
        String timestamp = jsonNode.get("timestamp").asText();
        query = jsonNode.get("query").asText();
        String endDate = null;
        if (!jsonNode.has("date")) {
//            edges = new Hashtable<>();
            allEdges = new ArrayList<>();
        } else {
            endDate = jsonNode.get("date").asText();
            if (!beforeEdgeLimit) {
//                edges = new Hashtable<>();
                allEdges = new ArrayList<>();
            }
        }
        String json = "";
        long startReply = System.currentTimeMillis();
        ObjectNode objectNode = objectMapper.createObjectNode();
        try {
            Edge.set_epsilon(9);
            objectNode = queryResult(query, endDate, lowerLongitude, upperLongitude, lowerLatitude, upperLatitude, timestamp, objectNode);
            long startBundling = System.currentTimeMillis();
            ArrayList<Vector> alldataNodes = new ArrayList<>();
            ArrayList<EdgeVector> alldataEdges = new ArrayList<>();
            ArrayList<Integer> weights = new ArrayList<>();
            if (beforeEdgeLimit) {
                loadListBeforeEdgeLimit(alldataNodes, alldataEdges, weights);
            } else {
                loadListAfterEdgeLimit(alldataNodes, alldataEdges, weights);
            }
            ArrayList<Path> pathResult = runBundling(alldataNodes, alldataEdges);
            objectMapper = new ObjectMapper();
            ArrayNode pathJson = objectMapper.createArrayNode();
            long startParse = System.currentTimeMillis();
            formatPath(objectMapper, weights, pathResult, pathJson);
            objectNode.set("data", pathJson);
            json = objectNode.toString();
            long endReply = System.currentTimeMillis();
            System.out.println("Total time in parsing data from Json is " + (endReply - startParse) + " ms");
            System.out.println("Total time in executing query in database is " + (startBundling - startReply) + " ms");
            System.out.println("Total time in edge bundling is " + (startParse - startBundling) + " ms");
            System.out.println("Total time in running replay() is " + (endReply - startReply) + " ms");
//            System.out.println("Total number of records " + edges.size());
            System.out.println("Total number of edges " + alldataEdges.size());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        bundleActor.returnData(json);
    }

    private ArrayList<Path> runBundling(ArrayList<Vector> alldataNodes, ArrayList<EdgeVector> alldataEdges) {
        ForceBundlingReturn fbr = (ForceBundlingReturn) bar;
        int prevLength = 0;
        BundlingAlgorithm forceBundling;
        if (bar == null) {
            forceBundling = new ForceBundling(alldataNodes, alldataEdges);
        } else {
            prevLength = fbr.lengths.size();
            // status indicates whether the changing state has happened
            forceBundling = new ForceBundling(alldataNodes, alldataEdges, fbr.lengths, beforeEdgeLimit);
        }
        bar = forceBundling.bundle();
        fbr = (ForceBundlingReturn) bar;
        ArrayList<Path> pathResult = fbr.subdivisionPoints;
        centerEdges = fbr.centerEdges;
        if (beforeEdgeLimit && Math.abs(fbr.lengths.size() - prevLength) / (1.0 * prevLength) < 0.1 && alldataEdges.size() > edgeLimit) {
            beforeEdgeLimit = false;
        }
        System.out.println("prevLength: " + prevLength + " nowLength: " + fbr.lengths.size());
        return pathResult;
    }

    private void formatPath(ObjectMapper objectMapper, ArrayList<Integer> weights, ArrayList<Path> pathResult, ArrayNode pathJson) {
        int edgeNum = 0;
        for (Path path : pathResult) {
            for (int j = 0; j < path.alv.size() - 1; j++) {
                ObjectNode lineNode = objectMapper.createObjectNode();
                ArrayNode fromArray = objectMapper.createArrayNode();
                fromArray.add(path.alv.get(j).x);
                fromArray.add(path.alv.get(j).y);
                ArrayNode toArray = objectMapper.createArrayNode();
                toArray.add(path.alv.get(j + 1).x);
                toArray.add(path.alv.get(j + 1).y);
                lineNode.putArray("from").addAll(fromArray);
                lineNode.putArray("to").addAll(toArray);
                lineNode.put("width", weights.get(edgeNum));
                pathJson.add(lineNode);
            }
            edgeNum++;
        }
    }

    private void loadListBeforeEdgeLimit(ArrayList<Vector> alldataNodes, ArrayList<EdgeVector> alldataEdges, ArrayList<Integer> weights) {
        detailedAddEdge(alldataNodes, alldataEdges, weights, allEdges);
    }

    private void loadListAfterEdgeLimit(ArrayList<Vector> alldataNodes, ArrayList<EdgeVector> alldataEdges, ArrayList<Integer> weights) {
        if (bar != null) {
            detailedAddEdge(alldataNodes, alldataEdges, weights, centerEdges);
        }
        detailedAddEdge(alldataNodes, alldataEdges, weights, allEdges);
    }

    private void detailedAddEdge(ArrayList<Vector> alldataNodes, ArrayList<EdgeVector> alldataEdges, ArrayList<Integer> weights, ArrayList<Edge> centerEdges) {
        for (Edge entry : centerEdges) {
            weights.add(entry.getWeight());
            alldataNodes.add(new Vector(entry.getFromLongitude(), entry.getFromLatitude()));
            alldataNodes.add(new Vector(entry.getToLongitude(), entry.getToLatitude()));
            alldataEdges.add(new EdgeVector(alldataNodes.size() - 2, alldataNodes.size() - 1));
        }
    }

    private Calendar incrementCalendar(int queryPeriod, String date, SimpleDateFormat sdf) throws ParseException {
        Calendar c = Calendar.getInstance();
        c.setTime(sdf.parse(date));
        c.add(Calendar.HOUR, queryPeriod);  // number of days to add
        return c;
    }

    private String getDate(String endDate, String firstDate) {
        String date;
        if (endDate == null) {
            date = firstDate;  // Start date
        } else {
            date = endDate;
        }
        return date;
    }

    private Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection("jdbc:postgresql://localhost:5432/graphtweet", "graphuser",
                "graphuser");
    }

    private ObjectNode queryResult(String query, String endDate, double lowerLongitude, double upperLongitude, double lowerLatitude,
                                   double upperLatitude, String timestamp, ObjectNode objectNode) {
        String firstDate = null;
        String lastDate = null;
        int queryPeriod = 0;

        try {
            configProps = PropertiesUtil.loadProperties(configProps);
            firstDate = configProps.getProperty("firstDate");
            lastDate = configProps.getProperty("lastDate");
            queryPeriod = Integer.parseInt(configProps.getProperty("queryPeriod"));
        } catch (IOException ex) {
            System.out.println("The config.properties file does not exist, default properties loaded.");
        }

        getData(query, endDate, lowerLongitude, upperLongitude, lowerLatitude, upperLatitude, timestamp, objectNode, firstDate, lastDate, queryPeriod);
        return objectNode;
    }

    private void getData(String query, String endDate, double lowerLongitude, double upperLongitude, double lowerLatitude, double upperLatitude, String timestamp, ObjectNode objectNode, String firstDate, String lastDate, int queryPeriod) {
        Connection conn;
        PreparedStatement state;
        ResultSet resultSet;
        try {
            conn = getConnection();

            String date = getDate(endDate, firstDate);
            String start = date;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            Calendar c = incrementCalendar(queryPeriod, date, sdf);
            date = sdf.format(c.getTime());
            Calendar lastDateCalendar = Calendar.getInstance();
            lastDateCalendar.setTime(sdf.parse(lastDate));

            bindFields(objectNode, timestamp, date, c, lastDateCalendar);
            objectNode.put("keyword", query);
            state = prepareState(query, lowerLongitude, upperLongitude, lowerLatitude, upperLatitude, conn, date, start);
            resultSet = state.executeQuery();
            while (resultSet.next()) {
                double fromLongitude = resultSet.getDouble("from_longitude");
                double fromLatitude = resultSet.getDouble("from_latitude");
                double toLongitude = resultSet.getDouble("to_longitude");
                double toLatitude = resultSet.getDouble("to_latitude");

                Edge currentEdge = new Edge(fromLatitude, fromLongitude, toLatitude, toLongitude, 1);
                allEdges.add(currentEdge);
            }
            resultSet.close();
            state.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void bindFields(ObjectNode objectNode, String timestamp, String date, Calendar c, Calendar lastDateCalendar) {
        objectNode.put("date", date);
        objectNode.put("timestamp", timestamp);
        if (!c.before(lastDateCalendar)) {
            System.out.println(finished + date);
            objectNode.put("flag", finished);
            beforeEdgeLimit = true;
        } else if (!beforeEdgeLimit) {
            System.out.println(afterEdgeLimit + date);
            objectNode.put("flag", afterEdgeLimit);
        } else {
            System.out.println(unfinished + date);
            objectNode.put("flag", unfinished);
        }
    }

    private PreparedStatement prepareState(String query, double lowerLongitude, double upperLongitude, double lowerLatitude, double upperLatitude, Connection conn, String date, String start) throws SQLException {
        PreparedStatement state;
        String searchQuery = "select from_longitude, from_latitude, to_longitude, to_latitude "
                + "from replytweets where ( to_tsvector('english', from_text) @@ to_tsquery( ? ) or "
                + "to_tsvector('english', to_text) "
                + "@@ to_tsquery( ? )) and ((from_longitude between ? AND ? AND from_latitude between ? AND ?) OR"
                + " (to_longitude between ? AND ? AND to_latitude between ? AND ?)) AND sqrt(pow(from_latitude - to_latitude, 2) + pow(from_longitude - to_longitude, 2)) >= ? "
                + "AND to_create_at::timestamp > TO_TIMESTAMP( ? , 'yyyymmddhh24miss') "
                + "AND to_create_at::timestamp <= TO_TIMESTAMP( ? , 'yyyymmddhh24miss');";
        state = conn.prepareStatement(searchQuery);
        state.setString(1, query);
        state.setString(2, query);
        state.setDouble(3, lowerLongitude);
        state.setDouble(4, upperLongitude);
        state.setDouble(5, lowerLatitude);
        state.setDouble(6, upperLatitude);
        state.setDouble(7, lowerLongitude);
        state.setDouble(8, upperLongitude);
        state.setDouble(9, lowerLatitude);
        state.setDouble(10, upperLatitude);
        state.setDouble(11, Math.sqrt(Math.pow(upperLongitude - lowerLongitude, 2) + Math.pow(upperLatitude - lowerLatitude, 2)) / 30);
        state.setString(12, start);
        state.setString(13, date);
        return state;
    }


}
