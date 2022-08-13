package com.codenjoy.dojo.snake.client.probed_methods;

import com.codenjoy.dojo.services.Point;
import com.codenjoy.dojo.snake.client.Board;
import com.codenjoy.dojo.snake.client.Dijkstra;

import java.util.*;
import java.util.stream.Collectors;

public class ProbedMethods {


    //---------------------TODO methods------------------------
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
    //TODO не дописан, не используется
    public Point findStickyPointFromBestChoice(Point bestChoice) {

        Dijkstra.Vertex bestChoiceVertex = getTargetFromGraph(graphApple, bestChoice);
        List<Point> snakeElements = new ArrayList<>();
        snakeElements.addAll(snake);
        snakeElements.remove(head);
        Point candidate = snakeElements.stream().filter((el) -> Math.abs(bestChoice.getX() - el.getX()) == 1
                        || Math.abs(bestChoice.getY() - el.getY()) == 1)
                .findFirst().orElse(null);
        if (candidate != null) {
            return candidate;
        } else {
            return bestChoice;
        }

    }
    // этод метод пока что НЕ актуален, т.к. не работает конвертатор arrangeSnake змеи в нормальный LinkedList
    public Point findseeableTailEndAndSetCurrentPathIfFound() {
        Point seeableTAilEnd = null;
        for (int i = this.snake.size() - 1; i >= 5; i--) {
            Dijkstra.Vertex newTailVertex = getTargetFromGraph(graphTail, this.snake.get(i));
            this.pathToTail = Dijkstra.buildPath(newTailVertex);
            if (!pathToTail.isEmpty()) {
                System.out.println("нашли pathToTail для элемента змеи № " + i + "из всего " + this.snake.size() + "звеньев.\n" +
                        "установили существующей путь в свойство  this.pathToTail");
                seeableTAilEnd = this.snake.get(i);
                break;
            } else {
                System.out.println("надо сходить на хвост, но его не видно. Продолжаем искать последнее видимое звено хвоста.");
            }
        }
        return seeableTAilEnd;
    }
    //TODO отключил этот метод arrangeSnake() и его помощников fillX() и fillY() сбора хаотичной snake в связанную структуру LinkedList. Метод работает не корректно: теряет некоторые звенья змеи. Нужна доработка.
    public LinkedList<Point> arrangeSnake(List<Point> snake) {
        // т.к. board.getSnake возвращает не связанную змейку, а хаотичный набор ее тела,
        // нам нужно этот набор превратить в связную змею, которую и возвращает этот метод  arrangeSnake и fillY и fillX
//        System.out.println("in arrangeSnake");
        if (snake == null || snake.isEmpty()) {
//            System.out.println("snake is empty or null! Exiting arrangeSnake");
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
//        System.out.println("in fillY");
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
//        System.out.println("in fillX");
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


}
