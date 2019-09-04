package models;

import java.util.Objects;

public class Edge {

    // from Point of edge
    private Point from;

    public Point getFromPoint() {
        return from;
    }

    public Point getToPoint() {
        return to;
    }

    // to Point of edge
    private Point to;

    public Edge(double fromX, double fromY, double toX, double toY) {
        this.from = new Point(fromX, fromY);
        this.to = new Point(toX, toY);
    }

    public double getFromX() {
        return this.from.getX();
    }

    public double getFromY() {
        return this.from.getY();
    }

    public double getToX() {
        return this.to.getX();
    }

    public double getToY() {
        return this.to.getY();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Edge)) {
            return false;
        }
        Edge edge = (Edge) o;
        return (from.equals(edge.from) && to.equals(edge.to)) || (from.equals(edge.to) && to.equals(edge.from));
    }

    public boolean cross(Edge e2) {
        return ((getToX() - getFromX()) * (e2.getFromY() - getFromY())
                - (e2.getFromX() - getFromX()) * (getToY() - getFromY()))
                * ((getToX() - getFromX()) * (e2.getToY() - getFromY())
                - (e2.getToX() - getFromX()) * (getToY() - getFromY())) < 0
                && ((e2.getToX() - e2.getFromX()) * (getFromY() - e2.getFromY())
                - (getFromX() - e2.getFromX()) * (e2.getToY() - e2.getFromY()))
                * ((e2.getToX() - e2.getFromX()) * (getToY() - e2.getFromY())
                - (getToX() - e2.getFromX()) * (e2.getToY() - e2.getFromY())) < 0;
    }

    @Override
    public int hashCode() {
        if (getFromX() > getToX()) {
            return Objects.hash(getToX(), getToY(), getFromX(), getFromY());
        } else {
            return Objects.hash(getFromX(), getFromY(), getToX(), getToY());
        }
    }

    @Override
    public String toString() {
        return "{" +
                "\"source\":" + getFromX() + "," + getFromY() +
                ",\"target\":" + getToX() + "," + getToY() +
                "}";
    }

    public double length() {
        return from.distanceTo(to);
    }

    public double getDegree() {
        return Math.toDegrees(Math.atan((getToY() - getFromY()) / (getToX() - getFromX())));
    }
}