package se.cha;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class RawFloatImage {
    private int width;
    private int height;

    private double[] r;
    private double[] g;
    private double[] b;

    private double intensityMinValue;
    private double intensityMaxValue;
    private double channelMaxValue;

    private BufferedImage image;

    public void loadFile(String filename) throws IOException {
        final FileInputStream fileInputStream = new FileInputStream(filename);
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

        // Find min and max intensity values
        double tmpIntensityMin = Double.MAX_VALUE;
        double tmpIntensityMax = -(Double.MAX_VALUE - 1);
        for (int pixelIndex = 0; pixelIndex < amountPixels; pixelIndex++) {
            final double pixelIntensity = getIntensityValue(pixelIndex);
            tmpIntensityMin = Math.min(tmpIntensityMin, pixelIntensity);
            tmpIntensityMax = Math.max(tmpIntensityMax, pixelIntensity);
        }
        this.intensityMinValue = tmpIntensityMin;
        this.intensityMaxValue = tmpIntensityMax;
    }

    public BufferedImage getImage(FunctionPanel functionPanel) {
        final int amountPixels = width * height;
        final int[] pixels = new int[amountPixels];

        for (int pixelIndex = 0; pixelIndex < amountPixels; pixelIndex++) {
            final double pixelIntensityFactor = getPixelIntensityFactor(pixelIndex, functionPanel);

            // Perceptive linear scaling of RGB channels according to pixel intensity (using CIE 1931 Lstar scale)
            final int rValue = (int) clamp(0.0, 255.0, 256.0 * pixelIntensityFactor * r[pixelIndex] / channelMaxValue);
            final int gValue = (int) clamp(0.0, 255.0, 256.0 * pixelIntensityFactor * g[pixelIndex] / channelMaxValue);
            final int bValue = (int) clamp(0.0, 255.0, 256.0 * pixelIntensityFactor * b[pixelIndex] / channelMaxValue);

            pixels[pixelIndex] = 0xFF000000 | (rValue << 16) | (gValue << 8) | (bValue << 0);
        }

        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }

    private double getPixelIntensityFactor(int pixelIndex, FunctionPanel functionPanel) {
        final double Ymax = 100.0;

        final double originalYluminance = Cie.sRGBtoYluminance(
                r[pixelIndex] / channelMaxValue,
                g[pixelIndex] / channelMaxValue,
                b[pixelIndex] / channelMaxValue);
        final double originalLstarIntensity = Cie.YtoLstar2(originalYluminance) / Ymax;
        final double originalNormalizedLstarIntensity = originalLstarIntensity / intensityMaxValue;
        final double newNormalizedLstarIntensity = functionPanel.getValue(originalNormalizedLstarIntensity);
        final double newLstarIntensity = newNormalizedLstarIntensity * intensityMaxValue;
        final double newYLuminance = Cie.LstarToY2(newLstarIntensity) * Ymax;

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

    public Histogram getIntensityHistogram(int amountBoxes) {
        final Histogram histogram = new Histogram(amountBoxes, 0.0, intensityMaxValue);

        final int amountPixels = width * height;
        for (int pixelIndex = 0; pixelIndex < amountPixels; pixelIndex++) {
            // Intensity histogram, RGB channels weighted by eye color sensitivity
            histogram.addValue(getIntensityValue(pixelIndex));

            // Average of RGB channels
//            histogram.addValue((r[pixelIndex] + g[pixelIndex] + b[pixelIndex]) / 3.0);

            // Separate values for each RGB channel
//            histogram.addValue(r[pixelIndex]);
//            histogram.addValue(g[pixelIndex]);
//            histogram.addValue(b[pixelIndex]);
        }

        return histogram;
    }

    public Histogram getIntensityHistogram(int amountBoxes, FunctionPanel functionPanel) {
        final Histogram histogram = new Histogram(amountBoxes, 0.0, intensityMaxValue);

        final int amountPixels = width * height;
        for (int pixelIndex = 0; pixelIndex < amountPixels; pixelIndex++) {
            final double normalizedIntensity = getIntensityValue(pixelIndex) / intensityMaxValue;
            histogram.addValue(functionPanel.getValue(normalizedIntensity) * intensityMaxValue);
        }

        return histogram;
    }

    double getIntensityValue(int pixelIndex) {
        final double Ymax = 100.0;
        final double luminanceYforsRGB = Cie.sRGBtoYluminance(
                r[pixelIndex] / channelMaxValue,
                g[pixelIndex] / channelMaxValue,
                b[pixelIndex] / channelMaxValue);
        return Cie.YtoLstar2(luminanceYforsRGB) / Ymax;
    }

/*
    double getIntensityValue2(int pixelIndex) {
        return r[pixelIndex] * 0.11 + g[pixelIndex] * 0.59 + b[pixelIndex] * 0.30;
    }
*/


    public double getIntensityValue(int x, int y) {
        if ((x < 0) || (x >= width) || (y < 0) || (y >= height)) {
            throw new IllegalArgumentException("Can't get pixel intensity outside image bounds {x:0-" + (width - 1) + ", y:0-" + (height - 1) + "}. Requested {x:" + x + ", y:" + y + "}");
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
