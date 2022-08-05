package class_works;


        import com.codenjoy.dojo.services.Point;
        import com.codenjoy.dojo.services.PointImpl;
        import com.codenjoy.dojo.snake.model.Elements;

        import java.util.ArrayList;
        import java.util.Arrays;
        import java.util.Collections;
        import java.util.Iterator;
        import java.util.List;
        import java.util.PriorityQueue;
        import java.util.stream.Collectors;

public class Snake {

    public static class Vertex implements Comparable<Vertex> {
        private Point point;
        private List<Edge> edges = new ArrayList<>();
        private double minDistance = Double.POSITIVE_INFINITY;
        private Vertex previous;

        public Vertex(Point point) {
            this.point = point;
        }

        public void addEdge(Point point) {
            edges.add(new Edge(new Vertex(point), 1));
        }

        public int compareTo(Vertex other) {
            return Double.compare(minDistance, other.minDistance);
        }

        @Override
        public String toString() {
            return "{" + point + '}';
        }
    }

    static record Edge(Vertex target, double weight) {}

    public static void addNeighbours(Vertex current, List<Point> points) {
        if (points.isEmpty()) {
            return;
        }

        Iterator<Point> pointsItr = points.iterator();
        while(pointsItr.hasNext()) {
            Point point = pointsItr.next();
            if (isNeighbour(current.point, point)) {
                current.addEdge(point);
                pointsItr.remove();
            }
        }

        for (Edge edge : current.edges) {
            addNeighbours(edge.target(), points);
        }
    }

    public static Vertex createGraph(List<Point> points, Point head) {
        Vertex headV = new Vertex(head);
        addNeighbours(headV, points);

        return headV;
    }

    public static boolean isNeighbour(Point current, Point point) {
        Point up = new PointImpl(current.getX(), current.getY() + 1);
        Point down = new PointImpl(current.getX(), current.getY() - 1);
        Point left = new PointImpl(current.getX() - 1, current.getY());
        Point right = new PointImpl(current.getX() + 1, current.getY());

        return point.itsMe(up) || point.itsMe(down) || point.itsMe(left) || point.itsMe(right);
    }

    public static void computePaths(Vertex source) {
        source.minDistance = 0;
        PriorityQueue<Vertex> vertexQueue = new PriorityQueue<Vertex>();
        vertexQueue.add(source);

        while (!vertexQueue.isEmpty()) {
            Vertex current = vertexQueue.poll();
            // Проходим по всем исходящим дугам
            for (Edge adjEdge : current.edges) {
                // У каждой дуги берём вершину с которой дуга связывает
                Vertex neighbour = adjEdge.target();
                // Берём вес дуги для вычисления расстояния от источника
                double edgeWeight = adjEdge.weight();
                // Вычисляем расстояние от источника до текущей вершины
                double distanceThroughCurrent = current.minDistance + edgeWeight;
                // Проверяем надо ли менять пометку
                // Проверяем меньше ли расстояние от источника до текущей вершины чем текущая пометка
                if (distanceThroughCurrent < neighbour.minDistance) {
                    // Меняем пометку для смежной вершины
                    // Удаляем из очереди вершину, на случай если это дубликат
                    vertexQueue.remove(neighbour);
                    // Ставим новую пометку
                    neighbour.minDistance = distanceThroughCurrent;
                    // Проставляем каждой смежной вершине ссылку на текущую, для определения пути
                    neighbour.previous = current;
                    // Помещаем смежную вершину в очередь с обновлённой пометкой
                    vertexQueue.add(neighbour);
                }
            }
        }
    }

    public static List<Vertex> getShortestPathTo(Vertex target) {
        List<Vertex> path = new ArrayList<Vertex>();
        for (Vertex vertex = target; vertex != null; vertex = vertex.previous) {
            path.add(vertex);
        }
        Collections.reverse(path);
        return path;
    }

    public static void main(String[] args) {
        Vertex s = new Vertex(null);
        Vertex a = new Vertex(null);
        Vertex b = new Vertex(null);
        Vertex c = new Vertex(null);
        Vertex d = new Vertex(null);
        Vertex e = new Vertex(null);

        s.edges.addAll(Arrays.asList(
                new Edge(a, 1),
                new Edge(b, 5)));
        a.edges.addAll(Arrays.asList(
                new Edge(c, 2),
                new Edge(b, 2),
                new Edge(d, 1)));
        b.edges.addAll(Arrays.asList(
                new Edge(d, 2)));
        c.edges.addAll(Arrays.asList(
                new Edge(e, 1),
                new Edge(d, 3)));
        d.edges.addAll(Arrays.asList(
                new Edge(e, 2)));
        // У вершины е нет инцедентных ребер
        computePaths(s);
        List<Vertex> path = getShortestPathTo(e);
        System.out.println("Path: " + path);
    }
}