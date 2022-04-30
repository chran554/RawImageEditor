package se.cha;

import com.formdev.flatlaf.FlatDarkLaf;
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
import java.text.NumberFormat;
import java.util.stream.IntStream;

import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.NONE;
import static java.awt.GridBagConstraints.NORTHEAST;
import static java.awt.GridBagConstraints.NORTHWEST;

public class RawImageEditor extends JFrame {

    public static void main(String[] args) {
        System.out.println("Running raw image editor...");

        FlatDarkLaf.setup();

        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }

        // Run the GUI codes on the Event-Dispatching thread for thread safety
        SwingUtilities.invokeLater(new Runnable() {
            private final Color originalColor = new Color(128, 255, 255, 128);
            private final Color outputColor = new Color(255, 128, 128, 196);

            private Double highlightIntensity = null;
            private double histogramGammaEnhancement = 1.0;
            private final ComboBoxDoubleItem histogramOptionLinear = new ComboBoxDoubleItem("Linear (γ=1.0)", 1.0);
            private final ComboBoxDoubleItem histogramOption075 = new ComboBoxDoubleItem("Enhance low values (γ=0.75)", 0.75);
            private final ComboBoxDoubleItem histogramOption025 = new ComboBoxDoubleItem("Enhance low values (γ=0.25)", 0.25);
            private final ComboBoxDoubleItem histogramOption012 = new ComboBoxDoubleItem("Enhance low values (γ=0.12)", 0.12);

            final ImagePanel imagePanel = new ImagePanel();
            final FunctionPanel functionPanel = new FunctionPanel(new SplineFunction());
            final RawFloatImage rawFloatImage = new RawFloatImage();
            final JLabel inputValueLabel = new JLabel("", SwingConstants.RIGHT);
            final JLabel outputValueLabel = new JLabel("", SwingConstants.RIGHT);

            @Override
            public void run() {
                inputValueLabel.setForeground(originalColor);
                outputValueLabel.setForeground(outputColor);

                final ImageCache originalHistogramImageCache = new ImageCache();
                final ImageCache outputHistogramImageCache = new ImageCache();
                final ImageCache combinedHistogramImageCache = new ImageCache();
                final ImageCache highLightImageCache = new ImageCache();

                final RawImageEditor frame = new RawImageEditor();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                final JComboBox<ComboBoxDoubleItem> histogramGammaComboBox = new JComboBox<>(new ComboBoxDoubleItem[]{histogramOptionLinear, histogramOption075, histogramOption025, histogramOption012});
                histogramGammaComboBox.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final ComboBoxDoubleItem selectedItem = (ComboBoxDoubleItem) ((JComboBox<ComboBoxDoubleItem>) e.getSource()).getSelectedItem();
                        histogramGammaEnhancement = selectedItem.getValue();

                        outputHistogramImageCache.invalidate();
                        originalHistogramImageCache.invalidate();
                        combinedHistogramImageCache.invalidate();
                        functionPanel.repaint();
                    }
                });
                histogramGammaComboBox.setSelectedItem(histogramOption025);

