package edgeBundling;

import models.Edge;
import models.Path;
import models.Point;

import java.util.ArrayList;

/**
 * Force Directed Edge Bundling Algorithm.
 */
public class ForceBundling {
    /** TODO replace EdgeVector to be Edge, and
     * use the point from edge class to get the position
     * from point data structure.
     */
    // All the data edges
    private ArrayList<Edge> dataEdges;
    // Turning points of the generated paths
    private ArrayList<Path> subdivisionPoints = new ArrayList<>();
    // Compatible edge indexes for each edge
    private ArrayList<ArrayList<Integer>> compatibilityList = new ArrayList<>();
    // Algorithm parameters
    // K: global bundling constant controlling edge stiffness
    private final double K = 0.1;
    // S: init. distance to move points
    private double S_initial = 0.02;
    // P_initial: init. subdivision number
    private final int P_initial = 1;
    // P_rate: subdivision rate increase
    private final int P_rate = 2;
    // C: number of cycles to perform
    private final double C = 3;
    // I_initial: init. number of iterations for cycle
    private final double I_initial = 50;
    // I_rate: rate at which iteration number decreases i.e. 2/3
    private final double I_rate = 2.0 / 3.0;
    // compatibility_threshold: the threshold score of deciding compatibility
    private final double compatibility_threshold = 0.6;
    // epsilon: decide the precision
    private final double eps = 1e-6;
    // isolatedEdgesCnt: unbundled edge count
    private int isolatedEdgesCnt = 0;

    /**
     * Constructor of fdeb algorithm.
     * @param dataEdges incoming data edges.
     */
    public ForceBundling(ArrayList<Edge> dataEdges) {
        this.dataEdges = dataEdges;
    }

    public int getIsolatedEdgesCnt() {
        return isolatedEdgesCnt;
    }

    /**
     * Sets different moving distance to each zoom level.
     * @param zoom current zoom level.
     */
    public void setS(int zoom) {
        S_initial = Math.max(0.025 - zoom * 0.0025, 0.001);
    }

    private double vectorDotProduct(Point p, Point q) {
        return p.getX() * q.getX() + p.getY() * q.getY();
    }

    /**
     * Calculates the vector format of edge.
     * @param e current edge.
     * @return corresponding vector.
     */
    private Point edgeAsVector(Edge e) {
        return new Point(e.getToX() - e.getFromX(), e.getToY() - e.getFromY());
    }

    private double edgeLength(Edge e) {
        if (Math.abs(e.getFromX() - e.getToX()) < eps &&
                Math.abs(e.getFromY() - e.getToY()) < eps) {
            return eps;
        }
        return e.length();
    }

    /**
     * Calculates the middle point of one edge.
     * @param e current edge.
     * @return corresponding middle point.
     */
    private Point edgeMidPoint(Edge e) {
        double midX = (e.getFromX() + e.getToX()) / 2.0;
        double midY = (e.getFromY() + e.getToY()) / 2.0;
        return new Point(midX, midY);
    }

    /**
     * Calculates distance between two points,
     * the distance of two edges is given by the distance
     * between the middle points of these two edges.
     * @param p source node.
     * @param q target node.
     * @return distance of two points.
     */
    private double euclideanDistance(Point p, Point q) {
        return Math.sqrt(Math.pow(p.getX() - q.getX(), 2) + Math.pow(p.getY() - q.getY(), 2));
    }

    /**
     * Calculates edge length after applying division.
     * @param e_ind the index of the edge.
     * @return edge length.
     */
    private double computeDividedEdgeLength(int e_ind) {
        double length = 0;
        for (int i = 1; i < subdivisionPoints.get(e_ind).getPath().size(); i++) {
            double segmentLength = euclideanDistance(subdivisionPoints.get(e_ind).getPath().get(i), subdivisionPoints.get(e_ind).getPath().get(i - 1));
            length += segmentLength;
        }
        return length;
    }

