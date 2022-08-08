package com.codenjoy.dojo.snake.client;

import com.codenjoy.dojo.services.Direction;
import com.codenjoy.dojo.services.Point;

import java.util.*;

import static java.lang.Double.POSITIVE_INFINITY;

public class Dijkstra {

    public static class Vertex implements Comparable<Vertex> {

        Point point;
        double minWeight;
        Set<Edge> edges;
        Vertex prev;
        boolean visited = false; //используется не в Дейкстре, а в DFS расчете возможных ходов "вглубину", когда не найдены пути и змейке нужно выпутываться

        public Vertex(Point p) {
            this.point = p;
            this.minWeight = POSITIVE_INFINITY;
            this.edges = new HashSet<>();
        }

        public Vertex(Point p, Set<Edge> edges) {
            this.point = p;
            this.minWeight = POSITIVE_INFINITY;
            this.edges = edges;
        }

        @Override
        public int compareTo(Vertex v) {
            return Double.compare(this.minWeight, v.minWeight);
        }

        @Override
        public int hashCode() {
            return this.point.getX() * 31 + this.point.getY();
        }
//        public int hashCode() {
//            return this.x * 31 + this.y * 37;
//        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (!o.getClass().getName().equals(this.getClass().getName())) return false;
            Vertex v = (Vertex) o;
            return (this.point.getX() == v.point.getX() && this.point.getY() == v.point.getY());
        }
//        public boolean equals(Object o) {
//            Vertex v = (Vertex) o;
//            return (this.x == v.x && this.y == v.y);
//        }

        @Override
        public String toString() {
            String str = "Vert-> %s. Prev: %s, Edges-> %s \n";
            return String.format(str,
                    this.point, (this.prev != null) ? this.prev.point : "none", this.edges);
        }
//        public String toString() {
//            return String.format("Vert-> x: %d, y: %d \n", this.x, this.y);
//        }


    }

    public static class Edge {
        Vertex v;
        double weight;

        public Edge(Vertex v, double weight) {
            this.v = v;
            this.weight = weight;
        }

        @Override
        public String toString() {
            return "edge-> ( point: " + this.v.point.toString() + ", weight: " + this.weight + ')';
        }
    }

    public static void computeGraph(Vertex source) {
//        System.out.println("computeGraph. Source: " + source);
        PriorityQueue<Vertex> q = new PriorityQueue<>();
        source.minWeight = 0;
        q.offer(source);
        while (!q.isEmpty()) {
            Vertex current = q.poll();
            for (Edge e : current.edges) {
                Vertex target = e.v;
                double testedWeight = current.minWeight + e.weight;
                if (testedWeight < target.minWeight) {
                    q.remove(target);
                    target.prev = current;
                    target.minWeight = testedWeight;
                    q.add(target);
//                    System.out.println("iterating in computeGraph. Current: " + source);
                }
            }
        }
    }

    public static LinkedList<Point> buildPath(Vertex destination) {
        LinkedList<Point> path = new LinkedList<>();
//        System.out.println("destination: " + destination);
        Vertex current = destination;
        Point nextStep = null;
        for (; current != null; current = current.prev) {
//            System.out.println("vertex: " + current + ".weight= " + current.minWeight);
            path.push(current.point);
        }
        return path;
    }

}
