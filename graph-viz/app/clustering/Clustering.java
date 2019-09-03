package clustering;

import models.Cluster;
import models.KdTree;
import models.Rectangle;

import java.util.ArrayList;

public class Clustering {

    // min zoom level in clustering tree
    private int minZoom;
    // max zoom level in clustering tree
    private int maxZoom;
    // radius in max zoom level
    private double radius = 80;
    // extent used to calculate the radius in different zoom level
    private double extent = 512;
    // kd-trees in different zoom level
    private KdTree[] trees;
    // max longitude
    private static final int MAX_LONGITUDE = 180;
    // max latitude
    private static final int MAX_LATITUDE = 90;
    // max degree
    private static final int MAX_DEGREE = 360;

    /**
     * Create an instance of hierarchical greedy clustering
     *
     * @param minZoom the minimum zoom level
     * @param maxZoom the maximum zoom level
     */
    public Clustering(int minZoom, int maxZoom) {
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        trees = new KdTree[maxZoom + 2];
        for (int z = minZoom; z <= maxZoom + 1; z++) {
            trees[z] = new KdTree();
        }
    }

    /**
     * @return the maximum zoom level
     */
    public int getMaxZoom() {
        return maxZoom;
    }


    /**
     * Load all the points and run clustering algorithm
     *
     * @param points input of points
     */
    public void load(ArrayList<Cluster> points) {
        for (Cluster point : points) {
            insert(new Cluster(lngX(point.getX()), latY(point.y()), null, 1));
        }
    }

    /**
     * insert one point into the tree
     *
     * @param point the input of point
     */
    public void insert(Cluster point) {
        trees[maxZoom + 1].insert(point);
        for (int z = maxZoom; z >= minZoom; z--) {
            // search if there are any neighbor near this point
            ArrayList<Cluster> neighbors = trees[z].rangeRadius(point.getX(), point.y(), getZoomRadius(z));
            // if no, insert it into kd-tree
            if (neighbors.isEmpty()) {
                Cluster c = new Cluster(point.getX(), point.y());
                c.setZoom(z);
                point.parent = c;
                trees[z].insert(c);
                point = c;
                // if have, choose which cluster this point belongs to
            } else {
                Cluster neighbor = null;
                point.setZoom(z + 1);
                int totNumOfPoints = 0;
                for (int i = 0; i < neighbors.size(); i++) {
                    neighbor = neighbors.get(i);
                    if (neighbor.getX() != point.getX() || neighbor.y() != point.y()) continue;
                    totNumOfPoints += neighbor.getNumPoints();
                }
                double rand = Math.random();
                for (int i = 0; i < neighbors.size(); i++) {
                    neighbor = neighbors.get(i);
                    if (neighbor.getX() != point.getX() || neighbor.y() != point.y()) continue;
                    double probability = neighbor.getNumPoints() * 1.0 / totNumOfPoints;
                    if (rand < probability) {
                        break;
                    } else {
                        rand -= probability;
                    }
                }
                // let this cluster be its parent
                point.parent = neighbor;
                // update its parents
                while (neighbor != null) {
                    double wx = neighbor.getX() * neighbor.getNumPoints() + point.getX();
                    double wy = neighbor.y() * neighbor.getNumPoints() + point.y();
                    neighbor.setNumPoints(neighbor.getNumPoints() + 1);
                    neighbor.setX(wx / neighbor.getNumPoints());
                    neighbor.setY(wy / neighbor.getNumPoints());
                    neighbor = neighbor.parent;
                }
                break;
            }
        }
    }

    /**
     * @param zoom the zoom level
     * @return the radius in this zoom level
     */
    private double getZoomRadius(int zoom) {
        return radius / (extent * Math.pow(2, zoom));
    }

