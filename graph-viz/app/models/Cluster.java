package models;

import algorithms.PointCluster;

import java.util.Comparator;

public class Cluster implements Comparable<Cluster> {
    // TODO don't use zoom in Kmeans class, in Kmeans, just use this class
    /**
     * Compares two points by x-coordinate.
     */
    public static final Comparator<Cluster> X_ORDER = new XOrder();

    /**
     * Compares two points by y-coordinate.
     */
    public static final Comparator<Cluster> Y_ORDER = new YOrder();
    //TODO using Point class
    private double x;    // x coordinate
    private double y;    // y coordinate
    private int numPoints = 0;
    private int zoom = 0;
    public Cluster parent;

    public int getZoom() {
        return zoom;
    }

    public void setZoom(int zoom) {
        this.zoom = zoom;
    }

    public int getNumPoints() {
        return numPoints;
    }

    public void setNumPoints(int numPoints) {
        this.numPoints = numPoints;
    }

    public Cluster(double x, double y) {
        if (Double.isInfinite(x) || Double.isInfinite(y))
            throw new IllegalArgumentException("Coordinates must be finite");
        if (Double.isNaN(x) || Double.isNaN(y))
            throw new IllegalArgumentException("Coordinates cannot be NaN");
        if (x == 0.0) x = 0.0;  // convert -0.0 to +0.0
        if (y == 0.0) y = 0.0;  // convert -0.0 to +0.0
        this.x = x;
        this.y = y;
        this.numPoints = 1;
        this.zoom = Integer.MAX_VALUE;
    }

    public Cluster(double x, double y, Cluster parent, int numPoints) {
        this.x = x;
        this.y = y;
        this.parent = parent;
        this.numPoints = numPoints;
        this.zoom = Integer.MAX_VALUE;
    }

    /**
     * Returns the x-coordinate.
     *
     * @return the x-coordinate
     */
    public double x() {
        return x;
    }

    /**
     * Returns the y-coordinate.
     *
     * @return the y-coordinate
     */
    public double y() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double distanceTo(Cluster that) {
        double dx = this.x - that.x;
        double dy = this.y - that.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public int compareTo(Cluster that) {
        if (this.y < that.y) return -1;
        if (this.y > that.y) return +1;
        if (this.x < that.x) return -1;
        if (this.x > that.x) return +1;
        return 0;
    }

    // compare points according to their x-coordinate
    private static class XOrder implements Comparator<Cluster> {
        public int compare(Cluster p, Cluster q) {
            if (p.x < q.x) return -1;
            if (p.x > q.x) return +1;
            return 0;
        }
    }

    // compare points according to their y-coordinate
    private static class YOrder implements Comparator<Cluster> {
        public int compare(Cluster p, Cluster q) {
            if (p.y < q.y) return -1;
            if (p.y > q.y) return +1;
            return 0;
        }
    }

    public boolean equals(Object other) {
        if (other == this) return true;
        if (other == null) return false;
        if (other.getClass() != this.getClass()) return false;
        Cluster that = (Cluster) other;
        return this.x == that.x && this.y == that.y;
    }

    public String toString() {
        return String.format("(%.2f, %.2f), zoom: %d, numPoints: %d", PointCluster.xLng(x), PointCluster.yLat(y), zoom, numPoints);
    }

    public int hashCode() {
        int hashX = ((Double) x).hashCode();
        int hashY = ((Double) y).hashCode();
        return 31 * hashX + hashY;
    }
}
