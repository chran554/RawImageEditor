package se.cha;

import lombok.Data;

import java.awt.*;
import java.awt.image.BufferedImage;

@Data
public class ImageCache {

    private BufferedImage cachedImage;
    private int width;
    private int height;

    public ImageCache() {
        invalidate();
    }

    public Graphics2D createImage(int width, int height) {
        cachedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.width = width;
        this.height = height;

        final Graphics2D graphics = (Graphics2D)cachedImage.getGraphics();
        graphics.setColor(new Color(0,0,0,0));
        graphics.fillRect(0,0,width, height);
        return graphics;
    }

    public BufferedImage getCachedImage() {
        return cachedImage;
    }

    public void invalidate() {
        cachedImage = null;
        width = -1;
        height = -1;
    }

    public boolean valid() {
        return cachedImage != null;
    }

    public boolean boundsMatch(int width, int height) {
        return (this.width == width) && (this.height == height);
    }

    public boolean hasDimension(int width, int height) {
        return valid() && (cachedImage.getWidth() == width) && (cachedImage.getHeight() == height);
     }

    public Graphics2D getImageGraphics() {
        return (Graphics2D) getCachedImage().getGraphics();
    }
}