    /**
     * get clusters within certain window and zoom level
     *
     * @param bbox the bounding box of the window
     * @param zoom the zoom level
     * @return all the clusters within this bounding box and this zoom level
     */
    public ArrayList<Cluster> getClusters(double[] bbox, int zoom) {
        double minLongitude = ((bbox[0] + MAX_LONGITUDE) % (MAX_LONGITUDE * 2) + MAX_LONGITUDE * 2) % (MAX_LONGITUDE * 2) - MAX_LONGITUDE;
        double minLatitude = Math.max(-MAX_LATITUDE, Math.min(MAX_LATITUDE, bbox[1]));
        double maxLongitude = bbox[2] == MAX_LONGITUDE ? MAX_LONGITUDE : ((bbox[2] + MAX_LONGITUDE) % (MAX_LONGITUDE * 2) + (MAX_LONGITUDE * 2)) % (MAX_LONGITUDE * 2) - MAX_LONGITUDE;
        double maxLatitude = Math.max(-MAX_LATITUDE, Math.min(MAX_LATITUDE, bbox[3]));
        if (bbox[2] - bbox[0] >= MAX_LONGITUDE * 2) {
            minLongitude = -MAX_LONGITUDE;
            maxLongitude = MAX_LONGITUDE;
        } else if (minLongitude > maxLongitude) {
            ArrayList<Cluster> results = getClusters(new double[]{minLongitude, minLatitude, MAX_LONGITUDE, maxLatitude}, zoom);
            results.addAll(getClusters(new double[]{-MAX_LONGITUDE, minLatitude, maxLongitude, maxLatitude}, zoom));
            return results;
        }
        KdTree kdTree = trees[limitZoom(zoom)];
        ArrayList<Cluster> neighbors = kdTree.range(new Rectangle(lngX(minLongitude), latY(maxLatitude), lngX(maxLongitude), latY(minLatitude)));
        ArrayList<Cluster> clusters = new ArrayList<>();
        for (int i = 0; i < neighbors.size(); i++) {
            Cluster neighbor = neighbors.get(i);
            if (neighbor.getNumPoints() > 0) {
                clusters.add(neighbor);
            } else {
                clusters.add(neighbor);
            }
        }
        return clusters;
    }

    private int limitZoom(int z) {
        return Math.max(minZoom, Math.min(z, maxZoom + 1));
    }

    /**
     * get the parent cluster in certain zoom level
     *
     * @param cluster the input cluster
     * @param zoom    the zoom level of its parent
     * @return the parent cluster of this cluster
     */
    public Cluster parentCluster(Cluster cluster, int zoom) {
        Cluster c = trees[maxZoom + 1].findPoint(cluster);
        while (c != null) {
            if (c.getZoom() == zoom) {
                break;
            }
            c = c.parent;
        }
        return c;
    }

    /**
     * translate longitude to spherical mercator in [0..1] range
     *
     * @param lng longitude
     * @return a number in [0..1] range
     */
    public static double lngX(double lng) {
        return lng / (MAX_LONGITUDE * 2) + 0.5;
    }

    /**
     * translate latitude to spherical mercator in [0..1] range
     *
     * @param lat latitude
     * @return a number in [0..1] range
     */
    public static double latY(double lat) {
        double sin = Math.sin(lat * Math.PI / (MAX_LATITUDE * 2));
        double y = (0.5 - 0.25 * Math.log((1 + sin) / (1 - sin)) / Math.PI);
        return y < 0 ? 0 : y > 1 ? 1 : y;
    }

    /**
     * translate spherical mercator in [0..1] range to longitude
     *
     * @param x a number in [0..1] range
     * @return longitude
     */
    public static double xLng(double x) {
        return (x - 0.5) * (MAX_LONGITUDE * 2);
    }


    /**
     * translate spherical mercator in [0..1] range to latitude
     *
     * @param y a number in [0..1] range
     * @return latitude
     */
    public static double yLat(double y) {
        double y2 = (MAX_LATITUDE * 2 - y * 360) * Math.PI / 180;
        return MAX_DEGREE * Math.atan(Math.exp(y2)) / Math.PI - MAX_LATITUDE;
    }
}
