package controllers;

import play.mvc.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;

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


    public Result index(String query) {
        long startIndex = System.currentTimeMillis();
        ArrayNode replies = Json.newArray();
        try {
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/graphtweet",
                            "graphuser", "graphuser");
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery("select " +
                    "from_longitude, from_latitude, " +
                    "to_longitude, to_latitude " +
                    "from replytweets where " +
                    "to_tsvector('english', from_text) " +
                    "@@ to_tsquery('"+query+"') or " +
                    "to_tsvector('english', to_text) " +
                    "@@ to_tsquery('"+query+"');");
            long startParse = System.currentTimeMillis();

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
                replies.add(reply);
            }
            long endIndex = System.currentTimeMillis();

            resultSet.close();
            statement.close();
            conn.close();
            System.out.print("Total time in parsing data from Json is " + (endIndex - startParse) + " ms\n" );
            System.out.print("Total time in executing query in database is " + (startParse - startIndex) + " ms\n" );
            System.out.print("Total time in running index() is " + (endIndex - startIndex) + " ms\n" );
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