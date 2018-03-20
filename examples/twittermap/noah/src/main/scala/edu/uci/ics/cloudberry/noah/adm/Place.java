package edu.uci.ics.cloudberry.noah.adm;

import edu.uci.ics.cloudberry.noah.adm.MyTweet.Places;
import org.apache.commons.lang3.StringEscapeUtils;

public class Place {

    public static String COUNTRY = "country";
    public static String COUNTRY_CODE = "country_code";
    public static String FULL_NAME = "full_name";
    public static String ID = "id";
    public static String NAME = "name";
    public static String PLACE_TYPE = "place_type";
    public static String BOUNDING_BOX = "bounding_box";

    public static String toADM(Places places) {
        StringBuilder placeSB=new StringBuilder();
        placeSB.append("{");
        ADM.keyValueToSbWithComma(placeSB, COUNTRY, "\""+StringEscapeUtils.escapeJava(places.getCountry())+"\"");
        ADM.keyValueToSbWithComma(placeSB, COUNTRY_CODE,"\""+places.getCountry_code()+"\"");
        ADM.keyValueToSbWithComma(placeSB, FULL_NAME, "\""+StringEscapeUtils.escapeJava(places.getFull_name())+"\"");
        ADM.keyValueToSbWithComma(placeSB, ID, "\""+places.getId()+"\"");
        ADM.keyValueToSbWithComma(placeSB, NAME, "\""+ StringEscapeUtils.escapeJava(places.getName())+"\"");
        ADM.keyValueToSbWithComma(placeSB, PLACE_TYPE,"\""+places.getPlace_type()+"\"");
        if(!places.getBounding_box().isNull()){
            if(places.getBounding_box().getCoordinates().length == 1 && places.getBounding_box().getCoordinates()[0].length == 4) {
                placeSB.append("\"").append(BOUNDING_BOX).append("\":rectangle(\"");
                if (places.getBounding_box().getCoordinates()[0][0][0].equals(places.getBounding_box().getCoordinates()[0][2][0]) && places.getBounding_box().getCoordinates()[0][0][1].equals(places.getBounding_box().getCoordinates()[0][2][1])) {

                    placeSB.append(places.getBounding_box().getCoordinates()[0][0][0] - 0.0000001).append(',');
                    placeSB.append(places.getBounding_box().getCoordinates()[0][0][1] - 0.0000001).append(' ');
                }else{
                    placeSB.append(places.getBounding_box().getCoordinates()[0][0][0]).append(',');
                    placeSB.append(places.getBounding_box().getCoordinates()[0][0][1]).append(' ');
                }
                placeSB.append(places.getBounding_box().getCoordinates()[0][2][0]).append(',');
                placeSB.append(places.getBounding_box().getCoordinates()[0][2][1]);
                placeSB.append("\")");
            }
        }
        placeSB.append("}");
        return placeSB.toString();
    }
}
