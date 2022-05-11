package se.cha;

import com.formdev.flatlaf.FlatDarkLaf;
import se.cha.function.SplineFunction;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.stream.IntStream;

import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.NONE;
import static java.awt.GridBagConstraints.NORTHEAST;
import static java.awt.GridBagConstraints.NORTHWEST;
import static java.awt.GridBagConstraints.VERTICAL;

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
            private final Color originalMarkerColor = new Color(128, 255, 255, 128);
            private final Color outputMarkerColor = new Color(255, 128, 128, 196);

            private Double highlightIntensity = null;
            private Color originalColor = null;
            private Color outputColor = null;

            private double histogramGammaEnhancement = 1.0;
            private final ComboBoxDoubleItem histogramOptionLinear = new ComboBoxDoubleItem("Linear (γ=1.0)", 1.0);
            private final ComboBoxDoubleItem histogramOption075 = new ComboBoxDoubleItem("Enhance low values (γ=0.75)", 0.75);
            private final ComboBoxDoubleItem histogramOption050 = new ComboBoxDoubleItem("Enhance low values (γ=0.50)", 0.50);
            private final ComboBoxDoubleItem histogramOption025 = new ComboBoxDoubleItem("Enhance low values (γ=0.25)", 0.25);
            private final ComboBoxDoubleItem histogramOption012 = new ComboBoxDoubleItem("Enhance low values (γ=0.12)", 0.12);

            private final JComboBox<ComboBoxDoubleItem> histogramGammaComboBox = new JComboBox<>(new ComboBoxDoubleItem[]{histogramOptionLinear, histogramOption075, histogramOption050, histogramOption025, histogramOption012});

            final ImagePanel imagePanel = new ImagePanel();
            final FunctionPanel functionPanel = new FunctionPanel(new SplineFunction());
            final RawFloatImage rawFloatImage = new RawFloatImage();

            final JLabel imageZoomLabel = new JLabel("Image scale:");

            final ColorPanel originalColorPanel = new ColorPanel(50);
            final ColorPanel outputColorPanel = new ColorPanel(50);

            final JLabel originalColorRedValueLabel = new JLabel("", SwingConstants.RIGHT);
            final JLabel originalColorGreenValueLabel = new JLabel("", SwingConstants.RIGHT);
            final JLabel originalColorBlueValueLabel = new JLabel("", SwingConstants.RIGHT);
            final JLabel outputColorRedValueLabel = new JLabel("", SwingConstants.RIGHT);
            final JLabel outputColorGreenValueLabel = new JLabel("", SwingConstants.RIGHT);
            final JLabel outputColorBlueValueLabel = new JLabel("", SwingConstants.RIGHT);
            final JLabel originalIntensityValueLabel = new JLabel("", SwingConstants.RIGHT);
            final JLabel outputIntensityValueLabel = new JLabel("", SwingConstants.RIGHT);

            @Override
            public void run() {
                originalIntensityValueLabel.setForeground(originalMarkerColor);
                outputIntensityValueLabel.setForeground(outputMarkerColor);

                final ImageCache originalHistogramImageCache = new ImageCache();
                final ImageCache outputHistogramImageCache = new ImageCache();
                final ImageCache combinedHistogramImageCache = new ImageCache();
                final ImageCache highLightImageCache = new ImageCache();

                final RawImageEditor frame = new RawImageEditor();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

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

                final JButton histogramZoomCheckBox = new JButton(new AbstractAction("Zoom to extent points") {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        final boolean isZoomed = functionPanel.isZoomed();

                        if (!isZoomed) {
                            functionPanel.setZoom();
                        } else {
                            functionPanel.resetZoom();
                        }

                        outputHistogramImageCache.invalidate();
                        originalHistogramImageCache.invalidate();
                        combinedHistogramImageCache.invalidate();
                        functionPanel.repaint();

                        ((JButton) event.getSource()).setText(isZoomed ? "Zoom to extent points" : "Zoom out");
                    }
                });

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

                imagePanel.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent e) {
                        updateImageScaleInformation();
                    }
                });

                imagePanel.addMousePositionListener(point -> {
                    if (point != null) {
                        final double intensityValue = rawFloatImage.getIntensityValue(point.x, point.y);
                        highlightIntensity = intensityValue / rawFloatImage.getIntensityMaxValue();

                        originalColor = rawFloatImage.getRGB(point.x, point.y);
                        outputColor = rawFloatImage.getRGB(point.x, point.y, functionPanel);
                    } else {
                        highlightIntensity = null;

                        originalColor = null;
                        outputColor = null;
                    }

                    updateIntensityInformationLabels();
                    highLightImageCache.invalidate();

                    updateColorInformation();

                    functionPanel.repaint();
                });

                final JPanel imageBorder = setupImagePanel();
                final JPanel functionBorder = setupFunctionPanel();

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

                            updateImageScaleInformation();
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

                final JPanel controlPanel = getControlPanel(histogramZoomCheckBox, histogramCheckBox, loadButton, saveButton);

                final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, imageBorder, functionBorder);
                splitPane.setDividerLocation(0.7);
                splitPane.setResizeWeight(1);

                frame.add(controlPanel, BorderLayout.EAST);
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

            private void updateImageScaleInformation() {
                final double zoom = 100.0 / imagePanel.getScaleFactor();
                final NumberFormat percentNumberFormat = NumberFormat.getInstance();
                percentNumberFormat.setMaximumFractionDigits(0);
                percentNumberFormat.setRoundingMode(RoundingMode.HALF_UP);
                imageZoomLabel.setText("Image scale: " + percentNumberFormat.format(zoom) + "%");
            }

            private void updateColorInformation() {
                originalColorPanel.setColor(originalColor);
                outputColorPanel.setColor(outputColor);

                originalColorRedValueLabel.setText(originalColor != null ? Integer.toString(originalColor.getRed()) : "");
                originalColorGreenValueLabel.setText(originalColor != null ? Integer.toString(originalColor.getGreen()) : "");
                originalColorBlueValueLabel.setText(originalColor != null ? Integer.toString(originalColor.getBlue()) : "");
                outputColorRedValueLabel.setText(outputColor != null ? Integer.toString(outputColor.getRed()) : "");
                outputColorGreenValueLabel.setText(outputColor != null ? Integer.toString(outputColor.getGreen()) : "");
                outputColorBlueValueLabel.setText(outputColor != null ? Integer.toString(outputColor.getBlue()) : "");
            }

            private JPanel getControlPanel(JButton histogramZoomCheckBox, JCheckBox histogramCheckBox, JButton loadButton, JButton saveButton) {
                final Insets noInsets = new Insets(0, 0, 0, 0);

                final JLabel inputCaption = new JLabel("Input:");
                final JLabel outputCaption = new JLabel("Output:");
                inputCaption.setForeground(originalMarkerColor);
                outputCaption.setForeground(outputMarkerColor);

                final JPanel colorInformationPanel = getColorInformationPanel();

                final JPanel controlPanel = new JPanel(new GridBagLayout());
                controlPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

                int columnIndex = 0;
                controlPanel.add(loadButton, new GridBagConstraints(0, columnIndex++, 1, 1, 0, 0, NORTHWEST, HORIZONTAL, new Insets(4, 4, 0, 4), 0, 0));
                controlPanel.add(saveButton, new GridBagConstraints(0, columnIndex++, 1, 1, 0, 0, NORTHWEST, HORIZONTAL, new Insets(4, 4, 0, 4), 0, 0));
                controlPanel.add(imageZoomLabel, new GridBagConstraints(0, columnIndex++, 1, 1, 0, 0, NORTHWEST, HORIZONTAL, new Insets(4, 4, 0, 4), 0, 0));
                controlPanel.add(colorInformationPanel, new GridBagConstraints(0, columnIndex++, 1, 1, 0, 0, NORTHWEST, HORIZONTAL, new Insets(4, 4, 0, 4), 0, 0));

                controlPanel.add(new JPanel(), new GridBagConstraints(0, columnIndex++, 1, 1, 0, 1, NORTHWEST, BOTH, noInsets, 0, 0));

                controlPanel.add(histogramCheckBox, new GridBagConstraints(0, columnIndex++, 1, 1, 0, 0, NORTHWEST, HORIZONTAL, noInsets, 0, 0));
                controlPanel.add(histogramZoomCheckBox, new GridBagConstraints(0, columnIndex++, 1, 1, 0, 0, NORTHWEST, HORIZONTAL, noInsets, 0, 0));
                controlPanel.add(new JLabel("Histogram value scale:"), new GridBagConstraints(0, columnIndex++, 1, 1, 0, 0, NORTHWEST, NONE, new Insets(8, 4, 0, 4), 0, 0));
                controlPanel.add(histogramGammaComboBox, new GridBagConstraints(0, columnIndex++, 1, 1, 0, 0, NORTHWEST, NONE, new Insets(0, 4, 0, 4), 0, 0));

                return controlPanel;
            }

            private JPanel getColorInformationPanel() {
                final JPanel colorInformationPanel = new JPanel(new GridBagLayout());

                final JLabel inputRgbLabel = new JLabel("Input:");
                inputRgbLabel.setForeground(originalMarkerColor);
                final JLabel outputRgbLabel = new JLabel("Output:");
                outputRgbLabel.setForeground(outputMarkerColor);

                final JLabel originalRedLabel = new JLabel("Red:");
                final JLabel originalGreenLabel = new JLabel("Green:");
                final JLabel originalBlueLabel = new JLabel("Blue:");
                final JLabel originalIntensityLabel = new JLabel("Intensity:");
                final JLabel outputRedLabel = new JLabel("Red:");
                final JLabel outputGreenLabel = new JLabel("Green:");
                final JLabel outputBlueLabel = new JLabel("Blue:");
                final JLabel outputIntensityLabel = new JLabel("Intensity:");
                final Color labelOriginalForeground = originalRedLabel.getForeground();
                final Color labelRgbForegroundColor = new Color(labelOriginalForeground.getRed(), labelOriginalForeground.getGreen(), labelOriginalForeground.getBlue(), 96);

                originalRedLabel.setForeground(labelRgbForegroundColor);
                originalGreenLabel.setForeground(labelRgbForegroundColor);
                originalBlueLabel.setForeground(labelRgbForegroundColor);
                originalIntensityLabel.setForeground(labelRgbForegroundColor);
                outputRedLabel.setForeground(labelRgbForegroundColor);
                outputGreenLabel.setForeground(labelRgbForegroundColor);
                outputBlueLabel.setForeground(labelRgbForegroundColor);
                outputIntensityLabel.setForeground(labelRgbForegroundColor);

                int rowIndex = -1;

                final Insets rgbInsets = new Insets(0, 4, 0, 4);

                colorInformationPanel.add(inputRgbLabel, new GridBagConstraints(0, ++rowIndex, 3, 1, 0, 0, NORTHWEST, HORIZONTAL, new Insets(4, 4, 0, 4), 0, 0));
                colorInformationPanel.add(originalColorPanel, new GridBagConstraints(0, ++rowIndex, 1, 4, 0, 0, NORTHWEST, VERTICAL, new Insets(4, 4, 0, 4), 0, 0));
                colorInformationPanel.add(originalRedLabel, new GridBagConstraints(1, rowIndex + 0, 1, 1, 0, 0, NORTHWEST, NONE, rgbInsets, 0, 0));
                colorInformationPanel.add(originalGreenLabel, new GridBagConstraints(1, rowIndex + 1, 1, 1, 0, 0, NORTHWEST, NONE, rgbInsets, 0, 0));
                colorInformationPanel.add(originalBlueLabel, new GridBagConstraints(1, rowIndex + 2, 1, 1, 0, 0, NORTHWEST, NONE, rgbInsets, 0, 0));
                colorInformationPanel.add(originalIntensityLabel, new GridBagConstraints(1, rowIndex + 3, 1, 1, 0, 0, NORTHWEST, NONE, new Insets(4, 4, 0, 4), 0, 0));
                colorInformationPanel.add(originalColorRedValueLabel, new GridBagConstraints(2, rowIndex + 0, 1, 1, 1, 0, NORTHEAST, HORIZONTAL, rgbInsets, 0, 0));
                colorInformationPanel.add(originalColorGreenValueLabel, new GridBagConstraints(2, rowIndex + 1, 1, 1, 1, 0, NORTHEAST, HORIZONTAL, rgbInsets, 0, 0));
                colorInformationPanel.add(originalColorBlueValueLabel, new GridBagConstraints(2, rowIndex + 2, 1, 1, 1, 0, NORTHEAST, HORIZONTAL, rgbInsets, 0, 0));
                colorInformationPanel.add(originalIntensityValueLabel, new GridBagConstraints(2, rowIndex + 3, 1, 1, 1, 0, NORTHEAST, HORIZONTAL, new Insets(4, 4, 0, 4), 0, 0));

                rowIndex += 3;

                colorInformationPanel.add(outputRgbLabel, new GridBagConstraints(0, ++rowIndex, 3, 1, 0, 0, NORTHWEST, HORIZONTAL, new Insets(4, 4, 0, 4), 0, 0));
                colorInformationPanel.add(outputColorPanel, new GridBagConstraints(0, ++rowIndex, 1, 4, 0, 0, NORTHWEST, VERTICAL, new Insets(4, 4, 0, 4), 0, 0));
                colorInformationPanel.add(outputRedLabel, new GridBagConstraints(1, rowIndex + 0, 1, 1, 0, 0, NORTHWEST, NONE, rgbInsets, 0, 0));
                colorInformationPanel.add(outputGreenLabel, new GridBagConstraints(1, rowIndex + 1, 1, 1, 0, 0, NORTHWEST, NONE, rgbInsets, 0, 0));
                colorInformationPanel.add(outputBlueLabel, new GridBagConstraints(1, rowIndex + 2, 1, 1, 0, 0, NORTHWEST, NONE, rgbInsets, 0, 0));
                colorInformationPanel.add(outputIntensityLabel, new GridBagConstraints(1, rowIndex + 3, 1, 1, 0, 0, NORTHWEST, NONE, new Insets(4, 4, 0, 4), 0, 0));
                colorInformationPanel.add(outputColorRedValueLabel, new GridBagConstraints(2, rowIndex + 0, 1, 1, 1, 0, NORTHEAST, HORIZONTAL, rgbInsets, 0, 0));
                colorInformationPanel.add(outputColorGreenValueLabel, new GridBagConstraints(2, rowIndex + 1, 1, 1, 1, 0, NORTHEAST, HORIZONTAL, rgbInsets, 0, 0));
                colorInformationPanel.add(outputColorBlueValueLabel, new GridBagConstraints(2, rowIndex + 2, 1, 1, 1, 0, NORTHEAST, HORIZONTAL, rgbInsets, 0, 0));
                colorInformationPanel.add(outputIntensityValueLabel, new GridBagConstraints(2, rowIndex + 3, 1, 1, 1, 0, NORTHEAST, HORIZONTAL, new Insets(4, 4, 0, 4), 0, 0));

                return colorInformationPanel;
            }

            private JPanel setupImagePanel() {
                final JPanel imageBorder = new JPanel(new BorderLayout());
                imageBorder.setBorder(new EmptyBorder(8, 8, 8, 8));
                imageBorder.add(imagePanel, BorderLayout.CENTER);

                return imageBorder;
            }

            private JPanel setupFunctionPanel() {
                final JPanel imageBorder = new JPanel(new BorderLayout());
                imageBorder.setBorder(new EmptyBorder(8, 8, 8, 8));
                imageBorder.add(functionPanel, BorderLayout.CENTER);

                return imageBorder;
            }

            private void updateIntensityInformationLabels() {
                if (highlightIntensity != null) {
                    final double inputValue = highlightIntensity;
                    final double outputValue = functionPanel.getValue(highlightIntensity);

                    final NumberFormat numberFormat = NumberFormat.getInstance();
                    numberFormat.setMinimumIntegerDigits(1);
                    numberFormat.setMinimumFractionDigits(4);
                    numberFormat.setMaximumFractionDigits(4);

                    originalIntensityValueLabel.setText(numberFormat.format(inputValue * 100.0) + "%");
                    outputIntensityValueLabel.setText(numberFormat.format(outputValue * 100.0) + "%");
                } else {
                    originalIntensityValueLabel.setText("");
                    outputIntensityValueLabel.setText("");
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
                    final Histogram histogram = rawFloatImage.getIntensityHistogram(width, functionPanel.getZoomRange());

                    final Color fillColor = new Color(128, 255, 255, 16);
                    final Color lineColor = new Color(128, 255, 255, 64);
                    drawHistogramImage(histogram, height, g, fillColor, lineColor);
                    g.dispose();
                }

                if (!outputHistogramImageCache.valid() || !outputHistogramImageCache.boundsMatch(width, height)) {
                    final Graphics2D g = outputHistogramImageCache.createImage(width, height);
                    final Histogram histogram = rawFloatImage.getIntensityHistogram(width, functionPanel, functionPanel.getZoomRange());

                    final Color fillColor = new Color(255, 128, 128, 16);
                    final Color lineColor = new Color(255, 128, 128, 64);
                    drawHistogramImage(histogram, height, g, fillColor, lineColor);
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
                            g.fillRect(0, 0, width, height);
                        } else {
                            g = highLightImageCache.createImage(width, height);
                        }

                        final int pixelX = (int) Math.round(width * normalizedIntensityValue);
                        final int pixelXOutput = (int) Math.round(width * normalizedOutputIntensityValue);

                        g.setColor(originalMarkerColor);
                        g.drawLine(pixelX, 0, pixelX, height);

                        g.setColor(outputMarkerColor);
                        g.drawLine(pixelXOutput, 0, pixelXOutput, height);

                        g.dispose();
                    }
                }

                return highLightImageCache.getCachedImage();
            }

            private void drawHistogramImage(Histogram histogram, int height, Graphics2D g, Color fillColor, Color lineColor) {
                final int width = histogram.getAmountBoxes();
                final double[] histogramValue = new double[width];
                IntStream.range(0, width).forEach(pixelX -> {
                    histogramValue[pixelX] = Math.pow(histogram.getValueRGB(pixelX), histogramGammaEnhancement);
                });

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