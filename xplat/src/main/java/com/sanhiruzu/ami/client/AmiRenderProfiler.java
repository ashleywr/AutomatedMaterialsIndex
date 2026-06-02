package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.AmiCore;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JVM-property gated render timing for AMI UI work. Disabled unless
 * -Dami.renderProfiler=true is present.
 */
public final class AmiRenderProfiler {
    private static final String ENABLE_PROPERTY = "ami." + "renderProfiler";
    private static final String FRAMES_PROPERTY = "ami." + "renderProfiler.frames";
    private static final int DEFAULT_REPORT_FRAMES = 120;
    private static final boolean ENABLED = Boolean.getBoolean(ENABLE_PROPERTY);
    private static final Section NOOP_SECTION = new Section(null, 0L, false);
    private static final Map<String, Stat> STATS = new LinkedHashMap<>();
    private static final Map<String, Long> COUNTERS = new LinkedHashMap<>();
    private static final int REPORT_FRAMES = Math.max(1, Integer.getInteger(FRAMES_PROPERTY, DEFAULT_REPORT_FRAMES));
    private static int frames;
    private static long frameStartedAt;

    private AmiRenderProfiler() {
    }

    public static boolean enabled() {
        return ENABLED;
    }

    public static void beginFrame() {
        if (!enabled()) return;
        frameStartedAt = System.nanoTime();
    }

    public static void endFrame() {
        if (!enabled() || frameStartedAt == 0L) return;
        addTime("frame.total", System.nanoTime() - frameStartedAt);
        frameStartedAt = 0L;
        frames++;
        if (frames >= REPORT_FRAMES) {
            reportAndReset();
        }
    }

    public static Section section(String name) {
        if (!enabled()) return NOOP_SECTION;
        return new Section(name, System.nanoTime(), true);
    }

    public static void count(String name) {
        add(name, 1L);
    }

    public static void add(String name, long amount) {
        if (!enabled() || amount == 0L) return;
        COUNTERS.merge(name, amount, Long::sum);
    }

    private static void addTime(String name, long nanos) {
        STATS.computeIfAbsent(name, ignored -> new Stat()).add(nanos);
    }

    private static void reportAndReset() {
        StringBuilder out = new StringBuilder(512);
        out.append("AMI render profiler over ").append(frames).append(" frame(s):");
        for (Map.Entry<String, Stat> entry : STATS.entrySet()) {
            Stat stat = entry.getValue();
            out.append(System.lineSeparator())
                    .append("  ")
                    .append(entry.getKey())
                    .append(" avg=")
                    .append(formatMs(stat.totalNanos / Math.max(1L, stat.samples)))
                    .append("ms max=")
                    .append(formatMs(stat.maxNanos))
                    .append("ms samples=")
                    .append(stat.samples);
        }
        if (!COUNTERS.isEmpty()) {
            out.append(System.lineSeparator()).append("  counters:");
            for (Map.Entry<String, Long> entry : COUNTERS.entrySet()) {
                out.append(System.lineSeparator())
                        .append("    ")
                        .append(entry.getKey())
                        .append(" avg=")
                        .append(formatCount(entry.getValue() / (double) Math.max(1, frames)))
                        .append(" total=")
                        .append(entry.getValue());
            }
        }
        AmiCore.LOGGER.info(out.toString());
        STATS.clear();
        COUNTERS.clear();
        frames = 0;
    }

    private static String formatMs(long nanos) {
        return String.format(java.util.Locale.ROOT, "%.3f", nanos / 1_000_000.0);
    }

    private static String formatCount(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    public static final class Section implements AutoCloseable {
        private final String name;
        private final long startedAt;
        private final boolean active;

        private Section(String name, long startedAt, boolean active) {
            this.name = name;
            this.startedAt = startedAt;
            this.active = active;
        }

        @Override
        public void close() {
            if (!active) return;
            addTime(name, System.nanoTime() - startedAt);
        }
    }

    private static final class Stat {
        private long samples;
        private long totalNanos;
        private long maxNanos;

        private void add(long nanos) {
            samples++;
            totalNanos += nanos;
            maxNanos = Math.max(maxNanos, nanos);
        }
    }
}