    /**
     * Project a point onto an edge.
     * @param p point to be projected.
     * @param q1 edge source.
     * @param q2 edge target.
     * @return Projected vector origins from edge source node.
     */
    private Point projectPointOnLine(Point p, Point q1, Point q2) {
        double L = Math.sqrt(Math.pow(q2.getX() - q1.getX(), 2) + Math.pow(q2.getY() - q1.getY(), 2));
        double r = ((q1.getY() - p.getY()) * (q1.getY() - q2.getY()) - (q1.getX() - p.getX()) * (q2.getX() - q1.getX())) / (Math.pow(L, 2));
        double x = q1.getX() + r * (q2.getX() - q1.getX());
        double y = q1.getY() + r * (q2.getY() - q1.getY());
        return new Point(x, y);
    }

    /**
     * Initialize the generated paths.
     */
    private void initializeEdgeSubdivisions() {
        for (int i = 0; i < dataEdges.size(); i++) {
            subdivisionPoints.add(new Path());
        }
    }

    /**
     * Initialize the compatibility lists.
     */
    private void initializeCompatibilityLists() {
        for (int i = 0; i < dataEdges.size(); i++) {
            compatibilityList.add(new ArrayList<>());
        }
    }

    /**
     * Apply the spring force within nodes in the same edge.
     * @param e_ind the index of the edge.
     * @param cp_ind the index of the control points within the edge.
     // TODO read what kp means from paper
     * @param kP parameter for calculation.
     * @return force vector.
     */
    private Point applySpringForce(int e_ind, int cp_ind, double kP) {
        if (subdivisionPoints.get(e_ind).getPath().size() <= 2) {
            return new Point(0, 0);
        }
        Point prev = subdivisionPoints.get(e_ind).getPath().get(cp_ind - 1);
        Point next = subdivisionPoints.get(e_ind).getPath().get(cp_ind + 1);
        Point crnt = subdivisionPoints.get(e_ind).getPath().get(cp_ind);
        double x = prev.getX() - crnt.getX() + next.getX() - crnt.getX();
        double y = prev.getY() - crnt.getY() + next.getY() - crnt.getY();
        x *= kP;
        y *= kP;
        return new Point(x, y);
    }

    /**
     * Apply the electrostatic force between edges.
     * @param e_ind the index of the edge
     * @param i the index of the node within the edge.
     * @return force vector
     */
    private Point applyElectrostaticForce(int e_ind, int i) {
        Point sumOfForces = new Point(0, 0);
        ArrayList<Integer> compatibleEdgeList = compatibilityList.get(e_ind);
        for (int oe = 0; oe < compatibleEdgeList.size(); oe++) {
            double x = subdivisionPoints.get(compatibleEdgeList.get(oe)).getPath().get(i).getX() - subdivisionPoints.get(e_ind).getPath().get(i).getX();
            double y = subdivisionPoints.get(compatibleEdgeList.get(oe)).getPath().get(i).getY() - subdivisionPoints.get(e_ind).getPath().get(i).getY();
            Point force = new Point(x, y);
            if ((Math.abs(force.getX()) > eps || Math.abs(force.getY()) > eps)) {
                Point source = subdivisionPoints.get(compatibleEdgeList.get(oe)).getPath().get(i);
                Point target = subdivisionPoints.get(e_ind).getPath().get(i);
                double diff = euclideanDistance(source, target);
                sumOfForces.setX(sumOfForces.getX() + force.getX() / diff);
                sumOfForces.setY(sumOfForces.getY() + force.getY() / diff);
            }
        }
        return sumOfForces;
    }

