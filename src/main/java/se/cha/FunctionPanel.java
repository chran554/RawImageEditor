package se.cha;

import lombok.Value;
import se.cha.function.SplineFunction;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FunctionPanel extends JPanel implements MouseListener, MouseMotionListener, SplineFunction.FunctionChangedListener {

    private static final double PIXEL_CLOSE_RADIUS = 7.0;
    private static final int CONTROL_POINT_WIDTH = 4;

    private se.cha.function.Point dragPoint = null;
    private java.awt.Point mousePosition;
    private double highlightPosition;

    private final List<FunctionChangedListener> functionChangedListeners = new ArrayList<>();
    private final List<CurrentValueListener> currentValueListeners = new ArrayList<>();

    private final SplineFunction function;
    private BackgroundImageProducer backgroundImageProducer = null;

    private Range zoomRange = new Range(0.0, 1.0);

    public FunctionPanel(SplineFunction function) {
        super();

        this.function = function;
        function.addFunctionChangedListener(this);

        try {
            final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("cross.png");
            final BufferedImage crossHairCursorImage = ImageIO.read(inputStream);
            final Cursor hairCrossCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                    crossHairCursorImage,
                    new Point(10, 10),
                    "hair cross cursor");
            setCursor(hairCrossCursor);
        } catch (IOException e) {
            setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        }

        addMouseListener(this);
        addMouseMotionListener(this);
        setMinimumSize(new Dimension(100, 75));
    }

    public void addPoint(se.cha.function.Point point) {
        function.addPoint(point);
    }

    public void reset() {
        function.reset();
        zoomRange = null;
        repaint();
    }

    public void removePoint(se.cha.function.Point point) {
        function.removePoint(point);
    }

    public double getValue(double x) {
        if (zoomRange != null) {
            if (x <= zoomRange.getMin()) {
                return function.getFirstPoint().getY();
            }

            if (x >= zoomRange.getMax()) {
                return function.getLastPoint().getY();
            }

            final double zoomLength = zoomRange.getMax() - zoomRange.getMin();
            return function.getValue((x - zoomRange.getMin()) / zoomLength);
        } else {
            return function.getValue(x);
        }
    }

    public void setBackgroundImageProducer(BackgroundImageProducer backgroundImageProducer) {
        this.backgroundImageProducer = backgroundImageProducer;
    }

    @Override
    public void paintComponent(Graphics g) {
        final Graphics2D graphics2D = (Graphics2D) g;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        final int width = graphics2D.getClipBounds().width;
        final int height = graphics2D.getClipBounds().height;

        // Fill canvas
        graphics2D.setColor(Color.GRAY);
        graphics2D.fillRect(0, 0, width - 1, height - 1);
        //graphics2D.setColor(Color.RED);
        //graphics2D.drawRect(0, 0, width - 1, height - 1);

        if (backgroundImageProducer != null) {
            final Image image = backgroundImageProducer.getBackgroundImage(width, height);
            graphics2D.drawImage(image, 0, 0, width, height, null);
        }

        // Draw spline curve
        graphics2D.setColor(Color.LIGHT_GRAY);
        drawCurve(graphics2D, width, height);

        // Draw curve before first control point and after last control point
        drawLineBeforeAndAfterCurve(graphics2D, width, height);

        // Draw 1:1 response line
        graphics2D.setColor(new Color(255, 255, 255, 32));
        drawLine1to1response(graphics2D, width, height);

        // Draw line from first control point to last control point
        graphics2D.setColor(new Color(255, 255, 255, 32));
        drawLineFromStartPointToEndPoint(graphics2D, width, height);

        // Draw highlighted position
        graphics2D.setColor(new Color(255, 128, 128, 128));
        drawHighlightPosition(graphics2D, width, height);
        if ((backgroundImageProducer != null) && (highlightPosition != -Double.MAX_VALUE)) {
            final double x = highlightPosition;
            final Image image = backgroundImageProducer.getForegroundImage(width, height, x);
            graphics2D.drawImage(image, 0, 0, width, height, null);
        }

        // Draw cross-hair
        // drawMousePosition(graphics2D, width, height);

        // Draw spline curve control points
        drawControlPoints(graphics2D, width, height, Color.WHITE, Color.RED);

        graphics2D.dispose();
    }

    private void drawHighlightPosition(Graphics2D graphics2D, int width, int height) {
        if (highlightPosition != -Double.MAX_VALUE) {
            final double x = highlightPosition;
            //final double y = getValue(x);
            final int pixelX = (int) Math.round(width * x);
            //final int pixelY = (int) Math.round(height * (1.0 - y));

            graphics2D.setColor(new Color(128, 196, 196, 128));
            // graphics2D.fillOval(pixelX - (CONTROL_POINT_WIDTH / 2), pixelY - (CONTROL_POINT_WIDTH / 2), CONTROL_POINT_WIDTH, CONTROL_POINT_WIDTH);
            graphics2D.drawLine(pixelX, 0, pixelX, height);
        }
    }

    private void drawMousePosition(Graphics2D graphics2D, int width, int height) {
        if (mousePosition != null) {
            final double x = mousePosition.x / (1.0 * width);
            final double y = getValue(x);
            final int pixelX = mousePosition.x;
            final int pixelY = (int) Math.round(height * (1.0 - y));

            graphics2D.fillOval(pixelX - 2, pixelY - 2, 4, 4);
            graphics2D.drawLine(pixelX, 0, pixelX, height);
        }
    }

    private void drawLine1to1response(Graphics2D graphics2D, int width, int height) {
        graphics2D.drawLine(0, height, width, 0);
    }

    private void drawLineFromStartPointToEndPoint(Graphics2D graphics2D, int width, int height) {
        final se.cha.function.Point startPoint = function.getFirstPoint();
        final se.cha.function.Point endPoint = function.getLastPoint();
        final int startPixelX = (int) Math.round(startPoint.getX() * width);
        final int startPixelY = (int) Math.round(height * (1.0 - startPoint.getY()));
        final int endPixelX = (int) Math.round(endPoint.getX() * width);
        final int endPixelY = (int) Math.round(height * (1.0 - endPoint.getY()));
        graphics2D.drawLine(startPixelX, startPixelY, endPixelX, endPixelY);
    }

    private void drawLineBeforeAndAfterCurve(Graphics2D graphics2D, int width, int height) {
        final int pixelYStartLine = (int) Math.round(height - function.getFirstPoint().getY() * height);
        final int pixelStartXStartLine = 0;
        final int pixelEndXStartLine = (int) Math.round(function.getFirstPoint().getX() * width);
        graphics2D.drawLine(pixelStartXStartLine, pixelYStartLine, pixelEndXStartLine, pixelYStartLine);

        final int pixelYEndLine = (int) Math.round(height - function.getLastPoint().getY() * height);
        final int pixelStartXEndLine = (int) Math.round(function.getLastPoint().getX() * width);
        final int pixelEndXEndLine = width;
        graphics2D.drawLine(pixelStartXEndLine, pixelYEndLine, pixelEndXEndLine, pixelYEndLine);
    }

    private void drawControlPoints(Graphics2D graphics2D, int width, int height, Color color, Color draggedColor) {
        final int halfcontrolPointWidth = CONTROL_POINT_WIDTH / 2;
        for (se.cha.function.Point point : function.getPoints()) {
            graphics2D.setColor(point == dragPoint ? draggedColor : color);
            graphics2D.fillOval((int) Math.round(point.getX() * width) - halfcontrolPointWidth, height - (int) Math.round(point.getY() * height) - halfcontrolPointWidth, CONTROL_POINT_WIDTH, CONTROL_POINT_WIDTH);
        }
    }

    private void drawCurve(Graphics2D graphics2D, int width, int height) {
        final double minX = function.getFirstPoint().getX();
        final double maxX = function.getLastPoint().getX();
        final int amountSteps = (int) ((maxX - minX) * width);
        final double deltaX = (maxX - minX) / (1.0 * amountSteps);

        se.cha.function.Point previousPoint = null;
        for (int i = 0; i < amountSteps; i++) {
            final double x = minX + i * deltaX;
            final double y = function.getValue(x);
            final se.cha.function.Point point = new se.cha.function.Point(x * width, y * height);
            if (previousPoint != null) {
                graphics2D.drawLine(
                        (int) Math.round(previousPoint.getX()),
                        height - (int) Math.round(previousPoint.getY()),
                        (int) Math.round(point.getX()),
                        height - (int) Math.round(point.getY()));
            }
            previousPoint = point;
        }
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        // Add point
        if (mouseEvent.getClickCount() == 1) {
            final se.cha.function.Point point = getPixelClosestValidPoint(mouseEvent.getPoint(), PIXEL_CLOSE_RADIUS);

            if (point == null) {
                addControlPointAtPixelX(mouseEvent.getPoint());
            }
        }

        // Remove point
        if (mouseEvent.getClickCount() == 2) {
            final se.cha.function.Point point = getPixelClosestValidPoint(mouseEvent.getPoint(), PIXEL_CLOSE_RADIUS);
            if (point != null) {
                try {
                    removePoint(point);
                } catch (IllegalArgumentException e) {
                    // Nothing by intention
                }
            }
        }
    }

    private void addControlPointAtPixelX(java.awt.Point point) {
        final double clickPixelX = point.getX();
        final double clickPixelY = point.getY();
        final double splineX = clickPixelX / (1.0 * getWidth());
        final double splineY = function.getValue(splineX);
        final double splinePixelY = getHeight() * (1.0 - splineY);

        final boolean close = distance(clickPixelX, clickPixelY, clickPixelX, splinePixelY) < PIXEL_CLOSE_RADIUS;

        if (close) {
            final se.cha.function.Point newSplinePoint = new se.cha.function.Point(splineX, splineY);
            addPoint(newSplinePoint);
        }
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        final se.cha.function.Point point = getPixelClosestValidPoint(mouseEvent.getPoint(), PIXEL_CLOSE_RADIUS);
        if (point != null) {
            dragPoint = point;
        }
    }

    private double distance(double x1, double y1, double x2, double y2) {
        final double dx = x1 - x2;
        final double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private se.cha.function.Point getPixelClosestValidPoint(java.awt.Point probePoint, double pixelCloseRadius) {
        for (se.cha.function.Point point : function.getPoints()) {
            final double x1 = probePoint.x;
            final double y1 = getHeight() - probePoint.y;
            final double x2 = point.getX() * getWidth();
            final double y2 = point.getY() * getHeight();
            final double pointDistance = distance(x1, y1, x2, y2);

            final boolean close = pointDistance < pixelCloseRadius;

            if (close) {
                return point;
            }
        }

        return null;
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {
        if (dragPoint != null) {
            dragPoint = null;
            repaint();
            notifyFunctionChangedListeners();
        }
    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {
        mousePosition = mouseEvent.getPoint();
        notifyCurrentValueListeners();
        repaint();
    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {
        mousePosition = null;
        notifyCurrentValueListeners();
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
        mousePosition = mouseEvent.getPoint();
        notifyCurrentValueListeners();

        if (dragPoint != null) {
            final double newX = clamp(mouseEvent.getX() / (1.0 * getWidth()), 0.0, 1.0);
            final double newY = clamp(1.0 - (mouseEvent.getY() / (1.0 * getHeight())), 0.0, 1.0);

            final List<se.cha.function.Point> points = function.getPoints();

            boolean validNewPosition;
            final int dragPointIndex = points.indexOf(dragPoint);
            if (dragPointIndex == 0) {
                validNewPosition = newX < points.get(dragPointIndex + 1).getX();
            } else if (dragPointIndex == (points.size() - 1)) {
                validNewPosition = newX > points.get(dragPointIndex - 1).getX();
            } else {
                validNewPosition = (newX < points.get(dragPointIndex + 1).getX()) && (newX > points.get(dragPointIndex - 1).getX());
            }

            if (validNewPosition) {
                dragPoint.set(newX, newY);
            }
        }
        repaint();
    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {
        mousePosition = mouseEvent.getPoint();
        notifyCurrentValueListeners();
        repaint();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(Math.min(value, max), min);
    }

    private void notifyFunctionChangedListeners() {
        for (FunctionChangedListener listener : functionChangedListeners) {
            listener.functionChanged();
        }
    }

    public void addFunctionChangedListener(FunctionChangedListener listener) {
        functionChangedListeners.add(listener);
    }

    public boolean removeFunctionChangedListener(FunctionChangedListener listener) {
        return functionChangedListeners.remove(listener);
    }

    @Override
    public void functionChanged() {
        notifyFunctionChangedListeners();
        repaint();
    }

    public void setZoom() {
        if (zoomRange == null) {
            zoomRange = new Range(function.getFirstPoint().getX(), function.getLastPoint().getX());
        } else {
            final double zoomLength = zoomRange.getMax() - zoomRange.getMin();
            zoomRange = new Range(
                    zoomRange.getMin() + zoomLength * function.getFirstPoint().getX(),
                    zoomRange.getMax() - zoomLength * (1.0 - function.getLastPoint().getX())
            );
        }

//        if ((zoomRange.getMin() == 0.0) || (zoomRange.getMax() == 1.0)) {
//            zoomRange = null;
//        } else {
            final double zoomLength = zoomRange.getMax() - zoomRange.getMin();

            final List<se.cha.function.Point> points = function.getPoints();
            final List<se.cha.function.Point> newPoints = new ArrayList<>(points.size());

            for (final se.cha.function.Point point : points) {
                newPoints.add(new se.cha.function.Point((point.getX() - zoomRange.getMin()) / zoomLength, point.getY()));
            }

            function.replacePoints(newPoints);
//        }

        repaint();
    }

    public void resetZoom() {
        final List<se.cha.function.Point> points = function.getPoints();
        final List<se.cha.function.Point> newPoints = new ArrayList<>(points.size());
        final double zoomLength = zoomRange.getMax() - zoomRange.getMin();

        for (final se.cha.function.Point point : points) {
            newPoints.add(new se.cha.function.Point(zoomRange.getMin() + point.getX() * zoomLength, point.getY()));
        }

        zoomRange = null;
        function.replacePoints(newPoints);

        repaint();
    }

    public boolean isZoomed() {
        return getZoomRange() != null;
    }

    public se.cha.Range getZoomRange() {
        return zoomRange;
    }

    public interface FunctionChangedListener {
        void functionChanged();
    }

    private void notifyCurrentValueListeners() {
        if (mousePosition != null) {
            final double originalNormalizedIntensity = mousePosition.x / (1.0 * (getWidth() - 1));
            final double outputNormalizedIntensity = getValue(originalNormalizedIntensity);
            final CurrentValueEvent currentValueEvent = new CurrentValueEvent(originalNormalizedIntensity, outputNormalizedIntensity);

            notifyCurrentValueListeners(currentValueEvent);
        } else {
            notifyCurrentValueListeners(null);
        }
    }

    private void notifyCurrentValueListeners(CurrentValueEvent event) {
        for (CurrentValueListener listener : currentValueListeners) {
            listener.currentValueChanged(event);
        }
    }

    public void addCurrentValueListener(CurrentValueListener listener) {
        currentValueListeners.add(listener);
    }

    public boolean removeCurrentValueListener(CurrentValueListener listener) {
        return currentValueListeners.remove(listener);
    }

    public interface CurrentValueListener {
        void currentValueChanged(CurrentValueEvent event);
    }

    public void setHighlightPosition(double x) {
        if ((x < 0.0) || (x > 1.0)) {
            throw new IllegalArgumentException("Highlight position most be in range 0.0 to 1.0 (inclusive) but was " + x);
        }

        highlightPosition = x;
        repaint();
    }

    public void removeHighlightPosition() {
        highlightPosition = -Double.MAX_VALUE;
    }

    public interface BackgroundImageProducer {
        Image getBackgroundImage(int width, int height);

        Image getForegroundImage(int width, int height, double x);
    }

    @Value
    public static class CurrentValueEvent {
        double inputValue;
        double outputValue;
    }

}
