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
    public double getMinX() {
        return minPoint.getX();
    }

    public double getMinY() {
        return minPoint.getY();
    }

    public double getMaxX() {
        return maxPoint.getX();
    }

    public double getMaxY() {
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
    public double distanceSquaredTo(Cluster p) {
        double dx = 0.0, dy = 0.0;
        if (p.getX() < getMinX()) dx = p.getX() - getMinX();
        else if (p.getX() > getMaxX()) dx = p.getX() - getMaxX();
        if (p.y() < getMinY()) dy = p.y() - getMinY();
        else if (p.y() > getMaxY()) dy = p.y() - getMaxY();
        return dx * dx + dy * dy;
    }

    // does this axis-aligned rectangle contain p?
    public boolean contains(Cluster p) {
        return (p.getX() >= getMinX()) && (p.getX() <= getMaxX())
                && (p.y() >= getMinY()) && (p.y() <= getMaxY());
    }

    // are the two axis-aligned rectangles equal?
    public boolean equals(Object y) {
        if (y == this) return true;
        if (y == null) return false;
        if (y.getClass() != this.getClass()) return false;
        Rectangle that = (Rectangle) y;
        if (this.minPoint != that.minPoint) return false;
        if (this.maxPoint != that.maxPoint) return false;
        return true;
    }

    // return a string representation of this axis-aligned rectangle
    public String toString() {
        return "[" + getMinX() + ", " + getMaxX() + "] getX [" + getMinY() + ", " + getMaxY() + "]";
    }

}
