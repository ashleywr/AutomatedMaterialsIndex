package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GeneratedVariantCollapseCompatTest {

    @Test
    void syntheticComponentVariantsCollapseToBaseItem() {
        Map<String, String> meta = new HashMap<>();

        GeneratedVariantCollapseCompat.enrichItem(
                new ResourceLocation("pastel", "artists_palette/variant/artist_s_palette_a4fd27a1b4b4"),
                meta
        );

        assertEquals("pastel:artists_palette", meta.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Artists Palette", meta.get(SearchNodeKeys.COLLAPSE_LABEL));
        assertEquals("default_collapsed", meta.get(SearchNodeKeys.VARIANT_COLLAPSE_MODE));
    }

    @Test
    void registeredVisualStateSiblingsCollapseToBaseItem() {
        Map<String, String> inventory = new HashMap<>();
        GeneratedVariantCollapseCompat.enrichItem(new ResourceLocation("alexscaves", "dreadbow_inventory"), inventory);
        assertEquals("alexscaves:dreadbow", inventory.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Dreadbow", inventory.get(SearchNodeKeys.COLLAPSE_LABEL));

        Map<String, String> pulling = new HashMap<>();
        GeneratedVariantCollapseCompat.enrichItem(
                new ResourceLocation("alexscaves", "dreadbow_pulling_2_inventory"),
                pulling
        );
        assertEquals("alexscaves:dreadbow", pulling.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Dreadbow", pulling.get(SearchNodeKeys.COLLAPSE_LABEL));

        Map<String, String> hand = new HashMap<>();
        GeneratedVariantCollapseCompat.enrichItem(new ResourceLocation("alexsmobs", "stink_ray_empty_hand"), hand);
        assertEquals("alexsmobs:stink_ray", hand.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Stink Ray", hand.get(SearchNodeKeys.COLLAPSE_LABEL));
    }

    @Test
    void normalChargedAndEmptyItemsDoNotCollapseByNameAlone() {
        Map<String, String> meta = new HashMap<>();

        GeneratedVariantCollapseCompat.enrichItem(new ResourceLocation("create", "empty_blaze_burner"), meta);

        assertFalse(meta.containsKey(SearchNodeKeys.COLLAPSE_FAMILY));
    }

    @Test
    void guideBooksDoNotAutoCollapse() {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.FACETS, "book,guide_book");

        GeneratedVariantCollapseCompat.enrichItem(new ResourceLocation("patchouli", "guide_book/variant/book_abc"), meta);

        assertFalse(meta.containsKey(SearchNodeKeys.COLLAPSE_FAMILY));
    }
}
