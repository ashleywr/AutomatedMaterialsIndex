package com.sanhiruzu.ami.client;

/**
 * Cross-loader progress snapshot for entity icon atlas warmup. Loader renderers
 * own rendering, but profiling scripts need one stable xplat place to read
 * completion state.
 */
public final class EntityIconWarmupMetrics {
    private static long revision = -1L;
    private static int total;
    private static int visited;
    private static int queuedOrCached;
    private static int skipped;
    private static int renderFailures;

    private EntityIconWarmupMetrics() {
    }

    public static synchronized void reset(long newRevision, int newTotal) {
        revision = newRevision;
        total = Math.max(0, newTotal);
        visited = 0;
        queuedOrCached = 0;
        skipped = 0;
        renderFailures = 0;
    }

    public static synchronized void recordQueuedOrCached() {
        visited++;
        queuedOrCached++;
    }

    public static synchronized void recordSkipped() {
        visited++;
        skipped++;
    }

    public static synchronized void recordRenderFailure() {
        visited++;
        renderFailures++;
    }

    public static synchronized Snapshot snapshot() {
        return new Snapshot(revision, total, visited, queuedOrCached, skipped, renderFailures);
    }

    public record Snapshot(long revision, int total, int visited, int queuedOrCached, int skipped, int renderFailures) {
        public boolean done() {
            return visited >= total;
        }

        public int remaining() {
            return Math.max(0, total - visited);
        }
    }
}
