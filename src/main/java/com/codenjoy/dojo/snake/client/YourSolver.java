package com.codenjoy.dojo.snake.client;

import com.codenjoy.dojo.client.Solver;
import com.codenjoy.dojo.client.WebSocketRunner;
import com.codenjoy.dojo.services.*;
import com.codenjoy.dojo.snake.model.Elements;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static scala.collection.immutable.Nil.head;

/**
 * User: Vlad Ryab
 * pw: slyslysly
 * email: rvy@ukr.net
 */
public class YourSolver implements Solver<Board> {

    private Dice dice;
    private Board board;

    private List<Dijkstra.Vertex> graph = null;
    private LinkedList<Point> path = new LinkedList<>();

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


        try {
            this.graph = createGraph(this.board, false);

            System.out.println("тестируем код");

            // находим в графе Vertex головы змеи и (если найдена) - отправляем её в качестве source для просчета Дейкстры
            this.graph.stream().filter(
                            (v) -> (v.point.getX() == head.getX() && v.point.getY() == head.getY()))
                    .findFirst().ifPresent(Dijkstra::computeGraph);

            System.out.println("нашли голову в графе. Направляемся к яблоку ");

            // Направляемся к яблоку.   Сначала получим из графа Vertex яблока и присвоим его в destination
            Dijkstra.Vertex destination = this.graph.stream().filter(
                            (v) -> (v.point.getX() == apple.getX() && v.point.getY() == apple.getY()))
                    .findFirst().orElse(null);

            System.out.println("получили destination: " + destination);
            //  получили под destination искомый path
            this.path = getPath(destination);
            String dir = null;

            if (this.path.size() > 1) {
                System.out.println("сработало: if (this.path.size() > 1) и заходим в getDirection ");
                dir = getDirection(this.board, this.path);  // и передали path в модуль получения Direction
                System.out.println("получили в getDirection dir= " + dir + "и вышли в .get()");
            } else
            // Если не найден путь к яблоку (в пути только один элемент -голова змени) - направляемся к камню. Для этого нужно переформировать graph вставив туда камень вместо яблока
            {
                System.out.println("!!! path not found. Snakes heads out toward a stone!");
                this.graph = createGraph(board, true);
                destination = new Dijkstra.Vertex(board.getStones().get(0));
                this.path = getPath(destination);
                dir = getDirection(board, this.path);
            }

            // TODO прописать алгоритм прохода к яблоку по единственному пути- узкому тоннелю, когда на пути к яблоку есть камень,
            // TODO чтобы шла на камень, сьедала, но доходила до яблока.

            // Eсли не найден путь ни к яблоку, ни к камню - шаг на любую ближайшую пустую клетку
            if (path.size() < 2 || dir == null) {
                if (board.isAt(head.getX() - 1, head.getY(), Elements.NONE)) {
                    dir = Direction.LEFT.toString();
                } else if (board.isAt(head.getX() + 1, head.getY(), Elements.NONE)) {
                    dir = Direction.RIGHT.toString();
                } else if (board.isAt(head.getX(), head.getY() - 1, Elements.NONE)) {
                    dir = Direction.DOWN.toString();
                } else {
                    dir = Direction.UP.toString();
                }
            }
            System.out.println("direction before quitting:  " + dir);
            return dir;


        } catch (Exception e) {
            System.out.println("общее исключение .get()");
            return Direction.DOWN.toString();
        }
    }


    public List<Dijkstra.Vertex> createGraph(Board board, boolean isRockIncluded) {
        System.out.println("in createGraph");
        List<Dijkstra.Vertex> graph;

        List<Point> freeSpace = null;
        if (!isRockIncluded) {
            freeSpace = board.get(Elements.NONE, Elements.GOOD_APPLE);
        } else {
            freeSpace = board.get(Elements.NONE, Elements.GOOD_APPLE, Elements.BAD_APPLE);
        }


        freeSpace.add(board.getHead());
        graph = freeSpace.stream().map(Dijkstra.Vertex::new).collect(Collectors.toList());

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

        graph.forEach((v) -> setEdges.accept(v));
//        System.out.println("new graph is: " + graph);
        return graph;

    }

    public LinkedList<Point> getPath(Dijkstra.Vertex destination) {
        System.out.println("in getPath");
        return Dijkstra.buildPath(destination);
    }

    public static String getDirection(Board board, LinkedList<Point> path) {
        System.out.println("in getDirection");
        System.out.println("path: " + path);
        Point head = board.getHead();

        String dir = null;
        Point nextStep = path.get(1); // TODO:  тут раньше получали NullPointerException
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
        }
        return dir;
    }


    public static void main(String[] args) {
        WebSocketRunner.runClient(
                // paste here board page url from browser after registration
                "http://46.101.237.57/codenjoy-contest/board/player/12e9swp7xv1y40cwd0z2?code=828367227519872101",
                new YourSolver(new RandomDice()),
                new Board());
    }

}
