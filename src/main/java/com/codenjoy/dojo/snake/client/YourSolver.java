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

        try {
            Point nextStep = null;
            String dir = null;
            this.path = findPathToApple();
            if (this.path.size() > 1) { // по длине змейки получаем нужные params для задания режима дальнейшей работы
                YourSolver.SnakeParams modeParams = getSnakeParams(board.getSnake().size());

                // режим прямого поиска яблока по Дейкстре (пока змейка маленькая):
                if (modeParams.snakeSmall) {
                    System.out.println("змея короче чем smallEndPoint");
                    nextStep = getDirection(this.board, this.path);

                } else {// режим скручивания в змеевик (когда змейка подросла):
                    System.out.println("змея длиннее чем smallEndPoint");
                    nextStep = getZdirection(
                            this.board, this.path, modeParams.leftEndpoint, modeParams.rightEndpoint);
                }


            } else // Если не найден путь к яблоку (в пути только один элемент -голова змеи) - направляемся к камню. Для этого нужно переформировать graph вставив туда камень вместо яблока
            {
                this.path = findPathToRock();
                if (this.path.size() > 1) { // если найден путь хотя бы к камню - идём на камень
                    nextStep = getDirection(board, this.path);


                } else {//иначе, если не найден путь ни к яблоку, ни к камню - шаг на любую ближайшую пустую клетку
                    Point bestV = findBestDetour(head);
                    System.out.println("best V is:" + bestV);
                    nextStep = bestV;

                }
            }


            dir = finalizeDirection(nextStep);
//----------------------конец ------------------------
            System.out.println("direction before quitting:  " + dir);
            return dir;

        } catch (Exception e) {
            System.out.println("общее исключение .get()");
            return Direction.DOWN.toString();
        }
    }
