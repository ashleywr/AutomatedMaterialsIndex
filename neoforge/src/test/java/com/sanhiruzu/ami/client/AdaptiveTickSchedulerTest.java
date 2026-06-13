package com.sanhiruzu.ami.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdaptiveTickSchedulerTest {
    @Test
    void adaptiveSchedulerRunsOnConfiguredInterval() {
        AdaptiveTickScheduler scheduler = new AdaptiveTickScheduler(config(true, 3, 1, 10));

        assertFalse(scheduler.shouldRunThisTick());
        assertFalse(scheduler.shouldRunThisTick());
        assertTrue(scheduler.shouldRunThisTick());
        assertFalse(scheduler.shouldRunThisTick());
    }

    @Test
    void expensiveWorkBacksOffInterval() {
        AdaptiveTickScheduler scheduler = new AdaptiveTickScheduler(config(true, 2, 1, 10));

        scheduler.recordWorkNanos(6_000_000L);

        assertEquals(4, scheduler.intervalTicksForTests());
    }

    @Test
    void repeatedCheapWorkSpeedsUpGradually() {
        AdaptiveTickScheduler scheduler = new AdaptiveTickScheduler(
                new AdaptiveTickScheduler.Config(true, 5, 2, 10, 2_000_000L, 5_000_000L, 2));

        scheduler.recordWorkNanos(500_000L);
        assertEquals(5, scheduler.intervalTicksForTests());

        scheduler.recordWorkNanos(500_000L);
        assertEquals(4, scheduler.intervalTicksForTests());
    }

    @Test
    void nonAdaptiveSchedulerRunsEveryTick() {
        AdaptiveTickScheduler scheduler = new AdaptiveTickScheduler(config(false, 20, 1, 40));

        assertTrue(scheduler.shouldRunThisTick());
        assertTrue(scheduler.shouldRunThisTick());
        scheduler.recordWorkNanos(100_000_000L);
        assertTrue(scheduler.shouldRunThisTick());
    }

    private static AdaptiveTickScheduler.Config config(boolean adaptive, int initial, int min, int max) {
        return new AdaptiveTickScheduler.Config(adaptive, initial, min, max, 2_000_000L, 5_000_000L, 8);
    }
}
