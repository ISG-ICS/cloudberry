package models;

public class Rectangle {

    // the left bottom point of the rectangle
    private Point minPoint;
    // the right top point of the rectangle
    private Point maxPoint;

    // construct the axis-aligned rectangle [getMinX, getMaxX] getX [getMinY, getMaxY]
    public Rectangle(double xmin, double ymin, double xmax, double ymax) {
        this.minPoint = new Point(xmin, ymin);
        this.maxPoint = new Point(xmax, ymax);
    }

    // accessor methods for 4 coordinates
    double getMinX() {
        return minPoint.getX();
    }

    double getMinY() {
        return minPoint.getY();
    }

    double getMaxX() {
        return maxPoint.getX();
    }

    double getMaxY() {
        return maxPoint.getY();
    }

    // width and height of rectangle
    public double width() {
        return maxPoint.getX() - minPoint.getX();
    }

    public double height() {
        return maxPoint.getY() - minPoint.getY();
    }

    // distance from p to closest point on this axis-aligned rectangle
    public double distanceTo(Cluster p) {
        return Math.sqrt(this.distanceSquaredTo(p));
    }

    // distance squared from p to closest point on this axis-aligned rectangle
    private double distanceSquaredTo(Cluster p) {
        double dx = 0.0, dy = 0.0;
        if (p.getX() < getMinX()) dx = p.getX() - getMinX();
        else if (p.getX() > getMaxX()) dx = p.getX() - getMaxX();
        if (p.getY() < getMinY()) dy = p.getY() - getMinY();
        else if (p.getY() > getMaxY()) dy = p.getY() - getMaxY();
        return dx * dx + dy * dy;
    }

    // does this axis-aligned rectangle contain p?
    public boolean contains(Cluster p) {
        return (p.getX() >= getMinX()) && (p.getX() <= getMaxX())
                && (p.getY() >= getMinY()) && (p.getY() <= getMaxY());
    }

    // are the two axis-aligned rectangles equal?
    public boolean equals(Object y) {
        if (y == this) return true;
        if (y == null) return false;
        if (y.getClass() != this.getClass()) return false;
        Rectangle that = (Rectangle) y;
        return this.minPoint == that.minPoint && this.maxPoint == that.maxPoint;
    }

    // return a string representation of this axis-aligned rectangle
    public String toString() {
        return "[" + getMinX() + ", " + getMaxX() + "] getX [" + getMinY() + ", " + getMaxY() + "]";
    }

}
