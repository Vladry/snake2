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
            return this.point.getX() * 31 + this.point.getY() * 37;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if(this == o) return true;
            if (!o.getClass().getName().equals(this.getClass().getName())) return false;
            Vertex v = (Vertex) o;
            return (this.point.getX() == v.point.getX() && this.point.getY() == v.point.getY());
        }


        @Override
        public String toString() {
            String str = "Vert-> %s. Prev: %s, Edges-> %s \n";
            return String.format(str,
                    this.point, (this.prev != null) ? this.prev.point : "none", this.edges);
        }

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
        PriorityQueue<Vertex> q = new PriorityQueue<>();
        source.minWeight = 0;
        q.offer(source);
        while (!q.isEmpty()) {
            Vertex current = q.poll();
            for (Edge e : current.edges) {
                //TODO вставить остановку: if(current.equals(searched)) - break; ввести в computeGraph новый аргумент searched -сугубо для остановки дальнейшего расчета пути
                Vertex target = e.v;
                double testedWeight = current.minWeight + e.weight;
                if (testedWeight < target.minWeight) {
                    q.remove(target);
                    target.prev = current;
                    target.minWeight = testedWeight;
                    q.add(target);
                }
            }
        }
    }

    public static LinkedList<Point> buildPath(Vertex destination) {
        LinkedList<Point> path = new LinkedList<>();
        Vertex current = destination;
        for (; current != null; current = current.prev) {
            path.push(current.point);
        }
        if(!path.isEmpty()){
            path.pop();//удалили из пути голову
        }
        return path;
    }

}
