package com.sanhiruzu.ami.index;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
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

    @Test
    void readPreservesEncodedOrder() {
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put(SearchNodeKeys.SEMANTIC_VERBS, "stores_items,sleep_rest");

        assertEquals(
                List.of(SemanticVerb.STORES_ITEMS, SemanticVerb.SLEEP_REST),
                List.copyOf(SemanticVerbCodec.read(meta))
        );
    }

    @Test
    void evidenceDelimitersAreSanitized() {
        Map<String, String> meta = new LinkedHashMap<>();

        SemanticVerbCodec.add(meta, SemanticVerb.SLEEP_REST, " block,class|Dog=Bed ");

        assertEquals("sleep_rest=block;class;Dog:Bed", meta.get(SearchNodeKeys.SEMANTIC_VERB_EVIDENCE));
    }

    @Test
    void firstEvidenceWinsForDuplicateVerb() {
        Map<String, String> meta = new LinkedHashMap<>();

        SemanticVerbCodec.add(meta, SemanticVerb.SLEEP_REST, "block_class:DogBedBlock");
        SemanticVerbCodec.add(meta, SemanticVerb.SLEEP_REST, "block_class:OtherBedBlock");

        assertEquals("sleep_rest=block_class:DogBedBlock", meta.get(SearchNodeKeys.SEMANTIC_VERB_EVIDENCE));
    }

    @Test
    void removingLastVerbRemovesMetadataKeys() {
        Map<String, String> meta = new LinkedHashMap<>();
        SemanticVerbCodec.add(meta, SemanticVerb.SLEEP_REST, "block_class:DogBedBlock");

        SemanticVerbCodec.remove(meta, SemanticVerb.SLEEP_REST);

        assertFalse(meta.containsKey(SearchNodeKeys.SEMANTIC_VERBS));
        assertFalse(meta.containsKey(SearchNodeKeys.SEMANTIC_VERB_EVIDENCE));
    }
}
