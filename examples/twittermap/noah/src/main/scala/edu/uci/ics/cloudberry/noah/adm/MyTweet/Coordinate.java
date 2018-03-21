package edu.uci.ics.cloudberry.noah.adm.MyTweet;

public class Coordinate {
    private String cLat;
    private String cLong;

    @Override
    public String toString() {
        return cLat+cLong;
    }

    public String getcLat() {
        return cLat;
    }

    public String getcLong() {
        return cLong;
    }

    public void setcLat(String cLat) {
        this.cLat = cLat;
    }

    public void setcLong(String cLong) {
        this.cLong = cLong;
    }
}
