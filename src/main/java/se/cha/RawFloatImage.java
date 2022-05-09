package se.cha;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.stream.IntStream;

public class RawFloatImage {
    private int width;
    private int height;

    private double[] r;
    private double[] g;
    private double[] b;
    private double[] intensityLstar; // Intensity values CIE 1931 L*

    private double intensityMinValue;
    private double intensityMaxValue;
    private double channelMaxValue;

    private Histogram intensityHistogram = null;
    private Range intensityHistogramRange = null;

    private BufferedImage image;

    public void clear() {
        width = -1;
        height = -1;
        image = null;
        intensityHistogram = null;
        intensityHistogramRange = null;
        r = new double[] {};
        g = new double[] {};
        b = new double[] {};
        intensityMaxValue = -Double.MAX_VALUE;
        intensityMinValue = Double.MAX_VALUE;
        channelMaxValue = -Double.MAX_VALUE;
    }

    public boolean isValid() {
        return image != null;
    }

    public void loadFile(File file) throws IOException {
        loadFile(new FileInputStream(file));
    }
    public void loadFile(String filename) throws IOException {
        final FileInputStream fileInputStream = new FileInputStream(filename);
        loadFile(fileInputStream);
    }

    private void loadFile(FileInputStream fileInputStream) throws IOException {
        final DataInputStream dis = new DataInputStream(new BufferedInputStream(fileInputStream));

        final int majorVersion = dis.readInt();
        final int minorVersion = dis.readInt();
        // System.out.println("Version:    " + majorVersion + "." + minorVersion);

        this.width = dis.readInt();
        this.height = dis.readInt();

        final int amountPixels = width * height;

        this.r = new double[amountPixels];
        this.g = new double[amountPixels];
        this.b = new double[amountPixels];

        for (int pixelIndex = 0; pixelIndex < amountPixels; pixelIndex++) {
            this.r[pixelIndex] = dis.readFloat();
            this.g[pixelIndex] = dis.readFloat();
            this.b[pixelIndex] = dis.readFloat();
        }

        dis.close();

        // Find channel max value
        double tmpChannelMax = -(Double.MAX_VALUE - 1);
        for (int pixelIndex = 0; pixelIndex < amountPixels; pixelIndex++) {
            tmpChannelMax = Math.max(tmpChannelMax, Math.max(this.r[pixelIndex], Math.max(this.g[pixelIndex], this.b[pixelIndex])));
        }
        this.channelMaxValue = tmpChannelMax;

        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.intensityHistogram = null;
        this.intensityHistogramRange = null;

        // Calculate pixel intensities. Find min and max intensity values
        this.intensityLstar = new double[amountPixels];
        double tmpIntensityMin = Double.MAX_VALUE;
        double tmpIntensityMax = -(Double.MAX_VALUE - 1);
        for (int pixelIndex = 0; pixelIndex < amountPixels; pixelIndex++) {
            final double pixelIntensity = calculateLstarIntensityValue(pixelIndex);
            intensityLstar[pixelIndex] = pixelIntensity;
            tmpIntensityMin = Math.min(tmpIntensityMin, pixelIntensity);
            tmpIntensityMax = Math.max(tmpIntensityMax, pixelIntensity);
        }
        this.intensityMinValue = tmpIntensityMin;
        this.intensityMaxValue = tmpIntensityMax;
    }

    public BufferedImage getImage(FunctionPanel functionPanel) {
        if (image != null) {
            final int amountPixels = width * height;
            final int[] pixels = new int[amountPixels];

            final double conversionConstant = 256.0 / channelMaxValue;

            for (int pixelIndex = 0; pixelIndex < amountPixels; pixelIndex++) {
                final double pixelIntensityFactor = getPixelIntensityFactor(pixelIndex, functionPanel);

                // Perceptive linear scaling of RGB channels according to pixel intensity (using CIE 1931 Lstar scale)
                final int rValue = (int) clamp(0.0, 255.0, pixelIntensityFactor * r[pixelIndex] * conversionConstant);
                final int gValue = (int) clamp(0.0, 255.0, pixelIntensityFactor * g[pixelIndex] * conversionConstant);
                final int bValue = (int) clamp(0.0, 255.0, pixelIntensityFactor * b[pixelIndex] * conversionConstant);

                pixels[pixelIndex] = 0xFF000000 | (rValue << 16) | (gValue << 8) | (bValue << 0);
            }

            image.setRGB(0, 0, width, height, pixels, 0, width);
        }

        return image;
    }