    /**
     * Calculates the net force.
     * @param e_ind the index of the edge.
     * @param P the subdivision number.
     * @param S the moving distance.
     * @return net forces
     */
    private ArrayList<Point> applyResultingForcesOnSubdivisionPoints(int e_ind, int P, double S) {
        double kP = K / (edgeLength(dataEdges.get(e_ind)) * (P + 1));
        ArrayList<Point> resultingForcesForSubdivisionPoints = new ArrayList<>();
        resultingForcesForSubdivisionPoints.add(new Point(0, 0));
        for (int i = 1; i < P + 1; i++) {
            Point resultingForce = new Point(0, 0);
            Point springForce = applySpringForce(e_ind, i, kP);
            Point electrostaticForce = applyElectrostaticForce(e_ind, i);
            resultingForce.setX(S * (springForce.getX() + electrostaticForce.getX()));
            resultingForce.setY(S * (springForce.getY() + electrostaticForce.getY()));
            resultingForcesForSubdivisionPoints.add(resultingForce);
        }
        resultingForcesForSubdivisionPoints.add(new Point(0, 0));
        return resultingForcesForSubdivisionPoints;
    }

    /**
     * Update the path points by applying net forces
     * @param P the subdivision number
     */
    private void updateEdgeDivisions(int P) {
        for (int e_ind = 0; e_ind < dataEdges.size(); e_ind++) {
            if (P == 1) {
                subdivisionPoints.get(e_ind).getPath().add(dataEdges.get(e_ind).getFromPoint());
                subdivisionPoints.get(e_ind).getPath().add(edgeMidPoint(dataEdges.get(e_ind)));
                subdivisionPoints.get(e_ind).getPath().add((dataEdges.get(e_ind).getToPoint()));
            } else {
                double dividedEdgeLength = computeDividedEdgeLength(e_ind);
                double segmentLength = dividedEdgeLength / (P + 1);
                double currentSegmentLength = segmentLength;
                ArrayList<Point> newDivisionPoints = new ArrayList<>();
                newDivisionPoints.add(dataEdges.get(e_ind).getFromPoint());
                // TODO revise the meaning they are iteratively dividing the edges again
                for (int i = 1; i < subdivisionPoints.get(e_ind).getPath().size(); i++) {
                    double oldSegmentLength = euclideanDistance(subdivisionPoints.get(e_ind).getPath().get(i), subdivisionPoints.get(e_ind).getPath().get(i - 1));
                    while (oldSegmentLength > currentSegmentLength) {
                        double percentPosition = currentSegmentLength / oldSegmentLength;
                        double newDivisionPointX = subdivisionPoints.get(e_ind).getPath().get(i - 1).getX();
                        double newDivisionPointY = subdivisionPoints.get(e_ind).getPath().get(i - 1).getY();
                        newDivisionPointX += percentPosition * (subdivisionPoints.get(e_ind).getPath().get(i).getX() - subdivisionPoints.get(e_ind).getPath().get(i - 1).getX());
                        newDivisionPointY += percentPosition * (subdivisionPoints.get(e_ind).getPath().get(i).getY() - subdivisionPoints.get(e_ind).getPath().get(i - 1).getY());
                        newDivisionPoints.add(new Point(newDivisionPointX, newDivisionPointY));
                        oldSegmentLength -= currentSegmentLength;
                        currentSegmentLength = segmentLength;
                    }
                    currentSegmentLength -= oldSegmentLength;
                }
                newDivisionPoints.add(dataEdges.get(e_ind).getToPoint());
                subdivisionPoints.get(e_ind).setPath(newDivisionPoints);
            }
        }
    }

    /**
     * Metric to measure the angle compatibility.
     * @param P the first edge
     * @param Q the second edge
     * @return score of compatibility
     */
    private double angleCompatibility(Edge P, Edge Q) {
        return Math.abs(vectorDotProduct(edgeAsVector(P), edgeAsVector(Q)) / (edgeLength(P) * edgeLength(Q)));
    }

    /**
     * Metric to measure the scale compatibility.
     * @param P the first edge
     * @param Q the second edge
     * @return score of compatibility
     */
    private double scaleCompatibility(Edge P, Edge Q) {
        double lavg = (edgeLength(P) + edgeLength(Q)) / 2.0;
        return 2.0 / (lavg / Math.min(edgeLength(P), edgeLength(Q)) + Math.max(edgeLength(P), edgeLength(Q)) / lavg);
    }

