package se.cha.function;

import java.util.ArrayList;
import java.util.List;

public class Point implements Comparable<Point> {

    private final List<PointChangedListener> listeners = new ArrayList<>();

    private double x;
    private double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Point(double x, double y, PointChangedListener pointChangedListener) {
        this.x = x;
        this.y = y;

        addPointChangedListener(pointChangedListener);
    }

    public static boolean isClose(Point point1, Point point2, double radius) {
        final double dx = point1.x - point2.x;
        final double dy = point1.y - point2.y;
        return Math.sqrt(dx * dx + dy * dy) < radius;
    }

    /**
     * Comparison is made by primary x and secondary y.
     */
    @Override
    public int compareTo(Point point) {
        if (point.x != x) {
            return Double.compare(x, point.x);
        } else {
            return Double.compare(y, point.y);
        }
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public void set(double x, double y) {
        if ((this.x == x) && (this.y == y)) {
            return;
        }

        this.x = x;
        this.y = y;
        notifyPointChangedListeners(this);
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Point)) return false;
        final Point other = (Point) o;
        if (!other.canEqual(this)) return false;
        if (Double.compare(this.getX(), other.getX()) != 0) return false;
        if (Double.compare(this.getY(), other.getY()) != 0) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Point;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $x = Double.doubleToLongBits(this.getX());
        result = result * PRIME + (int) ($x >>> 32 ^ $x);
        final long $y = Double.doubleToLongBits(this.getY());
        result = result * PRIME + (int) ($y >>> 32 ^ $y);
        return result;
    }

    public String toString() {
        return "Point(x=" + this.getX() + ", y=" + this.getY() + ")";
    }

    public void addPointChangedListener(PointChangedListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public boolean removePointChangedListener(PointChangedListener listener) {
        return listeners.remove(listener);
    }

    private void notifyPointChangedListeners(Point point) {
        for (PointChangedListener listener : listeners) {
            listener.pointChanged(point);
        }
    }

    public interface PointChangedListener {
        void pointChanged(Point point);
    }
}
