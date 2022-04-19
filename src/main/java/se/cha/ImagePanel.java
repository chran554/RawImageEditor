package se.cha;

import se.cha.function.Point;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

public class ImagePanel extends JPanel implements MouseMotionListener, MouseListener {

    private final List<MousePositionListener> mousePositionListeners = new ArrayList<>();
    private Image image = null;

    private int scaledImageWidth;
    private int scaledImageHeight;

    public ImagePanel() {
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        final Graphics2D graphics2D = (Graphics2D) g;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        final int width = graphics2D.getClipBounds().width;
        final int height = graphics2D.getClipBounds().height;

        graphics2D.setColor(new Color(0, 0, 0, 0));
        graphics2D.fillRect(0, 0, width - 1, height - 1);

        if (image != null) {
            final int imageWidth = image.getWidth(null);
            final int imageHeight = image.getHeight(null);

            double scaleFactor = 1.0;
            if ((imageWidth > width) || (imageHeight > height)) {
                scaleFactor = Math.max(imageWidth / (1.0 * width), imageHeight / (1.0 * height));
            }

            scaledImageWidth = (int) (imageWidth / scaleFactor);
            scaledImageHeight = (int) (imageHeight / scaleFactor);

            // final int x1 = Math.max((width - newImageWidth) / 2, 0);
            // final int y1 = Math.max((height - newImageHeight) / 2, 0);
            graphics2D.drawImage(image, 0, 0, scaledImageWidth, scaledImageHeight, null);
        }

        graphics2D.dispose();
    }

    public void setImage(Image image) {
        this.image = image;
        setPreferredSize(new Dimension(image.getWidth(null), image.getHeight(null)));
        repaint();
    }

    public interface MousePositionListener {
        void mousePositionChanged(Point point);
    }

    private void notifyMousePositionListeners(Point point) {
        for (final MousePositionListener mousePositionListener : mousePositionListeners) {
            mousePositionListener.mousePositionChanged(point);
        }
    }

    public void addMousePositionListener(MousePositionListener listener) {
        mousePositionListeners.add(listener);
    }

    public boolean removeMousePositionListener(MousePositionListener listener) {
        return mousePositionListeners.remove(listener);
    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
        final Point mousePositionPoint = getMousePositionPoint(mouseEvent);
        notifyMousePositionListeners(mousePositionPoint);
    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {
        final Point mousePositionPoint = getMousePositionPoint(mouseEvent);
        notifyMousePositionListeners(mousePositionPoint);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Nothing by intention...
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // Nothing by intention...
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Nothing by intention...
    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {
        final Point mousePositionPoint = getMousePositionPoint(mouseEvent);
        notifyMousePositionListeners(mousePositionPoint);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        notifyMousePositionListeners(null);
    }

    private Point getMousePositionPoint(MouseEvent mouseEvent) {
        final int width = scaledImageWidth;
        final int height = scaledImageHeight;

        final java.awt.Point pixelPoint = mouseEvent.getPoint();

        if (pixelPoint.y < height) {
            final double normalizedX = pixelPoint.x / (1.0 * width);
            final double normalizedY = pixelPoint.y / (1.0 * height);
            return new Point(normalizedX, normalizedY);
        } else {
            return null;
        }

    }
}
