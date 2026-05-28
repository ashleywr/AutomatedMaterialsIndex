package it.unimi.dsi.fastutil.objects;

import java.util.LinkedHashMap;
import java.util.Map;

public class Object2FloatOpenHashMap<K> implements Object2FloatMap<K> {
    private final Map<K, Float> values = new LinkedHashMap<>();

    @Override
    public boolean containsKey(Object key) {
        return values.containsKey(key);
    }

    @Override
    public float put(K key, float value) {
        Float previous = values.put(key, value);
        return previous != null ? previous : 0.0f;
    }
}
