package controllers;

import play.mvc.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
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


    public Result index(int numOfReplies) {
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
                    "from replytweets limit " +
                    numOfReplies + ";");
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
            resultSet.close();
            statement.close();
            conn.close();
        }
        catch ( Exception e ) {
            System.out.println(e.getMessage());
        }
        return ok(Json.toJson(replies));
    }
}