                final JCheckBox histogramCheckBox = new JCheckBox(new AbstractAction("Apply intensity response curve") {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        final boolean selected = ((JCheckBox) event.getSource()).isSelected();
                        histogramFunctionChanged(imagePanel, rawFloatImage, functionPanel, originalHistogramImageCache, outputHistogramImageCache, combinedHistogramImageCache, selected);
                    }
                });

                functionPanel.addFunctionChangedListener(() -> histogramFunctionChanged(imagePanel, rawFloatImage, functionPanel, originalHistogramImageCache, outputHistogramImageCache, combinedHistogramImageCache, histogramCheckBox.isSelected()));
                functionPanel.addCurrentValueListener((currentValueEvent) -> {
                    if (currentValueEvent != null && rawFloatImage.isValid()) {
                        highlightIntensity = currentValueEvent.getInputValue();
                    } else {
                        highlightIntensity = null;
                    }
                    updateIntensityInformationLabels();
                    highLightImageCache.invalidate();
                    functionPanel.repaint();
                });

                functionPanel.setBackgroundImageProducer(new FunctionPanel.BackgroundImageProducer() {
                    @Override
                    public Image getBackgroundImage(int width, int height) {
                        return createHistogramImage(originalHistogramImageCache, outputHistogramImageCache, combinedHistogramImageCache, rawFloatImage, width, height, functionPanel);
                    }

                    @Override
                    public Image getForegroundImage(int width, int height, double x) {
                        return createHighLightImage(highLightImageCache, width, height, functionPanel);
                    }
                });

                imagePanel.addMousePositionListener(point -> {
                    if (point != null) {
                        final double intensityValue = rawFloatImage.getIntensityValue(point.x, point.y);
                        highlightIntensity = intensityValue / rawFloatImage.getIntensityMaxValue();
                    } else {
                        highlightIntensity = null;
                    }

                    updateIntensityInformationLabels();
                    highLightImageCache.invalidate();
                    functionPanel.repaint();
                });

                final JPanel imageBorder = new JPanel(new BorderLayout());
                imageBorder.setBorder(new EmptyBorder(10, 10, 10, 10));
                imageBorder.add(imagePanel, BorderLayout.CENTER);

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
                                final File selectedFile = fileChooser.getSelectedFile();
                                rawFloatImage.loadFile(selectedFile);
                                imagePanel.setImage(rawFloatImage.getImage(functionPanel));

                                frame.setTitle(selectedFile.getName());
                            }

                            functionPanel.reset();
                            outputHistogramImageCache.invalidate();
                            originalHistogramImageCache.invalidate();
                            combinedHistogramImageCache.invalidate();

                            // frame.pack();
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

                final JLabel inputCaption = new JLabel("Input:");
                final JLabel outputCaption = new JLabel("Output:");
                inputCaption.setForeground(originalColor);
                outputCaption.setForeground(outputColor);

                final JPanel informationPanel = new JPanel(new GridBagLayout());
                informationPanel.setBorder(new TitledBorder("Intensity"));
                informationPanel.add(inputCaption, new GridBagConstraints(0, 0, 1, 1, 0, 0, NORTHWEST, HORIZONTAL, noInsets, 0, 0));
                informationPanel.add(outputCaption, new GridBagConstraints(0, 1, 1, 1, 0, 0, NORTHWEST, HORIZONTAL, noInsets, 0, 0));
                informationPanel.add(inputValueLabel, new GridBagConstraints(1, 0, 1, 1, 1, 0, NORTHEAST, HORIZONTAL, noInsets, 0, 0));
                informationPanel.add(outputValueLabel, new GridBagConstraints(1, 1, 1, 1, 1, 0, NORTHEAST, HORIZONTAL, noInsets, 0, 0));

                final JPanel bottomPanel = new JPanel(new GridBagLayout());
                bottomPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

                int columnIndex = 0;
                bottomPanel.add(histogramCheckBox, new GridBagConstraints(1, columnIndex++, 1, 1, 0, 0, NORTHWEST, HORIZONTAL, noInsets, 0, 0));
                bottomPanel.add(new JLabel("Histogram value scale:"), new GridBagConstraints(1, columnIndex++, 1, 1, 0, 0, NORTHWEST, NONE, new Insets(8, 4, 0, 4), 0, 0));
                bottomPanel.add(histogramGammaComboBox, new GridBagConstraints(1, columnIndex++, 1, 1, 0, 0, NORTHWEST, NONE, new Insets(0, 4, 0, 4), 0, 0));
                bottomPanel.add(informationPanel, new GridBagConstraints(1, columnIndex++, 1, 1, 0, 0, NORTHWEST, HORIZONTAL, new Insets(8, 4, 0, 4), 0, 0));
                bottomPanel.add(new JPanel(), new GridBagConstraints(1, columnIndex++, 1, 1, 0, 1, NORTHWEST, BOTH, noInsets, 0, 0));
                bottomPanel.add(loadButton, new GridBagConstraints(1, columnIndex++, 1, 1, 0, 0, NORTHWEST, HORIZONTAL, new Insets(4, 4, 0, 4), 0, 0));
                bottomPanel.add(saveButton, new GridBagConstraints(1, columnIndex++, 1, 1, 0, 0, NORTHWEST, HORIZONTAL, new Insets(4, 4, 0, 4), 0, 0));

                bottomPanel.add(functionPanel, new GridBagConstraints(0, 0, 1, columnIndex, 1, 0, NORTHWEST, BOTH, noInsets, 0, 0));

                final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, imageBorder, bottomPanel);
                splitPane.setDividerLocation(0.8);
                splitPane.setResizeWeight(1);

                frame.add(splitPane, BorderLayout.CENTER);
                frame.pack();

                final GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
                final Rectangle maximumWindowBounds = env.getMaximumWindowBounds();
                final Dimension preferredSize = frame.getPreferredSize();

                final int width = Math.min(Math.max(preferredSize.width, 800), (int) (maximumWindowBounds.width * 0.8));
                final int height = Math.min(Math.max(preferredSize.height, 600), (int) (maximumWindowBounds.height * 0.8));
                frame.setSize(new Dimension(width, height));

                frame.setVisible(true);
            }

            private void updateIntensityInformationLabels() {
                if (highlightIntensity != null) {
                    final double inputValue = highlightIntensity;
                    final double outputValue = functionPanel.getValue(highlightIntensity);

                    final NumberFormat numberFormat = NumberFormat.getInstance();
                    numberFormat.setMinimumIntegerDigits(1);
                    numberFormat.setMinimumFractionDigits(4);
                    numberFormat.setMaximumFractionDigits(4);

                    inputValueLabel.setText(numberFormat.format(inputValue * 100.0) + "%");
                    outputValueLabel.setText(numberFormat.format(outputValue * 100.0) + "%");
                } else {
                    inputValueLabel.setText("");
                    outputValueLabel.setText("");
                }
            }

            public void histogramFunctionChanged(ImagePanel imagePanel, RawFloatImage rawFloatImage, FunctionPanel functionPanel,
                                                 ImageCache originalHistogramImageCache, ImageCache outputHistogramImageCache, ImageCache combinedHistogramImageCache, boolean histogramEnabled) {
                originalHistogramImageCache.invalidate();
                outputHistogramImageCache.invalidate();
                combinedHistogramImageCache.invalidate();

                if (histogramEnabled) {
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
                    g.drawImage(outputHistogramImageCache.getCachedImage(), 0, 0, null);
                    g.drawImage(originalHistogramImageCache.getCachedImage(), 0, 0, null);
                    g.dispose();
                }

                return combinedHistogramImageCache.getCachedImage();
            }

            public Image createHighLightImage(ImageCache highLightImageCache, int width, int height, FunctionPanel functionPanel) {
                if (!highLightImageCache.valid()) {

                    if (highlightIntensity != null) {
                        final double normalizedIntensityValue = highlightIntensity;
                        final double normalizedOutputIntensityValue = functionPanel.getValue(normalizedIntensityValue);

                        final Graphics2D g;
                        if (highLightImageCache.valid() && highLightImageCache.hasDimension(width, height)) {
                            g = highLightImageCache.getImageGraphics();
                            g.setColor(new Color(0, 0, 0, 0));
                            g.fillRect(0,0, width, height);
                        } else {
                            g = highLightImageCache.createImage(width, height);
                        }

                        final int pixelX = (int) Math.round(width * normalizedIntensityValue);
                        final int pixelXOutput = (int) Math.round(width * normalizedOutputIntensityValue);

                        g.setColor(originalColor);
                        g.drawLine(pixelX, 0, pixelX, height);

                        g.setColor(outputColor);
                        g.drawLine(pixelXOutput, 0, pixelXOutput, height);

                        g.dispose();
                    }
                }

                return highLightImageCache.getCachedImage();
            }

            private void drawHistogramImage(Histogram histogram, int width, int height, Graphics2D g, Color fillColor, Color lineColor) {
                final double[] histogramValue = new double[width];
                IntStream.range(0, width).forEach(pixelX -> histogramValue[pixelX] = Math.pow(histogram.getValueRGB(pixelX), histogramGammaEnhancement));

                g.setColor(fillColor);
                for (int pixelX = 0; pixelX < width; pixelX++) {
                    g.drawLine(pixelX, height, pixelX, height - (int) (histogramValue[pixelX] * height));
                }
                g.setColor(lineColor);
                for (int pixelX = 1; pixelX < width; pixelX++) {
                    g.drawLine(pixelX - 1, height - (int) (histogramValue[pixelX - 1] * height), pixelX, height - (int) (histogramValue[pixelX] * height));
                }
            }
        });
    }

}