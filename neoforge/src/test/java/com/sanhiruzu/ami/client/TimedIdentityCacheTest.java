package com.sanhiruzu.ami.client;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TimedIdentityCacheTest {
    @Test
    void reusesOnlyTheSameObjectUntilItsLifetimeExpires() {
        AtomicLong clock = new AtomicLong(100);
        TimedIdentityCache<String, Object> cache = new TimedIdentityCache<>(2, 250, clock::get);
        AtomicInteger resolutions = new AtomicInteger();
        String firstKey = new String("same");
        String equalButDistinctKey = new String("same");

        Object first = cache.getOrCompute(firstKey, () -> newValue(resolutions));
        assertSame(first, cache.getOrCompute(firstKey, () -> newValue(resolutions)));
        cache.getOrCompute(equalButDistinctKey, () -> newValue(resolutions));
        assertEquals(2, resolutions.get());

        clock.addAndGet(250);
        cache.getOrCompute(firstKey, () -> newValue(resolutions));
        assertEquals(3, resolutions.get());
    }

    @Test
    void evictsTheOldestIdentityWhenBounded() {
        TimedIdentityCache<Object, Object> cache = new TimedIdentityCache<>(2, 250, () -> 0L);
        AtomicInteger resolutions = new AtomicInteger();
        Object first = new Object();

        cache.getOrCompute(first, () -> newValue(resolutions));
        cache.getOrCompute(new Object(), () -> newValue(resolutions));
        cache.getOrCompute(new Object(), () -> newValue(resolutions));
        cache.getOrCompute(first, () -> newValue(resolutions));

        assertEquals(4, resolutions.get());
    }

    private static Object newValue(AtomicInteger resolutions) {
        resolutions.incrementAndGet();
        return new Object();
    }
}
