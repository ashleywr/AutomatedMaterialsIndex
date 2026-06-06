package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SearchIndexTest {

    @AfterEach
    public void cleanup() {
        GlobalIndex.getInstance().clear();
    }

    @Test
    public void prefixAndSubstringSearchWork() {
        SearchIndex idx = new SearchIndex();

        var nodeA = new SearchNode(new ResourceLocation("ami:pack"), NodeType.ITEM, "Pack", 0, 0, new HashMap<>());
        var nodeB = new SearchNode(new ResourceLocation("ami:packable_box"), NodeType.ITEM, "Packable Box", 0, 0, new HashMap<>());
        var nodeC = new SearchNode(new ResourceLocation("ami:soph_backpack"), NodeType.ITEM, "Sophisticated Backpack", 0, 0, new HashMap<>());

        idx.addNode(nodeA);
        idx.addNode(nodeB);
        idx.addNode(nodeC);

        List<SearchNode> prefix = idx.prefixSearch("pack");
        // prefix should find Pack and Packable Box (exact-start matches)
        assertTrue(prefix.stream().anyMatch(n -> n.displayName().equals("Pack")));
        assertTrue(prefix.stream().anyMatch(n -> n.displayName().equals("Packable Box")));

        List<SearchNode> substring = idx.substringSearch("backpack");
        assertTrue(substring.stream().anyMatch(n -> n.displayName().equals("Sophisticated Backpack")));
    }

    @Test
    public void metadataAliasesAreSearchable() {
        SearchIndex idx = new SearchIndex();

        var oakStairs = new SearchNode(
                new ResourceLocation("minecraft:oak_stairs"),
                NodeType.ITEM,
                "Oak Stairs",
                0,
                0,
                Map.of(
                        SearchNodeKeys.FACETS, "placeable,stairs,wood_block",
                        SearchNodeKeys.MATERIAL_GROUP, "minecraft:oak",
                        SearchNodeKeys.VARIANT_GROUP, "stairs",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "building_blocks"
                )
        );

        idx.addNode(oakStairs);

        assertTrue(idx.prefixSearch("stairs").contains(oakStairs));
        assertTrue(idx.substringSearch("building blocks").contains(oakStairs));
        assertTrue(idx.substringSearch("building_blocks").contains(oakStairs));
        assertTrue(idx.substringSearch("minecraft:oak").contains(oakStairs));
        assertTrue(idx.substringSearch("wood block").contains(oakStairs));
    }

    @Test
    public void facetFactsCanUseSimplePropertySyntax() {
        GlobalIndex index = GlobalIndex.getInstance();
        SearchNode spiderEye = item("minecraft", "spider_eye", "Spider Eye", Map.of(
                SearchNodeKeys.FACETS, "edible,magic_reagent",
                SearchNodeKeys.ONTOLOGY_CATEGORY, "magic",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "reagents"
        ));
        SearchNode blazePowder = item("minecraft", "blaze_powder", "Blaze Powder", Map.of(
                SearchNodeKeys.FACETS, "magic_reagent",
                SearchNodeKeys.ONTOLOGY_CATEGORY, "magic",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "reagents"
        ));
        index.addNode(spiderEye);
        index.addNode(blazePowder);

        SearchService service = SearchService.buildFrom(index, false);
        List<SearchNode> simpleProperty = service.query("?edible").getOrDefault(NodeType.ITEM, List.of());
        List<SearchNode> explicitFact = service.query("?fact:edible").getOrDefault(NodeType.ITEM, List.of());

        assertTrue(simpleProperty.contains(spiderEye));
        assertFalse(simpleProperty.contains(blazePowder));
        assertTrue(explicitFact.contains(spiderEye));
    }

    @Test
    public void visibleCollapseLabelsArePlainSearchableAliases() {
        SearchIndex idx = new SearchIndex(false);

        var magentaShard = new SearchNode(
                new ResourceLocation("quark:magenta_shard"),
                NodeType.ITEM,
                "Magenta Glass Shard",
                0,
                0,
                Map.of(
                        SearchNodeKeys.COLLAPSE_FAMILY, "quark:shards",
                        SearchNodeKeys.COLLAPSE_LABEL, "Glass Shards"
                )
        );

        idx.addNode(magentaShard);

        assertTrue(idx.prefixSearch("shard").contains(magentaShard));
        assertTrue(idx.prefixSearch("shards").contains(magentaShard));
    }

    @Test
    public void prefixAndSubstringSearchWorkForUnicodeItemNames() {
        SearchIndex idx = new SearchIndex();

        SearchNode russianItem = item("ami", "russian_brick", "кирпич", new HashMap<>());
        SearchNode chineseItem = item("ami", "diamond", "钻石", new HashMap<>());
        SearchNode japaneseItem = item("ami", "diamond_block", "木工台", new HashMap<>());

        idx.addNode(russianItem);
        idx.addNode(chineseItem);
        idx.addNode(japaneseItem);

        assertTrue(idx.prefixSearch("кир").contains(russianItem));
        assertTrue(idx.substringSearch("кирп").contains(russianItem));

        assertTrue(idx.prefixSearch("钻").contains(chineseItem));
        assertTrue(idx.substringSearch("石").contains(chineseItem));

        assertTrue(idx.prefixSearch("木").contains(japaneseItem));
        assertTrue(idx.substringSearch("工台").contains(japaneseItem));
    }

    @Test
    public void japaneseWidthVariantsMatchInSearchIndex() {
        SearchIndex idx = new SearchIndex();

        SearchNode fullWidthJapanese = item("ami", "katakana", "カタカナ", new HashMap<>());
        idx.addNode(fullWidthJapanese);

        assertTrue(idx.prefixSearch("カタ").contains(fullWidthJapanese));
        assertTrue(idx.substringSearch("タカ").contains(fullWidthJapanese));
        assertTrue(idx.prefixSearch("\uFF76\uFF80\uFF76\uFF85").contains(fullWidthJapanese));
        assertTrue(idx.substringSearch("\uFF76\uFF80\uFF76\uFF85").contains(fullWidthJapanese));
    }

    @Test
    public void searchServiceCanResolveUnicodeQueriesAcrossLanguages() {
        GlobalIndex index = GlobalIndex.getInstance();

        SearchNode russianItem = item("ami", "russian_brick", "кирпич", new HashMap<>());
        SearchNode chineseItem = item("ami", "jade_stone", "钻石", new HashMap<>());
        SearchNode japaneseItem = item("ami", "workbench", "木工台", new HashMap<>());
        SearchNode katakanaItem = item("ami", "katakana", "カタカナ", new HashMap<>());

        index.addNode(russianItem);
        index.addNode(chineseItem);
        index.addNode(japaneseItem);
        index.addNode(katakanaItem);

        SearchService service = SearchService.buildFrom(index, false);

        assertTrue(service.query("кир").get(NodeType.ITEM).contains(russianItem));
        assertTrue(service.query("钻").get(NodeType.ITEM).contains(chineseItem));
        assertTrue(service.query("木工").get(NodeType.ITEM).contains(japaneseItem));
        assertTrue(service.query("\uFF76\uFF80\uFF76\uFF85").get(NodeType.ITEM).contains(katakanaItem));

        assertFalse(service.query("diamond").getOrDefault(NodeType.ITEM, List.of()).contains(russianItem));
        assertFalse(service.query("diamond").getOrDefault(NodeType.ITEM, List.of()).contains(chineseItem));
        assertFalse(service.query("diamond").getOrDefault(NodeType.ITEM, List.of()).contains(japaneseItem));
    }

    @Test
    public void localeIndependentCaseNormalizationInSearchService() {
        GlobalIndex index = GlobalIndex.getInstance();
        Locale original = Locale.getDefault();

        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));

            SearchNode item = item("ami", "ingot", "Ingot", new HashMap<>());
            index.addNode(item);

            SearchService service = SearchService.buildFrom(index, false);
            assertTrue(service.query("IN").get(NodeType.ITEM).contains(item));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    public void colonDelimitedSearchWorks() {
        SearchIndex idx = new SearchIndex();

        var cricket = new SearchNode(
                new ResourceLocation("zen_amphibia:cricket"),
                NodeType.ENTITY,
                "Cricket",
                0,
                0,
                new HashMap<>()
        );

        idx.addNode(cricket);

        assertTrue(idx.prefixSearch("zen_amphibia:cricket").contains(cricket));
        assertTrue(idx.prefixSearch("cricket").contains(cricket));
        assertTrue(idx.substringSearch("amphibia").contains(cricket));
    }

    @Test
    public void globalIndexGetNodesReturnsStableSnapshot() {
        GlobalIndex index = GlobalIndex.getInstance();
        var codBucket = new SearchNode(
                new ResourceLocation("minecraft:cod_bucket"),
                NodeType.ITEM,
                "Bucket of Cod",
                0,
                0,
                Map.of(SearchNodeKeys.ONTOLOGY_CATEGORY, "food")
        );
        index.addNode(codBucket);

        List<SearchNode> snapshot = index.getNodes(NodeType.ITEM);
        index.replaceNode(
                codBucket.id(),
                codBucket.type(),
                codBucket.withMetadata(Map.of(SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry"))
        );

        assertEquals(1, snapshot.size());
        assertEquals("food", snapshot.get(0).meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("masonry", index.getNodes(NodeType.ITEM).get(0).meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
    }

    @Test
    public void globalIndexAddNodeReplacesDuplicateIds() {
        GlobalIndex index = GlobalIndex.getInstance();
        var first = new SearchNode(
                new ResourceLocation("minecraft:stone"),
                NodeType.ITEM,
                "Stone",
                0,
                0,
                Map.of(SearchNodeKeys.ONTOLOGY_CATEGORY, "building")
        );
        var second = first.withMetadata(Map.of(SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry"));

        index.addNode(first);
        index.addNode(second);

        assertEquals(1, index.getNodes(NodeType.ITEM).size());
        assertEquals(second, index.getNode(first.id(), NodeType.ITEM).orElseThrow());
        assertEquals(List.of(second), index.getNodesByCategory("masonry"));
        assertTrue(index.getNodesByCategory("building").isEmpty());
    }

    private static SearchNode item(String namespace, String path, String displayName, Map<String, String> metadata) {
        return new SearchNode(new ResourceLocation(namespace, path), NodeType.ITEM, displayName, 0, 0, metadata);
    }
}
