package com.sanhiruzu.ami.index;

import java.util.EnumSet;
import java.util.StringJoiner;

public final class FacetCodec {
    private FacetCodec() {}

    public static String encode(EnumSet<ItemFacet> facets) {
        if (facets == null || facets.isEmpty()) return "";

        StringJoiner joiner = new StringJoiner(",");
        for (ItemFacet facet : facets) {
            joiner.add(facet.id());
        }
        return joiner.toString();
    }

    public static EnumSet<ItemFacet> decode(String encoded) {
        EnumSet<ItemFacet> facets = EnumSet.noneOf(ItemFacet.class);
        if (encoded == null || encoded.isBlank()) return facets;

        for (String raw : encoded.split(",")) {
            ItemFacet facet = ItemFacet.byId(raw.trim().toLowerCase(java.util.Locale.ROOT));
            if (facet != null) {
                facets.add(facet);
            }
        }
        return facets;
    }
}
