package com.sanhiruzu.ami.neoforge;

import org.junit.jupiter.api.Test;

import java.util.OptionalLong;
import java.util.function.IntToLongFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NeoForgePlatformHelperTransferTest {
    @Test
    void fluidCapacityProbeIgnoresBrokenHandler() {
        IntToLongFunction reader = index -> {
            throw new NullPointerException("broken capability");
        };

        assertEquals(OptionalLong.empty(), NeoForgePlatformHelper.sumPositiveLongs(1, reader));
    }

    @Test
    void fluidAmountProbeIgnoresBrokenHandler() {
        IntToLongFunction reader = index -> {
            throw new NullPointerException("broken capability");
        };

        assertEquals(OptionalLong.empty(), NeoForgePlatformHelper.sumPositiveLongs(1, reader));
    }
}
