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
            if (this == o) return true;
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


        @Override
        public int hashCode() {
            return this.v.point.getX() * 31 + this.v.point.getY() * 37 + (int) this.v.minWeight * 29;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (this == o) return true;
            if (!o.getClass().getName().equals(this.getClass().getName())) return false;
            Edge e = (Edge) o;
            return (this.v.point.getX() == e.v.point.getX() && this.v.point.getY() == e.v.point.getY()
                    && (int) this.v.minWeight == (int) e.v.minWeight);
        }

    }

    public static void computeGraph(Vertex source, Point to) {
        if (to != null) {
            System.out.println("in computeGraph(to= " + to + ')');
        }
        PriorityQueue<Vertex> q = new PriorityQueue<>();
        source.minWeight = 0;
        q.offer(source);
        while (!q.isEmpty()) {
            Vertex current = q.poll();
            for (Edge e : current.edges) {
                //TODO вставить остановку: if(current.equals(searched)) - break; ввести в computeGraph новый аргумент searched -сугубо для остановки дальнейшего расчета пути
                Vertex target = e.v;
                if (
                        to != null &&
                        Math.abs(target.point.getX() - to.getX()) <= 1
                        && Math.abs(target.point.getY() - to.getY()) == 0
                        ||
                        to != null && Math.abs(target.point.getX() - to.getX()) == 0
                                && Math.abs(target.point.getY() - to.getY()) <= 1

                ) {
                    System.out.println("in computeGraph found: a vertex close to destination " + to + " is found");
                    System.out.println("found vertex: " + target);
                }
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
        if (!path.isEmpty()) {
            path.pop();//удалили из пути голову
        }
        return path;
    }

}
