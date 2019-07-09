package controllers;
import java.util.HashSet;
import java.util.Set;
public class EdgeTest{
    public static void main(String[] args) {
        Set<Edge> edges = new HashSet<>();
        Edge e1 = new Edge(0.1, 0.2, 0.3, 0.4);
        Edge e2 = new Edge(0.1, 0.2, 0.3, 0.4);
        Edge e3 = new Edge(0.4, 0.3, 0.2, 0.1);

        //Check if e1 and e2 are the same
        System.out.print(e1.equals(e2));

        //Check if the edges only contains e1 and e3
        edges.add(e1);
        edges.add(e2);
        edges.add(e3);

        System.out.print(edges.contains(e1));
        System.out.print(edges.contains(e2));
    }


}