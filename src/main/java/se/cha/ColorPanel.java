package se.cha;

import javax.swing.*;
import java.awt.*;

public class ColorPanel extends JPanel {

    private Color color = null;

    public ColorPanel(int size) {
        setPreferredSize(new Dimension(size, size));
    }

    public void setColor(Color color) {
        this.color = color;
        repaint();
    }

    public void resetColor() {
        setColor(null);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        final Graphics2D g = (Graphics2D) graphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
        final Stroke originalStroke = g.getStroke();

        final int arcWidth = 8;

        if (color != null) {
            g.setColor(color);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arcWidth, arcWidth);
        }

        final Color borderColor = UIManager.getColor("Button.background");
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(borderColor);
        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arcWidth, arcWidth);
        g.setStroke(originalStroke);

        g.dispose();
    }
}
