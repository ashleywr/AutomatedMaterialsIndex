package com.sanhiruzu.ami.index;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FacetCodecTest {

    @Test
    void encodeAndDecodeRoundTripStableIds() {
        EnumSet<ItemFacet> input = EnumSet.of(
                ItemFacet.EDIBLE,
                ItemFacet.PLACEABLE,
                ItemFacet.REDSTONE_SIGNAL
        );

        String encoded = FacetCodec.encode(input);

        assertEquals("edible,placeable,redstone_signal", encoded);
        assertEquals(input, FacetCodec.decode(encoded));
    }

    @Test
    void decodeIgnoresUnknownFacetIds() {
        EnumSet<ItemFacet> decoded = FacetCodec.decode("edible,unknown_fact,placeable");

        assertEquals(EnumSet.of(ItemFacet.EDIBLE, ItemFacet.PLACEABLE), decoded);
        assertTrue(FacetCodec.decode("").isEmpty());
    }
}
