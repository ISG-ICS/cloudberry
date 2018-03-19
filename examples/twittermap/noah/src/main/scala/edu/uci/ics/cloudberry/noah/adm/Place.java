package edu.uci.ics.cloudberry.noah.adm;

import org.apache.commons.lang3.StringEscapeUtils;

public class Place {

    public static String COUNTRY = "country";
    public static String COUNTRY_CODE = "country_code";
    public static String FULL_NAME = "full_name";
    public static String ID = "id";
    public static String NAME = "name";
    public static String PLACE_TYPE = "place_type";
    public static String BOUNDING_BOX = "bounding_box";
    public static StringBuilder placeSB = new StringBuilder();

    public static String toADM() {
        placeSB.delete(0,placeSB.length());
        placeSB.append("{");
        ADM.keyValueToSbWithComma(placeSB, COUNTRY, "\""+StringEscapeUtils.escapeJava(Tweet.Place_Country)+"\"");
        ADM.keyValueToSbWithComma(placeSB, COUNTRY_CODE,"\""+Tweet.Place_Country_Code+"\"");
        ADM.keyValueToSbWithComma(placeSB, FULL_NAME, "\""+StringEscapeUtils.escapeJava(Tweet.Place_Full_Name)+"\"");
        ADM.keyValueToSbWithComma(placeSB, ID, "\""+Tweet.Place_Id+"\"");
        ADM.keyValueToSbWithComma(placeSB, NAME, "\""+ StringEscapeUtils.escapeJava(Tweet.Place_Name)+"\"");
        ADM.keyValueToSbWithComma(placeSB, PLACE_TYPE,"\""+Tweet.Place_Place_Type+"\"");
        if(!Tweet.Place_BoudingBox.isNull()){
            if(Tweet.Place_BoudingBox.getCoordinates().length == 1 && Tweet.Place_BoudingBox.getCoordinates()[0].length == 4) {
                placeSB.append("\"").append(BOUNDING_BOX).append("\":rectangle(\"");
                if (Tweet.Place_BoudingBox.getCoordinates()[0][0][0].equals(Tweet.Place_BoudingBox.getCoordinates()[0][2][0]) && Tweet.Place_BoudingBox.getCoordinates()[0][0][1].equals(Tweet.Place_BoudingBox.getCoordinates()[0][2][1])) {

                    placeSB.append(Tweet.Place_BoudingBox.getCoordinates()[0][0][0] - 0.0000001).append(',');
                    placeSB.append(Tweet.Place_BoudingBox.getCoordinates()[0][0][1] - 0.0000001).append(' ');
                }else{
                    placeSB.append(Tweet.Place_BoudingBox.getCoordinates()[0][0][0]).append(',');
                    placeSB.append(Tweet.Place_BoudingBox.getCoordinates()[0][0][1]).append(' ');
                }
                placeSB.append(Tweet.Place_BoudingBox.getCoordinates()[0][2][0]).append(',');
                placeSB.append(Tweet.Place_BoudingBox.getCoordinates()[0][2][1]);
                placeSB.append("\")");
            }
        }
        placeSB.append("}");
        return placeSB.toString();
    }
}
