package algorithms;

import models.Cluster;
import models.Edge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Implementation of tree cut algorithm
 */
public class TreeCut {

    /**
     * Main process of tree cut algorithm.
     *
     * @param pointCluster    hierarchical structure from HGC algorithm
     * @param lowerLongitude  lowerLongitude of user screen
     * @param upperLongitude  upperLongitude of user screen
     * @param lowerLatitude   lowerLatitude of user screen
     * @param upperLatitude   upperLatitude of user screen
     * @param zoom            zoom level of user screen
     * @param edges           edge set to be returned
     * @param externalEdgeSet edge set with only one node inside screen
     * @param externalCluster outside cluster corresponding to edge set with only one node inside screen
     * @param internalCluster inside screen clusters
     */
    public void treeCut(PointCluster pointCluster, double lowerLongitude,
                        double upperLongitude, double lowerLatitude,
                        double upperLatitude, int zoom,
                        HashMap<Edge, Integer> edges, HashSet<Edge> externalEdgeSet,
                        HashSet<Cluster> externalCluster, HashSet<Cluster> internalCluster) {
        // The final ancestor cluster to be mapped to for each external cluster
        HashMap<Cluster, Cluster> externalClusterMap = new HashMap<>();
        // Current ancestor points to the list of its descendant external clusters
        HashMap<Cluster, ArrayList<Cluster>> externalHierarchy = new HashMap<>();
        // Current ancestor points of internal clusters
        HashSet<Cluster> internalHierarchy = new HashSet<>();
        // initialize the hierarchy
        addExternalClusterHierarchy(externalCluster, externalHierarchy, externalClusterMap);
        addInternalClusterHierarchy(internalCluster, internalHierarchy);
        // TODO change breaking clause
        while (true) {
            // The ancestors has conflict
            ArrayList<Cluster> removeAncestors = new ArrayList<>();
            // Find clusters has common ancestor with internal clusters at this level
            for (Map.Entry<Cluster, ArrayList<Cluster>> clusterEntry : externalHierarchy.entrySet()) {
                Cluster ancestorCluster = clusterEntry.getKey();
                if (internalHierarchy.contains(ancestorCluster)) {
                    int level = clusterEntry.getKey().getZoom();
                    // use a level lower of ancestor to be mapped to
                    int elevateLevel = clusterEntry.getValue().get(0).getZoom() - level - 1;
                    updateExternalClusterMap(externalClusterMap, clusterEntry, elevateLevel);
                    removeAncestors.add(ancestorCluster);
                }
            }
            // remove all clusters stop at this level
            for (Cluster c : removeAncestors) {
                externalHierarchy.remove(c);
            }
            if (externalHierarchy.size() == 0) {
                break;
            }
            // elevate all remaining clusters to a higher level
            externalHierarchy = elevateExternalHierarchy(externalHierarchy, externalClusterMap);
            internalHierarchy = elevateInternalHierarchy(internalHierarchy);
        }
        for (Edge edge : externalEdgeSet) {
            // add the edge in the edge set
            updateEdgeSet(pointCluster, lowerLongitude, upperLongitude, lowerLatitude, upperLatitude, zoom, edges, externalClusterMap, edge);
        }
    }

    // TODO change method name
    /**
     * Elevates the hierarchy of internal clusters.
     *
     * @param internalHierarchy internal cluster set
     * @return elevated cluster
     */
    private HashSet<Cluster> elevateInternalHierarchy(HashSet<Cluster> internalHierarchy) {
        HashSet<Cluster> tempInternalHierarchy = new HashSet<>();
        for (Cluster c : internalHierarchy) {
            if (c.parent != null) {
                tempInternalHierarchy.add(c.parent);
            }
        }
        return tempInternalHierarchy;
    }

    /**
     * Elevates the hierarchy of external clusters
     *
     * @param externalHierarchy  external cluster map to the ancestor
     * @param externalClusterMap cluster in the final result each external cluster mapped to
     * @return elevated cluster
     */
    private HashMap<Cluster, ArrayList<Cluster>> elevateExternalHierarchy(HashMap<Cluster, ArrayList<Cluster>> externalHierarchy, HashMap<Cluster, Cluster> externalClusterMap) {
        HashMap<Cluster, ArrayList<Cluster>> tempExternalHierarchy = new HashMap<>();
        for (Map.Entry<Cluster, ArrayList<Cluster>> entry : externalHierarchy.entrySet()) {
            Cluster key = entry.getKey();
            if (key.parent != null) {
                if (!tempExternalHierarchy.containsKey(key.parent)) {
                    tempExternalHierarchy.put(key.parent, new ArrayList<>());
                }
                tempExternalHierarchy.get(key.parent).addAll(entry.getValue());
            }
            // has arrived highest level
            else {
                // use level 1 to make the elevation
                int elevateLevel = entry.getValue().get(0).getZoom() - 1;
                updateExternalClusterMap(externalClusterMap, entry, elevateLevel);
            }
        }
        return tempExternalHierarchy;
    }

    // TODO change method name
    /**
     * Initializes the hierarchy of internal clusters.
     *
     * @param internalHierarchy initialized ancestor cluster set
     * @param internalCluster   original internal cluster
     */
    private void addInternalClusterHierarchy(HashSet<Cluster> internalCluster, HashSet<Cluster> internalHierarchy) {
        for (Cluster c : internalCluster) {
            if (c.parent != null) {
                internalHierarchy.add(c.parent);
            }
        }
    }

