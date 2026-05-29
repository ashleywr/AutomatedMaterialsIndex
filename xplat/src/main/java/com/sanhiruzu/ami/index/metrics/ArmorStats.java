package com.sanhiruzu.ami.index.metrics;

public record ArmorStats(int defense, double toughness) {
    public boolean hasAny() {
        return defense > 0 || toughness > 0.0D;
    }
}
