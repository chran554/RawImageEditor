package se.cha;

import org.junit.Assert;
import org.junit.Test;

public class CieTest {

    @Test
    public void cieLstarConversionZero() {
        final double Y1 = 100.0 * Cie.sRGBtoYluminance(0, 0, 0);
        final double lstar = Cie.YtoLstar2(Y1);
        final double Y2 = Cie.LstarToY2(lstar);

        Assert.assertEquals(Y1, Y2, 0.001);
    }

    @Test
    public void cieLstarConversionOne() {
        final double Y1 = 100.0 * Cie.sRGBtoYluminance(1.0, 1.0, 1.0);
        final double lstar = Cie.YtoLstar2(Y1);
        final double Y2 = Cie.LstarToY2(lstar);

        Assert.assertEquals(Y1, Y2, 0.001);
    }

    @Test
    public void cieLstarConversionRange() {
        final double deltaRGB = 0.05;
        double rgb = 0.0;
        for (int i = 0; rgb < 1.0; i++) {
            rgb = i * deltaRGB;

            final double Y1 = 100.0 * Cie.sRGBtoYluminance(rgb, rgb, rgb);
            final double lstar = Cie.YtoLstar2(Y1);
            final double Y2 = Cie.LstarToY2(lstar);

            Assert.assertEquals(Y1, Y2, 0.001);
        }
    }
}