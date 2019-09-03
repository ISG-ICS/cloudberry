package clustering;

import models.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Incremental K-Means Algorithm
 */
// TODO extend Kmeans
public class IKmeans {
    private Kmeans kmeans;
    private int k; // the number of clusters desired
    private List<Point> dataSet; // the dataset for clustering
    private ArrayList<Point> centers; // the list of centers of clusters
    private List<List<Point>> clusters; // the list of clusters for the current batch of data
    private List<List<Point>> allClusters; // the list of clusters for all accumulated data
    private int pointsCnt; // the count of points in all accumulated data
    private HashMap<Point, Integer> parents = new HashMap<>(); // map of points and its cluster

    /**
     * Constructor for k
     *
     * @param k Number of clusters
     */
    public IKmeans(int k) {
        if (k <= 0) {
            k = 1;
        }
        this.k = k;
        kmeans = new Kmeans(k);
    }

    public void setDataSet(List<Point> dataSet) {
        this.dataSet = dataSet;
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

    public List<List<Point>> getAllClusters() {
        return allClusters;
    }

    public ArrayList<Point> getCenters() {
        return centers;
    }

    public HashMap<Point, Integer> getParents() {
        return parents;
    }

    public int getPointsCnt() {
        return pointsCnt;
    }

    public void updateK() {
        int dataSetLength = getDataSetLength();
        if (k > dataSetLength) {
            k = dataSetLength;
            kmeans.setK(dataSetLength);
        } else if (k == 0) {
            k = dataSetLength;
            kmeans.setK(dataSetLength);
        }
    }

    /**
     * Initialization of the whole I-KMeans process
     */
    public void init() {
        allClusters = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            allClusters.add(new ArrayList<>());
        }
        centers = kmeans.initCenters(getDataSetLength(), dataSet);
        clusters = kmeans.initCluster();
    }

    /**
     * Add each point to its closest cluster
     */
    private void clusterSet() {
        for (int i = 0; i < getDataSetLength(); i++) {
            double currentDistance;
            double minDistance = kmeans.distance(dataSet.get(i), centers.get(0));
            int minLocation = 0;
            for (int j = 1; j < k; j++) {
                currentDistance = kmeans.distance(dataSet.get(i), centers.get(j));
                if (currentDistance < minDistance) {
                    minDistance = currentDistance;
                    minLocation = j;
                } else if (currentDistance == minDistance) {
                    if (Math.random() < 0.5) {
                        minLocation = j;
                    }
                }
            }
            clusters.get(minLocation).add(dataSet.get(i)); // add each point to its closest cluster
            Point point = new Point(dataSet.get(i).getX(), dataSet.get(i).getY());
            parents.put(point, minLocation); // Map each point to the cluster it belongs to
        }
    }

    /**
     * Set the new center for each cluster
     */
    private void setNewCenter() {
        for (int i = 0; i < k; i++) {
            int n = clusters.get(i).size();
            if (n != 0) {
                Point newCenter = new Point(0, 0);
                for (int j = 0; j < n; j++) {
                    newCenter.setX(newCenter.getX() + clusters.get(i).get(j).getX());
                    newCenter.setY(newCenter.getY() + clusters.get(i).get(j).getY());
                }
                // Calculate the average coordinate of all points in the cluster
                newCenter.setX(newCenter.getX() + centers.get(i).getX() * allClusters.get(i).size());
                newCenter.setX(newCenter.getX() / (n + allClusters.get(i).size()));
                newCenter.setY(newCenter.getY() + centers.get(i).getY() * allClusters.get(i).size());
                newCenter.setY(newCenter.getY() / (n + allClusters.get(i).size()));
                centers.set(i, newCenter);
            }
        }
    }

    /**
     * load the new batch of data and do incremental K-Means
     *
     * @param data the new batch of data
     */
    // TODO call init instead of reinit
    public void execute(List<Point> data) {
        setDataSet(data);
        if (getDataSetLength() == 0) {
            return;
        } else if (k == 0) {
            updateK();
            init();
        }
        clusterSet();
        setNewCenter();
        for (int j = 0; j < getK(); j++) {
            allClusters.get(j).addAll(clusters.get(j));
        }
        clusters.clear();
        clusters = kmeans.initCluster();
        pointsCnt += data.size();
    }
}
