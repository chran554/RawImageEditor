package se.cha;

import se.cha.function.SplineFunction;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.stream.IntStream;

import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.NORTHWEST;

public class RawImageEditor extends JFrame {

    public static void main(String[] args) {
        System.out.println("Spline test running...");

        // Run the GUI codes on the Event-Dispatching thread for thread safety
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final ImageCache originalHistogramImageCache = new ImageCache();
                final ImageCache outputHistogramImageCache = new ImageCache();
                final ImageCache combinedHistogramImageCache = new ImageCache();

                final RawImageEditor frame = new RawImageEditor();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                final ImagePanel imagePanel = new ImagePanel();

                final FunctionPanel functionPanel = new FunctionPanel(new SplineFunction());

                final RawFloatImage rawFloatImage = new RawFloatImage();

                final JCheckBox histogramCheckBox = new JCheckBox(new AbstractAction("Apply intensity response curve") {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        final boolean selected = ((JCheckBox) event.getSource()).isSelected();
                        histogramFunctionChanged(imagePanel, rawFloatImage, functionPanel, originalHistogramImageCache, outputHistogramImageCache, combinedHistogramImageCache, selected);
                    }
                });

                functionPanel.setPreferredSize(new Dimension(400, 300));
                functionPanel.addFunctionChangedListener(() -> histogramFunctionChanged(imagePanel, rawFloatImage, functionPanel, originalHistogramImageCache, outputHistogramImageCache, combinedHistogramImageCache, histogramCheckBox.isSelected()));
                functionPanel.setBackgroundImageProducer((width, height) -> createHistogramImage(originalHistogramImageCache, outputHistogramImageCache, combinedHistogramImageCache, rawFloatImage, width, height, functionPanel));

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

                final JPanel imageBorder = new JPanel(new BorderLayout());
                imageBorder.setBorder(new EmptyBorder(10, 10, 10, 10));
                imageBorder.add(imagePanel, BorderLayout.CENTER);

                final JPanel rightColumn = new JPanel(new GridBagLayout());
                rightColumn.setBorder(new EmptyBorder(10, 10, 10, 10));
                final Insets noInsets = new Insets(0, 0, 0, 0);
                final JButton loadButton = new JButton(new AbstractAction("Load raw image...") {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        try {
                            final JFileChooser fileChooser = new JFileChooser();
                            fileChooser.setDialogTitle("Choose raw image file");
                            fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
                            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                            fileChooser.setMultiSelectionEnabled(false);
                            fileChooser.setCurrentDirectory(new File("."));
                            final int result = fileChooser.showDialog(imagePanel, "Load");

                            if (result == JFileChooser.APPROVE_OPTION) {
                                rawFloatImage.loadFile(fileChooser.getSelectedFile());
                                imagePanel.setImage(rawFloatImage.getImage(functionPanel));
                            }

                            outputHistogramImageCache.invalidate();
                            originalHistogramImageCache.invalidate();
                            combinedHistogramImageCache.invalidate();

                            frame.pack();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                final JButton saveButton = new JButton(new AbstractAction("Save png image...") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            final JFileChooser fileChooser = new JFileChooser();
                            fileChooser.setDialogTitle("Save image as png file");
                            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
                            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                            fileChooser.setMultiSelectionEnabled(false);
                            fileChooser.setCurrentDirectory(new File("."));
                            final int result = fileChooser.showDialog(imagePanel, "Save");

                            if (result == JFileChooser.APPROVE_OPTION) {
                                final BufferedImage image = imagePanel.getImage();
                                ImageIO.write(image, "png", fileChooser.getSelectedFile());
                            }
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }

                    }
                });
                histogramCheckBox.setSelected(true);

                final JLabel inputLabel = new JLabel("Input: ");
                inputLabel.setForeground(Color.CYAN.darker().darker());
                final JLabel outputLabel = new JLabel("Output: ");
                outputLabel.setForeground(Color.RED.darker());
                final JPanel informationPanel = new JPanel(new GridBagLayout());
                informationPanel.setBorder(new TitledBorder("Intensity information"));
                informationPanel.add(inputLabel, new GridBagConstraints(0,0,1,1,1,0, NORTHWEST, HORIZONTAL, noInsets, 0,5));
                informationPanel.add(outputLabel, new GridBagConstraints(0,1,1,1,1,0, NORTHWEST, HORIZONTAL, noInsets, 0,5));