    /**
     * Initializes the hierarchy of external clusters.
     *
     * @param externalCluster    original external cluster
     * @param externalHierarchy  external cluster map to the ancestor
     * @param externalClusterMap cluster in the final result each external cluster mapped to
     */
    private void addExternalClusterHierarchy(HashSet<Cluster> externalCluster, HashMap<Cluster, ArrayList<Cluster>> externalHierarchy, HashMap<Cluster, Cluster> externalClusterMap) {
        for (Cluster c : externalCluster) {
            if (c.parent != null) {
                if (!externalHierarchy.containsKey(c.parent)) {
                    externalHierarchy.put(c.parent, new ArrayList<>());
                }
                externalHierarchy.get(c.parent).add(c);
            }
            // has arrived highest level
            else {
                // use level 1 (self) to make the elevation
                externalClusterMap.put(c, c);
            }
        }

    }

    /**
     * add the mapping results to the externalCLusterMap
     * @param externalClusterMap cluster in the final result each external cluster mapped to
     * @param entry external hierarchy map entry
     * @param elevateLevel the level that the clusters to be elevated
     */
    private void updateExternalClusterMap(HashMap<Cluster, Cluster> externalClusterMap, Map.Entry<Cluster, ArrayList<Cluster>> entry, int elevateLevel) {
        Cluster mappedToCluster;
        for (Cluster c : entry.getValue()) {
            mappedToCluster = c;
            for (int i = 0; i < elevateLevel; i++) {
                mappedToCluster = mappedToCluster.parent;
            }
            externalClusterMap.put(c, mappedToCluster);
        }
    }

    /**
     * add the results to the returning edge set
     * @param pointCluster    hierarchical structure from HGC algorithm
     * @param lowerLongitude  lowerLongitude of user screen
     * @param upperLongitude  upperLongitude of user screen
     * @param lowerLatitude   lowerLatitude of user screen
     * @param upperLatitude   upperLatitude of user screen
     * @param zoom            zoom level of user screen
     * @param edges           edge set to be returned
     * @param externalClusterMap cluster in the final result each external cluster mapped to
     * @param edge returning edge set
     */
    private void updateEdgeSet(PointCluster pointCluster, double lowerLongitude, double upperLongitude, double lowerLatitude, double upperLatitude, int zoom, HashMap<Edge, Integer> edges, HashMap<Cluster, Cluster> externalClusterMap, Edge edge) {
        Cluster fromCluster = pointCluster.parentCluster(new Cluster(PointCluster.lngX(edge.getFromLongitude()), PointCluster.latY(edge.getFromLatitude())), zoom);
        Cluster toCluster = pointCluster.parentCluster(new Cluster(PointCluster.lngX(edge.getToLongitude()), PointCluster.latY(edge.getToLatitude())), zoom);
        double fromLongitude = PointCluster.xLng(fromCluster.x());
        double fromLatitude = PointCluster.yLat(fromCluster.y());
        double insideLat, insideLng, outsideLat, outsideLng;
        Cluster elevatedCluster;
        if (lowerLongitude <= fromLongitude && fromLongitude <= upperLongitude
                && lowerLatitude <= fromLatitude && fromLatitude <= upperLatitude) {
            insideLng = fromLongitude;
            insideLat = fromLatitude;
            elevatedCluster = externalClusterMap.get(toCluster);
        } else {
            insideLng = PointCluster.xLng(toCluster.x());
            insideLat = PointCluster.yLat(toCluster.y());
            elevatedCluster = externalClusterMap.get(fromCluster);
        }
        outsideLng = PointCluster.xLng(elevatedCluster.x());
        outsideLat = PointCluster.yLat(elevatedCluster.y());
        Edge e = new Edge(insideLat, insideLng, outsideLat, outsideLng);
        if (edges.containsKey(e)) {
            edges.put(e, edges.get(e) + 1);
        } else {
            edges.put(e, 1);
        }
    }


    /**
     * Not applying tree cut algorithm
     *
     * @param pointCluster    hierarchical structure from HGC algorithm
     * @param zoom            zoom level of user screen
     * @param edges           edge set to be returned
     * @param externalEdgeSet edge set with only one node inside screen
     */
    public void nonTreeCut(PointCluster pointCluster, int zoom, HashMap<Edge, Integer> edges, HashSet<Edge> externalEdgeSet) {
        for (Edge edge : externalEdgeSet) {
            Cluster fromCluster = pointCluster.parentCluster(new Cluster(PointCluster.lngX(edge.getFromLongitude()), PointCluster.latY(edge.getFromLatitude())), zoom);
            Cluster toCluster = pointCluster.parentCluster(new Cluster(PointCluster.lngX(edge.getToLongitude()), PointCluster.latY(edge.getToLatitude())), zoom);
            double fromLongitude = PointCluster.xLng(fromCluster.x());
            double fromLatitude = PointCluster.yLat(fromCluster.y());
            double toLongitude = PointCluster.xLng(toCluster.x());
            double toLatitude = PointCluster.yLat(toCluster.y());
            Edge e = new Edge(fromLatitude, fromLongitude, toLatitude, toLongitude);
            if (edges.containsKey(e)) {
                edges.put(e, edges.get(e) + 1);
            } else {
                edges.put(e, 1);
            }
        }
    }
}
