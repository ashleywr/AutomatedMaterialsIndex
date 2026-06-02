package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmiOntologyKindsTokenTest {
    @Test
    void kindRulesDoNotMatchPartialPathTokens() {
        SearchNode gearing = item("example", "copper_gearing");

        Optional<AmiOntologyKinds.Kind> partsKind = AmiOntologyKinds.classify(gearing, "tech", "parts");

        assertTrue(partsKind.isEmpty(), "gearing should not match the shorter gear token in tech parts");
    }

    @Test
    void kindRulesStillMatchExplicitPhraseTokens() {
        SearchNode pressurePlate = item("minecraft", "oak_pressure_plate");

        Optional<AmiOntologyKinds.Kind> redstoneKind = AmiOntologyKinds.classify(pressurePlate, "masonry", "redstone");

        assertTrue(redstoneKind.isPresent());
        assertEquals("pressure_plates", redstoneKind.get().id());
    }

    private static SearchNode item(String namespace, String path) {
        return new SearchNode(new ResourceLocation(namespace, path), NodeType.ITEM, path, 0, 0, Map.of());
    }
}
