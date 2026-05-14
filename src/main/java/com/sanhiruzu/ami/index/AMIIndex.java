package com.sanhiruzu.ami.index;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AMI Index - Client-side only.
 * Holds indexed item data categorized by color, mod origin, material tier, and variant groups.
 */
public class AMIIndex {
    private static final AMIIndex INSTANCE = new AMIIndex();

    private final Map<IndexCategory, Map<String, List<MaterialEntry>>> index;
    private long indexBuildTimeMs;
    private int totalItemsIndexed;

    public AMIIndex() {
        this.index = new HashMap<>();
        for (IndexCategory category : IndexCategory.values()) {
            this.index.put(category, new HashMap<>());
        }
    }

    public static AMIIndex getInstance() {
        return INSTANCE;
    }

    public Map<IndexCategory, Map<String, List<MaterialEntry>>> getIndex() {
        return index;
    }

    public Map<String, List<MaterialEntry>> getCategoryIndex(IndexCategory category) {
        return index.getOrDefault(category, new HashMap<>());
    }

    public void setIndexBuildTime(long timeMs) {
        this.indexBuildTimeMs = timeMs;
    }

    public long getIndexBuildTimeMs() {
        return indexBuildTimeMs;
    }

    public void setTotalItemsIndexed(int count) {
        this.totalItemsIndexed = count;
    }

    public int getTotalItemsIndexed() {
        return totalItemsIndexed;
    }

    public void clear() {
        for (Map<String, List<MaterialEntry>> categoryMap : index.values()) {
            categoryMap.clear();
        }
        totalItemsIndexed = 0;
        indexBuildTimeMs = 0;
    }
}
