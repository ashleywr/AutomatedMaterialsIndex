package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StructureProviderTest {
    @Test
    void createStructureNodeCarriesEnvironmentMetadata() {
        SearchNode node = StructureProvider.createStructureNode(
                ResourceLocation.fromNamespaceAndPath("minecraft", "trial_chambers"));

        assertEquals(NodeType.STRUCTURE, node.type());
        assertEquals("Trial Chambers Structure", node.displayName());
        assertEquals("minecraft", node.meta(SearchNodeKeys.MOD_ID, ""));
        assertEquals(AmiOntology.ENVIRONMENT.id, node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, ""));
        assertEquals("structures", node.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, ""));
    }
}