                int rowIndex = 0;
                rightColumn.add(functionPanel, new GridBagConstraints(0,rowIndex++, 1,1, 1, 0, NORTHWEST, HORIZONTAL, noInsets, 0, 0));
                rightColumn.add(histogramCheckBox, new GridBagConstraints(0,rowIndex++, 1,1, 1, 0, NORTHWEST, HORIZONTAL, noInsets, 0, 20));
                rightColumn.add(informationPanel, new GridBagConstraints(0,rowIndex++, 1,1, 1, 0, NORTHWEST, HORIZONTAL, noInsets, 0, 20));
                rightColumn.add(loadButton, new GridBagConstraints(0,rowIndex++, 1,1, 1, 0, NORTHWEST, HORIZONTAL, noInsets, 0, 0));
                rightColumn.add(saveButton, new GridBagConstraints(0,rowIndex++, 1,1, 1, 0, NORTHWEST, HORIZONTAL, noInsets, 0, 0));
                rightColumn.add(new JPanel(), new GridBagConstraints(0,rowIndex++, 1,1, 1, 1, NORTHWEST, HORIZONTAL, noInsets, 0, 0));

                frame.add(imageBorder, BorderLayout.CENTER);
                frame.add(rightColumn, BorderLayout.EAST);
                frame.pack();
                frame.setVisible(true);
            }

            public void histogramFunctionChanged(ImagePanel imagePanel, RawFloatImage rawFloatImage, FunctionPanel functionPanel,
                                                 ImageCache originalHistogramImageCache, ImageCache outputHistogramImageCache, ImageCache combinedHistogramImageCache, boolean selected) {
                originalHistogramImageCache.invalidate();
                outputHistogramImageCache.invalidate();
                combinedHistogramImageCache.invalidate();

                if (selected) {
                    imagePanel.setImage(rawFloatImage.getImage(functionPanel));
                } else {
                    final FunctionPanel noChangeFunctionPanel = new FunctionPanel(new SplineFunction());
                    imagePanel.setImage(rawFloatImage.getImage(noChangeFunctionPanel));
                }
            }

            public Image createHistogramImage(ImageCache originalHistogramImageCache, ImageCache outputHistogramImageCache, ImageCache combinedHistogramImageCache, RawFloatImage rawFloatImage, int width, int height, FunctionPanel functionPanel) {
                if (!originalHistogramImageCache.valid() || !originalHistogramImageCache.boundsMatch(width, height)) {
                    final Graphics2D g = originalHistogramImageCache.createImage(width, height);
                    final Histogram histogram = rawFloatImage.getIntensityHistogram(width);

                    final Color fillColor = new Color(128, 255, 255, 16);
                    final Color lineColor = new Color(128, 255, 255, 64);
                    drawHistogramImage(histogram, width, height, g, fillColor, lineColor);
                    g.dispose();
                }

                if (!outputHistogramImageCache.valid() || !outputHistogramImageCache.boundsMatch(width, height)) {
                    final Graphics2D g = outputHistogramImageCache.createImage(width, height);
                    final Histogram histogram = rawFloatImage.getIntensityHistogram(width, functionPanel);

                    final Color fillColor = new Color(255, 128, 128, 16);
                    final Color lineColor = new Color(255, 128, 128, 64);
                    drawHistogramImage(histogram, width, height, g, fillColor, lineColor);
                    g.dispose();
                }

                if (!combinedHistogramImageCache.valid() || !combinedHistogramImageCache.boundsMatch(width, height)) {
                    final Graphics2D g = combinedHistogramImageCache.createImage(width, height);
                    g.drawImage(outputHistogramImageCache.getCachedImage(), 0,0, null);
                    g.drawImage(originalHistogramImageCache.getCachedImage(), 0,0, null);
                    g.dispose();
                }

                return combinedHistogramImageCache.getCachedImage();
            }

            private void drawHistogramImage(Histogram histogram, int width, int height, Graphics2D g, Color fillColor, Color lineColor) {
                final double[] histogramValue = new double[width];
                IntStream.range(0, width).forEach(pixelX  -> histogramValue[pixelX] = Math.pow(histogram.getValueRGB(pixelX), 0.25));

                g.setColor(fillColor);
                for (int pixelX = 0; pixelX < width; pixelX++) {
                    g.drawLine(pixelX, height, pixelX, height - (int) (histogramValue[pixelX] * height));
                }
                g.setColor(lineColor);
                for (int pixelX = 1; pixelX < width; pixelX++) {
                    g.drawLine(pixelX -1 , height - (int) (histogramValue[pixelX-1] * height), pixelX, height - (int) (histogramValue[pixelX] * height));
                }
            }
        });
    }

}