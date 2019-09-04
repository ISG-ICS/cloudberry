package clustering;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Incremental K-Means Algorithm
 */
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

    @Override
    public int getDataSetLength() {
        return pointsCnt;
    }

    @Override
    public void setDataSet(List<Point> dataSet) {
        this.dataSet = dataSet;
        pointsCnt += dataSet.size();
    }

    /**
     * Initialization of the whole I-KMeans process
     */
    public void init() {
        allClusters = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            allClusters.add(new ArrayList<>());
        }
        centers = initCenters();
        clusters = initCluster();
    }

    /**
     * Add each point to its closest cluster
     */
    @Override
    void clusterSet() {
        int minLocation;
        for (int i = 0; i < dataSet.size(); i++) {
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
        boolean isFirst = dataSet == null;
        setDataSet(data);
        if (isFirst) {
            if (k > dataSet.size()) {
                k = dataSet.size();
            }
            if (k != 0) {
                init();
            }
        } else {
            if (k == 0 && dataSet.size() > 0) {
                k = dataSet.size();
                init();
            }
        }
        if (k == 0) return;
        clusterSet();
        setNewCenter();
        for (int j = 0; j < getK(); j++) {
            allClusters.get(j).addAll(clusters.get(j));
        }
        clusters.clear();
        clusters = initCluster();
    }

    @Override
    public ArrayNode getClustersJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (int i = 0; i < getK(); i++) {
            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.putArray("coordinates").add(getCenters().get(i).getX()).add(getCenters().get(i).getY());
            objectNode.put("size", getAllClusters().get(i).size());
            arrayNode.add(objectNode);
        }
        return arrayNode;
    }
}
