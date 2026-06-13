package com.sanhiruzu.ami.client;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry for cooperative client-thread work lanes. This is for tasks that
 * must run on the Minecraft client/render thread but should adapt their cadence
 * to recent cost, such as GL-backed icon baking.
 */
public final class AmiClientWorkScheduler {
    private static final Map<String, Lane> LANES = new LinkedHashMap<>();

    private AmiClientWorkScheduler() {
    }

    public static synchronized Lane lane(String name, AdaptiveTickScheduler.Config config) {
        return LANES.computeIfAbsent(name, ignored -> new Lane(name, new AdaptiveTickScheduler(config)));
    }

    public static final class Lane {
        private final String name;
        private final AdaptiveTickScheduler scheduler;

        private Lane(String name, AdaptiveTickScheduler scheduler) {
            this.name = name;
            this.scheduler = scheduler;
        }

        public String name() {
            return name;
        }

        public boolean shouldRunThisTick() {
            return scheduler.shouldRunThisTick();
        }

        public void recordWorkNanos(long elapsedNanos) {
            scheduler.recordWorkNanos(elapsedNanos);
        }

        int intervalTicksForTests() {
            return scheduler.intervalTicksForTests();
        }
    }
}
