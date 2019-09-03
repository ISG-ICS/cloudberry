package clustering;

import models.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Incremental K-Means Algorithm
 */
// TODO extend Kmeans
public class IKmeans {
    private Kmeans kmeans;
    private int k; // the number of clusters desired
    private List<double[]> dataSet; // the dataset for clustering
    private ArrayList<double[]> centers; // the list of centers of clusters
    private List<List<double[]>> clusters; // the list of clusters for the current batch of data
    private List<List<double[]>> allClusters; // the list of clusters for all accumulated data
    private int pointsCnt; // the count of points in all accumulated data
    private HashMap<Point, Integer> parents = new HashMap<>(); // map of points and its cluster

    public void setDataSet(List<double[]> dataSet) {
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

    public List<List<double[]>> getAllClusters() {
        return allClusters;
    }

    public ArrayList<double[]> getCenters() {
        return centers;
    }

    public HashMap<Point, Integer> getParents() {
        return parents;
    }

    public int getPointsCnt() {
        return pointsCnt;
    }

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

    /**
     * Initialization of the whole I-KMeans process
     */
    public void init() {
        int dataSetLength = getDataSetLength();
        if (k > dataSetLength) {
            k = dataSetLength;
            kmeans.setK(dataSetLength);
        }
        if (dataSet != null && dataSet.size() != 0) {
            allClusters = new ArrayList<>();
            for (int i = 0; i < k; i++) {
                allClusters.add(new ArrayList<>());
            }
            centers = kmeans.initCenters(dataSetLength, dataSet);
            clusters = kmeans.initCluster();
        }
    }

    /**
     * Add each point to its closest cluster
     */
    private void clusterSet() {
        Random random = new Random();
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
                    if (random.nextInt(10) < 5) {
                        minLocation = j;
                    }
                }
            }
            clusters.get(minLocation).add(dataSet.get(i)); // add each point to its closest cluster
            Point point = new Point(dataSet.get(i)[0], dataSet.get(i)[1]);
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
                double[] newCenter = {0, 0};
                for (int j = 0; j < n; j++) {
                    newCenter[0] += clusters.get(i).get(j)[0];
                    newCenter[1] += clusters.get(i).get(j)[1];
                }
                // Calculate the average coordinate of all points in the cluster
                newCenter[0] += centers.get(i)[0] * allClusters.get(i).size();
                newCenter[0] = newCenter[0] / (n + allClusters.get(i).size());
                newCenter[1] += centers.get(i)[1] * allClusters.get(i).size();
                newCenter[1] = newCenter[1] / (n + allClusters.get(i).size());
                centers.set(i, newCenter);
            }
        }
    }

    /**
     * Re-initialize the data structure when the latest batch size is no longer zero
     */
    private void reInit() {
        int dataSetLength = getDataSetLength();
        k = dataSetLength;
        kmeans.setK(dataSetLength);
        allClusters = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            allClusters.add(new ArrayList<>());
        }
        centers = kmeans.initCenters(dataSetLength, dataSet);
        clusters = kmeans.initCluster();
    }

    /**
     * load the new batch of data and do incremental K-Means
     *
     * @param data the new batch of data
     */
    // TODO call init instead of reinit
    public void execute(List<double[]> data) {
        setDataSet(data);
        if (dataSet == null || dataSet.size() == 0) {
            return;
        }
        if (k == 0 && getDataSetLength() > 0) {
            reInit();
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
