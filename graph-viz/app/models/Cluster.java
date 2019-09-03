package models;

import clustering.Clustering;

public class Cluster implements Comparable<Cluster> {
    // TODO don't use zoom in Kmeans class, in Kmeans, just use this class
    private Point point;
    private int numPoints;
    private int zoom;
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
        this.point = new Point(x, y);
        this.numPoints = 1;
        this.parent = null;
        this.zoom = Integer.MAX_VALUE;
    }

    public Cluster(double x, double y, Cluster parent, int numPoints) {
        this.point = new Point(x, y);
        this.parent = parent;
        this.numPoints = numPoints;
        this.zoom = Integer.MAX_VALUE;
    }

    /**
     * Returns the getX-coordinate.
     *
     * @return the getX-coordinate
     */
    public double getX() {
        return point.getX();
    }

    /**
     * Returns the getY-coordinate.
     *
     * @return the getY-coordinate
     */
    public double getY() {
        return point.getY();
    }

    public void setX(double x) {
        this.point.setX(x);
    }

    public void setY(double y) {
        this.point.setY(y);
    }

    public double distanceTo(Cluster that) {
        return point.distanceTo(that.point);
    }

    public int compareTo(Cluster that) {
        if (this.getY() < that.getY()) return -1;
        if (this.getY() > that.getY()) return +1;
        if (this.getX() < that.getX()) return -1;
        if (this.getX() > that.getX()) return +1;
        return 0;
    }

    public boolean equals(Object other) {
        if (other == this) return true;
        if (other == null) return false;
        if (other.getClass() != this.getClass()) return false;
        Cluster that = (Cluster) other;
        return this.getX() == that.getX() && this.getY() == that.getY();
    }

    public String toString() {
        return String.format("(%.2f, %.2f), zoom: %d, numPoints: %d", Clustering.xLng(getX()), Clustering.yLat(getY()), zoom, numPoints);
    }

    public int hashCode() {
        int hashX = ((Double) getX()).hashCode();
        int hashY = ((Double) getY()).hashCode();
        return 31 * hashX + hashY;
    }
}
