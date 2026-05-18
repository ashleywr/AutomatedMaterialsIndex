package it.unimi.dsi.fastutil.objects;

public interface Object2FloatMap<K> {
    boolean containsKey(Object key);
    float put(K key, float value);
}
