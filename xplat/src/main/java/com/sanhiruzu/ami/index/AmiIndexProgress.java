package com.sanhiruzu.ami.index;

public record AmiIndexProgress(
        boolean active,
        String phase,
        String detail,
        int current,
        int total,
        long startedAtMs
) {
    private static final AmiIndexProgress IDLE = new AmiIndexProgress(false, "Ready", "", 0, 0, 0L);

    public static AmiIndexProgress idle() {
        return IDLE;
    }

    public static AmiIndexProgress start(String phase) {
        return start(phase, "", 0);
    }

    public static AmiIndexProgress start(String phase, String detail, int total) {
        return new AmiIndexProgress(true, clean(phase), clean(detail), 0, Math.max(0, total), System.currentTimeMillis());
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public AmiIndexProgress withProgress(int current) {
        if (!active) return this;
        return new AmiIndexProgress(true, phase, detail, Math.max(0, current), total, startedAtMs);
    }

    public AmiIndexProgress withDetail(String detail) {
        if (!active) return this;
        return new AmiIndexProgress(true, phase, clean(detail), current, total, startedAtMs);
    }

    public int percent() {
        if (total <= 0) return -1;
        int bounded = Math.max(0, Math.min(current, total));
        return (int) Math.round((bounded * 100.0D) / total);
    }

    public long elapsedMs() {
        return active && startedAtMs > 0L ? Math.max(0L, System.currentTimeMillis() - startedAtMs) : 0L;
    }

    public String message() {
        if (!active) return phase;
        StringBuilder out = new StringBuilder(phase);
        int pct = percent();
        if (pct >= 0) {
            out.append(" ").append(pct).append("%");
            if (total > 0) {
                out.append(" (").append(Math.max(0, Math.min(current, total))).append('/').append(total).append(')');
            }
        }
        if (!detail.isBlank()) {
            out.append(" - ").append(detail);
        }
        long elapsed = elapsedMs();
        if (elapsed >= 1000L) {
            out.append(" - ").append(elapsed / 1000L).append('s');
        }
        return out.toString();
    }
}
