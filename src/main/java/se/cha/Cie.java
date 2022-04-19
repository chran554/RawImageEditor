package se.cha;

public class Cie {
    public static double sRGBtoLinearRGB(double colorChannel) {
        // Send this function a decimal sRGB gamma encoded color channel value (R, G, or B)
        // between 0.0 and 1.0, and it returns a linearized value.

        if (colorChannel <= 0.04045) {
            return colorChannel / 12.92;
        } else {
            return Math.pow(((colorChannel + 0.055) / 1.055), 2.4);
        }
    }

    public static double sRGBtoYluminance(double linearR, double linearG, double linearB) {
        final double Y = (0.2126 * linearR + 0.7152 * linearG + 0.0722 * linearB);
        return Y;
    }

    /**
     * https://stackoverflow.com/questions/596216/formula-to-determine-perceived-brightness-of-rgb-color
     * <p>
     * L* is a value from 0 (black) to 100 (white)
     * where 50 is the perceptual "middle grey".
     * L* = 50 is the equivalent of Y = 18.4, or in other words an 18% grey card,
     * representing the middle of a photographic exposure.
     */
/*
    static double YtoLstar(double Y) {
        // Send this function a luminance value between 0.0 and 1.0,
        // and it returns L* which is "perceptual lightness"

        if (Y <= (216.0 / 24389.0)) {       // The CIE standard states 0.008856 but 216/24389 is the intent for 0.008856451679036
            return Y * (24389.0 / 27.0);    // The CIE standard states 903.3, but 24389/27 is the intent, making 903.296296296296296
        } else {
            return Math.pow(Y, (1.0 / 3.0)) * 116.0 - 16.0;
        }
    }
*/

    /**
     * http://cs.haifa.ac.il/hagit/courses/ist/Lectures/Demos/ColorApplet2/t_convert.html#XYZ%20to%20CIE%20L*a*b*%20(CIELAB)%20&%20CIELAB%20to%20XYZ
     */
/*
    static double LstarToY(double Lstar) {
        return Math.pow((Lstar + 16.0) / 116.0, 3.0);
    }
*/

    /**
     * https://en.wikipedia.org/wiki/CIELAB_color_space
     */
    public static double YtoLstar2(double Y) {
        final double delta = 6.0 / 29.0;
        final double deltaPow3 = 216.0 / 24389.0; // Math.pow(6.0 / 29.0, 3.0);
        final double Yn = 100.0;
        final double t = Y / Yn;

        final double f;
        if (t > deltaPow3) {
            f = Math.pow(t, 1.0 / 3.0);
        } else {
            f = t / (3.0 * delta * delta) + 4.0 / 29.0;
        }

        return 116.0 * f - 16.0;
    }

    /**
     * https://en.wikipedia.org/wiki/CIELAB_color_space
     */
    public static double LstarToY2(double Lstar) {
        final double Yn = 100.0;
        final double delta = 6.0 / 29.0;
        final double t = (Lstar + 16.0) / 116.0;

        final double f;
        if (t > delta) {
            f = t * t * t;
        } else {
            f = 3.0 * delta * delta * (t - (4.0 / 29.0));
        }

        return Yn * f;
    }

}
