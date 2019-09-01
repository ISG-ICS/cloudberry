package models;

import java.util.Comparator;
import java.util.ArrayList;

public class KdTree {
    public static class Node {
        private Cluster point;
        private Rectangle rect;
        private boolean isVertical;
        private Node left;
        private Node right;

        private Node(Cluster p, boolean vertical, Node left, Node right, Rectangle rectangle) {
            this.point = p;
            this.isVertical = vertical;
            this.left = left;
            this.right = right;
            this.rect = rectangle;
        }

        public Rectangle getRect() {
            return this.rect;
        }

        public Cluster getPoint() {
            return point;
        }

        public boolean vertical() {
            return isVertical;
        }

        public Node getLeft() {
            return left;
        }

        public Node getRight() {
            return right;
        }

        public void setLeftNode(Node leftNode) {
            this.left = leftNode;
        }

        public void setRightNode(Node rightNode) {
            this.right = rightNode;
        }
    }

    private Node root;
    private int size;

    /**
     * return an instance of kd tree
     */
    public KdTree() {
        root = null;
        size = 0;
    }

    /**
     * check if the tree is empty
     *
     * @return if the tree is empty
     */
    public boolean isEmpty() {
        return root == null;
    }

    /**
     * get the number of points in the tree
     *
     * @return the number of points
     */
    public int size() {
        return size;
    }

    /**
     * Insert a cluster into kd tree
     *
     * @param p cluster
     */
    public void insert(Cluster p) {
        // base case: empty tree
        if (size == 0) {
            Rectangle rect = new Rectangle(0.0, 0.0, 1.0, 1.0);
            root = new Node(p, true, null, null, rect);
            size++;
            return;
        }

        // point at root initially
        Node n = root;

        Comparator<Cluster> comparator;

        while (n != null) {
            // if we are at a vertical node
            if (n.vertical()) {
                comparator = Cluster.X_ORDER;
            } else {
                comparator = Cluster.Y_ORDER;
            }
            // TODO create a new method split
            // go to the left if point left to vertical point or below a horizontal point
            if (comparator.compare(p, n.getPoint()) < 0) {
                // if the left point is null then create new node and set it
                if (n.getLeft() == null) {
                    Rectangle rect = null;
                    if (n.vertical()) {
                        // point to left of current point
                        rect = new Rectangle(n.getRect().xmin(), n.getRect().ymin(), n.getPoint().x(), n.getRect().ymax());
                    } else {
                        // point at bottom of current point
                        rect = new Rectangle(n.getRect().xmin(), n.getRect().ymin(), n.getRect().xmax(), n.getPoint().y());
                    }
                    // create new node to be inserted to
                    Node leftNode = new Node(p, !n.vertical(), null, null, rect);
                    n.setLeftNode(leftNode);
                    size++;
                    break;
                }

                n = n.getLeft();
            } else {
                // reached end so insert new node to right
                if (n.getRight() == null) {
                    Rectangle rect = null;

                    if (n.vertical()) {
                        // right to vertical point
                        rect = new Rectangle(n.getPoint().x(), n.getRect().ymin(), n.getRect().xmax(), n.getRect().ymax());
                    } else {
                        // top of horizontal point
                        rect = new Rectangle(n.getRect().xmin(), n.getPoint().y(), n.getRect().xmax(), n.getRect().ymax());
                    }
                    Node rightNode = new Node(p, !n.vertical(), null, null, rect);
                    n.setRightNode(rightNode);
                    size++;
                    break;
                }

                n = n.getRight();
            }
        }
    }

    /**
     * find if tree contains this cluster
     *
     * @param p cluster
     * @return if tree contains this custer
     */
    public boolean contains(Cluster p) {
        return containsRecursive(root, p);
    }

    private boolean containsRecursive(Node node, Cluster point) {
        if (node == null) {
            return false;
        }

        Cluster nPoint = node.getPoint();
        if (nPoint.equals(point)) {
            return true;
        }

        Comparator<Cluster> comparator;
        if (node.vertical()) {
            comparator = Cluster.X_ORDER;
        } else {
            comparator = Cluster.Y_ORDER;
        }

        if (comparator.compare(point, nPoint) < 0) {
            return containsRecursive(node.getLeft(), point);
        } else {
            return containsRecursive(node.getRight(), point);
        }
    }

    /**
     * find the cluster in tree
     *
     * @param point cluster
     * @return the cluster in tree
     */
    public Cluster findPoint(Cluster point) {
        return findPointRecursive(root, point);
    }

    private Cluster findPointRecursive(Node node, Cluster point) {
        if (node == null) {
            return null;
        }
        Cluster nPoint = node.getPoint();
        if (node.getPoint().equals(point)) {
            return node.getPoint();
        }
        Comparator<Cluster> comparator;
        if (node.vertical()) {
            comparator = Cluster.X_ORDER;
        } else {
            comparator = Cluster.Y_ORDER;
        }

        if (comparator.compare(point, nPoint) < 0) {
            return findPointRecursive(node.getLeft(), point);
        } else {
            return findPointRecursive(node.getRight(), point);
        }
    }

    /**
     * find all the cluster in certain rectangle
     *
     * @param rect rectangle
     * @return array of cluster
     */
    public ArrayList<Cluster> range(Rectangle rect) {
        ArrayList<Cluster> pointsInRange = new ArrayList<>();
        rangeRecursive(pointsInRange, rect, root);
        return pointsInRange;
    }

    private void rangeRecursive(ArrayList<Cluster> rangeList, Rectangle rect, Node n) {
        if (n == null)
            return;

        Cluster p = n.getPoint();
        if (rect.contains(p))
            rangeList.add(p);

        double pointCoord = p.y();
        double rectMin = rect.ymin();
        double rectMax = rect.ymax();
        if (n.vertical()) {
            pointCoord = p.x();
            rectMin = rect.xmin();
            rectMax = rect.xmax();
        }

        if (pointCoord > rectMin)
            rangeRecursive(rangeList, rect, n.getLeft());
        if (pointCoord <= rectMax)
            rangeRecursive(rangeList, rect, n.getRight());
    }

    /**
     * find all the cluster in certain circle
     *
     * @param x x coordinate of center of circle
     * @param y y coordinate of center of circle
     * @param r radius of circle
     * @return array of cluster
     */
    public ArrayList<Cluster> rangeRadius(double x, double y, double r) {
        ArrayList<Cluster> pointsInRange = new ArrayList<>();
        rangeRadiusRecursive(pointsInRange, x, y, r, root);
        return pointsInRange;
    }

    private void rangeRadiusRecursive(ArrayList<Cluster> rangeList, double x, double y, double r, Node n) {
        if (n == null) {
            return;
        }
        Cluster p = n.getPoint();
        if (p.distanceTo(new Cluster(x, y)) <= r) {
            rangeList.add(p);
        }
        double pointCoord = p.y();
        double circleMin = y - r;
        double circleMax = y + r;
        if (n.vertical()) {
            pointCoord = p.x();
            circleMin = x - r;
            circleMax = x + r;
        }
        if (pointCoord > circleMin) rangeRadiusRecursive(rangeList, x, y, r, n.getLeft());
        if (pointCoord <= circleMax) rangeRadiusRecursive(rangeList, x, y, r, n.getRight());
    }
}
