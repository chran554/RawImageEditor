package se.cha;

import lombok.Value;

@Value
public class Range {
    double min;
    double max;

    public double getLength() {
        return max - min;
    }

    public boolean isInRange(double value) {
        return (value >= min) && (value <= max);
    }
}

