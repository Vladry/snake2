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

@SuppressWarnings("ALL")
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

            //  получили под destination искомый path
            this.path = getPath(destination);
            String dir = null;

            if (this.path.size() > 1) {
                System.out.println("this.path.size()= " + this.path.size());



                int snakeSizeLimit = 28;
                if (board.getSnake().size() < snakeSizeLimit) {
                    System.out.println("змея короче чем snakeSizeLimit");
                    // и передали path в модуль получения Direction
                    Point nextStep = getDirection(this.board, this.path);
                    System.out.println("nextStep: " + nextStep);
                    dir = finalizeDirection(nextStep);
                } else {
                    System.out.println("змея длиннее чем snakeSizeLimit");
                    Point nextStep = getStickyDirection(this.board, this.path);
                    System.out.println("nextStep: " + nextStep);
                    dir = finalizeDirection(nextStep);
                }


            } else
            // Если не найден путь к яблоку (в пути только один элемент -голова змени) - направляемся к камню. Для этого нужно переформировать graph вставив туда камень вместо яблока
            {
                System.out.println("путь к яблоку не найден!  Вставляем в граф Камень и destination = board.getStones().get(0)");
                this.graph = createGraph(board, true);
                destination = new Dijkstra.Vertex(board.getStones().get(0));
                this.path = getPath(destination);
                System.out.println("this.path.size()= " + this.path.size());
                if (this.path.size() > 1) {//если найден путь хотя бы к камню - идём на камень
                    Point nextStep = getDirection(board, this.path);
                    dir = finalizeDirection(nextStep);
                    System.out.println("dir = finalizeDirection() = " +dir);
                } else {//иначе, если не найден путь ни к яблоку, ни к камню - шаг на любую ближайшую пустую клетку
                    dir = finalizeIfNoPathFound();
                    System.out.println("dir = finalizeIfNoPathFound() = " +dir);
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

    public static Point getDirection(Board board, LinkedList<Point> path) {
        Point head = board.getHead();
        String dir = null;
        Point nextStep = path.get(1);
        return nextStep;
    }

    public static Point getStickyDirection(Board board, LinkedList<Point> path) {
        Point head = board.getHead();
        Point apple = board.getApples().get(0);
        Point nextStep = null;
        String dir = null;

        int leftEndpoint = 1;
        int rightEndpoint = 2;

        // если поблизости яблоко -хватаем его
        if (Math.abs(head.getX() - apple.getX()) < 2
                && Math.abs(head.getY() - apple.getY()) == 0
//                && Math.abs(head.getY() - apple.getY()) < 2
                && (board.getSnakeDirection().equals(Direction.RIGHT.toString())
                || board.getSnakeDirection().equals(Direction.LEFT.toString()))
        ) {
            nextStep = path.get(1);
        } else if (board.isAt(head.getX() - leftEndpoint, head.getY(), Elements.NONE)
                && head.getX() > rightEndpoint
        ) {
            nextStep = new PointImpl(head.getX() - 1, head.getY());
        } else if (board.isAt(head.getX() + 1, head.getY(), Elements.NONE)
                && head.getX() < board.size() - 3
        ) {
            nextStep = new PointImpl(head.getX() + 1, head.getY());
        } else {
            nextStep = path.get(1);
        }

        return nextStep;
    }

    public String finalizeDirection(Point nextStep) {
        Point head = this.board.getHead();
        String dir = null;
        if (nextStep.getX() > head.getX()
                && nextStep.getY() == head.getY()
                && (board.isAt(head.getX() + 1, head.getY(), Elements.NONE)
                || board.isAt(head.getX() + 1, head.getY(), Elements.GOOD_APPLE))
        ) {
            dir = Direction.RIGHT.toString();
        }

        else if (nextStep.getX() < head.getX()
                && nextStep.getY() == head.getY()
                && (board.isAt(head.getX() - 1, head.getY(), Elements.NONE)
                || board.isAt(head.getX() - 1, head.getY(), Elements.GOOD_APPLE))
        ) {
            dir = Direction.LEFT.toString();
        }

        else if (nextStep.getY() > head.getY()
                && nextStep.getX() == head.getX()
                && (board.isAt(head.getX(), head.getY() + 1, Elements.NONE)
                || board.isAt(head.getX(), head.getY()+1, Elements.GOOD_APPLE))
        ) {
            dir = Direction.UP.toString();
        }

        else if (nextStep.getY() < head.getY()
                && nextStep.getX() == head.getX()
                && (board.isAt(head.getX(), head.getY() - 1, Elements.NONE)
                || board.isAt(head.getX(), head.getY() - 1, Elements.GOOD_APPLE))
        ) {
            dir = Direction.DOWN.toString();
        }
        return dir;
    }

    public String finalizeIfNoPathFound() {
        // Eсли не найден путь ни к яблоку, ни к камню - шаг на любую ближайшую пустую клетку
        Point head = board.getHead();
        String dir = null;
        if (this.board.isAt(head.getX() - 1, head.getY(), Elements.NONE)) {
            dir = Direction.LEFT.toString();
        } else if (this.board.isAt(head.getX() + 1, head.getY(), Elements.NONE)) {
            dir = Direction.RIGHT.toString();
        } else if (this.board.isAt(head.getX(), head.getY() - 1, Elements.NONE)) {
            dir = Direction.DOWN.toString();
        } else {
            dir = Direction.UP.toString();
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
