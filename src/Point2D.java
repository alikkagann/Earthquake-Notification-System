import java.util.Objects;

public class Point2D {
    public double x;
    public double y;
    public String name;

    public Point2D(double x, double y, String name) {
        this.x = x;
        this.y = y;
        this.name = name;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public String getName() { return name; }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setName(String name) { this.name = name; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Point2D))
            return false;
        Point2D other = (Point2D) obj;
        return Double.compare(x, other.x) == 0 &&
                Double.compare(y, other.y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    public String toString() {
        return "(" + x + ", " + y + ", " + name + ")";
    }
}
