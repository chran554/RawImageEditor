package se.cha;

public class Histogram {

    private final int[] boxes;
    private final double minValue;
    private final double maxValue;
    private final double boxRangeInv;
    private int maxCount;

    public Histogram(int amountBoxes, double minValue, double maxValue) {
        this.boxes = new int[amountBoxes];
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.boxRangeInv = 1.0/ ((maxValue - minValue) / (1.0 * boxes.length));
    }

    public void addValue(double value) {
        final int boxIndex = Math.max(0, Math.min(boxes.length - 1, (int)((value - minValue) * boxRangeInv)));
        boxes[boxIndex]++;
        maxCount = Math.max(maxCount, boxes[boxIndex]);
    }

    public double getValueRGB(int boxNr) {
        return boxes[boxNr] / (1.0*maxCount);
    }
}