// -------------------методы--------------------------

    public LinkedList<Point> findPathToApple() {
        this.graph = createGraph(this.board, false);
        Point head = this.board.getHead();
        Point apple = this.board.getApples().get(0);

        // находим в графе Vertex головы змеи и (если найдена) - отправляем её в качестве source для просчета Дейкстры
        this.graph.stream().filter(
                        (v) -> (v.point.getX() == head.getX() && v.point.getY() == head.getY()))
                .findFirst().ifPresent(Dijkstra::computeGraph);

        System.out.println("нашли голову в графе. Направляемся к яблоку ");

        // Направляемся к яблоку.   Сначала получим из графа Vertex яблока и присвоим его в destination
        Dijkstra.Vertex destination = this.graph.stream().filter(
                        (v) -> (v.point.getX() == apple.getX() && v.point.getY() == apple.getY()))
                .findFirst().orElse(null);

        //  получили искомый path для заданного destination
        return getPath(destination);
    }
    public LinkedList<Point> findPathToRock() {
        System.out.println("путь к яблоку не найден!  Пере-содаём граф с Камнем и destination = board.getStones().get(0)");
        this.graph = createGraph(board, true);
        Dijkstra.Vertex destination = this.graph.stream().filter((v) -> v.point.equals((board.getStones().get(0)))).findFirst().orElse(null);
        System.out.println("в граф вставили Камень и записали его в destination в виде:" + destination);
        return getPath(destination);
    }
    private class SnakeParams {
        boolean snakeSmall;
        // ограничители скручивания змейки в змеевик в зависимости от длины змейки snakeLength (snakeSize)
        int leftEndpoint;
        int rightEndpoint;

        public SnakeParams(boolean snakeSmall, int leftEndpoint, int rightEndpoint) {
            this.snakeSmall = snakeSmall;
            this.leftEndpoint = leftEndpoint;
            this.rightEndpoint = rightEndpoint;
        }
    }
    public YourSolver.SnakeParams getSnakeParams(int snakeLength) {
        System.out.println("in getSnakeParams(int snakeLength)");
        System.out.println("snakeLength: " + snakeLength);
        boolean snakeSmall = true; //параметр для переключения с режима прямого поиска по Дейскстре в режим
        //большой змеи, когда она начинает скручиваться в змеевик и включается getStickyDirection()

        //ниже задаются эндпоинты для длин змеи, при которых переключаются режимы работы алгоритма скручивания в змеевик:
        int mediumStartPoint = 30;
        int largeStartPoint = 40;
        // ограничители скручивания змейки в змеевик в зависимости от длины змейки snakeLength (snakeSize)
        int leftEndpoint = 1;
        int rightEndpoint = this.board.size() - 1;

        if (snakeLength > mediumStartPoint
                && snakeLength < largeStartPoint) {
            System.out.println("snakeMedium, ");
            snakeSmall = false;
            leftEndpoint = 5;
            rightEndpoint = this.board.size() - 6;
        }
        if (snakeLength >= largeStartPoint) {
            System.out.println("snakeLarge, ");
            snakeSmall = false;
            leftEndpoint = 2;
            rightEndpoint = this.board.size() - 3;
        }
        System.out.println("leftEndpoint: " + leftEndpoint + ",  rightEndpoint: " + rightEndpoint);
        return new YourSolver.SnakeParams(snakeSmall, leftEndpoint, rightEndpoint);
    }
    public Point findBestDetour(Point head) {
        // сюда отправляем nextStep в случаях, когда пути к яблоку и камню не найдены
        System.out.println("in findBestDetour()");
        Dijkstra.Vertex headV = this.graph.stream().filter((v) -> v.point.equals(head)).findFirst().orElse(null);
        System.out.println("headV found: " + headV.point);
        Dijkstra.Vertex bestVertex = null;
        int totalVertexesFound = 0;
        for (Dijkstra.Edge e : headV.edges) {
//берем у головы все её три смежные вершины и, в цикле ищем, для какой вершины будет макс.число totalVertexesFound. Эту вершины и возвращаем как bestVertex
//            System.out.println("inside of for-loop");
            System.out.println("checking sub-vertex: " + e.v.point);
            Set<Dijkstra.Vertex> visited = new HashSet<>();
            int vertCount = countSubVertexes(e.v, visited);
            System.out.println("a headBranch " + e.v.point + " has " + vertCount + " sub-branches");
            System.out.println("visited.size(): " + visited.size());
            if (vertCount > totalVertexesFound) {
                totalVertexesFound = vertCount;
                bestVertex = e.v;
            }
        }
        System.out.println("exiting findBestDetour.  Возвращаем bestVertex= " + bestVertex.point);
        return bestVertex.point;
    }
    public int countSubVertexes(Dijkstra.Vertex current, Set<Dijkstra.Vertex> visited) {
        if (current == null || visited.contains(current)) return 0;
        visited.add(current);
        int vertexCounter = 1;
        for (Dijkstra.Edge e : current.edges) {
            vertexCounter += countSubVertexes(e.v, visited);
        }
        return vertexCounter;
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
    public Point getDirection(Board board, LinkedList<Point> path) {
        Point head = board.getHead();
        String dir = null;
        Point nextStep = path.get(1);
        return nextStep;
    }
    public Point getZdirection(Board board, LinkedList<Point> path, int leftEndpoint, int rightEndpoint) {
        // leftEndpoint, int rightEndpoint - левый и правый ограничители ширины скручивания змеи в змеевик. Чем длиннее змея -тем шире змеевик (т.е. эти границы расширяются)
        Point head = board.getHead();
        Point apple = board.getApples().get(0);
        Point nextStep = null;
        String dir = null;

        // если унюхали поблизости яблоко -хватаем его!
        if ((Math.abs(head.getX() - apple.getX()) < 15) //длина "нюха" по оси Х
//                && Math.abs(head.getY() - apple.getY()) == 0 // длина "нюха" по оси Y -нулевая, либо:
                && (Math.abs(head.getY() - apple.getY()) <= 2) //длина "нюха" по оси Y
                || Math.abs(head.getY() - apple.getY()) > 3 //либо прямой ход по Дейкстре работает далее без скрутки в змеевик, если расстояние по Y более указанного
        ) {
            nextStep = path.get(1);
        } else //иначе проверяем
            // складываем змейку в змеевик
            if (board.isAt(head.getX() - 1, head.getY(), Elements.NONE, Elements.GOOD_APPLE)
                    && head.getX() > leftEndpoint
            ) {
                nextStep = new PointImpl(head.getX() - 1, head.getY());
            } else if (board.isAt(head.getX() + 1, head.getY(), Elements.NONE, Elements.GOOD_APPLE)
                    && head.getX() < rightEndpoint
            ) {
                nextStep = new PointImpl(head.getX() + 1, head.getY());
            } else {
                nextStep = path.get(1);
            }

        boolean isStepAllowed = inspectForWayBack(board, path, nextStep);
        if (isStepAllowed) {
            return nextStep;
        } else {

        }
        return nextStep;
    }
    public boolean inspectForWayBack(Board board, List<Point> path, Point nextStep) {
        Point head = board.getHead();
        Point apple = board.getApples().get(0);
        List<Dijkstra.Vertex> graphMocked = new ArrayList<Dijkstra.Vertex>(this.graph);
        LinkedList<Point> pathMocked = new LinkedList<>(path);
        List<Dijkstra.Vertex> pathInVertexes = pathMocked.stream().map((point) -> new Dijkstra.Vertex(point)).collect(Collectors.toList());
        pathInVertexes.remove(new Dijkstra.Vertex(head));
        graphMocked.removeAll(pathInVertexes);
//        ArrayList<Point> snake = new ArrayList<>(board.getSnake());
//                    snake.subList()
        Dijkstra.Vertex appleMocked = graphMocked.stream().filter((v) -> v.point.equals(apple)).findFirst().orElse(null);


        Set<Dijkstra.Vertex> visited = new HashSet<>();
        int vertCount = countSubVertexes(appleMocked, visited);
        if (vertCount > 30) {
            return true;
        }

        return false;
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
        } else if (nextStep.getX() < head.getX()
                && nextStep.getY() == head.getY()
                && (board.isAt(head.getX() - 1, head.getY(), Elements.NONE)
                || board.isAt(head.getX() - 1, head.getY(), Elements.GOOD_APPLE))
        ) {
            dir = Direction.LEFT.toString();
        } else if (nextStep.getY() > head.getY()
                && nextStep.getX() == head.getX()
                && (board.isAt(head.getX(), head.getY() + 1, Elements.NONE)
                || board.isAt(head.getX(), head.getY() + 1, Elements.GOOD_APPLE))
        ) {
            dir = Direction.UP.toString();
        } else if (nextStep.getY() < head.getY()
                && nextStep.getX() == head.getX()
                && (board.isAt(head.getX(), head.getY() - 1, Elements.NONE)
                || board.isAt(head.getX(), head.getY() - 1, Elements.GOOD_APPLE))
        ) {
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
