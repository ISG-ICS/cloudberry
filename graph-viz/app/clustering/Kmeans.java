package clustering;

import models.Point;

import java.util.*;

/**
 * K-Means algorithm
 */
public class Kmeans {
    private int k; // the number of clusters desired
    private int I; // the number of iterations
    // TODO use the dataset from Point class
    private List<double[]> dataSet; // the dataset for clustering
    private ArrayList<double[]> centers; // the list of centers of clusters
    // TODO use list of cluster class
    private List<List<double[]>> clusters; // the list of clusters for the whole dataset
    private double lastSquaredErrorSum;
    private HashMap<Point, Integer> parents = new HashMap<>(); // map of points and its cluster

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
    }

    public List<double[]> getDataSet() {
        return dataSet;
    }

    public HashMap<Point, Integer> getParents() {
        return parents;
    }

    public ArrayList<double[]> getCenters() {
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

    private void setDataSet(List<double[]> dataSet) {
        this.dataSet = dataSet;
    }

    public List<List<double[]>> getClusters() {
        return clusters;
    }


    /**
     * Initialization of the whole K-Means process
     */
    private void init() {
        I = 0;
        int dataSetLength = getDataSetLength();
        if (k > dataSetLength) {
            k = dataSetLength;
        }
        if (dataSet == null || dataSet.size() == 0) {
            return;
        }
        centers = initCenters(dataSetLength, dataSet);
        clusters = initCluster();
        lastSquaredErrorSum = 0;
    }

    /**
     * Initialize the list of centers corresponding to each cluster
     *
     * @return the list of centers
     */
    ArrayList<double[]> initCenters(int dataSetLength, List<double[]> dataSet) {
        Random random = new Random();
        ArrayList<double[]> center = new ArrayList<>();
        int[] randoms = new int[k];
        boolean flag;
        int temp = random.nextInt(dataSetLength);
        randoms[0] = temp;
        for (int i = 1; i < k; i++) {
            flag = true;
            while (flag) {
                temp = random.nextInt(dataSetLength);
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
        }
        for (int i = 0; i < k; i++) {
            center.add(dataSet.get(randoms[i]));
        }
        return center;
    }

    /**
     * Initialize the set of clusters
     *
     * @return a set of k empty clusters
     */
    List<List<double[]>> initCluster() {
        List<List<double[]>> cluster = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            cluster.add(new ArrayList<>());
        }

        return cluster;
    }

    /**
     * Calculate the distance between two points
     *
     * @param element points in dataset
     * @param center  centers of clusters
     * @return the computed distance
     */
    double distance(double[] element, double[] center) {
        double distance;
        double x = element[0] - center[0];
        double y = element[1] - center[1];
        double z = x * x + y * y;
        distance = Math.sqrt(z);

        return distance;
    }

    /**
     * Add each point to its closest cluster
     */
    private void clusterSet() {
        Random random = new Random();
        for (int i = 0; i < getDataSetLength(); i++) {
            double currentDistance;
            double minDistance = distance(dataSet.get(i), centers.get(0));
            int minLocation = 0;
            for (int j = 1; j < k; j++) {
                currentDistance = distance(dataSet.get(i), centers.get(j));
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
        }
    }

    /**
     * Map each point to the cluster it belongs to
     */
    private void findParents() {
        for (int i = 0; i < clusters.size(); i++) {
            for (int j = 0; j < clusters.get(i).size(); j++) {
                Point point = new Point(clusters.get(i).get(j)[0], clusters.get(i).get(j)[1]);
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
                newCenter[0] = newCenter[0] / n;
                newCenter[1] = newCenter[1] / n;
                centers.set(i, newCenter);
            }
        }
    }

    /**
     * the core method of K-Means
     */
    public void execute(List<double[]> dataSet) {
        setDataSet(dataSet);
        init();
        if (dataSet == null || dataSet.size() == 0) {
            return;
        }
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
            clusters.clear();
            clusters = initCluster();
            lastSquaredErrorSum = currentSquaredErrorSum;
        }
    }
}