    /**
     * Metric to measure the position compatibility.
     * @param P the first edge
     * @param Q the second edge
     * @return score of compatibility
     */
    private double positionCompatibility(Edge P, Edge Q) {
        double lavg = (edgeLength(P) + edgeLength(Q)) / 2.0;
        Point midP = edgeMidPoint(P);
        Point midQ = edgeMidPoint(Q);
        return lavg / (lavg + euclideanDistance(midP, midQ));
    }

    /**
     * Metric to measure the visibility compatibility. (intersection part)
     * @param P the first edge
     * @param Q the second edge
     * @return score of compatibility
     */
    private double edgeVisibility(Edge P, Edge Q) {
        Point I0 = projectPointOnLine(Q.getFromPoint(), P.getFromPoint(), P.getToPoint());
        Point I1 = projectPointOnLine(Q.getToPoint(), P.getFromPoint(), P.getToPoint());
        Point midI = new Point(
                (I0.getX() + I1.getX()) / 2.0,
                (I0.getY() + I1.getY()) / 2.0
        );
        Point midP = edgeMidPoint(P);
        return Math.max(0, 1 - 2 * euclideanDistance(midP, midI) / euclideanDistance(I0, I1));
    }

    /**
     * Metric to measure the visibility compatibility.
     * @param P the first edge
     * @param Q the second edge
     * @return score of compatibility
     */
    private double visibilityCompatibility(Edge P, Edge Q) {
        return Math.min(edgeVisibility(P, Q), edgeVisibility(Q, P));
    }

    /**
     * Calculates the compatibility score.
     * @param P the first edge
     * @param Q the second edge
     * @return score of compatibility
     */
    private double compatibilityScore(Edge P, Edge Q) {
        return (angleCompatibility(P, Q) * scaleCompatibility(P, Q) * positionCompatibility(P, Q) * visibilityCompatibility(P, Q));
    }

    /**
     * Returns the compatible judgement.
     * @param P the first edge
     * @param Q the second edge
     * @return compatible result
     */
    private boolean areCompatible(Edge P, Edge Q) {
        return (compatibilityScore(P, Q) > compatibility_threshold);
    }

    /**
     * Calculates the compatibility lists.
     */
    private void computeCompatibilityLists() {
        for (int e = 0; e < dataEdges.size() - 1; e++) {
            // TODO compare based on some other metrics
            for (int oe = e + 1; oe < dataEdges.size(); oe++) {
                if (areCompatible(dataEdges.get(e), dataEdges.get(oe))) {
                    compatibilityList.get(e).add(oe);
                    compatibilityList.get(oe).add(e);
                }
            }
            if (compatibilityList.get(e).isEmpty()) {
                isolatedEdgesCnt++;
            }
        }
    }

    /**
     * Runs the edge bundle
     * @return bundling results (path)
     */
    public ArrayList<Path> forceBundle() {
        double S = S_initial;
        double I = I_initial;
        int P = P_initial;
        initializeEdgeSubdivisions();
        initializeCompatibilityLists();
        updateEdgeDivisions(P);
        computeCompatibilityLists();
        for (int cycle = 0; cycle < C; cycle++) {
            for (int iteration = 0; iteration < I; iteration++) {
                ArrayList<ArrayList<Point>> forces = new ArrayList<>();
                for (int edge = 0; edge < dataEdges.size(); edge++) {
                    forces.add(applyResultingForcesOnSubdivisionPoints(edge, P, S));
                }
                for (int e = 0; e < dataEdges.size(); e++) {
                    for (int i = 0; i < P + 1; i++) {
                        subdivisionPoints.get(e).getPath().get(i).setX(subdivisionPoints.get(e).getPath().get(i).getX() + forces.get(e).get(i).getX());
                        subdivisionPoints.get(e).getPath().get(i).setY(subdivisionPoints.get(e).getPath().get(i).getY() + forces.get(e).get(i).getY());
                    }
                }
            }
            S = S / 2;
            P = P * P_rate;
            I = I_rate * I;
            updateEdgeDivisions(P);
        }
        return subdivisionPoints;
    }
}
