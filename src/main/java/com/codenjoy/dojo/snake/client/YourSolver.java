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

    private List<Dijkstra.Vertex> graphApple = null;
    private List<Dijkstra.Vertex> graphStone = null;
    private List<Dijkstra.Vertex> graphTail = null;
    private LinkedList<Point> currentPath = new LinkedList<>();

    public YourSolver(Dice dice) {
        this.dice = dice;
    }

    @Override
    public String get(Board board) {
        this.board = board;
        Point head = board.getHead();
        Point apple = board.getApples().get(0);
        Point stone = board.getStones().get(0);
        LinkedList<Point> snake = (LinkedList) board.getSnake();
        Point tail = snake.get(snake.size() - 1);
        System.out.println("tail at: "+ tail);
//        System.out.println(board.toString());
//        List<Point> barriers = board.getBarriers();
//        List<Point> walls = board.getWalls();
//        double dist = head.distance(apple);
//        int headX = head.getX();
//        double head.distance(point);
        // Direction  head.direction(point)
//        board.getSnakeDirection()


        if (board.isGameOver()) {
            return Direction.UP.toString();
        }

        try {
            Point nextStep = null;
            String dir = null;
            Point target = null;
            this.graphApple = createAppleGraph();
            this.graphStone = createStoneGraph();
            this.graphTail = createTailGraph();

            Dijkstra.Vertex headVertexInAppleGraph = getTargetFromGraph(graphApple, head);
            Dijkstra.computeGraph(headVertexInAppleGraph);

            Dijkstra.Vertex headVertexInStoneGraph = getTargetFromGraph(graphStone, head);
            Dijkstra.computeGraph(headVertexInStoneGraph);

            Dijkstra.Vertex headVertexInTailGraph = getTargetFromGraph(graphTail, head);
            Dijkstra.computeGraph(headVertexInTailGraph);
//            System.out.println("headVertexInTailGraph: " +headVertexInTailGraph);
//            System.out.println("graphTail: " + graphTail);
            Dijkstra.Vertex appleVertexInGraph = getTargetFromGraph(graphApple, apple);
            Dijkstra.Vertex stoneVertexInGraph = getTargetFromGraph(graphStone, stone);
            Dijkstra.Vertex tailVertexInGraph = getTargetFromGraph(graphTail, tail);

            LinkedList<Point> pathToApple = Dijkstra.buildPath(appleVertexInGraph);
            LinkedList<Point> pathToStone = Dijkstra.buildPath(stoneVertexInGraph);
            LinkedList<Point> pathToTail = Dijkstra.buildPath(tailVertexInGraph);
            System.out.println("pathToApple: " + pathToApple);
            System.out.println("pathToStone: " + pathToStone);
            System.out.println("pathToTail: " + pathToTail);
            int requiredVertexMinimum = 30; //минимальное кол-во вершин доступных для прохода по пути path в случае выхода после достижения яблока или камня
//            boolean isSteptoAppleAllowed = inspectForWayBack(board, pathToApple, apple, requiredVertexMinimum);
//            boolean isSteptoStoneAllowed = inspectForWayBack(board, pathToStone, stone, requiredVertexMinimum);
//            System.out.println("isSteptoAppleAllowed: " + isSteptoAppleAllowed);
//            System.out.println("isSteptoAppleAllowed: " + isSteptoAppleAllowed);


            //-----------------блок управления режимом куда идти змейке------------------

            // сначала описываем "плохие случаи", когда направляемся на камень:
            if (snake.size() > 50 // если змея опасно-громадная
                    && pathToStone.size() > 0 && pathToApple.size() > 0 // и существуют пути,
                    && pathToApple.size() * 2 > pathToStone.size() //и если яблоко не прямо рядом, то идем на камень
            ) {//направляемся на камень
                currentPath = pathToStone;
                target = stone;
                System.out.println("snake is to large");
                System.out.println("heading to: stone");
            } else if (pathToApple.isEmpty() && pathToStone.size() > 0) {
                //если нет пути к яблоку, направляемся на камень
                currentPath = pathToStone;
                target = stone;
                System.out.println("path to apple is empty!");
                System.out.println("heading to: stone");
            }
            else if (pathToApple.isEmpty() && pathToStone.isEmpty()) {
                //если нет пути ни к яблоку, ни к камню, направляемся на свой хвост
                currentPath = pathToTail;
                target = tail;
                System.out.println("pathes to apple and stone are empty!");
                System.out.println("heading to: tail");
            } else if (pathToApple.size() > 0){ //и, в самом ХОРОШЕМ случае, направляемся на яблоко
                currentPath = pathToApple;
                target = apple;
                System.out.println("path to apple found,\n heading to: apple");
            }
            else if(pathToApple.isEmpty() && pathToStone.isEmpty() && pathToTail.isEmpty()) {//направляемся "куда-ни-будь"
                Point bestV = findBestDetour(head);
                System.out.println("best V is:" + bestV);
                nextStep = bestV;

            } else
            System.out.println("chosen currentPath: " + currentPath);


            //----------------блок выбора режима прохода, в зависимости от длины змеи:
            // по длине змейки получаем нужные params для задания режима дальнейшей работы
            YourSolver.SnakeParams modeParams = getSnakeParams(board.getSnake().size());

            // режим прямого поиска яблока по Дейкстре (пока змейка маленькая):
            if (modeParams.snakeSmall) {
                System.out.println("змея короче чем smallEndPoint");
                nextStep = getDirection(this.board, this.currentPath);

            } else {// режим скручивания в змеевик (когда змейка подросла):
                System.out.println("змея длиннее чем smallEndPoint");
                nextStep = getZdirection(
                        this.board, this.currentPath, target, modeParams.proximity, modeParams.leftEndpoint, modeParams.rightEndpoint);
            }
            //------------конец блока выбора режима прохода в зав-ти от длины змеи


            System.out.println("nextStep: " + nextStep);
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
    private class SnakeParams {
        boolean snakeSmall;
        // ограничители скручивания змейки в змеевик в зависимости от длины змейки snakeLength (snakeSize)
        int proximity;
        int leftEndpoint;
        int rightEndpoint;

        public SnakeParams(boolean snakeSmall, int proximity, int leftEndpoint, int rightEndpoint) {
            this.snakeSmall = snakeSmall;
            this.proximity = proximity;
            this.leftEndpoint = leftEndpoint;
            this.rightEndpoint = rightEndpoint;
        }
    }

    public YourSolver.SnakeParams getSnakeParams(int snakeLength) {
        boolean snakeSmall = true; //параметр для переключения с режима прямого поиска по Дейскстре в режим
        //большой змеи, когда она начинает скручиваться в змеевик и включается getStickyDirection()

        //ниже задаются эндпоинты для длин змеи, при которых переключаются режимы работы алгоритма скручивания в змеевик:
        int mediumStartPoint = 30;
        int largeStartPoint = 40;
        /* расстояние прямого подхода головы к яблоку перед началом скручивания. Если 0, то скручивается еще на дальнем расстоянии от яблока
        Голова подойдет к яблоку на расстояние proximity, потом начнет скручиваться в змеевик.
        Можно это перевернуть, изменив знак на "head<target как здесь: if (Math.abs(head.getY() - target.getY()) < proximity
        Тогда proximity будет означать что голова сначала скрутится, а потом бросится к яблоку с расстояния proximity.*/
        int proximity = 6;
        int leftEndpoint = 1;//левая граница скручивания
        int rightEndpoint = this.board.size() - 1; //правая граница скручивания

        if (snakeLength > mediumStartPoint
                && snakeLength < largeStartPoint) {
            System.out.println("snakeMedium, ");
            snakeSmall = false;
            proximity = 6;
            leftEndpoint = 5;
            rightEndpoint = this.board.size() - 6;
        }
        if (snakeLength >= largeStartPoint) {
            System.out.println("snakeLarge, ");
            snakeSmall = false;
            proximity = 1;
            leftEndpoint = 3;
            rightEndpoint = this.board.size() - 4;
        }
        System.out.println("leftEndpoint: " + leftEndpoint + ",  rightEndpoint: " + rightEndpoint);
        return new YourSolver.SnakeParams(snakeSmall, proximity, leftEndpoint, rightEndpoint);
    }

    public Point findBestDetour(Point head) {
        // сюда отправляем nextStep в случаях, когда пути к яблоку и камню не найдены
        System.out.println("in findBestDetour()");
        Dijkstra.Vertex headV = this.graphApple.stream().filter((v) -> v.point.equals(head)).findFirst().orElse(null);
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


    public Point getDirection(Board board, LinkedList<Point> path) {
        Point head = board.getHead();
        Point nextStep = path.get(0);
        return nextStep;
    }

    public Point getZdirection(Board board, LinkedList<Point> path, Point target, int proximity, int leftEndpoint, int rightEndpoint) {
        // leftEndpoint, int rightEndpoint - левый и правый ограничители ширины скручивания змеи в змеевик. Чем длиннее змея -тем шире змеевик (т.е. эти границы расширяются)
        Point head = board.getHead();
        Point nextStep = null;
        String dir = null;

        // если унюхали поблизости яблоко -хватаем его!
        if (Math.abs(head.getY() - target.getY()) < proximity ////длина "нюха" по оси Y: прямой ход по Дейкстре разрешен без скрутки в змеевик, пока Y более proximity
        ) {
            nextStep = path.get(0);
        } else //иначе проверяем
            // складываем змейку в змеевик
            if (board.isAt(head.getX() - 1, head.getY(), Elements.NONE, Elements.GOOD_APPLE)
                    && head.getX() > leftEndpoint

                    &&
                    //и обязательно, чтобы были свободны козырьки сверху либо снизу
                    (board.isAt(head.getX() - 1, head.getY() - 1, Elements.NONE, Elements.GOOD_APPLE)
                            ||
                            board.isAt(head.getX() - 1, head.getY() + 1, Elements.NONE, Elements.GOOD_APPLE))

                    &&
                    //и чтобы козырёк снизу не был при яблоке ниже головы
                    !(!board.isAt(head.getX() - 1, head.getY() - 1, Elements.NONE, Elements.GOOD_APPLE)
                            &&
                            head.getY() > target.getY())

                    &&
                    //и чтобы козырёк вверху не был при яблоке выше головы
                    !(!board.isAt(head.getX() - 1, head.getY() + 1, Elements.NONE, Elements.GOOD_APPLE)
                            &&
                            head.getY() < target.getY())


            ) {
                nextStep = new PointImpl(head.getX() - 1, head.getY());
            } else if (board.isAt(head.getX() + 1, head.getY(), Elements.NONE, Elements.GOOD_APPLE)
                    && head.getX() < rightEndpoint


                    &&
                    //и обязательно, чтобы были свободны козырьки сверху либо снизу
                    (board.isAt(head.getX() + 1, head.getY() - 1, Elements.NONE, Elements.GOOD_APPLE)
                            ||
                            board.isAt(head.getX() + 1, head.getY() + 1, Elements.NONE, Elements.GOOD_APPLE))

                    &&
                    //и чтобы козырёк снизу не был при яблоке ниже головы
                    !(!board.isAt(head.getX() + 1, head.getY() - 1, Elements.NONE, Elements.GOOD_APPLE)
                            &&
                            head.getY() > target.getY())

                    &&
                    //и чтобы козырёк вверху не был при яблоке выше головы
                    !(!board.isAt(head.getX() + 1, head.getY() + 1, Elements.NONE, Elements.GOOD_APPLE)
                            &&
                            head.getY() < target.getY())


            ) {
                nextStep = new PointImpl(head.getX() + 1, head.getY());
            } else {
                nextStep = path.get(0);
            }
        return nextStep;
    }

    public boolean inspectForWayBack(Board board, List<Point> path, Point target, int requiredVertexMinimum) {
        //проверка на заход за яблоком в замкнутый безвыходный контур
        Point head = board.getHead();
        List<Dijkstra.Vertex> graphMocked = createAppleGraph();
        LinkedList<Point> pathMocked = new LinkedList<>(path);
        List<Dijkstra.Vertex> pathInVertexes = pathMocked.stream().map((point) -> new Dijkstra.Vertex(point)).collect(Collectors.toList());
        pathInVertexes.remove(new Dijkstra.Vertex(head));
        graphMocked.removeAll(pathInVertexes);
//        ArrayList<Point> snake = new ArrayList<>(board.getSnake());
//                    snake.subList()
        Dijkstra.Vertex targetMocked = graphMocked.stream().filter((v) -> v.point.equals(target)).findFirst().orElse(null);


        Set<Dijkstra.Vertex> visited = new HashSet<>();
        int vertCount = countSubVertexes(targetMocked, visited);
        if (vertCount > requiredVertexMinimum) {
            return true;
        }

        return false;
    }

    public String finalizeDirection(Point nextStep) {
        Point head = this.board.getHead();
        String dir = null;
        if (nextStep.getX() > head.getX()
                && nextStep.getY() == head.getY()
                && ((board.isAt(head.getX() + 1, head.getY(), Elements.NONE)
                || board.isAt(head.getX() + 1, head.getY(), Elements.GOOD_APPLE)))
        ) {
            dir = Direction.RIGHT.toString();
        } else if (nextStep.getX() < head.getX()
                && nextStep.getY() == head.getY()
                && ((board.isAt(head.getX() - 1, head.getY(), Elements.NONE)
                || board.isAt(head.getX() - 1, head.getY(), Elements.GOOD_APPLE)))
        ) {
            dir = Direction.LEFT.toString();
        } else if (nextStep.getY() > head.getY()
                && nextStep.getX() == head.getX()
                && ((board.isAt(head.getX(), head.getY() + 1, Elements.NONE)
                || board.isAt(head.getX(), head.getY() + 1, Elements.GOOD_APPLE)))
        ) {
            dir = Direction.UP.toString();
        } else if (nextStep.getY() < head.getY()
                && nextStep.getX() == head.getX()
                && ((board.isAt(head.getX(), head.getY() - 1, Elements.NONE)
                || board.isAt(head.getX(), head.getY() - 1, Elements.GOOD_APPLE)))
        ) {
            dir = Direction.DOWN.toString();
        }
        return dir;
    }


    //---------------------finalized methods------------------------
    public Dijkstra.Vertex getTargetFromGraph(List<Dijkstra.Vertex> graphTemp, Point target) {
        return graphTemp.stream().filter(
                        (v) -> (v.point.equals(target)))
                .findFirst().orElse(null);
    }

    public List<Dijkstra.Vertex> createAppleGraph() {
        List<Dijkstra.Vertex> graph;

        List<Point> freeSpace = null;

        freeSpace = this.board.get(Elements.NONE, Elements.GOOD_APPLE);

        freeSpace.add(this.board.getHead());
        graph = freeSpace.stream().map(Dijkstra.Vertex::new).collect(Collectors.toList());

        Consumer<Dijkstra.Vertex> setEdges = (v) -> {
            Point p = v.point;
            Point up = new PointImpl(p.getX(), p.getY() + 1);
            Point down = new PointImpl(p.getX(), p.getY() - 1);
            Point left = new PointImpl(p.getX() - 1, p.getY());
            Point right = new PointImpl(p.getX() + 1, p.getY());


            if (this.board.isAt(up, Elements.NONE) || this.board.isAt(up, Elements.GOOD_APPLE)) {
                graph.stream().filter((c) ->
                        c.point.equals(up)
                ).findFirst().ifPresent((vUp) -> v.edges.add(new Dijkstra.Edge(vUp, 1)));
            }


            if (this.board.isAt(down, Elements.NONE) || this.board.isAt(down, Elements.GOOD_APPLE)) {
                graph.stream().filter((c) ->
                        c.point.equals(down)
                ).findFirst().ifPresent((vDown) -> v.edges.add(new Dijkstra.Edge(vDown, 1)));

            }


            if (this.board.isAt(left, Elements.NONE) || this.board.isAt(left, Elements.GOOD_APPLE)) {
                graph.stream().filter((c) ->
                        c.point.equals(left)
                ).findFirst().ifPresent((vLeft) -> v.edges.add(new Dijkstra.Edge(vLeft, 1)));

            }


            if (this.board.isAt(right, Elements.NONE) || this.board.isAt(right, Elements.GOOD_APPLE)) {
                graph.stream().filter((c) ->
                        c.point.equals(right)
                ).findFirst().ifPresent(vRight -> v.edges.add(new Dijkstra.Edge(vRight, 1)));
            }
        };

        graph.forEach((v) -> setEdges.accept(v));
        return graph;

    }

    public List<Dijkstra.Vertex> createStoneGraph() {
        List<Dijkstra.Vertex> graph;

        List<Point> freeSpace = this.board.get(Elements.NONE, Elements.GOOD_APPLE, Elements.BAD_APPLE);

        freeSpace.add(this.board.getHead());
        graph = freeSpace.stream().map(Dijkstra.Vertex::new).collect(Collectors.toList());

        Consumer<Dijkstra.Vertex> setEdges = (v) -> {
            Point p = v.point;
            Point up = new PointImpl(p.getX(), p.getY() + 1);
            Point down = new PointImpl(p.getX(), p.getY() - 1);
            Point left = new PointImpl(p.getX() - 1, p.getY());
            Point right = new PointImpl(p.getX() + 1, p.getY());


            if (this.board.isAt(up, Elements.NONE) || this.board.isAt(up, Elements.GOOD_APPLE)
                    || this.board.isAt(up, Elements.BAD_APPLE)) {
                graph.stream().filter((c) ->
                        c.point.equals(up)
                ).findFirst().ifPresent((vUp) -> v.edges.add(new Dijkstra.Edge(vUp, 1)));
            }


            if (this.board.isAt(down, Elements.NONE) || this.board.isAt(down, Elements.GOOD_APPLE)
                    || this.board.isAt(down, Elements.BAD_APPLE)) {
                graph.stream().filter((c) ->
                        c.point.equals(down)
                ).findFirst().ifPresent((vDown) -> v.edges.add(new Dijkstra.Edge(vDown, 1)));

            }


            if (this.board.isAt(left, Elements.NONE) || this.board.isAt(left, Elements.GOOD_APPLE)
                    || this.board.isAt(left, Elements.BAD_APPLE)) {
                graph.stream().filter((c) ->
                        c.point.equals(left)
                ).findFirst().ifPresent((vLeft) -> v.edges.add(new Dijkstra.Edge(vLeft, 1)));

            }


            if (this.board.isAt(right, Elements.NONE) || this.board.isAt(right, Elements.GOOD_APPLE)
                    || this.board.isAt(right, Elements.BAD_APPLE)) {
                graph.stream().filter((c) ->
                        c.point.equals(right)
                ).findFirst().ifPresent(vRight -> v.edges.add(new Dijkstra.Edge(vRight, 1)));
            }
        };

        graph.forEach((v) -> setEdges.accept(v));
        return graph;

    }

    public List<Dijkstra.Vertex> createTailGraph() {
        List<Dijkstra.Vertex> graph;
        List<Point> snake = this.board.getSnake();
        Point tail = snake.get(snake.size()-1);
        System.out.println("in createTailGraph.  snake: "+snake);
        System.out.println("in createTailGraph.  tail: "+tail);
        List<Point> freeSpace = this.board.get(Elements.NONE, Elements.GOOD_APPLE, Elements.BAD_APPLE);

        freeSpace.add(this.board.getHead());
        freeSpace.add(tail);
        graph = freeSpace.stream().map(Dijkstra.Vertex::new).collect(Collectors.toList());

        Consumer<Dijkstra.Vertex> setEdges = (v) -> {
            Point p = v.point;
            Point up = new PointImpl(p.getX(), p.getY() + 1);
            Point down = new PointImpl(p.getX(), p.getY() - 1);
            Point left = new PointImpl(p.getX() - 1, p.getY());
            Point right = new PointImpl(p.getX() + 1, p.getY());


            if (this.board.isAt(up, Elements.NONE) || this.board.isAt(up, Elements.GOOD_APPLE)
                    || this.board.isAt(up, Elements.BAD_APPLE)

            || this.board.isAt(up, Elements.TAIL_END_DOWN, Elements.TAIL_END_UP, Elements.TAIL_END_LEFT, Elements.TAIL_END_RIGHT
//                    Elements.TAIL_HORIZONTAL, Elements.TAIL_LEFT_UP, Elements.TAIL_VERTICAL,
//                    Elements.TAIL_LEFT_DOWN, Elements.TAIL_RIGHT_DOWN, Elements.TAIL_RIGHT_UP
            )



            ) {
                graph.stream().filter((c) ->
                        c.point.equals(up)
                ).findFirst().ifPresent((vUp) -> v.edges.add(new Dijkstra.Edge(vUp, 1)));
            }


            if (this.board.isAt(down, Elements.NONE) || this.board.isAt(down, Elements.GOOD_APPLE)
                    || this.board.isAt(down, Elements.BAD_APPLE)

            || this.board.isAt(down, Elements.TAIL_END_DOWN, Elements.TAIL_END_UP, Elements.TAIL_END_LEFT, Elements.TAIL_END_RIGHT
//                    Elements.TAIL_HORIZONTAL, Elements.TAIL_LEFT_UP, Elements.TAIL_VERTICAL,
//                    Elements.TAIL_LEFT_DOWN, Elements.TAIL_RIGHT_DOWN, Elements.TAIL_RIGHT_UP
            )

            ) {
                graph.stream().filter((c) ->
                        c.point.equals(down)
                ).findFirst().ifPresent((vDown) -> v.edges.add(new Dijkstra.Edge(vDown, 1)));

            }


            if (this.board.isAt(left, Elements.NONE) || this.board.isAt(left, Elements.GOOD_APPLE)
                    || this.board.isAt(left, Elements.BAD_APPLE)

            || this.board.isAt(left, Elements.TAIL_END_DOWN, Elements.TAIL_END_UP, Elements.TAIL_END_LEFT, Elements.TAIL_END_RIGHT
//                    Elements.TAIL_HORIZONTAL, Elements.TAIL_LEFT_UP, Elements.TAIL_VERTICAL,
//                    Elements.TAIL_LEFT_DOWN, Elements.TAIL_RIGHT_DOWN, Elements.TAIL_RIGHT_UP
            )


            ) {
                graph.stream().filter((c) ->
                        c.point.equals(left)
                ).findFirst().ifPresent((vLeft) -> v.edges.add(new Dijkstra.Edge(vLeft, 1)));

            }


            if (this.board.isAt(right, Elements.NONE) || this.board.isAt(right, Elements.GOOD_APPLE)
                    || this.board.isAt(right, Elements.BAD_APPLE)

            || this.board.isAt(right, Elements.TAIL_END_DOWN, Elements.TAIL_END_UP, Elements.TAIL_END_LEFT, Elements.TAIL_END_RIGHT
//                    Elements.TAIL_HORIZONTAL, Elements.TAIL_LEFT_UP, Elements.TAIL_VERTICAL,
//                    Elements.TAIL_LEFT_DOWN, Elements.TAIL_RIGHT_DOWN, Elements.TAIL_RIGHT_UP
            )



            ) {
                graph.stream().filter((c) ->
                        c.point.equals(right)
                ).findFirst().ifPresent(vRight -> v.edges.add(new Dijkstra.Edge(vRight, 1)));
            }
        };

        graph.forEach((v) -> setEdges.accept(v));
        return graph;

    }


    public static void main(String[] args) {
        WebSocketRunner.runClient(
                // paste here board page url from browser after registration
                "http://46.101.237.57/codenjoy-contest/board/player/12e9swp7xv1y40cwd0z2?code=828367227519872101",
                new YourSolver(new RandomDice()),
                new Board());
    }

}
