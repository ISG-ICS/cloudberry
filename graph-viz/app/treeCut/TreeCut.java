package treeCut;

import clustering.Clustering;
import models.Cluster;
import models.Edge;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of tree cut algorithm
 * screen: |    ·A______________·B----------|---·E
 *         |     \                          |
 *         |      \     .F------------------|---.H
 *         |       \                        |
 *         |        ·C____________·D--------|---·G
 *         |                                |
 *
 * A B C D E G are the clusters, A B C D F inside screen, E G H outside screen
 * A-C, A-B, C-D are three edges that have both their two ends inside screen
 * B-E, D-G, F-H are two edges that has only one of their ends inside the screen, for B-E is B inside, for D-G is D inside
 *
 * assume A, B, C, D, E, F, G, H has their parent cluster as  A(W), B(X), C(W), D(Y), E(Z), F(X), G(Y), H(V)
 * lowerLongitude, upperLongitude, lowerLatitude, upperLatitude form the bounding box of the user screen
 *
 * For this screen: data structures in this algorithm have the following value:
 * externalCluster: [E, G, H]
 * internalCluster: [A, B, C, D]
 * externalAncestorToChildren: [X->(E), Y->(G), V->(H)]
 * internalAncestor: [W, X, Y, Z]
 * externalChildToAncestor: [E -> E, G -> G, H->V]
 */
public class TreeCut {

    // The final ancestor cluster to be mapped to for each external cluster
    private HashMap<Cluster, Cluster> externalChildToAncestor;
    // Current ancestor points to the list of its descendant external clusters
    private HashMap<Cluster, ArrayList<Cluster>> externalAncestorToChildren;
    // Current ancestor points of internal clusters
    private HashSet<Cluster> internalAncestor;

    /**
     * Constructor for TreeCut
     */
    public TreeCut() {
        externalChildToAncestor = new HashMap<>();
        externalAncestorToChildren = new HashMap<>();
        internalAncestor = new HashSet<>();
    }

