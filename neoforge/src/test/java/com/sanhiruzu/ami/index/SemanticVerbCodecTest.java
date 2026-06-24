package com.sanhiruzu.ami.index;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticVerbCodecTest {

    @Test
    void addVerbDeDuplicatesAndRecordsEvidence() {
        Map<String, String> meta = new LinkedHashMap<>();

        SemanticVerbCodec.add(meta, SemanticVerb.SLEEP_REST, "block_class:DogBedBlock");
        SemanticVerbCodec.add(meta, SemanticVerb.SLEEP_REST, "block_class:DogBedBlock");
        SemanticVerbCodec.add(meta, SemanticVerb.STORES_ITEMS, "component:container");

        assertEquals("sleep_rest,stores_items", meta.get(SearchNodeKeys.SEMANTIC_VERBS));
        assertEquals(
                "sleep_rest=block_class:DogBedBlock|stores_items=component:container",
                meta.get(SearchNodeKeys.SEMANTIC_VERB_EVIDENCE)
        );
        assertTrue(SemanticVerbCodec.has(meta, SemanticVerb.SLEEP_REST));
        assertTrue(SemanticVerbCodec.has(meta, SemanticVerb.STORES_ITEMS));
    }

    @Test
    void removeVerbUpdatesVerbListAndEvidence() {
        Map<String, String> meta = new LinkedHashMap<>();
        SemanticVerbCodec.add(meta, SemanticVerb.SLEEP_REST, "block_class:DogBedBlock");
        SemanticVerbCodec.add(meta, SemanticVerb.STORES_ITEMS, "component:container");

        SemanticVerbCodec.remove(meta, SemanticVerb.SLEEP_REST);

        assertEquals(Set.of(SemanticVerb.STORES_ITEMS), SemanticVerbCodec.read(meta));
        assertFalse(SemanticVerbCodec.has(meta, SemanticVerb.SLEEP_REST));
        assertEquals("stores_items=component:container", meta.get(SearchNodeKeys.SEMANTIC_VERB_EVIDENCE));
    }

    @Test
    void unknownVerbIdsAreIgnoredWhenReading() {
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put(SearchNodeKeys.SEMANTIC_VERBS, "sleep_rest,not_a_real_verb,stores_items");

        assertEquals(Set.of(SemanticVerb.SLEEP_REST, SemanticVerb.STORES_ITEMS), SemanticVerbCodec.read(meta));
    }
}
