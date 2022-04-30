package se.cha;

import lombok.Value;

@Value
public class ComboBoxDoubleItem {
    String label;
    double value;

    public String toString() {
        return label;
    }
}
