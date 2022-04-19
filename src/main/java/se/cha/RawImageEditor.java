package se.cha;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class RawImageEditor extends JFrame {

    public static void main(String[] args) {
        System.out.println("Spline test running...");

        // Run the GUI codes on the Event-Dispatching thread for thread safety
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final ImagePanel imagePanel = new ImagePanel();

                final SplineCanvas splineCanvas = new SplineCanvas();

                final RawFloatImage rawFloatImage = new RawFloatImage();
                try {
                    rawFloatImage.loadFile("images/cornellbox2.praw");
                    imagePanel.setImage(rawFloatImage.getImage(splineCanvas));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                splineCanvas.setPreferredSize(new Dimension(400, 300));
                splineCanvas.addFunctionChangedListener(() -> histogramFunctionChanged(imagePanel, rawFloatImage, splineCanvas));

                imagePanel.addMousePositionListener(point -> {
                    if (point == null) {
                        splineCanvas.removeHighlightPosition();
                    } else {
                        final int pixelX = (int) Math.round(point.x * rawFloatImage.getWidth());
                        final int pixelY = (int) Math.round(point.y * rawFloatImage.getHeight());

                        final double intensityValue = rawFloatImage.getIntensityValue(pixelX, pixelY);
                        final double intensityMaxValue = rawFloatImage.getIntensityMaxValue();

                        splineCanvas.setHighlightPosition(intensityValue / intensityMaxValue);
                    }
                });

                final ScrollPane imageScrollPanel = new ScrollPane();
                imageScrollPanel.add(imagePanel);

                final JPanel histogramBorder = new JPanel();
                histogramBorder.setBorder(new EmptyBorder(10, 10, 10, 10));
                histogramBorder.add(splineCanvas);

                final RawImageEditor frame = new RawImageEditor();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(imagePanel, BorderLayout.CENTER);
                frame.add(histogramBorder, BorderLayout.EAST);
                frame.pack();
                frame.setVisible(true);
            }

            public void histogramFunctionChanged(ImagePanel imagePanel, RawFloatImage rawFloatImage, SplineCanvas splineCanvas) {
                imagePanel.setImage(rawFloatImage.getImage(splineCanvas));

                final Rectangle splineCanvasBounds = splineCanvas.getBounds();
                final Image histogramBackgroundImage = createHistogramImage(rawFloatImage, splineCanvasBounds.width, splineCanvasBounds.height, splineCanvas);
                splineCanvas.setBackgroundImage(histogramBackgroundImage);
            }

            public Image createHistogramImage(RawFloatImage rawFloatImage, int width, int height, SplineCanvas splineCanvas) {
                final BufferedImage histogramImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                final Graphics2D graphics = (Graphics2D) histogramImage.getGraphics();
                graphics.setColor(new Color(255, 255, 255, 0));
                graphics.fillRect(0, 0, width - 1, height - 1);

                final Histogram histogram = rawFloatImage.getIntensityHistogram(width);
                final Histogram histogramOutput = rawFloatImage.getIntensityHistogram(width, splineCanvas);

                graphics.setColor(new Color(255, 255, 255, 32));
                graphics.setColor(new Color(255, 128, 128, 64));
                for (int pixelX = 0; pixelX < width; pixelX++) {
                    final double histogramValue = Math.pow(histogramOutput.getValueRGB(pixelX), 0.25);
                    graphics.drawLine(pixelX, height, pixelX, height - (int) (histogramValue * height));
                }

                graphics.setColor(new Color(0, 0, 0, 32));
                graphics.setColor(new Color(0, 0, 128, 64));
                for (int pixelX = 0; pixelX < width; pixelX++) {
                    final double histogramValue = Math.pow(histogram.getValueRGB(pixelX), 0.25);
                    graphics.drawLine(pixelX, height, pixelX, height - (int) (histogramValue * height));
                }

                graphics.dispose();

                return histogramImage;
            }
        });
    }

}