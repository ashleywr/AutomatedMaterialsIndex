package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.AmiCore;

/**
 * Tracks AMI overlay render phases so durable result content cannot drift into
 * the transient tooltip phase unnoticed.
 */
public final class AmiRenderPhase {
    private static final String STRICT_PROPERTY = "ami." + "strictRenderPhase";
    private static final boolean STRICT = Boolean.getBoolean(STRICT_PROPERTY);
    private static final ThreadLocal<Phase> CURRENT = ThreadLocal.withInitial(() -> Phase.NONE);
    private static boolean warnedUnexpectedBaseRender;

    public enum Phase {
        NONE,
        BASE,
        TOP
    }

    private AmiRenderPhase() {
    }

    public static Scope enter(Phase phase) {
        Phase previous = CURRENT.get();
        CURRENT.set(phase);
        return new Scope(previous);
    }

    public static Phase current() {
        return CURRENT.get();
    }

    public static void requireBase(String rendererName) {
        if (CURRENT.get() != Phase.TOP) {
            return;
        }

        AmiRenderProfiler.count("renderPhase.violation." + rendererName);
        String message = rendererName + " attempted to render durable result content during AMI top-layer rendering";
        if (STRICT) {
            throw new IllegalStateException(message);
        }
        if (!warnedUnexpectedBaseRender) {
            warnedUnexpectedBaseRender = true;
            AmiCore.LOGGER.warn("{}; ignoring once-per-session warning. Use -D{}=true to make this fatal.",
                    message, STRICT_PROPERTY);
        }
    }

    public static final class Scope implements AutoCloseable {
        private final Phase previous;
        private boolean closed;

        private Scope(Phase previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            CURRENT.set(previous);
            closed = true;
        }
    }
}
