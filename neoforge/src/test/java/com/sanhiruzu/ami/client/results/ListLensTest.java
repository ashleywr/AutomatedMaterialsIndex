package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListLensTest {
    @BeforeEach
    void setUp() {
        GlobalIndex.getInstance().clear();
        GlobalIndex.getInstance().markIndexReady();
        RowFieldConfig.setSubtitleFields(ListLens.ALL.subtitleFields());
    }

    @Test
    void weaponLensFiltersAndAppliesRankingDefaults() {
        SearchState state = new SearchState();
        state.setViewMode(ResultsToolbar.ViewMode.LIST);
        state.setListLens(ListLens.WEAPONS);

        ResultsViewProjector.Projection projection = ResultsViewProjector.project(List.of(
                item("stone", "Stone", Map.of(SearchNodeKeys.FACETS, "placeable")),
                item("iron_sword", "Iron Sword", Map.of(SearchNodeKeys.FACETS, "melee_weapon", SearchNodeKeys.DPS, "9.6")),
                item("oak_chest", "Oak Chest", Map.of(SearchNodeKeys.FACETS, "storage", SearchNodeKeys.ESM_CAPACITY, "1728"))
        ), state, null, false, false);

        assertEquals(1, projection.displayedItemCount());
        assertEquals("Iron Sword", projection.roots().get(0).getLabel().getString());
        assertEquals(ResultsProcessor.SortField.DPS, state.getSortField());
        assertEquals(ResultsProcessor.GroupBy.NONE, state.getGroupBy());
        assertEquals(List.of(RowField.MOD_NAME, RowField.DAMAGE, RowField.DPS), RowFieldConfig.getSubtitleFields());
    }

    @Test
    void weaponLensKeepsSingletonMaterialRootsFlat() {
        SearchState state = new SearchState();
        state.setViewMode(ResultsToolbar.ViewMode.LIST);
        state.setListLens(ListLens.WEAPONS);

        ResultsViewProjector.Projection projection = ResultsViewProjector.project(List.of(
                item("netherite_sword", "Netherite Sword", Map.of(
                        SearchNodeKeys.FACETS, "melee_weapon",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:netherite",
                        SearchNodeKeys.ATTACK_DAMAGE, "8.0",
                        SearchNodeKeys.DPS, "12.8"
                )),
                item("netherite_hoe", "Netherite Hoe", Map.of(
                        SearchNodeKeys.FACETS, "melee_weapon",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:netherite",
                        SearchNodeKeys.ATTACK_DAMAGE, "1.0",
                        SearchNodeKeys.DPS, "4.0"
                )),
                item("netherite_pickaxe", "Netherite Pickaxe", Map.of(
                        SearchNodeKeys.FACETS, "melee_weapon",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:netherite",
                        SearchNodeKeys.ATTACK_DAMAGE, "6.0",
                        SearchNodeKeys.DPS, "7.2"
                )),
                item("netherite_shovel", "Netherite Shovel", Map.of(
                        SearchNodeKeys.FACETS, "melee_weapon",
                        SearchNodeKeys.SUBTYPE_OF, "minecraft:netherite",
                        SearchNodeKeys.ATTACK_DAMAGE, "6.5",
                        SearchNodeKeys.DPS, "6.5"
                )),
                item("trident", "Trident", Map.of(
                        SearchNodeKeys.FACETS, "melee_weapon,ranged_weapon",
                        SearchNodeKeys.MATERIAL_GROUP, "minecraft:trident",
                        SearchNodeKeys.ATTACK_DAMAGE, "9.0",
                        SearchNodeKeys.DPS, "9.9"
                )),
                item("netherite_axe", "Netherite Axe", Map.of(
                        SearchNodeKeys.FACETS, "melee_weapon",
                        SearchNodeKeys.MATERIAL_GROUP, "minecraft:netherite",
                        SearchNodeKeys.ATTACK_DAMAGE, "10.0",
                        SearchNodeKeys.DPS, "10.0"
                ))
        ), state, null, false, false);

        assertEquals(6, projection.displayedItemCount());
        assertTrue(projection.roots().stream().allMatch(TreeNode::isLeaf));
        assertFalse(projection.roots().stream().anyMatch(node -> "Netherite".equals(node.getLabel().getString())));
        assertEquals("Netherite Sword", projection.roots().get(0).getLabel().getString());
        assertEquals("Netherite Axe", projection.roots().get(1).getLabel().getString());
        assertEquals("Trident", projection.roots().get(2).getLabel().getString());
    }

    @Test
    void storageLensKeepsStorageFacetItemsWithoutCapacity() {
        SearchState state = new SearchState();
        state.setViewMode(ResultsToolbar.ViewMode.LIST);
        state.setListLens(ListLens.STORAGE);

        ResultsViewProjector.Projection projection = ResultsViewProjector.project(List.of(
                item("iron_sword", "Iron Sword", Map.of(SearchNodeKeys.FACETS, "melee_weapon", SearchNodeKeys.DPS, "9.6")),
                item("oak_chest", "Oak Chest", Map.of(SearchNodeKeys.FACETS, "storage")),
                item("diamond_backpack", "Diamond Backpack", Map.of(SearchNodeKeys.ESM_CAPACITY, "6912"))
        ), state, null, false, false);

        assertEquals(2, projection.displayedItemCount());
        assertTrue(projection.summary().contains("lens=STORAGE"));
        assertEquals(ResultsProcessor.SortField.STORAGE_CAPACITY, state.getSortField());
        assertEquals(ResultsProcessor.GroupBy.NONE, state.getGroupBy());
        assertEquals(List.of(RowField.MOD_NAME, RowField.STORAGE_CAPACITY), RowFieldConfig.getSubtitleFields());
    }

    @Test
    void focusSortOptionsOnlyExposeRelevantFields() {
        assertEquals(List.of(
                ResultsProcessor.SortField.ARMOR_DEFENSE,
                ResultsProcessor.SortField.ARMOR_TOUGHNESS,
                ResultsProcessor.SortField.ALPHABETICAL,
                ResultsProcessor.SortField.MOD,
                ResultsProcessor.SortField.COLOR
        ), ListLens.ARMOR.sortFields());

        assertEquals(List.of(
                ResultsProcessor.SortField.DPS,
                ResultsProcessor.SortField.DAMAGE,
                ResultsProcessor.SortField.ALPHABETICAL,
                ResultsProcessor.SortField.MOD
        ), ListLens.RANGED.sortFields());

        assertEquals(List.of(
                ResultsProcessor.SortField.TOOL_SPEED,
                ResultsProcessor.SortField.TOOL_USES,
                ResultsProcessor.SortField.DAMAGE,
                ResultsProcessor.SortField.ALPHABETICAL,
                ResultsProcessor.SortField.MOD
        ), ListLens.TOOLS.sortFields());

        assertEquals(List.of(
                ResultsProcessor.SortField.FLUID_CAPACITY,
                ResultsProcessor.SortField.ALPHABETICAL,
                ResultsProcessor.SortField.MOD
        ), ListLens.FLUIDS.sortFields());

        assertEquals(List.of(
                ResultsProcessor.SortField.STORAGE_CAPACITY,
                ResultsProcessor.SortField.ALPHABETICAL,
                ResultsProcessor.SortField.MOD
        ), ListLens.STORAGE.sortFields());
        assertFalse(ListLens.STORAGE.sortFields().contains(ResultsProcessor.SortField.DAMAGE));
        assertFalse(ListLens.STORAGE.sortFields().contains(ResultsProcessor.SortField.DPS));

        assertEquals(List.of(
                ResultsProcessor.SortField.DAMAGE,
                ResultsProcessor.SortField.HEALTH,
                ResultsProcessor.SortField.ALPHABETICAL,
                ResultsProcessor.SortField.MOD
        ), ListLens.ENTITIES.sortFields());
        assertFalse(ListLens.ENTITIES.sortFields().contains(ResultsProcessor.SortField.STORAGE_CAPACITY));
        assertFalse(ListLens.ENTITIES.sortFields().contains(ResultsProcessor.SortField.DPS));

        assertEquals(List.of(
                ResultsProcessor.SortField.FOOD_NUTRITION,
                ResultsProcessor.SortField.FOOD_SATURATION,
                ResultsProcessor.SortField.ALPHABETICAL,
                ResultsProcessor.SortField.MOD
        ), ListLens.FOOD.sortFields());
        assertFalse(ListLens.FOOD.sortFields().contains(ResultsProcessor.SortField.DAMAGE));

        assertEquals(List.of(
                ResultsProcessor.SortField.ENERGY_GENERATION,
                ResultsProcessor.SortField.ENERGY_CAPACITY,
                ResultsProcessor.SortField.ALPHABETICAL,
                ResultsProcessor.SortField.MOD
        ), ListLens.POWER.sortFields());
        assertFalse(ListLens.POWER.sortFields().contains(ResultsProcessor.SortField.FOOD_NUTRITION));
    }

    @Test
    void practicalFocusesFilterAndApplyMetricDefaults() {
        SearchState state = new SearchState();
        state.setViewMode(ResultsToolbar.ViewMode.LIST);

        List<SearchNode> nodes = List.of(
                item("rifle", "Rifle", Map.of(SearchNodeKeys.FACETS, "ranged_weapon", SearchNodeKeys.AMMO_TYPE, "bullets")),
                item("hammock_green", "Green Hammock Cloth", Map.of(SearchNodeKeys.FACETS, "placeable,has_block_entity")),
                item("diamond_pickaxe", "Diamond Pickaxe", Map.of(SearchNodeKeys.FACETS, "harvest_tool", SearchNodeKeys.TOOL_SPEED, "8")),
                item("netherite_chestplate", "Netherite Chestplate", Map.of(SearchNodeKeys.FACETS, "armor_chest", SearchNodeKeys.ARMOR_DEFENSE, "8", SearchNodeKeys.ARMOR_TOUGHNESS, "3")),
                item("fluid_tank", "Fluid Tank", Map.of(SearchNodeKeys.FACETS, "fluid_container", SearchNodeKeys.FLUID_CAPACITY, "16")),
                item("stone", "Stone", Map.of(SearchNodeKeys.FACETS, "placeable"))
        );

        state.setListLens(ListLens.RANGED);
        assertEquals(1, ResultsViewProjector.project(nodes, state, null, false, false).displayedItemCount());
        assertEquals(ResultsProcessor.SortField.DPS, state.getSortField());
        assertEquals(List.of(RowField.MOD_NAME, RowField.AMMO_TYPE, RowField.DAMAGE, RowField.DPS), RowFieldConfig.getSubtitleFields());

        state.setListLens(ListLens.TOOLS);
        assertEquals(1, ResultsViewProjector.project(nodes, state, null, false, false).displayedItemCount());
        assertEquals(ResultsProcessor.SortField.TOOL_SPEED, state.getSortField());
        assertEquals(List.of(RowField.MOD_NAME, RowField.TOOL_SPEED, RowField.TOOL_USES), RowFieldConfig.getSubtitleFields());

        state.setListLens(ListLens.ARMOR);
        assertEquals(1, ResultsViewProjector.project(nodes, state, null, false, false).displayedItemCount());
        assertEquals(ResultsProcessor.SortField.ARMOR_DEFENSE, state.getSortField());
        assertEquals(List.of(RowField.MOD_NAME, RowField.ARMOR_DEFENSE, RowField.ARMOR_TOUGHNESS), RowFieldConfig.getSubtitleFields());

        state.setListLens(ListLens.FLUIDS);
        assertEquals(1, ResultsViewProjector.project(nodes, state, null, false, false).displayedItemCount());
        assertEquals(ResultsProcessor.SortField.FLUID_CAPACITY, state.getSortField());
        assertEquals(List.of(RowField.MOD_NAME, RowField.FLUID_CAPACITY), RowFieldConfig.getSubtitleFields());
    }

    @Test
    void entityLensFiltersToEntityNodes() {
        SearchState state = new SearchState();
        state.setViewMode(ResultsToolbar.ViewMode.LIST);
        state.setListLens(ListLens.ENTITIES);

        ResultsViewProjector.Projection projection = ResultsViewProjector.project(List.of(
                item("iron_sword", "Iron Sword", Map.of(SearchNodeKeys.FACETS, "melee_weapon")),
                entity("zombie", "Zombie", Map.of(SearchNodeKeys.ENTITY_HEALTH, "20", SearchNodeKeys.ENTITY_ATTACK_DAMAGE, "3")),
                item("stone", "Stone", Map.of(SearchNodeKeys.FACETS, "placeable"))
        ), state, null, false, false);

        assertEquals(1, projection.displayedItemCount());
        assertEquals("ami.category.bestiary", projection.roots().get(0).getLabel().getString());
        assertEquals(ResultsProcessor.SortField.DAMAGE, state.getSortField());
        assertEquals(ResultsProcessor.GroupBy.CATEGORY, state.getGroupBy());
        assertEquals(List.of(RowField.MOD_NAME, RowField.HEALTH, RowField.DAMAGE), RowFieldConfig.getSubtitleFields());
    }

    @Test
    void foodLensRanksByNutritionAndShowsFoodBadges() {
        SearchState state = new SearchState();
        state.setViewMode(ResultsToolbar.ViewMode.LIST);
        state.setListLens(ListLens.FOOD);

        ResultsViewProjector.Projection projection = ResultsViewProjector.project(List.of(
                item("bread", "Bread", Map.of(
                        SearchNodeKeys.FACETS, "edible",
                        SearchNodeKeys.FOOD_NUTRITION, "5",
                        SearchNodeKeys.FOOD_SATURATION, "6.0"
                )),
                item("cooked_beef", "Steak", Map.of(
                        SearchNodeKeys.FACETS, "edible,food_protein",
                        SearchNodeKeys.FOOD_NUTRITION, "8",
                        SearchNodeKeys.FOOD_SATURATION, "12.8"
                )),
                item("stone", "Stone", Map.of(SearchNodeKeys.FACETS, "placeable"))
        ), state, null, false, false);

        assertEquals(2, projection.displayedItemCount());
        assertEquals("Steak", projection.roots().get(0).getLabel().getString());
        assertEquals(ResultsProcessor.SortField.FOOD_NUTRITION, state.getSortField());
        assertEquals(List.of(RowField.MOD_NAME, RowField.FOOD_NUTRITION, RowField.FOOD_SATURATION), RowFieldConfig.getSubtitleFields());
    }

    @Test
    void powerLensOnlyAppearsWhenRuntimeDataHasEnergyNodes() {
        List<SearchNode> noEnergy = List.of(
                item("bread", "Bread", Map.of(SearchNodeKeys.FACETS, "edible")),
                item("stone", "Stone", Map.of(SearchNodeKeys.FACETS, "placeable"))
        );
        assertFalse(ListLens.availableFor(noEnergy).contains(ListLens.POWER));

        List<SearchNode> withEnergy = List.of(
                item("battery", "Battery", Map.of(SearchNodeKeys.ENERGY_CAPACITY, "100000")),
                item("stone", "Stone", Map.of(SearchNodeKeys.FACETS, "placeable"))
        );
        assertTrue(ListLens.availableFor(withEnergy).contains(ListLens.POWER));

        List<SearchNode> withGeneration = List.of(
                item("generator", "Generator", Map.of(SearchNodeKeys.ENERGY_GENERATION, "80")),
                item("stone", "Stone", Map.of(SearchNodeKeys.FACETS, "placeable"))
        );
        assertTrue(ListLens.availableFor(withGeneration).contains(ListLens.POWER));
    }

    @Test
    void powerLensRanksGeneratorsAndShowsPowerBadges() {
        SearchState state = new SearchState();
        state.setViewMode(ResultsToolbar.ViewMode.LIST);
        state.setListLens(ListLens.POWER);

        ResultsViewProjector.Projection projection = ResultsViewProjector.project(List.of(
                item("basic_generator", "Basic Generator", Map.of(SearchNodeKeys.ENERGY_GENERATION, "40")),
                item("advanced_generator", "Advanced Generator", Map.of(SearchNodeKeys.ENERGY_GENERATION, "120")),
                item("energy_cell", "Energy Cell", Map.of(SearchNodeKeys.ENERGY_CAPACITY, "100000")),
                item("stone", "Stone", Map.of(SearchNodeKeys.FACETS, "placeable"))
        ), state, null, false, false);

        assertEquals(3, projection.displayedItemCount());
        assertEquals("Advanced Generator", projection.roots().get(0).getLabel().getString());
        assertEquals(ResultsProcessor.SortField.ENERGY_GENERATION, state.getSortField());
        assertEquals(List.of(RowField.MOD_NAME, RowField.ENERGY_GENERATION, RowField.ENERGY_CAPACITY), RowFieldConfig.getSubtitleFields());
    }

    @Test
    void switchingViewModesResetsPresentationState() {
        SearchState state = new SearchState();
        state.setViewMode(ResultsToolbar.ViewMode.LIST);
        state.setListLens(ListLens.FOOD);
        state.setQuery("apple");

        state.setViewMode(ResultsToolbar.ViewMode.GRID);

        assertEquals("apple", state.getQuery());
        assertEquals(ListLens.ALL, state.getListLens());
        assertEquals(ResultsProcessor.SortField.REGISTRY, state.getSortField());
        assertEquals(ResultsProcessor.GroupBy.CATEGORY, state.getGroupBy());
        assertEquals(List.of(RowField.MOD_NAME), RowFieldConfig.getSubtitleFields());

        state.setSortField(ResultsProcessor.SortField.DPS);
        state.setGroupBy(ResultsProcessor.GroupBy.NONE);
        state.setViewMode(ResultsToolbar.ViewMode.LIST);

        assertEquals(ListLens.ALL, state.getListLens());
        assertEquals(ResultsProcessor.SortField.REGISTRY, state.getSortField());
        assertEquals(ResultsProcessor.GroupBy.CATEGORY, state.getGroupBy());
    }

    private static SearchNode item(String path, String displayName, Map<String, String> metadata) {
        return node(NodeType.ITEM, path, displayName, metadata);
    }

    private static SearchNode entity(String path, String displayName, Map<String, String> metadata) {
        return node(NodeType.ENTITY, path, displayName, metadata);
    }

    private static SearchNode node(NodeType type, String path, String displayName, Map<String, String> metadata) {
        return new SearchNode(
                new ResourceLocation("minecraft:" + path),
                type,
                displayName,
                0,
                0,
                metadata
        );
    }
}
