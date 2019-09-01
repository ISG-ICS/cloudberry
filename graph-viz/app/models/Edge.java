package models;

import java.util.Objects;

// TODO change longitude and latitude EVERYWHERE to be x and y
public class Edge {
    private double fromLatitude;
    private double fromLongitude;
    private double toLatitude;
    private double toLongitude;

    public Edge() {
    }

    public Edge(double fromLatitude, double fromLongitude, double toLatitude, double toLongitude, int weight) {
        this.fromLatitude = fromLatitude;
        this.fromLongitude = fromLongitude;
        this.toLatitude = toLatitude;
        this.toLongitude = toLongitude;
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    private int weight;


    public Edge(double fromLatitude, double fromLongitude, double toLatitude, double toLongitude) {
        this.fromLatitude = fromLatitude;
        this.fromLongitude = fromLongitude;
        this.toLatitude = toLatitude;
        this.toLongitude = toLongitude;
    }

    public double getFromLatitude() {
        return this.fromLatitude;
    }

    public double getFromLongitude() {
        return this.fromLongitude;
    }


    public double getToLatitude() {
        return this.toLatitude;
    }

    public double getToLongitude() {
        return this.toLongitude;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Edge)) {
            return false;
        }
        Edge edge = (Edge) o;
        return (fromLatitude == edge.fromLatitude && fromLongitude == edge.fromLongitude && toLatitude == edge.toLatitude && toLongitude == edge.toLongitude)
                || (fromLatitude == edge.toLatitude && fromLongitude == edge.toLongitude && toLatitude == edge.fromLatitude && toLongitude == edge.fromLongitude);
    }

    public boolean cross(Edge e2) {
        return ((toLongitude - fromLongitude) * (e2.fromLatitude - fromLatitude)
                - (e2.fromLongitude - fromLongitude) * (toLatitude - fromLatitude))
                * ((toLongitude - fromLongitude) * (e2.toLatitude - fromLatitude)
                - (e2.toLongitude - fromLongitude) * (toLatitude - fromLatitude)) < 0
                && ((e2.toLongitude - e2.fromLongitude) * (fromLatitude - e2.fromLatitude)
                - (fromLongitude - e2.fromLongitude) * (e2.toLatitude - e2.fromLatitude))
                * ((e2.toLongitude - e2.fromLongitude) * (toLatitude - e2.fromLatitude)
                - (toLongitude - e2.fromLongitude) * (e2.toLatitude - e2.fromLatitude)) < 0;
    }

    @Override
    public int hashCode() {
        if (fromLongitude > toLongitude) {
            return Objects.hash(toLongitude, toLatitude, fromLongitude, fromLatitude);
        } else {
            return Objects.hash(fromLongitude, fromLatitude, toLongitude, toLatitude);
        }
    }

    @Override
    public String toString() {
        return "{" +
                "\"source\":" + getFromLongitude() + "," + getFromLatitude() +
                ",\"target\":" + getToLongitude() + "," + getToLatitude() +
                "}";
    }

    public double getDegree() {
        return Math.toDegrees(Math.atan((getToLatitude() - getFromLatitude()) / (getToLongitude() - getFromLongitude())));
    }
}