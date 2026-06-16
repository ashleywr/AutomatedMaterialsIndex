package com.sanhiruzu.ami.index;

import net.minecraft.resources.Identifier;
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

    @Test
    void repeatedClassificationUsesCachedNodeScopeResult() {
        AmiOntologyKinds.clearClassificationCacheForTests();
        SearchNode pressurePlate = item("minecraft", "oak_pressure_plate");

        assertTrue(AmiOntologyKinds.classify(pressurePlate, "masonry", "redstone").isPresent());
        assertEquals(1, AmiOntologyKinds.cachedClassificationCountForTests());

        assertTrue(AmiOntologyKinds.classify(pressurePlate, "masonry", "redstone").isPresent());
        assertEquals(1, AmiOntologyKinds.cachedClassificationCountForTests());
    }

    @Test
    void backpackKindIgnoresIncidentalTrashBagBlacklistTags() {
        SearchNode magnet = item("simplemagnets", "basicmagnet", Map.of(
                SearchNodeKeys.FACETS, "curio",
                SearchNodeKeys.TAGS, "curios:charm,furniture:trash_bag_blacklist"
        ));

        Optional<AmiOntologyKinds.Kind> kind = AmiOntologyKinds.classify(magnet, "armor", "curios");

        assertTrue(kind.isEmpty() || !"backpacks".equals(kind.get().id()),
                "trash_bag_blacklist must not make unrelated curios look like bags");
    }

    @Test
    void backpackKindStillMatchesRealBackpackPathTokens() {
        SearchNode backpack = item("sophisticatedbackpacks", "diamond_backpack", Map.of(
                SearchNodeKeys.FACETS, "curio"
        ));

        Optional<AmiOntologyKinds.Kind> kind = AmiOntologyKinds.classify(backpack, "armor", "curios");

        assertTrue(kind.isPresent());
        assertEquals("backpacks", kind.get().id());
    }

    @Test
    void kindRulesIgnoreIncidentalBlockPlacementTags() {
        SearchNode grassBlock = item("minecraft", "grass_block", Map.of(
                SearchNodeKeys.FACETS, "placeable",
                SearchNodeKeys.BLOCK_TAGS, "atmospheric:yucca_flower_placeable,minecraft:dirt"
        ));

        Optional<AmiOntologyKinds.Kind> kind = AmiOntologyKinds.classify(grassBlock, "nature", "flora");

        assertTrue(kind.isEmpty() || !"flowers".equals(kind.get().id()),
                "placement tags must not make substrate blocks look like flowers");
    }

    @Test
    void necklaceKindIgnoresIncidentalCharmNamespaceTags() {
        SearchNode hook = item("rehooked", "wood_hook", Map.of(
                SearchNodeKeys.FACETS, "curio",
                SearchNodeKeys.TAGS, "farm_and_charm:hangable,curios:hook"
        ));

        Optional<AmiOntologyKinds.Kind> kind = AmiOntologyKinds.classify(hook, "armor", "curios");

        assertTrue(kind.isEmpty(), "farm_and_charm tags must not make hooks look like necklaces");
    }

    private static SearchNode item(String namespace, String path) {
        return item(namespace, path, Map.of());
    }

    private static SearchNode item(String namespace, String path, Map<String, String> metadata) {
        return new SearchNode(new Identifier(namespace, path), NodeType.ITEM, path, 0, 0, metadata);
    }
}
