import java.util.ArrayList;
import java.util.List;

public class KDTree2D {
    private Node root;

    private static class Node {
        Point2D point;
        Node left;
        Node right;

        public Node(Point2D point) {
            this.point = point;
        }
    }

    public KDTree2D() {
        root = null;
    }

    public void insert(Point2D point) {
        root = insertRecursive(root, point, 0);
    }

    private Node insertRecursive(Node current, Point2D point, int depth) {
        if (current == null) {
            return new Node(point);
        }

        int axis = depth % 2;
        double currentCoord = (axis == 0) ? current.point.getX() : current.point.getY();
        double pointCoord = (axis == 0) ? point.getX() : point.getY();

        if (current.point.equals(point)) {
            current.point.setName(point.getName());
        } else if (pointCoord < currentCoord) {
            current.left = insertRecursive(current.left, point, depth + 1);
        } else {
            current.right = insertRecursive(current.right, point, depth + 1);
        }
        return current;
    }

    public Point2D searchByName(String name) {
        return searchByNameRecursive(root, name);
    }

    private Point2D searchByNameRecursive(Node current, String name) {
        if (current == null)
            return null;
        if (current.point.getName().equals(name))
            return current.point;
        Point2D found = searchByNameRecursive(current.left, name);
        if (found != null)
            return found;
        return searchByNameRecursive(current.right, name);
    }

    public void remove(Point2D point) {
        root = removeRecursive(root, point, 0);
    }

    public boolean removeByName(String name) {
        Point2D point = searchByName(name);
        if (point != null) {
            remove(point);
            return true;
        }
        return false;
    }

    private Node removeRecursive(Node current, Point2D point, int depth) {
        if (current == null)
            return null;
        int axis = depth % 2;

        if (current.point.equals(point)) {
            if (current.right != null) {
                Node minNode = findMinNode(current.right, axis, depth + 1);
                current.point = minNode.point;
                current.right = removeRecursive(current.right, minNode.point, depth + 1);
            } else if (current.left != null) {
                Node minNode = findMinNode(current.left, axis, depth + 1);
                current.point = minNode.point;
                current.right = removeRecursive(current.left, minNode.point, depth + 1);
                current.left = null;
            } else {
                return null;
            }
        } else {
            double currentCoord = (axis == 0) ? current.point.getX() : current.point.getY();
            double pointCoord = (axis == 0) ? point.getX() : point.getY();

            if (pointCoord < currentCoord) {
                current.left = removeRecursive(current.left, point, depth + 1);
            } else {
                current.right = removeRecursive(current.right, point, depth + 1);
            }
        }
        return current;
    }

    private Node findMinNode(Node current, int dimension, int depth) {
        if (current == null)
            return null;
        int axis = depth % 2;

        if (axis == dimension) {
            if (current.left == null)
                return current;
            return findMinNode(current.left, dimension, depth + 1);
        }

        Node leftMin = findMinNode(current.left, dimension, depth + 1);
        Node rightMin = findMinNode(current.right, dimension, depth + 1);

        Node min = current;
        if (leftMin != null && getCoordinate(leftMin.point, dimension) < getCoordinate(min.point, dimension)) {
            min = leftMin;
        }
        if (rightMin != null && getCoordinate(rightMin.point, dimension) < getCoordinate(min.point, dimension)) {
            min = rightMin;
        }
        return min;
    }

    private double getCoordinate(Point2D point, int dimension) {
        return (dimension == 0) ? point.getX() : point.getY();
    }

    public List<Point2D> rangeQueryCircular(double x, double y, double radius) {
        List<Point2D> result = new ArrayList<>();
        rangeQueryCircularRecursive(root, x, y, radius, 0, result);
        return result;
    }

    private void rangeQueryCircularRecursive(Node current, double x, double y, double radius, int depth, List<Point2D> result) {
        if (current == null)
            return;

        double distance = Math.sqrt(Math.pow(current.point.getX() - x, 2) + Math.pow(current.point.getY() - y, 2));
        // Değiştirildi: <= yerine < kullanılıyor
        if (distance < radius) {
            result.add(result.size(), current.point);
        }

        int axis = depth % 2;
        double currentCoord = (axis == 0) ? current.point.getX() : current.point.getY();
        double pointCoord = (axis == 0) ? x : y;

        if (pointCoord - radius < currentCoord) {
            rangeQueryCircularRecursive(current.left, x, y, radius, depth + 1, result);
        }
        if (pointCoord + radius >= currentCoord) {
            rangeQueryCircularRecursive(current.right, x, y, radius, depth + 1, result);
        }
    }
}
