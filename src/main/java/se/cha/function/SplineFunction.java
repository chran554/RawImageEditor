package se.cha.function;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This is a spline function curve f(x).
 * With x in the definition range (0.0, 1.0) and the value of f in the range (0.0, 1.0).
 */
public class SplineFunction implements Point.PointChangedListener {

    private final List<Point> points = new ArrayList<>();
    private final List<FunctionChangedListener> listeners = new ArrayList<>();

    private PolynomialSplineFunction spline;
    private boolean splineUpToDate = false;

    public SplineFunction() {
        // 3 default control points
        points.add(new Point(0.00, 0.00, this));
        points.add(new Point(0.50, 0.50, this));
        points.add(new Point(1.00, 1.00, this));
        Collections.sort(this.points);
        splineUpToDate = false;
    }

    public void addPoint(Point point) {
        points.add(point);
        point.addPointChangedListener(this);
        Collections.sort(this.points);
        splineUpToDate = false;
        notifyFunctionChangedListeners();
    }

    public void removePoint(Point point) {
        if (points.size() > 3) {
            points.remove(point);
            point.removePointChangedListener(this);
            splineUpToDate = false;
            notifyFunctionChangedListeners();
        } else {
            throw new IllegalArgumentException("You need to keep at least 3 control points.");
        }
    }

    public void addPoints(List<Point> points) {
        this.points.addAll(points);
        points.forEach(p -> p.addPointChangedListener(this));
        Collections.sort(this.points);
        splineUpToDate = false;
        notifyFunctionChangedListeners();
    }

    public Point getFirstPoint() {
        return points.get(0);
    }

    public Point getLastPoint() {
        return points.get(points.size() - 1);
    }

    public List<Point> getPoints() {
        return Collections.unmodifiableList(points);
    }

    public void replacePoints(List<Point> points) {
        if (points.size() < 3) {
            throw new IllegalArgumentException("You need to supply at least 3 control points.");
        }
        this.points.forEach(p -> p.removePointChangedListener(this));
        this.points.clear();
        splineUpToDate = false;
        this.points.addAll(points);
        this.points.forEach(p -> p.addPointChangedListener(this));
        Collections.sort(this.points);
        splineUpToDate = false;
        notifyFunctionChangedListeners();
    }

    public double getValue(double x) {
        x = clamp(x, points.get(0).getX(), points.get(points.size() - 1).getX());
        return getSpline().value(x);
    }

    private PolynomialSplineFunction getSpline() {
        if (!splineUpToDate) {
            final double[] xValues = new double[points.size()];
            final double[] yValues = new double[points.size()];
            for (int i = 0; i < points.size(); i++) {
                xValues[i] = points.get(i).getX();
                yValues[i] = points.get(i).getY();
            }

            final SplineInterpolator interpolator = new SplineInterpolator();
            spline = interpolator.interpolate(xValues, yValues);
            splineUpToDate = true;
        }

        return spline;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(Math.min(value, max), min);
    }

    @Override
    public void pointChanged(Point point) {
        splineUpToDate = false;
        notifyFunctionChangedListeners();
    }

    private void notifyFunctionChangedListeners() {
        for (FunctionChangedListener listener : listeners) {
            listener.functionChanged();
        }
    }

    public void addFunctionChangedListener(FunctionChangedListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public boolean removeFunctionChangedListener(FunctionChangedListener listener) {
        return listeners.remove(listener);
    }

    public interface FunctionChangedListener {
        void functionChanged();
    }
}
