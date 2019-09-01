package models;

import java.util.ArrayList;

public class Path {
    // TODO rename alv
    private ArrayList<Point> alv;

    public Path() {
        alv = new ArrayList<>();
    }

    public void setAlv(ArrayList<Point> alv) {
        this.alv = alv;
    }

    public ArrayList<Point> getAlv() {
        return alv;
    }
}
