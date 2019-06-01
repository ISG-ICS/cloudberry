package controllers;

import play.mvc.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.HashSet;
import java.util.Set;
import java.sql.PreparedStatement;

import play.libs.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */

public class GraphController extends Controller {

    /**
     * An action that renders an HTML page with a welcome message.
     * The configuration in the <code>routes</code> file means that
     * this method will be called when the application receives a
     * <code>GET</code> request with a path of <code>/</code>.
     */


    public Result index(String query, double lowerLongitude, double upperLongitude, double lowerLatitude, double upperLatitude) {

        long startIndex = System.currentTimeMillis();

        ArrayNode replies = Json.newArray();
        try {
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/graphtweet",
                            "graphuser", "graphuser");
            String searchQuery = "select " +
                    "from_longitude, from_latitude, " +
                    "to_longitude, to_latitude " +
                    "from replytweets where " +
                    "to_tsvector('english', from_text) " +
                    "@@ to_tsquery( ? ) or " +
                    "to_tsvector('english', to_text) " +
                    "@@ to_tsquery( ? ) and ((from_longitude >= ? AND from_longitude <= ? AND from_latitude >= ? AND from_latitude <= ?) OR" +
                    " (to_longitude >= ? AND to_longitude <= ? AND to_latitude >= ? AND to_latitude <= ?));";
            
            PreparedStatement state = conn.prepareStatement(searchQuery);
            state.setString(1, query);
            state.setString(2, query);
            state.setDouble(3, lowerLongitude);
            state.setDouble(4, upperLongitude);
            state.setDouble(5,lowerLatitude);
            state.setDouble(6, upperLatitude);
            state.setDouble(7, lowerLongitude);
            state.setDouble(8, upperLongitude);
            state.setDouble(9,lowerLatitude);
            state.setDouble(10, upperLatitude);
            ResultSet resultSet = state.executeQuery();

            
            long startParse = System.currentTimeMillis();

            //HashSet for deduplicating same edge
            Set<Edge> edges = new HashSet<>();
            int numOfSameEdge = 0;
            while (resultSet.next()) {
                   
                ObjectNode reply = Json.newObject();
                ArrayNode fromCoordinate = Json.newArray();

                //Get all the coords
                double fromLongtitude = resultSet.getDouble("from_longitude");
                double fromLatitude = resultSet.getDouble("from_latitude");
                double toLongtitude = resultSet.getDouble("to_longitude");
                double toLatitude = resultSet.getFloat("to_latitude");

                //Create Edge and add it if not seen
                Edge currentEdge = new Edge(fromLatitude, fromLongtitude, toLatitude, toLongtitude);
                if (edges.contains(currentEdge)){
                    numOfSameEdge+=1;
                } 
                else {
                    edges.add(currentEdge);

                    fromCoordinate.add(fromLongtitude);
                    fromCoordinate.add(fromLatitude);
                    reply.set("source", fromCoordinate);
                    ArrayNode toCoordinate = Json.newArray();
                    toCoordinate.add(toLongtitude);
                    toCoordinate.add(toLatitude);
                    reply.set("target", toCoordinate);
                    replies.add(reply);
                }
            }
            long endIndex = System.currentTimeMillis();

            resultSet.close();
            state.close();
            conn.close();

            System.out.print("Total time in parsing data from Json is " + (endIndex - startParse) + " ms\n" );
            System.out.print("Total time in executing query in database is " + (startParse - startIndex) + " ms\n" );
            System.out.print("Total time in running index() is " + (endIndex - startIndex) + " ms\n" );
            System.out.print("Total number of Same Edge that are not added to Json is "+String.valueOf(numOfSameEdge));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return ok(Json.toJson(replies));
    }

    public Result getByLocation(float lowerLongitude, float upperLongitude, float lowerLatitude, float upperLatitude){
        ArrayNode result = Json.newArray();
        try{
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/graphtweet",
                            "graphuser", "graphuser");

            String query = "SELECT from_longitude, from_latitude, to_longitude, to_latitude FROM replytweets WHERE"
                    + " (from_longitude >= ? AND from_longitude <= ? AND from_latitude >= ? AND from_latitude <= ?) OR" +
                    "   (to_longitude >= ? AND to_longitude <= ? AND to_latitude >= ? AND to_latitude <= ?) ;";
            PreparedStatement state = conn.prepareStatement(query);
            state.setFloat(1, lowerLongitude);
            state.setFloat(2, upperLongitude);
            state.setFloat(3,lowerLatitude);
            state.setFloat(4, upperLatitude);
            state.setFloat(5, lowerLongitude);
            state.setFloat(6, upperLongitude);
            state.setFloat(7,lowerLatitude);
            state.setFloat(8, upperLatitude);
            ResultSet resultSet = state.executeQuery();
            while (resultSet.next()) {
                ObjectNode reply = Json.newObject();
                ArrayNode fromCoordinate = Json.newArray();
                fromCoordinate.add(resultSet.getFloat("from_longitude"));
                fromCoordinate.add(resultSet.getFloat("from_latitude"));
                reply.set("source", fromCoordinate);
                ArrayNode toCoordinate = Json.newArray();
                toCoordinate.add(resultSet.getFloat("to_longitude"));
                toCoordinate.add(resultSet.getFloat("to_latitude"));
                reply.set("target", toCoordinate);
                result.add(reply);
            }

            resultSet.close();
            state.close();
            conn.close();

        }
        catch ( Exception e ) {
            System.out.println(e.getMessage());
        }
        return ok(Json.toJson(result));
    }
}