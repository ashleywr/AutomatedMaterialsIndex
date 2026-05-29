package com.sanhiruzu.ami.index.metrics;

public record FluidStats(double buckets, String source) {
    public boolean hasAny() {
        return buckets > 0.0D;
    }
}
