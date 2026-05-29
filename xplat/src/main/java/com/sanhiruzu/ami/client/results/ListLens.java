package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.network.chat.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public enum ListLens {
    ALL("ami.list_lens.all",
            ResultsProcessor.SortField.REGISTRY,
            true,
            ResultsProcessor.GroupBy.CATEGORY,
            EnumSet.of(RowField.MOD_NAME),
            List.of(
                    ResultsProcessor.SortField.REGISTRY,
                    ResultsProcessor.SortField.ALPHABETICAL,
                    ResultsProcessor.SortField.COLOR,
                    ResultsProcessor.SortField.MOD
            )) {
        @Override
        public boolean matches(SearchNode node) {
            return true;
        }
    },

    WEAPONS("ami.list_lens.weapons",
            ResultsProcessor.SortField.DPS,
            false,
            ResultsProcessor.GroupBy.NONE,
            EnumSet.of(RowField.DAMAGE, RowField.DPS, RowField.MOD_NAME),
            List.of(
                    ResultsProcessor.SortField.DPS,
                    ResultsProcessor.SortField.DAMAGE,
                    ResultsProcessor.SortField.ALPHABETICAL,
                    ResultsProcessor.SortField.MOD
            )) {
        @Override
        public boolean matches(SearchNode node) {
            if (isRangedFalsePositivePath(node)) {
                return false;
            }
            return hasMetadata(node, SearchNodeKeys.DPS)
                    || hasMetadata(node, SearchNodeKeys.ATTACK_DAMAGE)
                    || hasFacet(node, "melee_weapon")
                    || hasFacet(node, "projectile")
                    || hasCategory(node, "tools", "melee")
                    || hasCategory(node, "tools", "ranged");
        }
    },

    ARMOR("ami.list_lens.armor",
            ResultsProcessor.SortField.ARMOR_DEFENSE,
            false,
            ResultsProcessor.GroupBy.CATEGORY,
            EnumSet.of(RowField.ARMOR_DEFENSE, RowField.ARMOR_TOUGHNESS, RowField.MOD_NAME),
            List.of(
                    ResultsProcessor.SortField.ARMOR_DEFENSE,
                    ResultsProcessor.SortField.ARMOR_TOUGHNESS,
                    ResultsProcessor.SortField.ALPHABETICAL,
                    ResultsProcessor.SortField.MOD,
                    ResultsProcessor.SortField.COLOR
            )) {
        @Override
        public boolean matches(SearchNode node) {
            return hasCategory(node, "armor", null)
                    || hasFacet(node, "armor_head")
                    || hasFacet(node, "armor_chest")
                    || hasFacet(node, "armor_legs")
                    || hasFacet(node, "armor_feet")
                    || hasFacet(node, "armor_animal")
                    || hasFacet(node, "curio");
        }
    },

    RANGED("ami.list_lens.ranged",
            ResultsProcessor.SortField.DPS,
            false,
            ResultsProcessor.GroupBy.NONE,
            EnumSet.of(RowField.AMMO_TYPE, RowField.DAMAGE, RowField.DPS, RowField.MOD_NAME),
            List.of(
                    ResultsProcessor.SortField.DPS,
                    ResultsProcessor.SortField.DAMAGE,
                    ResultsProcessor.SortField.ALPHABETICAL,
                    ResultsProcessor.SortField.MOD
            )) {
        @Override
        public boolean matches(SearchNode node) {
            if (isRangedFalsePositivePath(node)) {
                return false;
            }
            return hasFacet(node, "ranged_weapon")
                    || hasFacet(node, "projectile")
                    || hasCategory(node, "tools", "ranged")
                    || hasRangedPath(node);
        }
    },

    TOOLS("ami.list_lens.tools",
            ResultsProcessor.SortField.TOOL_SPEED,
            false,
            ResultsProcessor.GroupBy.CATEGORY,
            EnumSet.of(RowField.TOOL_SPEED, RowField.TOOL_USES, RowField.MOD_NAME),
            List.of(
                    ResultsProcessor.SortField.TOOL_SPEED,
                    ResultsProcessor.SortField.TOOL_USES,
                    ResultsProcessor.SortField.DAMAGE,
                    ResultsProcessor.SortField.ALPHABETICAL,
                    ResultsProcessor.SortField.MOD
            )) {
        @Override
        public boolean matches(SearchNode node) {
            return hasMetadata(node, SearchNodeKeys.TOOL_SPEED)
                    || hasFacet(node, "harvest_tool")
                    || hasFacet(node, "utility_tool")
                    || hasCategory(node, "tools", "harvest")
                    || hasCategory(node, "tools", "utility");
        }
    },

    STORAGE("ami.list_lens.storage",
            ResultsProcessor.SortField.STORAGE_CAPACITY,
            false,
            ResultsProcessor.GroupBy.NONE,
            EnumSet.of(RowField.STORAGE_CAPACITY, RowField.MOD_NAME),
            List.of(
                    ResultsProcessor.SortField.STORAGE_CAPACITY,
                    ResultsProcessor.SortField.ALPHABETICAL,
                    ResultsProcessor.SortField.MOD
            )) {
        @Override
        public boolean matches(SearchNode node) {
            return hasMetadata(node, SearchNodeKeys.ESM_CAPACITY)
                    || hasFacet(node, "storage");
        }
    },

    POWER("ami.list_lens.power",
            ResultsProcessor.SortField.ENERGY_GENERATION,
            false,
            ResultsProcessor.GroupBy.NONE,
            EnumSet.of(RowField.ENERGY_GENERATION, RowField.ENERGY_CAPACITY, RowField.MOD_NAME),
            List.of(
                    ResultsProcessor.SortField.ENERGY_GENERATION,
                    ResultsProcessor.SortField.ENERGY_CAPACITY,
                    ResultsProcessor.SortField.ALPHABETICAL,
                    ResultsProcessor.SortField.MOD
            )) {
        @Override
        public boolean matches(SearchNode node) {
            return hasMetadata(node, SearchNodeKeys.ENERGY_CAPACITY)
                    || hasMetadata(node, SearchNodeKeys.ENERGY_GENERATION)
                    || hasMetadata(node, SearchNodeKeys.ENERGY_CONSUMPTION)
                    || hasFacet(node, "has_energy");
        }
    },

    MACHINES("ami.list_lens.machines",
            ResultsProcessor.SortField.ENERGY_GENERATION,
            false,
            ResultsProcessor.GroupBy.MOD,
            EnumSet.of(RowField.ENERGY_GENERATION, RowField.ENERGY_CAPACITY, RowField.MOD_NAME),
            List.of(
                    ResultsProcessor.SortField.ENERGY_GENERATION,
                    ResultsProcessor.SortField.ENERGY_CAPACITY,
                    ResultsProcessor.SortField.ALPHABETICAL,
                    ResultsProcessor.SortField.MOD
            )) {
        @Override
        public boolean matches(SearchNode node) {
            if (isStorageOnlyMachineFalsePositive(node)) {
                return false;
            }
            return hasFacet(node, "machine")
                    || hasFacet(node, "interactive_block")
                    || hasFacet(node, "has_energy")
                    || hasCategory(node, "tech", "machines")
                    || hasCategory(node, "tech", "power")
                    || hasMachinePath(node);
        }
    },

    FLUIDS("ami.list_lens.fluids",
            ResultsProcessor.SortField.FLUID_CAPACITY,
            false,
            ResultsProcessor.GroupBy.NONE,
            EnumSet.of(RowField.FLUID_CAPACITY, RowField.MOD_NAME),
            List.of(
                    ResultsProcessor.SortField.FLUID_CAPACITY,
                    ResultsProcessor.SortField.ALPHABETICAL,
                    ResultsProcessor.SortField.MOD
            )) {
        @Override
        public boolean matches(SearchNode node) {
            return hasMetadata(node, SearchNodeKeys.FLUID_CAPACITY)
                    || hasFacet(node, "fluid_container")
                    || hasCategory(node, "utility", "fluids")
                    || pathContains(node, "tank", "reservoir", "drum", "fluid", "canister");
        }
    },

    FOOD("ami.list_lens.food",
            ResultsProcessor.SortField.FOOD_NUTRITION,
            false,
            ResultsProcessor.GroupBy.NONE,
            EnumSet.of(RowField.FOOD_NUTRITION, RowField.FOOD_SATURATION, RowField.MOD_NAME),
            List.of(
                    ResultsProcessor.SortField.FOOD_NUTRITION,
                    ResultsProcessor.SortField.FOOD_SATURATION,
                    ResultsProcessor.SortField.ALPHABETICAL,
                    ResultsProcessor.SortField.MOD
            )) {
        @Override
        public boolean matches(SearchNode node) {
            return hasMetadata(node, SearchNodeKeys.FOOD_NUTRITION)
                    || hasFacet(node, "edible")
                    || hasFacet(node, "food_meal")
                    || hasFacet(node, "food_drink")
                    || hasFacet(node, "food_protein")
                    || hasFacet(node, "placeable_food")
                    || hasCategory(node, "food", null)
                    || hasCategory(node, "nature", "meals")
                    || hasCategory(node, "nature", "snacks")
                    || hasCategory(node, "nature", "drinks")
                    || hasCategory(node, "nature", "proteins");
        }
    },

    BLOCKS("ami.list_lens.blocks",
            ResultsProcessor.SortField.ALPHABETICAL,
            true,
            ResultsProcessor.GroupBy.SHAPE,
            EnumSet.of(RowField.REQUIRED_TOOL, RowField.MOD_NAME),
            List.of(
                    ResultsProcessor.SortField.ALPHABETICAL,
                    ResultsProcessor.SortField.REGISTRY,
                    ResultsProcessor.SortField.COLOR,
                    ResultsProcessor.SortField.MOD
            )) {
        @Override
        public boolean matches(SearchNode node) {
            return hasFacet(node, "placeable")
                    || hasMetadata(node, SearchNodeKeys.BLOCKS_MATERIAL)
                    || hasCategory(node, "masonry", null)
                    || hasCategory(node, "geology", null);
        }
    },

    ENTITIES("ami.list_lens.entities",
            ResultsProcessor.SortField.DAMAGE,
            false,
            ResultsProcessor.GroupBy.CATEGORY,
            EnumSet.of(RowField.HEALTH, RowField.DAMAGE, RowField.MOD_NAME),
            List.of(
                    ResultsProcessor.SortField.DAMAGE,
                    ResultsProcessor.SortField.HEALTH,
                    ResultsProcessor.SortField.ALPHABETICAL,
                    ResultsProcessor.SortField.MOD
            )) {
        @Override
        public boolean matches(SearchNode node) {
            return node.type() == NodeType.ENTITY;
        }
    };

    public final Component displayName;
    private final ResultsProcessor.SortField sortField;
    private final boolean ascending;
    private final ResultsProcessor.GroupBy groupBy;
    private final Set<RowField> subtitleFields;
    private final List<ResultsProcessor.SortField> sortFields;

    ListLens(String translationKey,
             ResultsProcessor.SortField sortField,
             boolean ascending,
             ResultsProcessor.GroupBy groupBy,
             Set<RowField> subtitleFields,
             List<ResultsProcessor.SortField> sortFields) {
        this.displayName = Component.translatable(translationKey);
        this.sortField = sortField;
        this.ascending = ascending;
        this.groupBy = groupBy;
        this.subtitleFields = EnumSet.copyOf(subtitleFields);
        this.sortFields = List.copyOf(sortFields);
    }

    public abstract boolean matches(SearchNode node);

    public List<SearchNode> filter(List<SearchNode> nodes) {
        if (this == ALL) {
            return nodes;
        }
        return nodes.stream()
                .filter(this::matches)
                .toList();
    }

    public ResultsProcessor.SortField sortField() {
        return sortField;
    }

    public boolean ascending() {
        return ascending;
    }

    public ResultsProcessor.GroupBy groupBy() {
        return groupBy;
    }

    public Set<RowField> subtitleFields() {
        return EnumSet.copyOf(subtitleFields);
    }

    public List<ResultsProcessor.SortField> sortFields() {
        return sortFields;
    }

    public boolean shouldAlwaysShow() {
        return this == ALL;
    }

    public static List<ListLens> availableFor(List<SearchNode> nodes) {
        List<ListLens> available = new java.util.ArrayList<>();
        for (ListLens lens : values()) {
            if (lens.shouldAlwaysShow() || nodes.stream().anyMatch(lens::matches)) {
                available.add(lens);
            }
        }
        return List.copyOf(available);
    }

    private static boolean hasMetadata(SearchNode node, String key) {
        return !node.meta(key, "").isBlank();
    }

    private static boolean hasCategory(SearchNode node, String category, String subcategory) {
        if (!category.equalsIgnoreCase(node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, ""))) {
            return false;
        }
        return subcategory == null
                || subcategory.equalsIgnoreCase(node.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, ""));
    }

    private static boolean hasFacet(SearchNode node, String facet) {
        String facets = node.meta(SearchNodeKeys.FACETS, "");
        if (facets.isBlank()) {
            return false;
        }
        String needle = facet.toLowerCase(Locale.ROOT);
        for (String part : facets.split(",")) {
            if (needle.equals(part.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean pathContains(SearchNode node, String... parts) {
        String path = node.id().getPath().toLowerCase(Locale.ROOT);
        for (String part : parts) {
            if (path.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRangedPath(SearchNode node) {
        String path = node.id().getPath().toLowerCase(Locale.ROOT);
        if (isRangedFalsePositivePath(node)) {
            return false;
        }
        if (pathContains(node, "rifle", "pistol", "shotgun", "cannon", "launcher")) {
            return true;
        }
        for (String token : pathTokens(path)) {
            if (token.equals("gun")
                    || token.equals("guns")
                    || token.equals("smg")
                    || token.equals("sniper")
                    || token.equals("revolver")
                    || token.equals("ammo")
                    || token.equals("bullet")
                    || token.equals("bullets")) {
                return true;
            }
        }
        return false;
    }

    private static String[] pathTokens(String path) {
        return path.split("[_/\\-]");
    }

    private static boolean isRangedFalsePositivePath(SearchNode node) {
        String path = node.id().getPath().toLowerCase(Locale.ROOT);
        return path.contains("gunpowder") || path.contains("bulletproof");
    }

    private static boolean hasMachinePath(SearchNode node) {
        return pathContains(node, "machine", "generator", "crusher", "smelter", "pulverizer",
                "assembler", "fabricator", "processor", "charger");
    }

    private static boolean isStorageOnlyMachineFalsePositive(SearchNode node) {
        return hasFacet(node, "storage")
                && !hasFacet(node, "machine")
                && !hasFacet(node, "interactive_block")
                && !hasFacet(node, "has_energy")
                && !hasMachinePath(node);
    }
}
