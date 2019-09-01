package algorithms;

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
    private int dataSetLength; // the number of points in the dataset
    private List<double[]> dataSet; // the dataset for clustering
    private ArrayList<double[]> center; // the list of centers of clusters
    private List<List<double[]>> cluster; // the list of clusters for the current batch of data
    private List<List<double[]>> allCluster; // the list of clusters for all accumulated data
    private int pointsCnt; // the count of points in all accumulated data
    private Random random;
    private HashMap<Point, Integer> parents = new HashMap<>(); // map of points and its cluster

    public void setDataSet(List<double[]> dataSet) {
        this.dataSet = dataSet;
        if (dataSet == null || dataSet.size() == 0) {
            System.out.println("No Data for this batch.");
            dataSetLength = 0;
        } else {
            dataSetLength = dataSet.size();
        }
    }

    public int getK() {
        return k;
    }

    public List<List<double[]>> getAllCluster() {
        return allCluster;
    }

    public ArrayList<double[]> getCenter() {
        return center;
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
        random = new Random();
        if (k > dataSetLength) {
            k = dataSetLength;
            kmeans.setK(dataSetLength);
        }
        if (dataSet != null && dataSet.size() != 0) {
            allCluster = new ArrayList<>();
            for (int i = 0; i < k; i++) {
                allCluster.add(new ArrayList<>());
            }
            center = kmeans.initCenters(dataSetLength, dataSet, random);
            cluster = kmeans.initCluster();
        }
    }

    /**
     * Add each point to its closest cluster
     */
    private void clusterSet() {
        double[] distance = new double[k];
        for (int i = 0; i < dataSetLength; i++) {
            for (int j = 0; j < k; j++) {
                distance[j] = kmeans.distance(dataSet.get(i), center.get(j));
            }
            int minLocation = kmeans.minDistance(distance, random);
            cluster.get(minLocation).add(dataSet.get(i)); // add each point to its closest cluster
            Point point = new Point(dataSet.get(i)[0], dataSet.get(i)[1]);
            parents.put(point, minLocation); // Map each point to the cluster it belongs to
        }
    }

    /**
     * Set the new center for each cluster
     */
    private void setNewCenter() {
        for (int i = 0; i < k; i++) {
            int n = cluster.get(i).size();
            if (n != 0) {
                double[] newCenter = {0, 0};
                for (int j = 0; j < n; j++) {
                    newCenter[0] += cluster.get(i).get(j)[0];
                    newCenter[1] += cluster.get(i).get(j)[1];
                }
                // Calculate the average coordinate of all points in the cluster
                newCenter[0] += center.get(i)[0] * allCluster.get(i).size();
                newCenter[0] = newCenter[0] / (n + allCluster.get(i).size());
                newCenter[1] += center.get(i)[1] * allCluster.get(i).size();
                newCenter[1] = newCenter[1] / (n + allCluster.get(i).size());
                center.set(i, newCenter);
            }
        }
    }

    /**
     * Re-initialize the data structure when the latest batch size is no longer zero
     */
    private void reInit() {
        k = dataSetLength;
        kmeans.setK(dataSetLength);
        allCluster = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            allCluster.add(new ArrayList<>());
        }
        center = kmeans.initCenters(dataSetLength, dataSet, random);
        cluster = kmeans.initCluster();
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
        if (k == 0 && dataSetLength > 0) {
            reInit();
        }
        clusterSet();
        setNewCenter();
        for (int j = 0; j < getK(); j++) {
            allCluster.get(j).addAll(cluster.get(j));
        }
        cluster.clear();
        cluster = kmeans.initCluster();
        pointsCnt += data.size();
    }
}
