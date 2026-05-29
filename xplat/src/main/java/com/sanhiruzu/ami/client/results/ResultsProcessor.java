package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ResultsProcessor {
    public enum SortField {
        REGISTRY("ami.sort.registry"),
        ALPHABETICAL("ami.sort.alphabetical"),
        COLOR("ami.sort.color"),
        MOD("ami.sort.mod"),
        STORAGE_CAPACITY("ami.sort.storage"),
        ENERGY_CAPACITY("ami.sort.energy"),
        ENERGY_GENERATION("ami.sort.energy_generation"),
        FLUID_CAPACITY("ami.sort.fluid_capacity"),
        TOOL_SPEED("ami.sort.tool_speed"),
        TOOL_USES("ami.sort.tool_uses"),
        ARMOR_DEFENSE("ami.sort.armor_defense"),
        ARMOR_TOUGHNESS("ami.sort.armor_toughness"),
        FOOD_NUTRITION("ami.sort.food_nutrition"),
        FOOD_SATURATION("ami.sort.food_saturation"),
        DAMAGE("ami.sort.damage"),
        HEALTH("ami.sort.health"),
        DPS("ami.sort.dps"),
        COUNT("ami.sort.count");

        public final Component displayName;

        SortField(String key) {
            this.displayName = Component.translatable(key);
        }
    }

    public enum GroupBy {
        NONE("ami.group.none"),
        DIMENSION("ami.group.dimension"),
        MOD("ami.group.mod"),
        CATEGORY("ami.group.category"),
        CREATIVE("ami.group.creative"),
        MATERIAL("ami.group.material"),
        FAMILY("ami.group.family"),
        SHAPE("ami.group.shape"),
        TOPOLOGY("ami.group.topology"),
        SIMILARITY("ami.group.similarity"),
        PROPERTIES("ami.group.properties");

        public final Component displayName;

        GroupBy(String key) {
            this.displayName = Component.translatable(key);
        }
    }

    private final ResultsPresentationOptions options;
    private final ResultsFilter filter;
    private final ResultsSorter sorter;
    private final ResultsTreeBuilder treeBuilder;

    public ResultsProcessor(SortField sortField, boolean ascending, GroupBy groupBy,
                            Set<String> selectedMods, Set<String> activeFacets) {
        this(new ResultsPresentationOptions(sortField, ascending, groupBy, selectedMods, activeFacets));
    }

    public ResultsProcessor(ResultsPresentationOptions options) {
        this.options = options;
        this.filter = new ResultsFilter(options);
        this.sorter = new ResultsSorter(options);
        this.treeBuilder = new ResultsTreeBuilder(options, sorter);
    }

    public List<TreeNode> process(List<SearchNode> results) {
        if (!com.sanhiruzu.ami.index.GlobalIndex.getInstance().isIndexReady()) {
            return List.of(createIndexingNode());
        }

        List<SearchNode> filtered = filterAndSort(results);
        List<TreeNode> tree = treeBuilder.build(filtered);
        tree = ResultsGroupingPostProcessor.applyToTree(tree, options.groupBy());
        return ResultsTreeNormalizer.normalize(tree);
    }

    public List<TreeNode> processFlat(List<SearchNode> results) {
        if (!com.sanhiruzu.ami.index.GlobalIndex.getInstance().isIndexReady()) {
            return List.of(createIndexingNode());
        }

        return filterAndSort(results).stream()
                .map(node -> new TreeNode(Component.literal(node.displayName()), node))
                .collect(Collectors.toList());
    }

    public List<TreeNode> processFlatWithCardGrouping(List<SearchNode> results) {
        List<TreeNode> flat = processFlat(results);
        return ResultsGroupingPostProcessor.applyToFlatCards(flat);
    }

    private TreeNode createIndexingNode() {
        return new TreeNode("indexing", Component.translatable("ami.gui.background_indexing")
                .withStyle(s -> s.withColor(com.sanhiruzu.ami.client.AMITheme.CHEAT_INDICATOR)));
    }

    private List<SearchNode> filterAndSort(List<SearchNode> results) {
        return sorter.sort(filter.filter(results));
    }

    public Set<String> getAllMods(List<SearchNode> results) {
        return results.stream().map(n -> n.id().getNamespace()).collect(Collectors.toSet());
    }

    public SortField getSortField() {
        return options.sortField();
    }

    public boolean isAscending() {
        return options.ascending();
    }

    public GroupBy getGroupBy() {
        return options.groupBy();
    }

    public Set<String> getSelectedMods() {
        return options.selectedMods();
    }

    public Set<String> getActiveFacets() {
        return options.activeFacets();
    }
}
