package com.sanhiruzu.ami.client;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * A small, bounded cache for frame-adjacent values whose source objects must be compared by identity.
 */
final class TimedIdentityCache<K, V> {
    private final IdentityHashMap<K, Entry<V>> entries = new IdentityHashMap<>();
    private final ArrayDeque<K> insertionOrder = new ArrayDeque<>();
    private final int maximumSize;
    private final long lifetimeMs;
    private final LongSupplier clock;

    TimedIdentityCache(int maximumSize, long lifetimeMs) {
        this(maximumSize, lifetimeMs, System::currentTimeMillis);
    }

    TimedIdentityCache(int maximumSize, long lifetimeMs, LongSupplier clock) {
        if (maximumSize < 1) {
            throw new IllegalArgumentException("maximumSize must be positive");
        }
        if (lifetimeMs < 1) {
            throw new IllegalArgumentException("lifetimeMs must be positive");
        }
        this.maximumSize = maximumSize;
        this.lifetimeMs = lifetimeMs;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    synchronized V getOrCompute(K key, Supplier<V> resolver) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(resolver, "resolver");

        long now = clock.getAsLong();
        Entry<V> cached = entries.get(key);
        if (cached != null && now < cached.expiresAt()) {
            return cached.value();
        }

        V value = Objects.requireNonNull(resolver.get(), "resolver result");
        entries.put(key, new Entry<>(value, now + lifetimeMs));
        if (cached == null) {
            insertionOrder.addLast(key);
        }
        while (entries.size() > maximumSize) {
            entries.remove(insertionOrder.removeFirst());
        }
        return value;
    }

    synchronized void clear() {
        entries.clear();
        insertionOrder.clear();
    }

    private record Entry<V>(V value, long expiresAt) {
    }
}
