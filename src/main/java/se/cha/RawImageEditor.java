package se.cha;


import se.cha.function.SplineFunction;

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

                final FunctionPanel functionPanel = new FunctionPanel(new SplineFunction());

                final RawFloatImage rawFloatImage = new RawFloatImage();
                try {
                    rawFloatImage.loadFile("images/cornellbox2.praw");
                    imagePanel.setImage(rawFloatImage.getImage(functionPanel));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                functionPanel.setPreferredSize(new Dimension(400, 300));
                functionPanel.addFunctionChangedListener(() -> histogramFunctionChanged(imagePanel, rawFloatImage, functionPanel));

                imagePanel.addMousePositionListener(point -> {
                    if (point == null) {
                        functionPanel.removeHighlightPosition();
                    } else {
                        final int pixelX = (int) Math.round(point.getX() * rawFloatImage.getWidth());
                        final int pixelY = (int) Math.round(point.getY() * rawFloatImage.getHeight());

                        final double intensityValue = rawFloatImage.getIntensityValue(pixelX, pixelY);
                        final double intensityMaxValue = rawFloatImage.getIntensityMaxValue();

                        functionPanel.setHighlightPosition(intensityValue / intensityMaxValue);
                    }
                });

                final ScrollPane imageScrollPanel = new ScrollPane();
                imageScrollPanel.add(imagePanel);

                final JPanel histogramBorder = new JPanel();
                histogramBorder.setBorder(new EmptyBorder(10, 10, 10, 10));
                histogramBorder.add(functionPanel);

                final RawImageEditor frame = new RawImageEditor();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(imagePanel, BorderLayout.CENTER);
                frame.add(histogramBorder, BorderLayout.EAST);
                frame.pack();
                frame.setVisible(true);
            }

            public void histogramFunctionChanged(ImagePanel imagePanel, RawFloatImage rawFloatImage, FunctionPanel functionPanel) {
                imagePanel.setImage(rawFloatImage.getImage(functionPanel));

                final Rectangle splineCanvasBounds = functionPanel.getBounds();
                final Image histogramBackgroundImage = createHistogramImage(rawFloatImage, splineCanvasBounds.width, splineCanvasBounds.height, functionPanel);
                functionPanel.setBackgroundImage(histogramBackgroundImage);
            }

            public Image createHistogramImage(RawFloatImage rawFloatImage, int width, int height, FunctionPanel functionPanel) {
                final BufferedImage histogramImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                final Graphics2D graphics = (Graphics2D) histogramImage.getGraphics();
                graphics.setColor(new Color(255, 255, 255, 0));
                graphics.fillRect(0, 0, width - 1, height - 1);

                final Histogram histogram = rawFloatImage.getIntensityHistogram(width);
                final Histogram histogramOutput = rawFloatImage.getIntensityHistogram(width, functionPanel);

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