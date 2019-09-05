package clustering;

import models.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * K-Means algorithm
 */
public class Kmeans {
    // the number of clusters desired
    int k;
    // the dataset for clustering
    List<Point> dataSet;
    // the list of centers of clusters
    List<Point> centers;
    // the list of clusters for the whole dataset
    List<List<Point>> clusters;
    // map of points and its cluster
    HashMap<Point, Integer> parents;
    // the last squared error sum for determining the algorithm's termination
    private double lastSquaredErrorSum;
    // the number of iterations
    private int I;

    /**
     * Constructor for k
     *
     * @param k Number of clusters
     */
    public Kmeans(int k) {
        if (k <= 0) {
            k = 1;
        }
        this.k = k;
        centers = new ArrayList<>();
        clusters = new ArrayList<>();
        parents = new HashMap<>();
    }

    public List<Point> getCenters() {
        return centers;
    }

    public int getDataSetLength() {
        if (dataSet == null || dataSet.size() == 0) {
            return 0;
        } else {
            return dataSet.size();
        }
    }

    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
    }

    public void setDataSet(List<Point> dataSet) {
        this.dataSet = dataSet;
    }

    public List<List<Point>> getClusters() {
        return clusters;
    }


    /**
     * Initialization of the whole K-Means process
     */
    public void init() {
        I = 0;
        lastSquaredErrorSum = 0;
        initCenters();
        initCluster();
    }

    /**
     * Initialize the list of centers corresponding to each cluster
     */
    void initCenters() {
        centers.clear();
        int[] randoms = new int[k];
        boolean flag;
        int temp = (int) (Math.random() * getDataSetLength());
        randoms[0] = temp;
        centers.add(dataSet.get(randoms[0]));
        for (int i = 1; i < k; i++) {
            flag = true;
            while (flag) {
                temp = (int) (Math.random() * getDataSetLength());
                int j = 0;
                while (j < i) {
                    if (temp == randoms[j]) {
                        break;
                    }
                    j++;
                }
                if (j == i) {
                    flag = false;
                }
            }
            randoms[i] = temp;
            centers.add(dataSet.get(randoms[i]));
        }
    }

    /**
     * Initialize the set of clusters
     */
    void initCluster() {
        clusters.clear();
        for (int i = 0; i < k; i++) {
            clusters.add(new ArrayList<>());
        }

    }

    /**
     * Calculate the distance between two points
     *
     * @param element points in dataset
     * @param center  centers of clusters
     * @return the computed distance
     */
    private double distance(Point element, Point center) {

        return element.distanceTo(center);
    }

    /**
     * Add all points to their closest clusters
     */
    void clusterSet() {
        for (int i = 0; i < getDataSetLength(); i++) {
            assignPoint(i);
        }
    }

    /**
     * Add each point to its closest cluster
     *
     * @param pointIdx point index
     * @return closest cluster index
     */
    int assignPoint(int pointIdx) {
        double currentDistance;
        double minDistance = distance(dataSet.get(pointIdx), centers.get(0));
        int minLocation = 0;
        for (int j = 1; j < k; j++) {
            currentDistance = distance(dataSet.get(pointIdx), centers.get(j));
            if (currentDistance < minDistance) {
                minDistance = currentDistance;
                minLocation = j;
            } else if (currentDistance == minDistance) {
                if (Math.random() < 0.5) {
                    minLocation = j;
                }
            }
        }
        clusters.get(minLocation).add(dataSet.get(pointIdx)); // add each point to its closest cluster
        return minLocation;
    }

    /**
     * Map each point to the cluster it belongs to
     */
    private void findParents() {
        for (int i = 0; i < clusters.size(); i++) {
            for (int j = 0; j < clusters.get(i).size(); j++) {
                Point point = new Point(clusters.get(i).get(j).getX(), clusters.get(i).get(j).getY());
                parents.put(point, i);
            }
        }
    }

    /**
     * Calculate the sum of the squared error
     */
    private double countRule() {
        double squaredErrorSum = 0;
        for (int i = 0; i < clusters.size(); i++) {
            for (int j = 0; j < clusters.get(i).size(); j++) {
                squaredErrorSum += Math.pow(distance(clusters.get(i).get(j), centers.get(i)), 2);
            }
        }
        return squaredErrorSum;
    }

    /**
     * Set the new center for each cluster
     */
    void setNewCenter() {
        for (int i = 0; i < k; i++) {
            int n = clusters.get(i).size();
            if (n != 0) {
                Point newCenter = initNewCenter(i, n);
                // Calculate the average coordinate of all points in the cluster
                newCenter.setX(newCenter.getX() / n);
                newCenter.setY(newCenter.getY() / n);
                centers.set(i, newCenter);
            }
        }
    }

    /**
     * Initialize the new position for the cluster
     *
     * @param clusterIdx  the index of the cluster
     * @param clusterSize the size of the cluster
     * @return the initialized position for the cluster
     */
    Point initNewCenter(int clusterIdx, int clusterSize) {
        Point newCenter = new Point(0, 0);
        for (int j = 0; j < clusterSize; j++) {
            newCenter.setX(newCenter.getX() + clusters.get(clusterIdx).get(j).getX());
            newCenter.setY(newCenter.getY() + clusters.get(clusterIdx).get(j).getY());
        }
        return newCenter;
    }

    /**
     * the core method of K-Means
     */
    public void execute(List<Point> dataSet) {
        setDataSet(dataSet);
        if (k > getDataSetLength()) {
            k = getDataSetLength();
        }
        if (dataSet == null || dataSet.size() == 0) {
            return;
        }
        init();
        // iterate until no change in the sum of squared errors
        double currentSquaredErrorSum;
        while (true) {
            clusterSet();
            currentSquaredErrorSum = countRule();
            if (I != 0) {
                if (currentSquaredErrorSum == lastSquaredErrorSum) {
                    findParents();
                    break;
                }
            }
            setNewCenter();
            I++;
            initCluster();
            lastSquaredErrorSum = currentSquaredErrorSum;
        }
    }

    /**
     * Get the cluster to which a point belongs
     * @param point a given point
     * @return the cluster to which the point belongs
     */
    public Point getParent(Point point) {
        return centers.get(parents.get(point));
    }

    /**
     * Get the map containing the clusters and their sizes
     * @return the map containing the clusters and their sizes
     */
    public HashMap<Point, Integer> getClustersMap() {
        HashMap<Point, Integer> clustersSizes = new HashMap<>();
        for (int i = 0; i < getK(); i++) {
            clustersSizes.put(getCenters().get(i), getClusters().get(i).size());
        }
        return clustersSizes;
    }
}
