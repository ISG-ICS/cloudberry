package clustering;

import models.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Incremental K-Means Algorithm
 */
// TODO extend Kmeans
public class IKmeans extends Kmeans {
    private List<List<Point>> allClusters; // the list of clusters for all accumulated data
    private int pointsCnt; // the count of points in all accumulated data

    /**
     * Constructor for k
     *
     * @param k Number of clusters
     */
    public IKmeans(int k) {
        super(k);
    }

    public List<List<Point>> getAllClusters() {
        return allClusters;
    }

    public int getPointsCnt() {
        return pointsCnt;
    }

    /**
     * Update k according the new batch size
     */
    public void updateK() {
        int dataSetLength = getDataSetLength();
        if (k > dataSetLength) {
            k = dataSetLength;
            setK(dataSetLength);
        } else if (k == 0) {
            k = dataSetLength;
            setK(dataSetLength);
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
        centers = initCenters(getDataSetLength(), dataSet);
        clusters = initCluster();
    }

    /**
     * Add each point to its closest cluster
     */
    @Override
    void clusterSet() {
        int minLocation;
        for (int i = 0; i < getDataSetLength(); i++) {
            minLocation = assignPoint(i);
            Point point = new Point(dataSet.get(i).getX(), dataSet.get(i).getY());
            parents.put(point, minLocation); // Map each point to the cluster it belongs to
        }
    }

    /**
     * Set the new center for each cluster
     */
    @Override
    void setNewCenter() {
        for (int i = 0; i < k; i++) {
            int n = clusters.get(i).size();
            if (n != 0) {
                Point newCenter = initNewCenter(i, n);
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
        clusters = initCluster();
        pointsCnt += data.size();
    }
}
