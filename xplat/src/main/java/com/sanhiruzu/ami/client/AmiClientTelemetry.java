package com.sanhiruzu.ami.client;

/**
 * Lightweight timing for AMI's client tick handler and spacing between client
 * tick callbacks. Process/JVM samplers still own heap, CPU, and GC data.
 */
public final class AmiClientTelemetry {
    private static long lastTickStartedAt;
    private static long currentTickStartedAt;
    private static long tickSamples;
    private static long tickIntervalSamples;
    private static long totalTickNanos;
    private static long maxTickNanos;
    private static long totalTickIntervalNanos;
    private static long maxTickIntervalNanos;
    private static long lastFrameAt;
    private static long frameSamples;
    private static long totalFrameIntervalNanos;
    private static long maxFrameIntervalNanos;

    private AmiClientTelemetry() {
    }

    public static synchronized void beginClientTick() {
        long now = System.nanoTime();
        currentTickStartedAt = now;
        if (lastTickStartedAt > 0L) {
            long interval = now - lastTickStartedAt;
            tickIntervalSamples++;
            totalTickIntervalNanos += interval;
            maxTickIntervalNanos = Math.max(maxTickIntervalNanos, interval);
        }
        lastTickStartedAt = now;
    }

    public static synchronized void endClientTick() {
        if (currentTickStartedAt <= 0L) {
            return;
        }
        long elapsed = System.nanoTime() - currentTickStartedAt;
        currentTickStartedAt = 0L;
        tickSamples++;
        totalTickNanos += elapsed;
        maxTickNanos = Math.max(maxTickNanos, elapsed);
    }

    public static synchronized Snapshot snapshot() {
        return new Snapshot(
                tickSamples,
                tickIntervalSamples,
                frameSamples,
                averageNanos(totalTickNanos, tickSamples),
                maxTickNanos,
                averageNanos(totalTickIntervalNanos, tickIntervalSamples),
                maxTickIntervalNanos,
                averageNanos(totalFrameIntervalNanos, frameSamples),
                maxFrameIntervalNanos
        );
    }

    public static synchronized Snapshot snapshotAndReset() {
        Snapshot snapshot = snapshot();
        tickSamples = 0L;
        tickIntervalSamples = 0L;
        totalTickNanos = 0L;
        maxTickNanos = 0L;
        totalTickIntervalNanos = 0L;
        maxTickIntervalNanos = 0L;
        frameSamples = 0L;
        totalFrameIntervalNanos = 0L;
        maxFrameIntervalNanos = 0L;
        return snapshot;
    }

    public static synchronized void recordFrame() {
        long now = System.nanoTime();
        if (lastFrameAt > 0L) {
            long interval = now - lastFrameAt;
            frameSamples++;
            totalFrameIntervalNanos += interval;
            maxFrameIntervalNanos = Math.max(maxFrameIntervalNanos, interval);
        }
        lastFrameAt = now;
    }

    private static long averageNanos(long total, long samples) {
        return samples <= 0L ? 0L : total / samples;
    }

    public record Snapshot(
            long tickSamples,
            long tickIntervalSamples,
            long frameSamples,
            long averageTickNanos,
            long maxTickNanos,
            long averageTickIntervalNanos,
            long maxTickIntervalNanos,
            long averageFrameIntervalNanos,
            long maxFrameIntervalNanos
    ) {
        public double estimatedFps() {
            return averageFrameIntervalNanos <= 0L ? -1.0D : 1_000_000_000.0D / averageFrameIntervalNanos;
        }
    }
}
