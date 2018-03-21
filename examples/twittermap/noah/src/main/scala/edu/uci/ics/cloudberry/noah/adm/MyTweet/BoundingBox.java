package edu.uci.ics.cloudberry.noah.adm.MyTweet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class BoundingBox {
    private Double[][][] coordinates;
    @JsonIgnoreProperties(ignoreUnknown = true)
    private String type;
    //

    @Override
    public String toString() {
        return String.format("rectangle(\"\")");
    }

    public void setCoordinates(Double[][][] coordinates) {
        this.coordinates = coordinates;
    }

    public Double[][][] getCoordinates() {
        return coordinates;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    public Boolean isNull(){
        if(type.equals(""))
            return true;
        return false;
    }
}
