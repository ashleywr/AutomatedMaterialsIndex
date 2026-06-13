package com.sanhiruzu.ami.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AmiClientWorkSchedulerTest {
    @Test
    void returnsSameLaneForRegisteredName() {
        AdaptiveTickScheduler.Config config = new AdaptiveTickScheduler.Config(
                true, 4, 1, 10, 2_000_000L, 5_000_000L, 8);

        AmiClientWorkScheduler.Lane first = AmiClientWorkScheduler.lane("test.sharedLane", config);
        AmiClientWorkScheduler.Lane second = AmiClientWorkScheduler.lane("test.sharedLane", config);

        assertSame(first, second);
        assertEquals("test.sharedLane", first.name());
    }
}
