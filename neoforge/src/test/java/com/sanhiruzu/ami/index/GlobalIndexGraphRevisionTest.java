package com.sanhiruzu.ami.index;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalIndexGraphRevisionTest {
    @Test
    void graphEdgeUpdatesCanAdvanceIndexRevision() {
        GlobalIndex index = GlobalIndex.getInstance();
        index.clear();
        long before = index.revision();

        index.markGraphChanged();

        assertTrue(index.revision() > before);
    }
}
