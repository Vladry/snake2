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


    Point head;
    Point apple;
    Point stone;
    Point tail;
    List<Point> snake;

    private List<Dijkstra.Vertex> graphApple = null;
    private List<Dijkstra.Vertex> graphStone = null;
    private List<Dijkstra.Vertex> graphTail = null;
    private LinkedList<Point> currentPath = new LinkedList<>();

    LinkedList<Point> pathToApple = new LinkedList<>();
    LinkedList<Point> pathToStone = new LinkedList<>();
    LinkedList<Point> pathToTail = new LinkedList<>();
    int cycleCounterBeforeWalkingToStone = 0;

    boolean stoneAllowed = false;
    boolean appleAllowed = true;
    int requiredVertexMinimum = 50; //минимальное кол-во вершин доступных для прохода по пути path в случае выхода после достижения яблока или камня
    int freeSpaceAroundApple = 0;
    int freeSpaceAroundStone = 0;
    int freeSpaceAroundTail = 0;

    public YourSolver(Dice dice) {
        this.dice = dice;
    }

    @Override
    public String get(Board board) {


        this.board = board;
        this.head = board.getHead();
        this.apple = board.getApples().get(0);
        this.stone = board.getStones().get(0);
        snake = new ArrayList<>();
        this.snake.addAll(board.getSnake());
        LinkedList<Point> arrangedSnake = arrangeSnake(snake);
        System.out.println("arranged snake: " + arrangedSnake);
        this.tail = apple; //
        if (!arrangedSnake.isEmpty()) {
            tail = arrangedSnake.get(arrangedSnake.size() - 1);
        }
        System.out.println("tail at: " + tail);

        //чем больше змея -тем ниже требования к мин. кол-ву свободных ходов (см. стр.45)
        requiredVertexMinimum = 110 - arrangedSnake.size();

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

            this.pathToApple = Dijkstra.buildPath(appleVertexInGraph);
            this.pathToStone = Dijkstra.buildPath(stoneVertexInGraph);
            this.pathToTail = Dijkstra.buildPath(tailVertexInGraph);

            if (pathToTail.size() >= 1) {
                pathToTail.removeLast(); // сам конец хвоста удаляем, чтобы самого-себя не укусить!
            }
            System.out.println("pathToApple: " + pathToApple);
            System.out.println("pathToStone: " + pathToStone);
            System.out.println("pathToTail: " + pathToTail);

//            boolean isAppleAllowed = isAppleAllowed(board, pathToApple, apple, requiredVertexMinimum);
//            boolean isStoneAllowed = isAppleAllowed(board, pathToStone, stone, requiredVertexMinimum);
//            System.out.println("isAppleAllowed: " + isSteptoAppleAllowed);
//            System.out.println("isAppleAllowed: " + isSteptoAppleAllowed);


            //-----------------блок управления режимом куда идти змейке------------------

            // сначала описываем "плохие случаи", когда направляемся на камень:


//Здесь вся логика принятия решений КУДА идём. Тут много мутируем поля класса
            target = choosePathAndTarget();

            nextStep = chooseMovementMode(currentPath, target);

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

    // Основной метод выбора направления движения, включает внутри себя decideIfWeGoToApple()
    public Point choosePathAndTarget() {
        Point target = null;
        if (snake.size() > 100 // если змея опасно-громадная
                && pathToStone.size() > 0 && pathToApple.size() > 0 // и существуют пути,
                && pathToApple.size() * 2 > pathToStone.size() //и если яблоко не прямо рядом, то идем на камень
        ) {//направляемся на камень
            currentPath = pathToStone;
            target = stone;
            System.out.println("snake is to large");
            stoneAllowed = true;
            System.out.println("heading to: stone");
        } else if (pathToApple.isEmpty() && pathToStone.size() > 0 && pathToTail.size() > 0) {
            //если нет пути к яблоку- анализируем, идти ли нам на камень или еще погулять
            System.out.println("path to apple is empty!");
            Point probe = findBestDetour(head);
            if (cycleCounterBeforeWalkingToStone < 5) {
                cycleCounterBeforeWalkingToStone++;
                System.out.println("heading to a probe point while counterDelayGoingToStone < 5");
                currentPath = new LinkedList<>();
                currentPath.add(probe);
                target = probe;
            } else {
                System.out.println("heading to: stone because counterDelayGoingToStone is = " + cycleCounterBeforeWalkingToStone);
                currentPath = pathToStone;
                target = stone;
            }
        } else if (pathToApple.isEmpty() && pathToStone.isEmpty() && pathToTail.size() > 0) {
            //если нет пути ни к яблоку, ни к камню, но есть проход к хвосту- направляемся на свой хвост
            currentPath = pathToTail;
            target = tail;
            System.out.println("pathes to apple and stone are empty!");
            System.out.println("heading to: tail");
        } else if (pathToApple.isEmpty() && pathToStone.isEmpty() && pathToTail.isEmpty()) {//направляемся "куда-ни-будь"
            System.out.println("Paths to apple, stone and tail are empty!");

            currentPath = new LinkedList<>();
            target = null;
        } else if (pathToApple.size() > 0) { //и, в самом ХОРОШЕМ случае, думаем  идти ли нам на на яблоко
            target = decideIfWeGoToApple();//метод проверяет есть ли вокруг яблока достаточно места и решает, может пойти на хвост или на камень
        }
        System.out.println("chosen currentPath: " + currentPath);

        return target;
    }


    //метод проверяет есть ли вокруг яблока достаточно места и решает, может пойти на хвост или на камень
    public Point decideIfWeGoToApple() {
        Point target = null;
        Set<Dijkstra.Vertex> visited = new HashSet<>();
        Dijkstra.Vertex appleV = getTargetFromGraph(graphApple, apple);
        freeSpaceAroundApple = countSpaceAround(appleV, visited);

        if (freeSpaceAroundApple >= requiredVertexMinimum) {
            currentPath = pathToApple;
            target = apple;
            System.out.println("path to apple found, freeSpaceAround >= requiredVertexMinimum\n heading to: apple");
        } else {
            System.out.println("path to apple found, but can't go for apple, as freeSpaceAround < requiredVertexMinimum");

            if (!pathToTail.isEmpty()) {
                //проверяем, не опасно ли пойти на хвост:
                visited = new HashSet<>();
                Dijkstra.Vertex tailV = getTargetFromGraph(graphApple, tail);
                freeSpaceAroundTail = countSpaceAround(tailV, visited);
//---- проверяем, безопасно ли пойти на хвост вместо яблока. Требуем, чтобы вокруг хвоста было ощутимо больше ходов:
                if (freeSpaceAroundTail > freeSpaceAroundApple * 2) {
                    currentPath = pathToTail;
                    target = tail;
                    System.out.println("path to apple found, freeSpaceAroundApple < requiredVertexMinimum\n heading to: tail");
                } else if
                (!pathToStone.isEmpty()) {
                    //проверяем, не опасно ли пойти на хвост:
                    visited = new HashSet<>();
                    Dijkstra.Vertex stoneV = getTargetFromGraph(graphStone, tail);
                    freeSpaceAroundTail = countSpaceAround(stoneV, visited);
                    if (freeSpaceAroundTail > freeSpaceAroundApple * 2) {
                        currentPath = pathToStone;
                        target = stone;
                        System.out.println("path to apple found, but little space around, path to tail found, but also little space around");
                        System.out.println("path to stone found, space around stone is sufficient, heading to stone");
                    }

                } else { //если же альтернатив сьеданию яблока с плохоим freeSpaceAroundApple не найдена, таки идем на яблоко:
                    currentPath = pathToApple;
                    target = apple;
                }
            }
        }
        return target;
    }

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

    public Point chooseMovementMode(List<Point> currentPath, Point target) {
        Point nextStep = null;
        if (currentPath.isEmpty() || target == null) {
            nextStep = findBestDetour(this.board.getHead());
            System.out.println("best V is:" + nextStep);
            return nextStep;
        }
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
        return nextStep;
    }

    public YourSolver.SnakeParams getSnakeParams(int snakeLength) {
        boolean snakeSmall = true; //параметр для переключения с режима прямого поиска по Дейскстре в режим
        //большой змеи, когда она начинает скручиваться в змеевик и включается getStickyDirection()

        //ниже задаются эндпоинты для длин змеи, при которых переключаются режимы работы алгоритма скручивания в змеевик:
        int youngStartingPoint = 25;
        int mediumStartPoint = 35;
        int largeStartPoint = 45;
        /* расстояние прямого подхода головы к яблоку перед началом скручивания. Если 0, то скручивается еще на дальнем расстоянии от яблока
        Голова подойдет к яблоку на расстояние proximity, потом начнет скручиваться в змеевик.
        Можно это перевернуть, изменив знак на "head<target как здесь: if (Math.abs(head.getY() - target.getY()) < proximity
        Тогда proximity будет означать что голова сначала скрутится, а потом бросится к яблоку с расстояния proximity.*/
        int proximity = 6;
        int leftEndpoint = 1;//левая граница скручивания
        int rightEndpoint = this.board.size() - 1; //правая граница скручивания

        if (snakeLength >= youngStartingPoint
                && snakeLength < mediumStartPoint) {
            System.out.println("snakeMedium, ");
            snakeSmall = false;
            proximity = 6;
            leftEndpoint = 6;
            rightEndpoint = this.board.size() - 7;
        }

        if (snakeLength >= mediumStartPoint
                && snakeLength < largeStartPoint) {
            System.out.println("snakeMedium, ");
            snakeSmall = false;
            proximity = 4;
            leftEndpoint = 5;
            rightEndpoint = this.board.size() - 6;
        }
        if (snakeLength >= largeStartPoint) {
            System.out.println("snakeLarge, ");
            snakeSmall = false;
            proximity = 2;
            leftEndpoint = 3;
            rightEndpoint = this.board.size() - 4;
        }
        System.out.println("leftEndpoint: " + leftEndpoint + ",  rightEndpoint: " + rightEndpoint);
        return new YourSolver.SnakeParams(snakeSmall, proximity, leftEndpoint, rightEndpoint);
    }

    public Point findBestDetour(Point from) {
        System.out.println("in findBestDetour");
        // сюда отправляем nextStep в случаях, когда пути к яблоку и камню не найдены
        Dijkstra.Vertex headV = this.graphApple.stream().filter((v) -> v.point.equals(from)).findFirst().orElse(null);
//        System.out.println("headV found: " + headV.point);
        Dijkstra.Vertex bestVertex = null;
        int totalVertexesFound = 0;
        for (Dijkstra.Edge e : headV.edges) {
//берем у головы все её три смежные вершины и, в цикле ищем, для какой вершины будет макс.число totalVertexesFound. Эту вершины и возвращаем как bestVertex
//            System.out.println("inside of for-loop");
            System.out.println("checking sub-vertex: " + e.v.point);
            Set<Dijkstra.Vertex> visited = new HashSet<>();
            int vertCount = countSpaceAround(e.v, visited);
            System.out.println("a headBranch " + e.v.point + " has " + vertCount + " sub-branches");
            System.out.println("visited.size(): " + visited.size());
            if (vertCount > totalVertexesFound) {
                totalVertexesFound = vertCount;
                bestVertex = e.v;
            }
        }
//        System.out.println("exiting findBestDetour.  Возвращаем bestVertex= " + bestVertex.point);
        return bestVertex.point;
    }

    public int countSpaceAround(Dijkstra.Vertex current, Set<Dijkstra.Vertex> visited) {
        if (current == null || visited.contains(current)) return 0;
        visited.add(current);
        int vertexCounter = 1;
        for (Dijkstra.Edge e : current.edges) {
            vertexCounter += countSpaceAround(e.v, visited);
        }
        return vertexCounter;
    }


    public Point getZdirection(Board board, LinkedList<Point> path,
                               Point target, int proximity, int leftEndpoint, int rightEndpoint) {
        // leftEndpoint, int rightEndpoint - левый и правый ограничители ширины скручивания змеи в змеевик. Чем длиннее змея -тем шире змеевик (т.е. эти границы расширяются)
        Point head = board.getHead();
        Point nextStep = null;
        String dir = null;

        // если унюхали поблизости яблоко -хватаем его!
//proximity- длина "нюха" по оси Y: прямой ход по Дейкстре разрешен без скрутки в змеевик, пока Y более proximity
        if (Math.abs(head.getY() - target.getY()) < proximity
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

    //TODO isAppleAllowed не доделан, не задействован
    public boolean isAppleAllowed(Board board, List<Point> path, Point target, int requiredVertexMinimum) {
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
        int vertCount = countSpaceAround(targetMocked, visited);
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
        ) {
            dir = Direction.RIGHT.toString();
        } else if (nextStep.getX() < head.getX()
                && nextStep.getY() == head.getY()
        ) {
            dir = Direction.LEFT.toString();
        } else if (nextStep.getY() > head.getY()
                && nextStep.getX() == head.getX()
        ) {
            dir = Direction.UP.toString();
        } else if (nextStep.getY() < head.getY()
                && nextStep.getX() == head.getX()
        ) {
            dir = Direction.DOWN.toString();
        }


        if (dir == null) {
            dir = Direction.DOWN.toString();
        } //TODO потом убрать эту временную "затычку"
        return dir;
    }


    //---------------------finalized methods------------------------
    public Point getDirection(Board board, LinkedList<Point> path) {
        Point head = board.getHead();
        Point nextStep = null;
        if (!path.isEmpty()) {
            nextStep = path.get(0);
        }
        return nextStep;
    }


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
        Point tail = snake.get(snake.size() - 1);
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

    public LinkedList<Point> arrangeSnake(List<Point> snake) {
        // т.к. board.getSnake возвращает не связанную змейку, а хаотичный набор ее тела,
        // нам нужно этот набор превратить в связную змею, которую и возвращает этот метод  arrangeSnake и fillY и fillX
        System.out.println("in arrangeSnake");
        if (snake == null || snake.isEmpty()) {
            System.out.println("snake is empty or null! Exiting arrangeSnake");
            return new LinkedList<>();
        }
        List<Point> snakeElements = new ArrayList<Point>();
        snakeElements.addAll(snake);

        Point head = null;
        if (!snakeElements.isEmpty()) {
            head = snakeElements.remove(0);
        }
        LinkedList<Point> result = new LinkedList<>();
        result.add(head);

        System.out.println("before entering while");

        //----------------------------------------
        /* Аварийный выход по snakeElementsOldSize нужет потом, что когда змейка уходит с доски или себя кусает нарушается связность ее звеньев (голова смещается внуть тела), поэтому тут входим бесконечн.цикл
         * То есть, не опустошается snakeElements.  */
        boolean snakeElementsLengthChanged = true;
        while (!snakeElements.isEmpty() && snakeElementsLengthChanged) {
            int snakeElementsOldSize = snakeElements.size();
            fillY(snakeElements, result);
            fillX(snakeElements, result);
            if (snakeElements.size() == snakeElementsOldSize) {
                snakeElementsLengthChanged = false;
            }
        }
        System.out.println("exited while");
        return result;
    }

    public void fillY(List<Point> snakeElements, LinkedList<Point> result) {
        System.out.println("in fillY");
        Point startElem;
        if (result.isEmpty() || snakeElements.isEmpty()) {
            return;
        } else {
            startElem = result.get(result.size() - 1);
        }


        List<Point> sameXs = snakeElements.stream().filter((el) -> el.getX() == startElem.getX()).collect(Collectors.toList());
        if (sameXs.isEmpty()) return;
        boolean doIt = true;
        while (doIt) {
            int y = result.get(result.size() - 1).getY();
            Point newP = sameXs.stream().filter((el) -> Math.abs(el.getY() - y) == 1).findFirst().orElse(null);

            if (newP != null) {
                snakeElements.remove(newP);
                sameXs.remove(newP);
                result.add(newP);
            } else {
                doIt = false;
            }
        }
    }

    public void fillX(List<Point> snakeElements, LinkedList<Point> result) {
        System.out.println("in fillX");
        Point startElem;
        if (result.isEmpty() || snakeElements.isEmpty()) {
            return;
        } else {
            startElem = result.get(result.size() - 1);
        }


        List<Point> sameYs = snakeElements.stream().filter((el) -> el.getY() == startElem.getY()).collect(Collectors.toList());
        if (sameYs.isEmpty()) return;
        boolean doIt = true;
        while (doIt) {
            int y = result.get(result.size() - 1).getX();
            Point newP = sameYs.stream().filter((el) -> Math.abs(el.getX() - y) == 1).findFirst().orElse(null);

            if (newP != null) {
                snakeElements.remove(newP);
                sameYs.remove(newP);
                result.add(newP);
            } else {
                doIt = false;
            }
        }
    }


    public static void main(String[] args) {
        WebSocketRunner.runClient(
                // paste here board page url from browser after registration
                "http://46.101.237.57/codenjoy-contest/board/player/12e9swp7xv1y40cwd0z2?code=828367227519872101",
                new YourSolver(new RandomDice()),
                new Board());
    }

}
