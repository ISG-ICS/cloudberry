package models;

import java.util.ArrayList;

public class KdTree {

    /**
     * node in kd-tree
     */
    public static class Node {
        // drawPoints of this node
        private Cluster point;
        // the rectangle this node contains
        private Rectangle rect;
        // whether this node is in vertical version or not
        private boolean isVertical;
        // left son of this node
        private Node left;
        // right son of this node
        private Node right;

        /**
         * constructor of node
         *
         * @param p         drawPoints of this node
         * @param vertical  whether this node is in vertical version or not
         * @param left      left son of this node
         * @param right     right son of this node
         * @param rectangle the rectangle this node contains
         */
        private Node(Cluster p, boolean vertical, Node left, Node right, Rectangle rectangle) {
            this.point = p;
            this.isVertical = vertical;
            this.left = left;
            this.right = right;
            this.rect = rectangle;
        }

        Rectangle getRect() {
            return this.rect;
        }

        public Cluster getPoint() {
            return point;
        }

        boolean vertical() {
            return isVertical;
        }

        public Node getLeft() {
            return left;
        }

        public Node getRight() {
            return right;
        }

        void setLeftNode(Node leftNode) {
            this.left = leftNode;
        }

        void setRightNode(Node rightNode) {
            this.right = rightNode;
        }
    }

    // the root of kd-tree
    private Node root;
    // the number of nodes
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
     * Insert a drawPoints into kd tree
     *
     * @param p drawPoints
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
        // go to the left if point left to vertical point or below a horizontal point
        while (n != null) {
            // if we are at a vertical node
            if (compare(p, n)) {
                // if the left point is null then create new node and set it
                if (n.getLeft() == null) {
                    Rectangle rect;
                    if (n.vertical()) {
                        // point to left of current point
                        rect = new Rectangle(n.getRect().getMinX(), n.getRect().getMinY(), n.getPoint().getX(), n.getRect().getMaxY());
                    } else {
                        // point at bottom of current point
                        rect = new Rectangle(n.getRect().getMinX(), n.getRect().getMinY(), n.getRect().getMaxX(), n.getPoint().getY());
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
                    Rectangle rect;
                    if (n.vertical()) {
                        // right to vertical point
                        rect = new Rectangle(n.getPoint().getX(), n.getRect().getMinY(), n.getRect().getMaxX(), n.getRect().getMaxY());
                    } else {
                        // top of horizontal point
                        rect = new Rectangle(n.getRect().getMinX(), n.getPoint().getY(), n.getRect().getMaxX(), n.getRect().getMaxY());
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
     * find if tree contains this drawPoints
     *
     * @param p drawPoints
     * @return if tree contains this custer
     */
    public boolean contains(Cluster p) {
        return containsRecursive(root, p);
    }

    private boolean containsRecursive(Node node, Cluster point) {
        if (node == null) {
            return false;
        }

        if (node.getPoint().equals(point)) {
            return true;
        }

        if (compare(point, node)) {
            return containsRecursive(node.getLeft(), point);
        } else {
            return containsRecursive(node.getRight(), point);
        }
    }

    private boolean compare(Cluster p, Node n) {
        if (n.vertical()) return p.getX() < n.getPoint().getX();
        else return p.getY() < n.getPoint().getY();
    }

    /**
     * find the drawPoints in tree
     *
     * @param point drawPoints
     * @return the drawPoints in tree
     */
    public Cluster findPoint(Cluster point) {
        return findPointRecursive(root, point);
    }

    private Cluster findPointRecursive(Node node, Cluster point) {
        if (node == null) {
            return null;
        }
        if (node.getPoint().equals(point)) {
            return node.getPoint();
        }
        if (compare(point, node)) {
            return findPointRecursive(node.getLeft(), point);
        } else {
            return findPointRecursive(node.getRight(), point);
        }
    }

    /**
     * find all the drawPoints in certain rectangle
     *
     * @param rect rectangle
     * @return array of drawPoints
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

        double pointCoord = p.getY();
        double rectMin = rect.getMinY();
        double rectMax = rect.getMaxY();
        if (n.vertical()) {
            pointCoord = p.getX();
            rectMin = rect.getMinX();
            rectMax = rect.getMaxX();
        }

        if (pointCoord > rectMin)
            rangeRecursive(rangeList, rect, n.getLeft());
        if (pointCoord <= rectMax)
            rangeRecursive(rangeList, rect, n.getRight());
    }

    /**
     * find all the drawPoints in certain circle
     *
     * @param r radius of circle
     * @return array of drawPoints
     */
    public ArrayList<Cluster> rangeRadius(Point point, double r) {
        ArrayList<Cluster> pointsInRange = new ArrayList<>();
        rangeRadiusRecursive(pointsInRange, point, r, root);
        return pointsInRange;
    }

    private void rangeRadiusRecursive(ArrayList<Cluster> rangeList, Point point, double r, Node n) {
        if (n == null) {
            return;
        }
        Cluster p = n.getPoint();
        if (p.distanceTo(new Cluster(point)) <= r) {
            rangeList.add(p);
        }
        double pointCoord = p.getY();
        double circleMin = point.getY() - r;
        double circleMax = point.getY() + r;
        if (n.vertical()) {
            pointCoord = p.getX();
            circleMin = p.getX() - r;
            circleMax = p.getX() + r;
        }
        if (pointCoord > circleMin) rangeRadiusRecursive(rangeList, point, r, n.getLeft());
        if (pointCoord <= circleMax) rangeRadiusRecursive(rangeList, point, r, n.getRight());
    }
}
