package edu.uci.ics.cloudberry.noah.adm.MyTweet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.uci.ics.cloudberry.noah.adm.ADM;
import org.apache.commons.lang3.StringEscapeUtils;

public class MyPlace {
    private String id;//
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String url;
    private String place_type;//
    private String name;//
    private String full_name;//
    private String country_code;//
    private String country;//
    //
    private Double aLong;
    private Double aLat;
    private Double bLong;
    private Double bLat;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private Object attributes;
    //
    public static String COUNTRY = "country";
    public static String COUNTRY_CODE = "country_code";
    public static String FULL_NAME = "full_name";
    public static String ID = "id";
    public static String NAME = "name";
    public static String PLACE_TYPE = "place_type";
    public static String BOUNDING_BOX = "bounding_box";
    //

    public Double getaLong() {
        return aLong;
    }

    public Double getaLat() {
        return aLat;
    }

    public Double getbLat() {
        return bLat;
    }

    public Double getbLong() {
        return bLong;
    }

    public void setaLat(Double aLat) {
        this.aLat = aLat;
    }

    public void setaLong(Double aLong) {
        this.aLong = aLong;
    }

    public void setbLat(Double bLat) {
        this.bLat = bLat;
    }

    public void setbLong(Double bLong) {
        this.bLong = bLong;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getPlace_type() {
        return place_type;
    }

    public String getName() {
        return name;
    }

    public String getFull_name() {
        return full_name;
    }

    public String getCountry_code() {
        return country_code;
    }

    public String getCountry() {
        return country;
    }

    public Object getAttributes() {
        return attributes;
    }

    public void setAttributes(Object attributes) {
        this.attributes = attributes;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPlace_type(String place_type) {
        this.place_type = place_type;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setCountry_code(String country_code) {
        this.country_code = country_code;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return full_name;
    }

    public String toADM() {
        StringBuilder placeSB=new StringBuilder();
        placeSB.append("{");
        ADM.keyValueToSbWithComma(placeSB, COUNTRY, "\""+ StringEscapeUtils.escapeJava(country)+"\"");
        ADM.keyValueToSbWithComma(placeSB, COUNTRY_CODE,"\""+country_code+"\"");
        ADM.keyValueToSbWithComma(placeSB, FULL_NAME, "\""+StringEscapeUtils.escapeJava(full_name)+"\"");
        ADM.keyValueToSbWithComma(placeSB, ID, "\""+id+"\"");
        ADM.keyValueToSbWithComma(placeSB, NAME, "\""+ name+"\"");
        ADM.keyValueToSbWithComma(placeSB, PLACE_TYPE,"\""+place_type+"\"");
        if(!aLat.isNaN()){
                placeSB.append("\"").append(BOUNDING_BOX).append("\":rectangle(\"");
                if (aLat.equals(bLat) && aLong.equals(bLong)) {
                    placeSB.append(aLat - 0.0000001).append(',');
                    placeSB.append(aLong- 0.0000001).append(' ');
                }else{
                    placeSB.append(aLat).append(',');
                    placeSB.append(aLong).append(' ');
                }
                placeSB.append(bLat).append(',');
                placeSB.append(bLong);
                placeSB.append("\")");
        }
        placeSB.append("}");
        return placeSB.toString();
    }
}
