import java.sql.*;
import oracle.sql.*;
import oracle.spatial.geometry.JGeometry;

public class Main {



    public static void main(String[] args) {

        try {

            Class.forName("oracle.jdbc.driver.OracleDriver");
            String url = "jdbc:oracle:thin:@localhost:1521:orcl";
            String username = "berry";
            String password = "orcl";
            Connection con = null;
            System.out.println("here");
            con = DriverManager.getConnection(url, username, password);
            Statement statement = con.createStatement();
            System.out.println("here");
            ResultSet rs = statement.executeQuery("SELECT t.\"coordinate\" FROM \"twitter.ds_tweet\" t where \"id\" = 683196034458095616");

            while (rs.next()) {
                STRUCT st = (oracle.sql.STRUCT) rs.getObject(1);
                System.out.println("here");
                System.out.println(st);

                JGeometry j_geom = JGeometry.load(st);
                double [] test = j_geom.getPoint();
                for (int i = 0;i<test.length;i++){

                    System.out.println(test[i]);

                }
            }
        }
        catch (Exception e){
            System.out.println(e);

        }



    }
}
