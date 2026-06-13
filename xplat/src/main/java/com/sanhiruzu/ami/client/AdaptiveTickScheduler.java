package com.sanhiruzu.ami.client;

/**
 * Small reusable scheduler for client-thread background work that should run
 * opportunistically but back off when recent work exceeds a frame-time budget.
 */
public final class AdaptiveTickScheduler {
    private final boolean adaptive;
    private final int minIntervalTicks;
    private final int maxIntervalTicks;
    private final long targetNanos;
    private final long backoffNanos;
    private final int cheapSamplesBeforeSpeedup;
    private int intervalTicks;
    private int tickCounter;
    private int cheapSampleStreak;

    public AdaptiveTickScheduler(Config config) {
        this.adaptive = config.adaptive();
        this.minIntervalTicks = Math.max(1, config.minIntervalTicks());
        this.maxIntervalTicks = Math.max(minIntervalTicks, config.maxIntervalTicks());
        this.targetNanos = Math.max(100_000L, config.targetNanos());
        this.backoffNanos = Math.max(targetNanos, config.backoffNanos());
        this.cheapSamplesBeforeSpeedup = Math.max(1, config.cheapSamplesBeforeSpeedup());
        this.intervalTicks = clamp(config.initialIntervalTicks());
    }

    public boolean shouldRunThisTick() {
        if (!adaptive) {
            return true;
        }
        tickCounter++;
        if (tickCounter < intervalTicks) {
            return false;
        }
        tickCounter = 0;
        return true;
    }

    public void recordWorkNanos(long elapsedNanos) {
        if (!adaptive || elapsedNanos <= 0L) {
            return;
        }
        if (elapsedNanos >= backoffNanos) {
            intervalTicks = Math.min(maxIntervalTicks, Math.max(intervalTicks + 1, intervalTicks * 2));
            cheapSampleStreak = 0;
            return;
        }
        if (elapsedNanos <= targetNanos) {
            cheapSampleStreak++;
            if (cheapSampleStreak >= cheapSamplesBeforeSpeedup && intervalTicks > minIntervalTicks) {
                intervalTicks--;
                cheapSampleStreak = 0;
            }
            return;
        }
        cheapSampleStreak = 0;
        if (intervalTicks < maxIntervalTicks) {
            intervalTicks++;
        }
    }

    int intervalTicksForTests() {
        return intervalTicks;
    }

    private int clamp(int value) {
        return Math.max(minIntervalTicks, Math.min(maxIntervalTicks, value));
    }

    public record Config(
            boolean adaptive,
            int initialIntervalTicks,
            int minIntervalTicks,
            int maxIntervalTicks,
            long targetNanos,
            long backoffNanos,
            int cheapSamplesBeforeSpeedup
    ) {
    }
}
