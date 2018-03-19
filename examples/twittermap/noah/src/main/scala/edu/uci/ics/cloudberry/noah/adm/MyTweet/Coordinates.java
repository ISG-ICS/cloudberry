package edu.uci.ics.cloudberry.noah.adm.MyTweet;

public class Coordinates {
    private Double[] coordinates;
    private String type;

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return getType()+":"+getCoordinates().toString();
    }

    public String getType() {
        return type;
    }

    public void setCoordinates(Double[] coordinates) {
        this.coordinates = coordinates;
    }

    public Double[] getCoordinates() {
        return coordinates;
    }
}
