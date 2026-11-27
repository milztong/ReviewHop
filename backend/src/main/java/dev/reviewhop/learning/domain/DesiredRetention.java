package dev.reviewhop.learning.domain;

public record DesiredRetention(double value) {

    public static final DesiredRetention DEFAULT = new DesiredRetention(0.90);

    public DesiredRetention {
        if (!Double.isFinite(value) || value <= 0.0 || value >= 1.0) {
            throw new IllegalArgumentException("desired retention must be between 0 and 1");
        }
    }
}
