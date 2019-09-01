package algorithms;

import models.Cluster;
import models.KdTree;
import models.Rectangle;

import java.util.ArrayList;

// TODO try thinking about adding the degree into the cluster which would help in the tree cut
// TODO try to make the cluster not imaginary to make the incremental bundling easier
public class PointCluster {

    private int minZoom;
    private int maxZoom;
    // TODO understand radius
    private double radius = 80;
    // TODO explain and add comment for extent
    private double extent = 512;
    private KdTree[] trees;

    /**
     * Create an instance of hierarchical greedy clustering
     *
     * @param minZoom the minimum zoom level
     * @param maxZoom the maximum zoom level
     */
    public PointCluster(int minZoom, int maxZoom) {
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
            insert(new Cluster(lngX(point.x()), latY(point.y()), null, 1));
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
            ArrayList<Cluster> neighbors = trees[z].rangeRadius(point.x(), point.y(), getZoomRadius(z));
            if (neighbors.isEmpty()) {
                Cluster c = new Cluster(point.x(), point.y());
                c.setZoom(z);
                point.parent = c;
                trees[z].insert(c);
                point = c;
            } else {
                Cluster neighbor = null;
                point.setZoom(z + 1);
                int totNumOfPoints = 0;
                for (int i = 0; i < neighbors.size(); i++) {
                    neighbor = neighbors.get(i);
                    if (neighbor.x() != point.x() || neighbor.y() != point.y()) continue;
                    totNumOfPoints += neighbor.getNumPoints();
                }
                double rand = Math.random();
                for (int i = 0; i < neighbors.size(); i++) {
                    neighbor = neighbors.get(i);
                    if (neighbor.x() != point.x() || neighbor.y() != point.y()) continue;
                    double probability = neighbor.getNumPoints() * 1.0 / totNumOfPoints;
                    if (rand < probability) {
                        break;
                    } else {
                        rand -= probability;
                    }
                }
                point.parent = neighbor;
                while (neighbor != null) {
                    double wx = neighbor.x() * neighbor.getNumPoints() + point.x();
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
        double minLongitude = ((bbox[0] + 180) % 360 + 360) % 360 - 180;
        double minLatitude = Math.max(-90, Math.min(90, bbox[1]));
        double maxLongitude = bbox[2] == 180 ? 180 : ((bbox[2] + 180) % 360 + 360) % 360 - 180;
        double maxLatitude = Math.max(-90, Math.min(90, bbox[3]));
        if (bbox[2] - bbox[0] >= 360) {
            minLongitude = -180;
            maxLongitude = 180;
        } else if (minLongitude > maxLongitude) {
            ArrayList<Cluster> results = getClusters(new double[]{minLongitude, minLatitude, 180, maxLatitude}, zoom);
            results.addAll(getClusters(new double[]{-180, minLatitude, maxLongitude, maxLatitude}, zoom));
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
        return lng / 360 + 0.5;
    }

    /**
     * translate latitude to spherical mercator in [0..1] range
     *
     * @param lat latitude
     * @return a number in [0..1] range
     */
    public static double latY(double lat) {
        double sin = Math.sin(lat * Math.PI / 180);
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
        return (x - 0.5) * 360;
    }


    /**
     * translate spherical mercator in [0..1] range to latitude
     *
     * @param y a number in [0..1] range
     * @return latitude
     */
    public static double yLat(double y) {
        double y2 = (180 - y * 360) * Math.PI / 180;
        return 360 * Math.atan(Math.exp(y2)) / Math.PI - 90;
    }
}
