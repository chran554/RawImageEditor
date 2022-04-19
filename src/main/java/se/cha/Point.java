package se.cha;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Point implements Comparable<Point> {

    double x;
    double y;

    public static boolean close(Point point1, Point point2, double radius) {
        final double dx = point1.x - point2.x;
        final double dy = point1.y - point2.y;
        return Math.sqrt(dx * dx + dy * dy) < radius;
    }

    @Override
    public int compareTo(Point point) {
        if (point.x != x) {
            return Double.compare(x, point.x);
        } else {
            return Double.compare(y, point.y);
        }
    }
}
