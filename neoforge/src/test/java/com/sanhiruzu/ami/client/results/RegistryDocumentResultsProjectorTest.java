package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.AmiRegistryDocumentIndex;
import com.sanhiruzu.ami.index.RegistryDocument;
import com.sanhiruzu.ami.index.RegistryDocumentKind;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RegistryDocumentResultsProjectorTest {

    private static RegistryDocument doc(RegistryDocumentKind kind, String name) {
        return new RegistryDocument(
                kind,
                new Identifier("minecraft", name.toLowerCase().replace(' ', '_')),
                name,
                "Description of " + name,
                "minecraft",
                List.of(kind.name().toLowerCase())
        );
    }

    @Test
    void blankQueryReturnsEmpty() {
        AmiRegistryDocumentIndex index = new AmiRegistryDocumentIndex(List.of(doc(RegistryDocumentKind.ENCHANTMENT, "Looting")));
        assertTrue(RegistryDocumentResultsProjector.project("", Set.of(RegistryDocumentKind.ENCHANTMENT), index).isEmpty());
        assertTrue(RegistryDocumentResultsProjector.project("  ", Set.of(RegistryDocumentKind.ENCHANTMENT), index).isEmpty());
    }

    @Test
    void nullIndexReturnsEmpty() {
        assertTrue(RegistryDocumentResultsProjector.project("looting", Set.of(RegistryDocumentKind.ENCHANTMENT), null).isEmpty());
    }

    @Test
    void matchingQueryReturnsRow() {
        RegistryDocument looting = doc(RegistryDocumentKind.ENCHANTMENT, "Looting");
        AmiRegistryDocumentIndex index = new AmiRegistryDocumentIndex(List.of(looting));

        List<RegistryDocumentRow> rows = RegistryDocumentResultsProjector.project(
                "looting", Set.of(RegistryDocumentKind.ENCHANTMENT), index);

        assertEquals(1, rows.size());
        assertEquals("Looting", rows.get(0).title());
        assertEquals(looting, rows.get(0).document());
    }

    @Test
    void rowTitleEqualsDisplayName() {
        RegistryDocument effect = doc(RegistryDocumentKind.MOB_EFFECT, "Strength");
        AmiRegistryDocumentIndex index = new AmiRegistryDocumentIndex(List.of(effect));

        List<RegistryDocumentRow> rows = RegistryDocumentResultsProjector.project(
                "strength", Set.of(RegistryDocumentKind.MOB_EFFECT), index);

        assertEquals("Strength", rows.get(0).title());
        assertEquals("Description of Strength", rows.get(0).subtitleLine());
    }

    @Test
    void emptyEnabledKindsReturnsEmpty() {
        RegistryDocument looting = doc(RegistryDocumentKind.ENCHANTMENT, "Looting");
        AmiRegistryDocumentIndex index = new AmiRegistryDocumentIndex(List.of(looting));

        assertTrue(RegistryDocumentResultsProjector.project("looting", Set.of(), index).isEmpty());
    }
}
