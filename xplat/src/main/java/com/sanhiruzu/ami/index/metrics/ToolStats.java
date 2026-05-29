package com.sanhiruzu.ami.index.metrics;

public record ToolStats(double speed, int uses, double attackBonus) {
    public boolean hasAny() {
        return speed > 0.0D || uses > 0 || attackBonus > 0.0D;
    }
}
