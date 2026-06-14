package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RegistryDocumentIndexTest {

    private static RegistryDocument enchantment(String id, String name, String description, List<String> tokens) {
        return new RegistryDocument(
                RegistryDocumentKind.ENCHANTMENT,
                new ResourceLocation("minecraft", id),
                name,
                description,
                "minecraft",
                tokens
        );
    }

    private static RegistryDocument tag(String namespace, String path) {
        return new RegistryDocument(
                RegistryDocumentKind.TAG,
                new ResourceLocation(namespace, path),
                "#" + namespace + ":" + path,
                "item tag - 3 members",
                namespace,
                List.of("tag", "#" + namespace + ":" + path, path)
        );
    }

    @Test
    void queryMatchesDisplayName() {
        RegistryDocument looting = enchantment("looting", "Looting", "Increases mob drops. Max level: 3.", List.of("enchantment", "sword"));
        AmiRegistryDocumentIndex index = new AmiRegistryDocumentIndex(List.of(looting));

        List<RegistryDocument> results = index.query("looting", Set.of(RegistryDocumentKind.ENCHANTMENT));
        assertEquals(List.of(looting), results);
    }

    @Test
    void queryMatchesSearchToken() {
        RegistryDocument looting = enchantment("looting", "Looting", "Increases mob drops.", List.of("enchantment", "sword"));
        RegistryDocument efficiency = enchantment("efficiency", "Efficiency", "Faster mining.", List.of("enchantment", "pickaxe", "axe", "shovel"));
        AmiRegistryDocumentIndex index = new AmiRegistryDocumentIndex(List.of(looting, efficiency));

        List<RegistryDocument> results = index.query("sword", Set.of(RegistryDocumentKind.ENCHANTMENT));
        assertEquals(List.of(looting), results);
    }

    @Test
    void kindFilterExcludesOtherKinds() {
        RegistryDocument looting = enchantment("looting", "Looting", "Sword enchantment.", List.of("enchantment", "sword"));
        RegistryDocument strengthTag = tag("minecraft", "swords");
        AmiRegistryDocumentIndex index = new AmiRegistryDocumentIndex(List.of(looting, strengthTag));

        List<RegistryDocument> enchantOnly = index.query("sword", Set.of(RegistryDocumentKind.ENCHANTMENT));
        assertEquals(List.of(looting), enchantOnly);

        List<RegistryDocument> tagOnly = index.query("sword", Set.of(RegistryDocumentKind.TAG));
        assertEquals(List.of(strengthTag), tagOnly);
    }

    @Test
    void blankQueryReturnsEmpty() {
        RegistryDocument looting = enchantment("looting", "Looting", "Increases mob drops.", List.of("enchantment"));
        AmiRegistryDocumentIndex index = new AmiRegistryDocumentIndex(List.of(looting));

        assertTrue(index.query("", Set.of(RegistryDocumentKind.ENCHANTMENT)).isEmpty());
        assertTrue(index.query("  ", Set.of(RegistryDocumentKind.ENCHANTMENT)).isEmpty());
    }

    @Test
    void emptyKindSetReturnsEmpty() {
        RegistryDocument looting = enchantment("looting", "Looting", "Increases mob drops.", List.of("enchantment"));
        AmiRegistryDocumentIndex index = new AmiRegistryDocumentIndex(List.of(looting));

        assertTrue(index.query("looting", Set.of()).isEmpty());
    }

    @Test
    void dollarPrefixKindTokenFilters() {
        RegistryDocument looting = enchantment("looting", "Looting", "Mob drops.", List.of("enchantment"));
        RegistryDocument swords = tag("minecraft", "swords");
        AmiRegistryDocumentIndex index = new AmiRegistryDocumentIndex(List.of(looting, swords));

        // $enchantment strips to "enchantment" — matches the baked token in enchantment docs
        List<RegistryDocument> results = index.query("$enchantment", Set.of(RegistryDocumentKind.ENCHANTMENT, RegistryDocumentKind.TAG));
        assertEquals(List.of(looting), results);
    }

    @Test
    void hashPrefixMatchesTagId() {
        RegistryDocument ores = tag("forge", "ores/gold");
        RegistryDocument planks = tag("minecraft", "planks");
        AmiRegistryDocumentIndex index = new AmiRegistryDocumentIndex(List.of(ores, planks));

        List<RegistryDocument> results = index.query("#forge:ores/gold", Set.of(RegistryDocumentKind.TAG));
        assertEquals(List.of(ores), results);
    }

    @Test
    void tildeMatchesGameRule() {
        RegistryDocument keepInventory = new RegistryDocument(
                RegistryDocumentKind.GAME_RULE,
                new ResourceLocation("minecraft", "keepInventory"),
                "keepInventory",
                "Whether players keep their inventory after dying. Default: false",
                "minecraft",
                List.of("game rule", "boolean", "keepInventory")
        );
        AmiRegistryDocumentIndex index = new AmiRegistryDocumentIndex(List.of(keepInventory));

        List<RegistryDocument> results = index.query("~keepInventory", Set.of(RegistryDocumentKind.GAME_RULE));
        assertEquals(List.of(keepInventory), results);
    }

    @Test
    void allDocumentsAreAccessible() {
        RegistryDocument looting = enchantment("looting", "Looting", "Mob drops.", List.of("enchantment"));
        RegistryDocument ores = tag("forge", "ores");
        AmiRegistryDocumentIndex index = new AmiRegistryDocumentIndex(List.of(looting, ores));

        assertEquals(2, index.allDocuments().size());
    }
}