    /**
     * Main process of tree cut algorithm.
     *
     * @param clustering      hierarchical structure from HGC algorithm
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
    public void treeCut(Clustering clustering, double lowerLongitude,
                        double upperLongitude, double lowerLatitude,
                        double upperLatitude, int zoom,
                        HashMap<Edge, Integer> edges, HashSet<Edge> externalEdgeSet,
                        HashSet<Cluster> externalCluster, HashSet<Cluster> internalCluster) {

        // initialize the hierarchy
        addExternalClusterHierarchy(externalCluster);
        addInternalClusterHierarchy(internalCluster);
        while (externalAncestorToChildren.size() != 0) {
            // Find clusters has common ancestor with internal clusters at this level and remove
            externalAncestorToChildren.entrySet().removeIf(clusterEntry -> {
                Cluster ancestorCluster = clusterEntry.getKey();
                if (internalAncestor.contains(ancestorCluster)) {
                    int level = clusterEntry.getKey().getZoom();
                    // use a level lower of ancestor to be mapped to
                    int elevateLevel = clusterEntry.getValue().get(0).getZoom() - level - 1;
                    updateExternalClusterMap(clusterEntry, elevateLevel);
                    return true;
                }
                return false;
            });
            // elevate all remaining clusters to a higher level
            elevateExternalHierarchy();
            elevateInternalHierarchy();
        }
        updateEdgeSet(clustering, lowerLongitude, upperLongitude, lowerLatitude, upperLatitude, zoom, edges, externalEdgeSet);
    }


    /**
     * Elevates the hierarchy of internal clusters.
     *
     */
    private void elevateInternalHierarchy() {
        internalAncestor = internalAncestor.stream().map(ancestor -> ancestor.parent).filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Elevates the hierarchy of external clusters
     *
     */
    // TODO not creating new instance
    private void elevateExternalHierarchy() {
        externalAncestorToChildren = externalAncestorToChildren.entrySet().stream().filter(ancestorToChildren -> {
            Cluster ancestor = ancestorToChildren.getKey();
            if (ancestor.parent == null) {
                int elevateLevel = ancestorToChildren.getValue().get(0).getZoom() - 1;
                updateExternalClusterMap(ancestorToChildren, elevateLevel);
                return false;
            }
            return true;
        }).collect(Collectors.toMap(e -> e.getKey().parent, Map.Entry::getValue, (prev, next) -> {
            prev.addAll(next);
            return prev;
        }, HashMap::new));
    }

    /**
     * Initializes the hierarchy of internal clusters.
     *
     * @param internalCluster original internal cluster
     */
    private void addInternalClusterHierarchy(HashSet<Cluster> internalCluster) {
        for (Cluster ancestor : internalCluster) {
            if (ancestor.parent != null) {
                internalAncestor.add(ancestor.parent);
            }
        }
    }

    /**
     * Initializes the hierarchy of external clusters.
     *
     * @param externalCluster original external cluster
     */
    private void addExternalClusterHierarchy(HashSet<Cluster> externalCluster) {
        for (Cluster externalChild : externalCluster) {
            if (externalChild.parent != null) {
                if (!externalAncestorToChildren.containsKey(externalChild.parent)) {
                    externalAncestorToChildren.put(externalChild.parent, new ArrayList<>());
                }
                externalAncestorToChildren.get(externalChild.parent).add(externalChild);
            }
            // has arrived highest level
            else {
                // use level 1 (self) to make the elevation
                // ancestor is null to indicate the finally the cluster is mapped to itself
                externalChildToAncestor.put(externalChild, null);
            }
        }

    }

    /**
     * add the mapping results to the externalCLusterMap
     *
     * @param entry        external hierarchy map entry
     * @param elevateLevel the level that the clusters to be elevated
     */
    private void updateExternalClusterMap(Map.Entry<Cluster, ArrayList<Cluster>> entry, int elevateLevel) {
        Cluster ancestor;
        for (Cluster child : entry.getValue()) {
            // ancestor is null if the finally the cluster is mapped to itself
            ancestor = elevateLevel == 0 ? null : child;
            for (int i = 0; i < elevateLevel; i++) {
                ancestor = ancestor.parent;
            }
            externalChildToAncestor.put(child, ancestor);
        }
    }

    /**
     * add the results to the returning edge set
     *
     * @param clustering      hierarchical structure from HGC algorithm
     * @param lowerLongitude  lowerLongitude of user screen
     * @param upperLongitude  upperLongitude of user screen
     * @param lowerLatitude   lowerLatitude of user screen
     * @param upperLatitude   upperLatitude of user screen
     * @param zoom            zoom level of user screen
     * @param edges           edge set to be returned
     * @param externalEdgeSet edge set with only one node inside screen
     */
    private void updateEdgeSet(Clustering clustering, double lowerLongitude, double upperLongitude, double lowerLatitude, double upperLatitude, int zoom, HashMap<Edge, Integer> edges, HashSet<Edge> externalEdgeSet) {
        for (Edge edge : externalEdgeSet) {
            // add the edge in the edge set
            Cluster fromCluster = clustering.parentCluster(new Cluster(Clustering.lngX(edge.getFromX()), Clustering.latY(edge.getFromY())), zoom);
            Cluster toCluster = clustering.parentCluster(new Cluster(Clustering.lngX(edge.getToX()), Clustering.latY(edge.getToY())), zoom);
            double fromLongitude = Clustering.xLng(fromCluster.getX());
            double fromLatitude = Clustering.yLat(fromCluster.getY());
            double insideLat, insideLng, outsideLat, outsideLng;
            Cluster elevatedCluster;
            if (lowerLongitude <= fromLongitude && fromLongitude <= upperLongitude
                    && lowerLatitude <= fromLatitude && fromLatitude <= upperLatitude) {
                insideLng = fromLongitude;
                insideLat = fromLatitude;
                elevatedCluster = externalChildToAncestor.get(toCluster);
                if(elevatedCluster == null) elevatedCluster = toCluster;
            } else {
                insideLng = Clustering.xLng(toCluster.getX());
                insideLat = Clustering.yLat(toCluster.getY());
                elevatedCluster = externalChildToAncestor.get(fromCluster);
                if(elevatedCluster == null) elevatedCluster = fromCluster;
            }
            outsideLng = Clustering.xLng(elevatedCluster.getX());
            outsideLat = Clustering.yLat(elevatedCluster.getY());
            Edge e = new Edge(insideLng, insideLat, outsideLng, outsideLat);
            if (edges.containsKey(e)) {
                edges.put(e, edges.get(e) + 1);
            } else {
                edges.put(e, 1);
            }
        }
    }


    /**
     * Not applying tree cut algorithm
     *
     * @param clustering      hierarchical structure from HGC algorithm
     * @param zoom            zoom level of user screen
     * @param edges           edge set to be returned
     * @param externalEdgeSet edge set with only one node inside screen
     */
    public void nonTreeCut(Clustering clustering, int zoom, HashMap<Edge, Integer> edges, HashSet<Edge> externalEdgeSet) {
        for (Edge edge : externalEdgeSet) {
            Cluster fromCluster = clustering.parentCluster(new Cluster(Clustering.lngX(edge.getFromX()), Clustering.latY(edge.getFromY())), zoom);
            Cluster toCluster = clustering.parentCluster(new Cluster(Clustering.lngX(edge.getToX()), Clustering.latY(edge.getToY())), zoom);
            double fromLongitude = Clustering.xLng(fromCluster.getX());
            double fromLatitude = Clustering.yLat(fromCluster.getY());
            double toLongitude = Clustering.xLng(toCluster.getX());
            double toLatitude = Clustering.yLat(toCluster.getY());
            Edge e = new Edge(fromLongitude, fromLatitude, toLongitude, toLatitude);
            if (edges.containsKey(e)) {
                edges.put(e, edges.get(e) + 1);
            } else {
                edges.put(e, 1);
            }
        }
    }
}
