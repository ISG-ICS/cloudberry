package models;

import java.util.ArrayList;

public class Path {
    private ArrayList<Point> path;

    public Path() {
        path = new ArrayList<>();
    }

    public void setPath(ArrayList<Point> path) {
        this.path = path;
    }

    public ArrayList<Point> getPath() {
        return path;
    }
}
