package controllers;

import java.util.Objects;
public class Edge{
    private double fromLatitude;
    private double fromLongtitude;
    private double toLatitude;
    private double toLongtitude;


    public Edge() {
    }

    public Edge(double fromLatitude, double fromLongtitude, double toLatitude, double toLongtitude) {
        this.fromLatitude = fromLatitude;
        this.fromLongtitude = fromLongtitude;
        this.toLatitude = toLatitude;
        this.toLongtitude = toLongtitude;
    }

    public double getFromLatitude() {
        return this.fromLatitude;
    }

    public void setFromLatitude(double fromLatitude) {
        this.fromLatitude = fromLatitude;
    }

    public double getFromLongtitude() {
        return this.fromLongtitude;
    }

    public void setFromLongtitude(double fromLongtitude) {
        this.fromLongtitude = fromLongtitude;
    }

    public double getToLatitude() {
        return this.toLatitude;
    }

    public void setToLatitude(double toLatitude) {
        this.toLatitude = toLatitude;
    }

    public double getToLongtitude() {
        return this.toLongtitude;
    }

    public void setToLongtitude(double toLongtitude) {
        this.toLongtitude = toLongtitude;
    }

    public Edge fromLatitude(double fromLatitude) {
        this.fromLatitude = fromLatitude;
        return this;
    }

    public Edge fromLongtitude(double fromLongtitude) {
        this.fromLongtitude = fromLongtitude;
        return this;
    }

    public Edge toLatitude(double toLatitude) {
        this.toLatitude = toLatitude;
        return this;
    }

    public Edge toLongtitude(double toLongtitude) {
        this.toLongtitude = toLongtitude;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Edge)) {
            return false;
        }
        Edge edge = (Edge) o;
        return fromLatitude == edge.fromLatitude && fromLongtitude == edge.fromLongtitude && toLatitude == edge.toLatitude && toLongtitude == edge.toLongtitude;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromLatitude, fromLongtitude, toLatitude, toLongtitude);
    }

    @Override
    public String toString() {
        return "{" +
                "\"source\":" + getFromLongtitude() + "," + getFromLatitude() +
                ",\"target\":" + getToLongtitude() + "," + getToLatitude() +
                "}";
    }

}