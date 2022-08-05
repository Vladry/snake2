package com.codenjoy.dojo.snake.client;

import com.codenjoy.dojo.client.Solver;
import com.codenjoy.dojo.client.WebSocketRunner;
import com.codenjoy.dojo.services.*;
import com.codenjoy.dojo.snake.model.Elements;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.codenjoy.dojo.snake.client.Dijkstra.buildPath;
import static com.codenjoy.dojo.snake.client.Dijkstra.computeGraph;
import static scala.collection.immutable.Nil.head;

/**
 * User: Vlad Ryab
 * pw: slyslysly
 * email: rvy@ukr.net
 */
public class YourSolver implements Solver<Board> {

    private Dice dice;
    private Board board;

    private List<Dijkstra.Vertex> graph;

    public YourSolver(Dice dice) {
        this.dice = dice;
    }

    @Override
    public String get(Board board) {
        this.board = board;
        Point head = board.getHead();
        Point apple = board.getApples().get(0);
//        System.out.println(board.toString());
//        Point stone = board.getStones().get(0);
//        List<Point> barriers = board.getBarriers();
//        List<Point> snake = board.getSnake();
//        List<Point> walls = board.getWalls();
//        double dist = head.distance(apple);
//        int headX = head.getX();
        if (board.isGameOver()) {
            return Direction.UP.toString();
        }
//        if(head == null){return Direction.UP.toString();}


        List<Point> freeSpace = board.get(Elements.NONE, Elements.GOOD_APPLE);
        freeSpace.add(head);
        this.graph = freeSpace.stream().map(Dijkstra.Vertex::new).collect(Collectors.toList());

        this.graph.forEach((v) -> setEdges.accept(v));


        System.out.println("тестируем код");
//        System.out.println(graph);

        graph.stream().filter(
                        (v) -> (v.point.getX() == head.getX() && v.point.getY() == head.getY()))
                .findFirst().ifPresent(Dijkstra::computeGraph);


        Dijkstra.Vertex destination = graph.stream().filter(
                        (v) -> (v.point.getX() == apple.getX() && v.point.getY() == apple.getY()))
                .findFirst().orElse(null);


        String dir = null;
//        System.out.println("destination: " + destination);
//        if (destination != null) {
        LinkedList<Point> path = Dijkstra.buildPath(destination);
        Point nextStep;
        if (path.get(1) != null) {
            nextStep = path.get(1);
        } else {
            nextStep = path.get(0);
        }

        System.out.println("nextStep: " + nextStep);
        System.out.println("nextStep: " + nextStep);
        System.out.println("head: " + head);
        if (nextStep.getX() > head.getX()
                && nextStep.getY() == head.getY()) {
            dir = Direction.RIGHT.toString();
        } else if (nextStep.getX() < head.getX()
                && nextStep.getY() == head.getY()) {
            dir = Direction.LEFT.toString();
        } else if (nextStep.getY() > head.getY()
                && nextStep.getX() == head.getX()) {
            dir = Direction.UP.toString();
        } else if (nextStep.getY() < head.getY()
                && nextStep.getX() == head.getX()) {
            dir = Direction.DOWN.toString();
//            }

        }
        System.out.println("dir: " + dir);
        return (dir == null) ? Direction.UP.toString() : dir;
    }

    Consumer<Dijkstra.Vertex> setEdges = (v) -> {
        Point p = v.point;
        Point up = new PointImpl(p.getX(), p.getY() + 1);
        Point down = new PointImpl(p.getX(), p.getY() - 1);
        Point left = new PointImpl(p.getX() - 1, p.getY());
        Point right = new PointImpl(p.getX() + 1, p.getY());


        if (board.isAt(up, Elements.NONE) || board.isAt(up, Elements.GOOD_APPLE)) {
            graph.stream().filter((c) ->
                    c.point.equals(up)
            ).findFirst().ifPresent((vUp) -> v.edges.add(new Dijkstra.Edge(vUp, 1)));
        }


        if (board.isAt(down, Elements.NONE) || board.isAt(down, Elements.GOOD_APPLE)) {
            graph.stream().filter((c) ->
                    c.point.equals(down)
            ).findFirst().ifPresent((vDown) -> v.edges.add(new Dijkstra.Edge(vDown, 1)));

        }


        if (board.isAt(left, Elements.NONE) || board.isAt(left, Elements.GOOD_APPLE)) {
            graph.stream().filter((c) ->
                    c.point.equals(left)
            ).findFirst().ifPresent((vLeft) -> v.edges.add(new Dijkstra.Edge(vLeft, 1)));

        }


        if (board.isAt(right, Elements.NONE) || board.isAt(right, Elements.GOOD_APPLE)) {
            graph.stream().filter((c) ->
                    c.point.equals(right)
            ).findFirst().ifPresent(vRight -> v.edges.add(new Dijkstra.Edge(vRight, 1)));
        }
    };


    public static void main(String[] args) {
        WebSocketRunner.runClient(
                // paste here board page url from browser after registration
                "http://46.101.237.57/codenjoy-contest/board/player/12e9swp7xv1y40cwd0z2?code=828367227519872101",
                new YourSolver(new RandomDice()),
                new Board());
    }

}