    private double getPixelIntensityFactor(int pixelIndex, FunctionPanel functionPanel) {
        final double Ymax = 100.0;

        final double originalYluminance = Cie.sRGBtoYluminance(
                r[pixelIndex] / channelMaxValue,
                g[pixelIndex] / channelMaxValue,
                b[pixelIndex] / channelMaxValue);
        final double originalLstarIntensity = Cie.YtoLstar2(originalYluminance);
        final double originalNormalizedLstarIntensity = originalLstarIntensity / intensityMaxValue;
        final double newNormalizedLstarIntensity = functionPanel.getValue(originalNormalizedLstarIntensity);
        final double newLstarIntensity = newNormalizedLstarIntensity * intensityMaxValue;
        final double newYLuminance = Cie.LstarToY2(newLstarIntensity);

        return newYLuminance / originalYluminance;
    }

    private double clamp(double minValue, double maxValue, double value) {
        return Math.max(minValue, Math.min(value, maxValue));
    }

    public double getIntensityMinValue() {
        return intensityMinValue;
    }

    public double getIntensityMaxValue() {
        return intensityMaxValue;
    }

    public Histogram getIntensityHistogram(int amountBoxes, Range includedRange) {
        if ((intensityHistogram == null)
                || (intensityHistogram.getAmountBoxes() != amountBoxes)
                || (includedRange != null && !includedRange.equals(intensityHistogramRange))
                || (includedRange != intensityHistogramRange)) {
            if (includedRange == null) {
                intensityHistogram = new Histogram(amountBoxes, 0.0, intensityMaxValue);
            } else {
                final double minValue = includedRange.getMin() * intensityMaxValue;
                final double maxValue = includedRange.getMax() * intensityMaxValue;
                intensityHistogram = new Histogram(amountBoxes, minValue, maxValue);
            }

            IntStream.range(0, width * height).forEach(pixelIndex -> {
                final double intensityValue = getIntensityValue(pixelIndex);
                final double normalizedIntensityValue = intensityValue / intensityMaxValue;

                if ((includedRange == null) || includedRange.isInRange(normalizedIntensityValue)) {
                    intensityHistogram.addValue(intensityValue);
                }
            });
        }

        intensityHistogramRange = includedRange;

        return intensityHistogram;
    }

    public Histogram getIntensityHistogram(int amountBoxes, FunctionPanel functionPanel, Range includedRange) {
        final Histogram histogram;
        if (includedRange == null) {
            histogram = new Histogram(amountBoxes, 0.0, intensityMaxValue);
        } else {
            final double minValue = includedRange.getMin() * intensityMaxValue;
            final double maxValue = includedRange.getMax() * intensityMaxValue;
            histogram = new Histogram(amountBoxes, minValue, maxValue);
        }

        final double intensityMaxValueInv = 1.0 / intensityMaxValue;
        final int amountPixels = width * height;
        for (int pixelIndex = 0; pixelIndex < amountPixels; pixelIndex++) {
            final double normalizedIntensity = getIntensityValue(pixelIndex) * intensityMaxValueInv;
            final double normalizedOutputIntensity = functionPanel.getValue(normalizedIntensity);
            final double outputIntensity = normalizedOutputIntensity * intensityMaxValue;

            if ((includedRange == null)  || includedRange.isInRange(normalizedOutputIntensity)) {
                histogram.addValue(outputIntensity);
            }
        }

        return histogram;
    }

    double calculateLstarIntensityValue(int pixelIndex) {
        final double luminanceYforsRGB = Cie.sRGBtoYluminance(
                r[pixelIndex] / channelMaxValue,
                g[pixelIndex] / channelMaxValue,
                b[pixelIndex] / channelMaxValue);
        return Cie.YtoLstar2(luminanceYforsRGB);
    }

    double getIntensityValue(int pixelIndex) {
        return intensityLstar[pixelIndex];
    }

    public double getIntensityValue(int x, int y) {
        if ((x < 0) || (x >= width) || (y < 0) || (y >= height)) {
            System.err.println("Can't get pixel intensity outside image bounds {x:0-" + (width - 1) + ", y:0-" + (height - 1) + "}. Requested {x:" + x + ", y:" + y + "}");
            x = Math.max(0, Math.min(x, width -1));
            y = Math.max(0, Math.min(x, height -1));
        }

        return getIntensityValue(y * width + x);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public double[] getPixel(int x, int y) {
        final int pixelIndex = y * width + x;
        return new double[]{r[pixelIndex], g[pixelIndex], b[pixelIndex]};
    }
}
