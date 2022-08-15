package com.codenjoy.dojo.snake.client;

import com.codenjoy.dojo.client.Solver;
import com.codenjoy.dojo.client.WebSocketRunner;
import com.codenjoy.dojo.services.*;
import com.codenjoy.dojo.snake.model.Elements;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.codenjoy.dojo.snake.client.Path.*;

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
    LinkedList<Point> pathToDetour = new LinkedList<>();

    int cycleCounterBeforeWalkingToStone = 0;
    int minimumReqAmntOfStepsAroundThePoint = 40; //минимальное кол-во вершин доступных для прохода по пути path в случае выхода после достижения яблока или камня
    int freeSpaceAroundApple = 0;
    int freeSpaceAroundStone = 0;
    int freeSpaceAroundTail = 0;
    int maximumAllowedSnakeSize = 0;
    //    LinkedList<Point> arrangedSnake = new LinkedList<>();
    Path chosenPath;

    boolean loggingLevel1 = true;
    boolean loggingLevel2 = true;
    boolean loggingLevel3 = true;

    boolean tailToHeadDistanceIsOkay = false;
    boolean appleTailStonePathsNotFound = false;
    int headToTailAllowedDistance = 1;


    public YourSolver(Dice dice) {
        this.dice = dice;
    }

    @Override
    public String get(Board board) {
        this.board = board;
        setVariables();
        if (board.isGameOver()) {
            return Direction.UP.toString();
        }

        try {
            Point nextStep = null;
            String dir = null;

            createAndComputeGraphs();
            findPaths();

            if (pathToTail.size() >= 1) {
                pathToTail.removeLast(); // сам конец хвоста удаляем, чтобы самого-себя не укусить!
            }
            choosePath();
            setPath();
            nextStep = getNextStep(currentPath);
            if (loggingLevel2) {
                System.out.println("nextStep before possible check in selectFinalDagonalStepOutOfPossibleTwoWhenHeadNearApple: " + nextStep);
            }
            Point checkedNextStep = selectFinalDagonalStepOutOfPossibleTwoWhenHeadNearApple(nextStep);
            if (loggingLevel2) {
                System.out.println("checkedNextStep из которого получаем dir: " + checkedNextStep);
            }
            dir = convertNextStepToDirection(checkedNextStep);

            return dir;

        } catch (Exception e) {
            System.out.println("общее исключение .get()");
            return Direction.DOWN.toString();
        }
    }

    // -------------------методы--------------------------


    public Point getNextStep(List<Point> currentPath) {
        if (loggingLevel3) {
            System.out.println("in getNextStep(), currentPath: " + currentPath);
        }
        Point nextStep = null;
        if (!currentPath.isEmpty()) {
            nextStep = currentPath.get(0);
            if (loggingLevel3) {
                System.out.println("nextStep is taken from currentPath and is:  " + currentPath);
            }
        } else {
            nextStep = findBestDetour(this.board.getHead());
            this.currentPath.add(nextStep);//обязательно в пустой путь вложить, иначе не найдется target в аргументе z_Folder
            if (loggingLevel3) {
                System.out.println("currentPath был пустой. nextStep взяли findBestDetour(): " + nextStep);
                System.out.println("и положили nextStep в currentPath");
            }
        }


        //----------------блок выбора режима прохода, в зависимости от длины змеи:
        // по длине змейки получаем нужные params для задания режима дальнейшей работы
        YourSolver.SnakeParams modeParams = getSnakeParams(board.getSnake().size());

        // режим прямого поиска яблока по Дейкстре (пока змейка маленькая):
        if (modeParams.snakeSmall) {
            if (loggingLevel1) {
                System.out.println("змея короче чем smallEndPoint, поэтому nextStep остаётся = currentPath.get(0)");
            }
        } else {// режим скручивания в змеевик (когда змейка подросла):
            if (loggingLevel1) {
                System.out.println("змея длиннее чем smallEndPoint, поэтому отсылаем nextStep на обработку в z_Folder");
                System.out.println("calling next step = z_Folder(nextStep:" + nextStep + ")");
            }
            Point target = this.currentPath.get(currentPath.size() - 1);
            nextStep = z_Folder(nextStep, target, modeParams.proximity, modeParams.leftEndpoint, modeParams.rightEndpoint);
            if (loggingLevel1) {
                System.out.println("змея длиннее чем smallEndPoint");
                System.out.println("nextStep from z_Folder: " + nextStep);
            }
        }
        if (loggingLevel1) {
            System.out.println("returning nextStep from getNextStep(): " + nextStep);
        }
        return nextStep;
    }


    public Point z_Folder(Point nextStepFromCurrentPath, Point target, int proximity, int leftEndpoint, int rightEndpoint) {
        System.out.println("in z_Folder(nextStepFromCurrentPath:" + nextStepFromCurrentPath + ", target: " + target + ", proximity: " + proximity + ") -> currentPath: " + currentPath);
        Point head = board.getHead();
        Point nextStep = null;
        String dir = null;

        if (nextStepFromCurrentPath == target) { //всего одно значение в currentPath - это признак того, что
// либо сейчас укусим яблоко, либо что мы пришли сюда из findBestDetour.
// поэтому сразу выходим из z_Folder не меняя nextStepFromCurrentPath
            nextStep = nextStepFromCurrentPath;
            if (loggingLevel1) {
                System.out.println("in getZdirection. return nextStep:" + nextStep);
            }
            return nextStep;
        }


        Set<Dijkstra.Vertex> visited = new HashSet<>();
        int probeToLeft = countSpaceAround(new Dijkstra.Vertex(
                new PointImpl(this.head.getX() - 1, this.head.getY())), visited);
        visited = new HashSet<>();
        int probeToRight = countSpaceAround(new Dijkstra.Vertex(
                new PointImpl(this.head.getX() + 1, this.head.getY())), visited);
        boolean foldToLeftIsAllowed = false;
        boolean foldToRightIsAllowed = false;
        if (probeToLeft > probeToRight) {
            foldToLeftIsAllowed = true;
            System.out.println("-> foldToLeftIsAllowed = true");
        } else if (probeToLeft < probeToRight) {
            foldToRightIsAllowed = true;
            System.out.println("-> foldToRightIsAllowed = true");
        } else if (probeToLeft == probeToRight) {
            foldToLeftIsAllowed = true;
            foldToRightIsAllowed = true;
            System.out.println("-> both: foldToLeftIsAllowed and foldToRightIsAllowed = true ");
        }

        // если унюхали поблизости яблоко -хватаем его!
//proximity- длина "нюха" по оси Y: прямой ход по Дейкстре разрешен без скрутки в змеевик, пока Y более proximity
        if (Math.abs(head.getY() - target.getY()) > proximity
                || board.isNear(head, Elements.BAD_APPLE) //ни дай Бог рядом камень - может произойти сбой, поэтому не дуркуем и идем по маршруту!
                || Math.abs(head.getY() - target.getY()) <= 2 //сожрать яблоко, когда голова уже на соседней строке с яблоком при ЛЮБОМ proximity
        ) {
            nextStep = nextStepFromCurrentPath;
            if (loggingLevel1) {
                System.out.println("proximity: " + proximity);
                System.out.println("deltaY head vs apple: " + Math.abs(head.getY() - apple.getY()));
                System.out.println("малый proximity, либо рядом камень -> suggestedNextStep: " + nextStep);
            }
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

                    && foldToLeftIsAllowed
            ) {
                nextStep = new PointImpl(head.getX() - 1, head.getY());
                if (loggingLevel1) {
                    System.out.println("складываем змеевик- один шаг влево: " + nextStep);
                }
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
                    && foldToRightIsAllowed

            ) {
                nextStep = new PointImpl(head.getX() + 1, head.getY());
                if (loggingLevel1) {
                    System.out.println("складываем змеевик- один шаг вправо: " + nextStep);
                }
            } else {
                nextStep = nextStepFromCurrentPath;
                if (loggingLevel1) {
                    System.out.println("-> почему-то не смогли сложить в змеевик- устанавливаем nextStep: " + nextStep);
                }
            }


        if (loggingLevel1) {
            System.out.println("in getZdirection. return nextStep:" + nextStep);
        }
        return nextStep;
    }

    public Point findBestDetour(Point from) {// метод может вернуть null
        if (loggingLevel3) {
            System.out.println("in findBestDetour");
        }
        // сюда отправляем nextStep в случаях, когда никакие пути не найдены или являются опасными
        Dijkstra.Vertex headV = this.graphApple.stream().filter((v) -> v.point.equals(from)).findFirst().orElse(null);
//        System.out.println("headV found: " + headV.point);
        Dijkstra.Vertex bestVertex = null;
        int foundInPrevCycle = 0;
        for (Dijkstra.Edge e : headV.edges) {
            Dijkstra.Vertex currentCycleVertex = e.v; //взяли последнее попавшееся, на случай, если потом не найдем лУчшего хода, иначе вернулся бы null
            if (loggingLevel1) {
                System.out.println("checking sub-vertex: " + e.v.point);
            }
//берем у головы все её три смежные вершины и, в цикле ищем, для какой вершины будет макс.число totalVertexesFound. Эту вершины и возвращаем как bestVertex
            Set<Dijkstra.Vertex> visited = new HashSet<>();
            int vertCount = countSpaceAround(e.v, visited);
            if (loggingLevel1) {
                System.out.println("a headBranch " + e.v.point + " has " + vertCount + " sub-branches");
                System.out.println("visited.size(): " + visited.size());
            }
            if (vertCount > foundInPrevCycle) {
                foundInPrevCycle = vertCount;
                bestVertex = currentCycleVertex;
            }
        }
        if (loggingLevel1) {
            System.out.println("exiting findBestDetour.  Возвращаем bestVertex= " + bestVertex.point);
        }
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

    public Point selectFinalDagonalStepOutOfPossibleTwoWhenHeadNearApple(Point recommendedStep) {

        Point NextStepStage1 = recommendedStep;
        if ( // Эту проверку выполняем ТОЛЬКО когда голова находится по-диагонали рядом с яблоком, иначе возвращаем неизменённый nextStep
                Math.abs(head.getX() - apple.getX()) == 1 && Math.abs(head.getY() - apple.getY()) == 1
        ) {
            Point stepOption1 = new PointImpl(head.getX(), apple.getY());
            Point stepOption2 = new PointImpl(apple.getX(), head.getY());


            if (loggingLevel2) {
                System.out.println("in selectFinalDagonalStepOutOfPossibleTwoWhenHeadNearApple(" + recommendedStep + ")");
                System.out.println("два потенциальных шага на выбор: " + stepOption1 + " и " + stepOption2);
//                System.out.println("!!! distance between head and apple: " + nextStep.distance(apple));
            }
            List<Dijkstra.Vertex> emulatedTailGraphOption1Stage1 =
                    getEmulatedTaleGraphStage1(stepOption1);
            List<Dijkstra.Vertex> emulatedTailGraphOption2Stage1 =
                    getEmulatedTaleGraphStage1(stepOption2);

            Dijkstra.Vertex appleVertInEmulatedTailGraphOption1Step1 = getTargetFromGraph(emulatedTailGraphOption1Stage1, head);
            Dijkstra.Vertex appleVertInEmulatedTailGraphOption2Step1 = getTargetFromGraph(emulatedTailGraphOption2Stage1, head);
            Set<Dijkstra.Vertex> visted = new HashSet<Dijkstra.Vertex>();
            int firstOptionStage1 = countSpaceAround(appleVertInEmulatedTailGraphOption1Step1, visted);
            visted = new HashSet<Dijkstra.Vertex>();
            int secondOptionStage1 = countSpaceAround(appleVertInEmulatedTailGraphOption2Step1, visted);
            if (loggingLevel2) {
                System.out.println("freeSpaceAroundAple_stepOption1: " + firstOptionStage1);
                System.out.println("freeSpaceAroundAple_stepOption2: " + secondOptionStage1);
            }

            if (firstOptionStage1 > secondOptionStage1) {
                NextStepStage1 = stepOption1;
                if (loggingLevel2) {
                    System.out.println("из двух опций, выбрали лучший " + NextStepStage1);
                }
            } else if ((firstOptionStage1 < secondOptionStage1)) {
                NextStepStage1 = stepOption2;
                if (loggingLevel2) {
                    System.out.println("из двух опций, выбрали лучший " + NextStepStage1);
                }
            } else if (firstOptionStage1 == secondOptionStage1) {
                if (recommendedStep.equals(stepOption1)) {
                    NextStepStage1 = recommendedStep;//получили наилучший следующий шаг с точки зрения пространства вокруг яблока
                }
            }
        }
        System.out.println("NextStepStage1: " + NextStepStage1);
        return NextStepStage1;
    }

    public List<Dijkstra.Vertex> getEmulatedTaleGraphStage1(Point nextStepHeadPosition) {
//        System.out.println("disconnecting from nextStepHeadPosition: "+nextStepHeadPosition);
        List<Dijkstra.Vertex> emulatedTale_BasedGraph = new ArrayList<>();
        emulatedTale_BasedGraph.addAll(graphTail);

        Point up = new PointImpl(nextStepHeadPosition.getX(), nextStepHeadPosition.getY() + 1);
        Point down = new PointImpl(nextStepHeadPosition.getX(), nextStepHeadPosition.getY() - 1);
        Point left = new PointImpl(nextStepHeadPosition.getX() - 1, nextStepHeadPosition.getY());
        Point right = new PointImpl(nextStepHeadPosition.getX() + 1, nextStepHeadPosition.getY());
        final Set<Point> AdjacentPoints = new HashSet<>(List.of(up, down, left, right));

        Set<Dijkstra.Vertex> adjacentVertexes = emulatedTale_BasedGraph.stream().filter((c) ->
                AdjacentPoints.contains(c.point)
        ).collect(Collectors.toSet());
//        System.out.println("adjacentVertexes  to be disconnected from emulated nextStepHeadPosition: \n" + adjacentVertexes);

        List<Dijkstra.Edge> edgesToBeRemoved = new ArrayList<>();

        adjacentVertexes.forEach((vert) -> vert.edges.forEach((e) -> {
            if (e.v.point.equals(nextStepHeadPosition)) {
//                System.out.println("found: "+e.v);
                edgesToBeRemoved.add(e);

            }
        }));
//        System.out.println("edgesToBeRemoved: "+edgesToBeRemoved);
        adjacentVertexes.forEach((vert) -> {

            vert.edges.removeAll(edgesToBeRemoved);
//            System.out.println("after an attempt to remove, adjacentVertexes: "+adjacentVertexes);

        });
        return emulatedTale_BasedGraph;
    }

    public List<Dijkstra.Vertex> getEmulatedTaleGraphStage2(List<Dijkstra.Vertex> baseEmulatedTailGraphStep2, Point currEmuHeadSubstititedApple) {
        System.out.println("in getEmulatedTaleGraphStage2.  currentEmulatedHead: " + currEmuHeadSubstititedApple);
        System.out.println("если все ок, то яблоко должно быть в одном реальном шаге от currentEmulatedHead");
        System.out.println("apple: " + apple);
        System.out.println("сейчас сьедаем яблоко, т.е. дисконнектим яблочную позицию от доступных ходов, тем самым ставим на позицию яблока голову змеи");

        Point up = new PointImpl(currEmuHeadSubstititedApple.getX(), currEmuHeadSubstititedApple.getY() + 1);
        Point down = new PointImpl(currEmuHeadSubstititedApple.getX(), currEmuHeadSubstititedApple.getY() - 1);
        Point left = new PointImpl(currEmuHeadSubstititedApple.getX() - 1, currEmuHeadSubstititedApple.getY());
        Point right = new PointImpl(currEmuHeadSubstititedApple.getX() + 1, currEmuHeadSubstititedApple.getY());
        final Set<Point> AdjacentPoints = new HashSet<>(List.of(up, down, left, right));

        Set<Dijkstra.Vertex> adjacentVertexes = baseEmulatedTailGraphStep2.stream().filter((c) ->
                AdjacentPoints.contains(c.point)
        ).collect(Collectors.toSet());
//        System.out.println("adjacentVertexes  to be disconnected from emulated nextStepHeadPosition: \n" + adjacentVertexes);

        List<Dijkstra.Edge> edgesToBeRemoved = new ArrayList<>();

        adjacentVertexes.forEach((vert) -> vert.edges.forEach((e) -> {
            if (e.v.point.equals(currEmuHeadSubstititedApple)) {
//                System.out.println("found: "+e.v);
                edgesToBeRemoved.add(e);

            }
        }));
//        System.out.println("edgesToBeRemoved: "+edgesToBeRemoved);
        adjacentVertexes.forEach((vert) -> {

            vert.edges.removeAll(edgesToBeRemoved);
//            System.out.println("after an attempt to remove, adjacentVertexes: "+adjacentVertexes);

        });
        return baseEmulatedTailGraphStep2;
    }

    //---------------------finalized methods------------------------
    public YourSolver.SnakeParams getSnakeParams(int snakeLength) {
        boolean snakeSmall = true; //параметр для переключения с режима прямого поиска по Дейскстре в режим
        //большой змеи, когда она начинает скручиваться в змеевик и включается getStickyDirection()

        //ниже задаются эндпоинты для длин змеи, при которых переключаются режимы работы алгоритма скручивания в змеевик:
        int youngStartingPoint = 30;
        int mediumStartPoint = 40;
        int largeStartPoint = 55;
        /* расстояние прямого подхода головы к яблоку перед началом скручивания. Если 0, то скручивается еще на дальнем расстоянии от яблока
        Голова подойдет к яблоку на расстояние proximity, потом начнет скручиваться в змеевик.
        Можно это перевернуть, изменив знак на "head<target как здесь: if (Math.abs(head.getY() - target.getY()) < proximity
        Тогда proximity будет означать что голова сначала скрутится, а потом бросится к яблоку с расстояния proximity.*/
        int proximity = 0;
        int leftEndpoint = 1;//левая граница скручивания
        int rightEndpoint = this.board.size() - 1; //правая граница скручивания

        if (snakeLength >= youngStartingPoint/*25*/
                && snakeLength < mediumStartPoint) {
            if (loggingLevel1) {
                System.out.println("snakeMedium, ");
            }
            snakeSmall = false;
            proximity = 2;
            leftEndpoint = 6;
            rightEndpoint = this.board.size() - 7;
        }

        if (snakeLength >= mediumStartPoint/*35*/
                && snakeLength < largeStartPoint/*45*/) {
            if (loggingLevel1) {
                System.out.println("snakeMedium, ");
            }
            snakeSmall = false;
            proximity = 4;
            leftEndpoint = 5;
            rightEndpoint = this.board.size() - 6;
        }
        if (snakeLength >= largeStartPoint/*45*/) {
            if (loggingLevel1) {
                System.out.println("snakeLarge, ");
            }
            snakeSmall = false;
            proximity = 8;
            leftEndpoint = 3;
            rightEndpoint = this.board.size() - 4;
        }
        if (loggingLevel1) {
            System.out.println("leftEndpoint: " + leftEndpoint + ",  rightEndpoint: " + rightEndpoint);
        }
        return new YourSolver.SnakeParams(snakeSmall, proximity, leftEndpoint, rightEndpoint);
    }

    public void setVariables() {
        this.head = this.board.getHead();
        this.apple = this.board.getApples().get(0);
        this.stone = this.board.getStones().get(0);
        this.snake = new ArrayList<>();
        this.snake.addAll(this.board.getSnake());
//        arrangedSnake = new LinkedList<>();//обязательно пересоздать, иначе добавляет новую змею в существующую старую змею
//        arrangedSnake.addAll(arrangeSnake(snake));
        this.tail = this.board.get(Elements.TAIL_END_DOWN, Elements.TAIL_END_UP, Elements.TAIL_END_LEFT, Elements.TAIL_END_RIGHT).get(0);

        tailToHeadDistanceIsOkay = head.distance(tail) <= headToTailAllowedDistance;
        appleTailStonePathsNotFound = pathToApple.isEmpty() && pathToStone.isEmpty() && pathToTail.isEmpty();


        if (loggingLevel1) {
            System.out.println("tail: " + this.tail);
        }

        if (loggingLevel3) {
            System.out.println("snake.size(): " + this.snake.size());
        }

//        System.out.println("arrangedSnake.size(): "+arrangedSnake.size());
//        if (!arrangedSnake.isEmpty()) {
//            this.tail = arrangedSnake.get(arrangedSnake.size() - 1);
//        }

        this.minimumReqAmntOfStepsAroundThePoint = 30;
        this.maximumAllowedSnakeSize = 150;
    }

    public void createAndComputeGraphs() {
        this.graphApple = createAppleGraph();
        this.graphStone = createStoneGraph();
        this.graphTail = createTailGraph();


        Dijkstra.Vertex headVertexInAppleGraph = getTargetFromGraph(graphApple, head);
        Dijkstra.computeGraph(headVertexInAppleGraph, null);

        Dijkstra.Vertex headVertexInStoneGraph = getTargetFromGraph(graphStone, head);
        Dijkstra.computeGraph(headVertexInStoneGraph, null);


        Dijkstra.Vertex headVertexInTailGraph = getTargetFromGraph(graphTail, head);
        Dijkstra.computeGraph(headVertexInTailGraph, tail);
    }

    public void findPaths() {
        Dijkstra.Vertex appleVertexInGraph = getTargetFromGraph(graphApple, apple);
        Dijkstra.Vertex stoneVertexInGraph = getTargetFromGraph(graphStone, stone);
        Dijkstra.Vertex tailVertexInGraph = getTargetFromGraph(graphTail, tail);

        this.pathToApple = Dijkstra.buildPath(appleVertexInGraph);
        this.pathToStone = Dijkstra.buildPath(stoneVertexInGraph);
        this.pathToTail = Dijkstra.buildPath(tailVertexInGraph);
    }

    public void choosePath() {


// если змея опасно-громадная:
        if (snake.size() > maximumAllowedSnakeSize) {
            if (!pathToApple.isEmpty()
                    && !pathToStone.isEmpty()
                    && pathToApple.size() > pathToStone.size() * 3 //и если яблоко не прямо рядом, то идем на камень
            ) {//направляемся на камень
                this.chosenPath = TO_STONE;
                if (loggingLevel1) {
                    System.out.println("snake is to large");
                    System.out.println("and heading to apple because pathToStone is much sorter than pathToApple ");
                }

            } else if (!pathToApple.isEmpty()
                    && !pathToStone.isEmpty()
                    && pathToApple.size() <= pathToStone.size() * 3 //и если яблоко не прямо рядом, то идем на камень
            ) {
                if (loggingLevel1) {
                    System.out.println("snake is to large");
                    System.out.println("but heading to apple because pathToApple is much sorter than pathToStone");
                }
                this.chosenPath = TO_APPLE;
            }
        } else


//если змея НЕ опасно-громадная:


            //блочёк направления на хвост:
            if (pathToApple.isEmpty() && !pathToTail.isEmpty() && tailToHeadDistanceIsOkay) {
                if (loggingLevel1) {
                    System.out.println("path to apple is empty, but found pathToTail! Before Heading to tail проверим, можно ли увеличить разрыв между головой и хвостом:");
                }


//суб-блочёк попытки увеличения разрыва между головой и хвостом:
                Set<Dijkstra.Vertex> visited = new HashSet<>();
                Dijkstra.Vertex currentHeadVertex = getTargetFromGraph(graphTail, head);
                int currentSpaceAroundHead = countSpaceAround(currentHeadVertex, visited);
                if (head.distance(tail) < 4 && currentSpaceAroundHead > 20) {
//при движении "на хвост", все же стараемся добавить разрыв в цепочку между головой и хвостом на случай, если
// голова проходит рядом с относительно свободной зоной - для того, чтобы постепенно увеличивать расстояние между
//  головой и хвостом. Этот разрыв в расстоянии обеспечит возможность укусить яблоко в трудно-доступных
// местах и избежать бесконечной зацикленности похода на хвост.
                    this.chosenPath = TO_DETOUR;
                    System.out.println("находимся в режиме похода на хвост, но сделали шажок в сторону в to_detour, тобы увеличить разрыв между головой и хвостом");
                } else {//если разрыв голова-хвост достаточный или нет места для похода в detour -продолжаем идти к хвосту
                    System.out.println("увеличить разрыв не получилось - направляемся на хвост");
                    this.chosenPath = TO_TAIL;
                }


            } else if (pathToApple.isEmpty() && pathToTail.isEmpty() && !pathToStone.isEmpty()) {
                if (loggingLevel1) {
                    System.out.println("Paths to apple and tail are empty!  Heading to stone");
                }
                this.chosenPath = TO_STONE;
            } else if (appleTailStonePathsNotFound) {//направляемся "куда-ни-будь"
                if (loggingLevel1) {
                    System.out.println("All paths are empty!");
                }
                this.chosenPath = TO_DETOUR;
            } else if (!pathToApple.isEmpty()) { //и, в самом ХОРОШЕМ случае, думаем  идти ли нам на на яблоко
                decideIfWeGoToApple();//метод проверяет есть ли вокруг яблока достаточно места и решает, может пойти на хвост или на камень
            }
        if (loggingLevel3) {
            System.out.println("chosen currentPath: " + chosenPath);
        }

    }

    public void setPath() {
        switch (this.chosenPath) {
            case TO_APPLE:
                if (loggingLevel3) {
                    System.out.println("case:  TO_APPLE");
                }
                currentPath = pathToApple;
                break;

            case TO_TAIL:
                if (loggingLevel3) {
                    System.out.println("case:  TO_TAIL");
                }
                currentPath = pathToTail;
                break;

            case TO_STONE:
                if (loggingLevel3) {
                    System.out.println("case:  TO_STONE");
                }
                currentPath = pathToStone;
                break;

            case TO_DETOUR:
                if (loggingLevel3) {
                    System.out.println("case:  TO_DETOUR  (выполнили здесь:  pathToDetour.clear(),  currentPath.clear()");
                }
                pathToDetour.clear();//очистить, т.к. будет содержать предыдущий мусор
                currentPath.clear();//очистить, чтобы сработал findPathToDetour в методе findBestDetour()
                break;

            default:
                break;

        }
    }

    public void decideIfWeGoToApple() { //метод проверяет есть ли вокруг яблока достаточно места и решает, может пойти на хвост или на камень
        Set<Dijkstra.Vertex> visited = new HashSet<>();
        Dijkstra.Vertex appleV = getTargetFromGraph(graphStone, apple);//нужно брать именно граф с камнем, т.к. камень мешает "видеть" выход к другим свободным местам
        freeSpaceAroundApple = countSpaceAround(appleV, visited);

        if (freeSpaceAroundApple >= minimumReqAmntOfStepsAroundThePoint) {
            if (loggingLevel1) {
                System.out.println("path to apple found, freeSpaceAround >= requiredVertexMinimum\n heading to: apple");
            }
            this.chosenPath = TO_APPLE;
        } else// во всех остальных случаях, вокруг яблоко НЕТ достаточного пространства и анализируем дальше:
            if (!pathToTail.isEmpty()) {
                if (loggingLevel1) {
                    System.out.println("path to apple found, pathToTail found");
                    System.out.println("but can't go for apple, as freeSpaceAround < requiredVertexMinimum или расстояние от головы до хвоста меньше двух");
                }
                if (tailToHeadDistanceIsOkay) {//обязательно чтобы голова и хвост не были совсем рядом-иначе были случаи что умирала
                    if (loggingLevel1) {
                        System.out.println("расстояние от головы до хваста меньше двух. Не выполнилось: if (head.distance(tail)<= 3)");
                    }
                    System.out.println("heading to stone or detour");
                    forwardToStoneOrDetour();
                } else {
                    System.out.println("heading to tail");
                    this.chosenPath = TO_TAIL;
                }
            } else {//это опасный случай, когда ничего не найдено кроме пути к яблоку.
                // TODO это очень опасный случай - продумать его для разной длины змейки, может включить поиск "уменьшённого хвоста"
                forwardToStoneOrDetour();
            }

    }

    public void forwardToStoneOrDetour() {
        if (loggingLevel1) {
            System.out.println("in forwardToStoneOrDetour");
        }
        if (!pathToStone.isEmpty()) {
            if (loggingLevel1) {
                System.out.println("heading to stone");
            }
            this.chosenPath = TO_STONE;
        } else {
            Point detour = findBestDetour(head);
            if (detour != null) {
                currentPath.clear();
                currentPath.add(detour);//сначала поищем, а есть ли другие пустые холостые ходы
                this.chosenPath = TO_DETOUR;
            } else {// ну, это уже полная пизда, если даже и detour не найден, то-вздох перед смертью...
                if (loggingLevel1) {
                    System.out.println("настала полная пизда...");
                }
                currentPath = pathToApple;
            }
        }
    }

    public String convertNextStepToDirection(Point nextStep) {
        // метод работает как часы! Я сверял.
        System.out.println("in  convertNextStepToDirection(" + nextStep + ')');

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
            dir = Direction.DOWN.toString();//неадекватный шаг вниз-сигнализатор сбоя программы!
        }
        System.out.println("direction before quitting:  " + dir);
        return dir;
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

        List<Point> freeSpace = this.board.get(Elements.NONE, Elements.GOOD_APPLE, Elements.TAIL_END_DOWN, Elements.TAIL_END_UP, Elements.TAIL_END_LEFT, Elements.TAIL_END_RIGHT);
        freeSpace.add(this.board.getHead());
        freeSpace.add(this.tail);
        graph = freeSpace.stream().map(Dijkstra.Vertex::new).collect(Collectors.toList());

        Consumer<Dijkstra.Vertex> setEdges = (v) -> {
            Point p = v.point;
            Point up = new PointImpl(p.getX(), p.getY() + 1);
            Point down = new PointImpl(p.getX(), p.getY() - 1);
            Point left = new PointImpl(p.getX() - 1, p.getY());
            Point right = new PointImpl(p.getX() + 1, p.getY());


            if (this.board.isAt(up,
                    Elements.NONE, Elements.GOOD_APPLE, Elements.TAIL_END_DOWN, Elements.TAIL_END_UP, Elements.TAIL_END_LEFT, Elements.TAIL_END_RIGHT)
            ) {
                graph.stream().filter((c) ->
                        c.point.equals(up)
                ).findFirst().ifPresent((vUp) -> v.edges.add(new Dijkstra.Edge(vUp, 1)));
            }


            if (this.board.isAt(down, Elements.NONE, Elements.GOOD_APPLE, Elements.TAIL_END_DOWN, Elements.TAIL_END_UP, Elements.TAIL_END_LEFT, Elements.TAIL_END_RIGHT)
            ) {
                graph.stream().filter((c) ->
                        c.point.equals(down)
                ).findFirst().ifPresent((vDown) -> v.edges.add(new Dijkstra.Edge(vDown, 1)));
            }


            if (this.board.isAt(left,
                    Elements.NONE, Elements.GOOD_APPLE, Elements.TAIL_END_DOWN, Elements.TAIL_END_UP, Elements.TAIL_END_LEFT, Elements.TAIL_END_RIGHT)
            ) {
                graph.stream().filter((c) ->
                        c.point.equals(left)
                ).findFirst().ifPresent((vLeft) -> v.edges.add(new Dijkstra.Edge(vLeft, 1)));
            }


            if (this.board.isAt(right,
                    Elements.NONE, Elements.GOOD_APPLE, Elements.TAIL_END_DOWN, Elements.TAIL_END_UP, Elements.TAIL_END_LEFT, Elements.TAIL_END_RIGHT)
            ) {
                graph.stream().filter((c) ->
                        c.point.equals(right)
                ).findFirst().ifPresent(vRight -> v.edges.add(new Dijkstra.Edge(vRight, 1)));
            }
        };

        graph.forEach((v) -> setEdges.accept(v));
        return graph;

    }

    public Dijkstra.Vertex getTargetFromGraph(List<Dijkstra.Vertex> graphTemp, Point target) {
        return graphTemp.stream().filter(
                        (v) -> (v.point.equals(target)))
                .findFirst().orElse(null);
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


    public static void main(String[] args) {
        WebSocketRunner.runClient(
                // paste here board page url from browser after registration
                "http://46.101.237.57/codenjoy-contest/board/player/12e9swp7xv1y40cwd0z2?code=828367227519872101",
                new YourSolver(new RandomDice()),
                new Board());
    }

}
