package models;

import clustering.Clustering;

public class Cluster extends Point {
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

    public Cluster(Point point) {
        this.setX(point.getX());
        this.setY(point.getY());
        this.numPoints = 1;
        this.parent = null;
        this.zoom = Integer.MAX_VALUE;
    }

    public Cluster(Point point, Cluster parent, int numPoints) {
        this.setX(point.getX());
        this.setY(point.getY());
        this.parent = parent;
        this.numPoints = numPoints;
        this.zoom = Integer.MAX_VALUE;
    }

    @Override
    public String toString() {
        return String.format("(%.2f, %.2f), zoom: %d, numPoints: %d", Clustering.xLng(getX()), Clustering.yLat(getY()), zoom, numPoints);
    }
}
