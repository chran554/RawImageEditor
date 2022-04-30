package se.cha;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class ImagePanel extends JPanel implements MouseMotionListener, MouseListener {

    private final List<MousePositionListener> mousePositionListeners = new ArrayList<>();
    private BufferedImage image = null;

    private int scaledImageWidth;
    private int scaledImageHeight;

    public ImagePanel() {
        addMouseListener(this);
        addMouseMotionListener(this);

        setPreferredSize(new Dimension(400, 300));
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

    public void setImage(BufferedImage image) {
        this.image = image;
        if (image != null) {
            setPreferredSize(new Dimension(image.getWidth(null), image.getHeight(null)));
        } else {
            setPreferredSize(new Dimension(400, 300));
        }
        repaint();
    }

    public BufferedImage getImage() {
        return this.image;
    }

    public interface MousePositionListener {
        void mousePositionChanged(java.awt.Point point);
    }

    private void notifyMousePositionListeners(java.awt.Point point) {
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
        final java.awt.Point mousePositionPoint = getMousePositionPoint(mouseEvent);
        notifyMousePositionListeners(mousePositionPoint);
    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {
        final java.awt.Point mousePositionPoint = getMousePositionPoint(mouseEvent);
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
        final java.awt.Point mousePositionPoint = getMousePositionPoint(mouseEvent);
        notifyMousePositionListeners(mousePositionPoint);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        notifyMousePositionListeners(null);
    }

    private java.awt.Point getMousePositionPoint(MouseEvent mouseEvent) {
        if (image == null) {
            return null;
        }

        final int imageWidth = image.getWidth(null);
        final int imageHeight = image.getHeight(null);

        final int widthScaled = scaledImageWidth;
        final int heightScaled = scaledImageHeight;

        final java.awt.Point pixelPoint = mouseEvent.getPoint();

        if ((pixelPoint.y < heightScaled) && (pixelPoint.x < widthScaled)) {
            final double normalizedX = pixelPoint.x / (1.0 * (widthScaled - 1));
            final double normalizedY = pixelPoint.y / (1.0 * (heightScaled - 1));
            return new java.awt.Point((int) Math.round(normalizedX * imageWidth), (int) Math.round(normalizedY * imageHeight));
        } else {
            return null;
        }
    }
}
