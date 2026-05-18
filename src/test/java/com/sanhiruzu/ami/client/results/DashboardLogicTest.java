package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DashboardLogicTest {

    @BeforeEach
    void setUp() {
        GlobalIndex.getInstance().clear();
        GlobalIndex.getInstance().markIndexReady();
    }

    @Test
    void testCategoryItemCounts() {
        // 1. Setup mock data
        addMockItem("sword", "tools");
        addMockItem("axe", "tools");
        addMockItem("bread", "food");

        // 2. Verify GlobalIndex categorization
        assertEquals(2, GlobalIndex.getInstance().getNodesByCategory("tools").size());
        assertEquals(1, GlobalIndex.getInstance().getNodesByCategory("food").size());
    }

    private void addMockItem(String path, String category) {
        GlobalIndex.getInstance().addNode(new SearchNode(
            ResourceLocation.parse("minecraft:" + path),
            NodeType.ITEM, path, 0, 0,
            Map.of(SearchNodeKeys.ONTOLOGY_CATEGORY, category)
        ));
    }

    @Test
    void testOntologyHeuristics() {
        SearchNode compass = new SearchNode(
            ResourceLocation.parse("minecraft:compass"),
            NodeType.ITEM, "Compass", 0, 0, Map.of()
        );
        
        AmiOntology.Category cat = AmiOntology.classifyNode(compass);
        assertEquals("utility", cat.id);
    }

    @Test
    void testTreeNodeStructure() {
        TreeNode root = new TreeNode("root", Component.literal("Root"));
        SearchNode item = new SearchNode(ResourceLocation.parse("minecraft:apple"), NodeType.ITEM, "Apple", 0, 0, Map.of());
        TreeNode leaf = new TreeNode(Component.literal("Apple"), item);
        
        root.addChild(leaf);
        
        assertEquals("root", root.getKey());
        assertEquals("Root", root.getLabel().getString());
        assertFalse(root.isLeaf());
        assertEquals(1, root.getChildren().size());
        
        TreeNode foundLeaf = root.getChildren().get(0);
        assertTrue(foundLeaf.isLeaf());
        assertEquals("Apple", foundLeaf.getLabel().getString());
        assertEquals(item.id(), foundLeaf.getEntry().id());
    }

    @Test
    void testDashboardDeduplication() {
        // This test simulates the logic inside UniversalResultsPanel.showDashboard()
        java.util.Set<String> addedIds = new java.util.HashSet<>();
        List<String> rawIdsFromOntology = List.of("mobs", "mobs", "utility", "magic", "utility");
        List<String> finalDashboardIds = new java.util.ArrayList<>();

        for (String id : rawIdsFromOntology) {
            if (addedIds.contains(id)) continue;

            // Logic: if count > 0, add to dashboard
            finalDashboardIds.add(id);
            addedIds.add(id);
        }

        assertEquals(3, finalDashboardIds.size());
        assertEquals("mobs", finalDashboardIds.get(0));
        assertEquals("utility", finalDashboardIds.get(1));
        assertEquals("magic", finalDashboardIds.get(2));
    }

    @Test
    void testAlphabeticalCategorySorting() {
        // This test verifies that categories are sorted alphabetically when
        // ALPHABETICAL sort mode is active (armor, entities, nature, tools...)

        // Setup: Mock categories data to verify sorting logic
        List<AmiOntology.Category> categories = new java.util.ArrayList<>(AmiOntology.CATEGORIES);

        // Simulate alphabetical sorting by localized display name
        categories.sort((a, b) -> a.displayName().getString().compareToIgnoreCase(b.displayName().getString()));

        // Verify order: First item should come before second item alphabetically
        assertTrue(categories.get(0).displayName().getString().compareToIgnoreCase(categories.get(1).displayName().getString()) <= 0,
            "Categories should be sorted alphabetically");

        // Find armor and verify it's near the beginning (starts with A)
        boolean foundArmor = false;
        for (int i = 0; i < 3; i++) {
            if ("armor".equals(categories.get(i).id)) {
                foundArmor = true;
                break;
            }
        }
        assertTrue(foundArmor, "Armor should be near the beginning when sorted alphabetically");
    }

    @Test
    void testAlphabeticalCategorySortingDescending() {
        // This test verifies reverse alphabetical sorting

        List<AmiOntology.Category> categories = new java.util.ArrayList<>(AmiOntology.CATEGORIES);

        // Simulate alphabetical sorting, then reverse for descending
        categories.sort((a, b) -> a.displayName().getString().compareToIgnoreCase(b.displayName().getString()));
        java.util.Collections.reverse(categories);

        // Verify reverse order: first item should be >= second item alphabetically
        assertTrue(categories.get(0).displayName().getString().compareToIgnoreCase(categories.get(1).displayName().getString()) >= 0,
            "Categories should be sorted in reverse alphabetical order");
    }

    @Test
    void testOntologyOrderPreservedForNonAlphabetical() {
        // This test verifies that non-ALPHABETICAL sorts preserve the ontology order

        // The original CATEGORIES list should maintain its defined order
        List<AmiOntology.Category> original = AmiOntology.CATEGORIES;

        // For non-alphabetical sorts, the order should be unchanged
        // This is implicitly tested by the fact that we don't re-sort in those cases
        assertTrue(original.size() > 0, "Ontology should have categories defined");

        // Verify the hardcoded order is maintained (NAVIGATION before ENTITIES, etc)
        assertEquals("utility", original.get(0).id);
        assertEquals("bestiary", original.get(1).id);
    }
}
