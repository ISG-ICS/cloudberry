package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import play.mvc.*;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

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

    public Result index(String query, double lowerLongitude, double upperLongitude, double lowerLatitude,
            double upperLatitude) {
        String json = "";
        long startIndex = System.currentTimeMillis();
        try {
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/graphtweet", "graphuser",
                    "graphuser");
            String searchQuery = "select " + "from_longitude, from_latitude, " + "to_longitude, to_latitude "
                    + "from replytweets where (" + "to_tsvector('english', from_text) " + "@@ to_tsquery( ? ) or "
                    + "to_tsvector('english', to_text) "
                    + "@@ to_tsquery( ? )) and ((from_longitude between ? AND ? AND from_latitude between ? AND ?) OR"
                    + " (to_longitude between ? AND ? AND to_latitude between ? AND ?));";

            PreparedStatement state = conn.prepareStatement(searchQuery);
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
            ResultSet resultSet = state.executeQuery();
            Set<Edge> edges = new HashSet<>();
            // int numOfSameEdge = 0;
            long startParse = System.currentTimeMillis();
            while (resultSet.next()) {
                // Get all the coords
                double fromLongtitude = resultSet.getDouble("from_longitude");
                double fromLatitude = resultSet.getDouble("from_latitude");
                double toLongtitude = resultSet.getDouble("to_longitude");
                double toLatitude = resultSet.getFloat("to_latitude");

                // Create Edge and add it if not seen
                Edge currentEdge = new Edge(fromLatitude, fromLongtitude, toLatitude, toLongtitude);
                // if (edges.contains(currentEdge)) {
                // numOfSameEdge += 1;
                // } else {
                edges.add(currentEdge);
                // }
            }
            ObjectMapper objectMapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addSerializer(Edge.class, new EdgeSerializer());
            objectMapper.registerModule(module);
            json = objectMapper.writeValueAsString(edges);
            long endIndex = System.currentTimeMillis();
            resultSet.close();
            state.close();
            conn.close();
            System.out.println("Total time in parsing data from Json is " + (endIndex - startParse) + " ms");
            System.out.println("Total time in executing query in database is " + (startParse - startIndex) + " ms");
            System.out.println("Total time in running index() is " + (endIndex - startIndex) + " ms");
            // System.out.println("Total number of records " + edges.size());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return ok(json);
    }
}