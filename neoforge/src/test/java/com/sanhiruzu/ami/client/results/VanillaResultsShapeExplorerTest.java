package com.sanhiruzu.ami.client.results;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanhiruzu.ami.index.CategoryAssignment;
import com.sanhiruzu.ami.index.FacetCodec;
import com.sanhiruzu.ami.index.FacetProfile;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.PrimaryCategoryResolver;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class VanillaResultsShapeExplorerTest {
    private static final Pattern ITEM_FIELD = Pattern.compile("public static final Item ([A-Z0-9_]+) =");
    private static final Set<String> RAW_PROTEINS = Set.of(
            "beef", "porkchop", "chicken", "mutton", "rabbit", "cod", "salmon", "tropical_fish", "pufferfish",
            "cooked_beef", "cooked_porkchop", "cooked_chicken", "cooked_mutton", "cooked_rabbit",
            "cooked_cod", "cooked_salmon"
    );
    private static final Set<String> MEALS = Set.of(
            "mushroom_stew", "rabbit_stew", "beetroot_soup", "suspicious_stew", "pumpkin_pie", "cake"
    );
    private static final Set<String> SNACKS = Set.of(
            "apple", "golden_apple", "enchanted_golden_apple", "melon_slice", "sweet_berries", "glow_berries",
            "chorus_fruit", "carrot", "golden_carrot", "potato", "baked_potato", "poisonous_potato",
            "beetroot", "dried_kelp", "cookie", "bread"
    );

    @BeforeEach
    void setUp() {
        GlobalIndex.getInstance().clear();
        GlobalIndex.getInstance().markIndexReady();
    }

    @Test
    void writesVanillaResultShapeExplorationReport() throws IOException {
        Optional<Path> vanillaJar = locateVanillaJar();
        Path reportPath = repoRoot().resolve(Path.of("neoforge", "build", "reports", "ami-result-shapes", "vanilla-result-shapes.md"));
        Files.createDirectories(reportPath.getParent());

        if (vanillaJar.isEmpty()) {
            Files.writeString(reportPath, "# AMI Vanilla Result Shape Exploration\n\nNo local Minecraft 1.21.1 jar was found.\n");
            assertTrue(Files.exists(reportPath), "Expected diagnostic report at " + reportPath.toAbsolutePath());
            return;
        }

        List<SearchNode> fixture = loadVanillaNodes(vanillaJar.get());
        StringBuilder report = new StringBuilder();
        report.append("# AMI Vanilla Result Shape Exploration\n\n");
        report.append("Source jar: `").append(vanillaJar.get()).append("`\n\n");
        report.append("Nodes: ").append(fixture.size()).append("\n\n");

        for (ResultsProcessor.GroupBy groupBy : List.of(
                ResultsProcessor.GroupBy.CATEGORY,
                ResultsProcessor.GroupBy.FAMILY,
                ResultsProcessor.GroupBy.MATERIAL
        )) {
            for (ResultsProcessor.SortField sortField : List.of(
                    ResultsProcessor.SortField.REGISTRY,
                    ResultsProcessor.SortField.ALPHABETICAL,
                    ResultsProcessor.SortField.COUNT
            )) {
                try {
                    SearchState listState = state(sortField, groupBy, ResultsToolbar.ViewMode.LIST);
                    SearchState compactState = state(sortField, groupBy, ResultsToolbar.ViewMode.GRID);
                    List<TreeNode> tree = ResultsViewProjector.project(fixture, listState, null, false, false).roots();
                    List<TreeNode> compact = ResultsViewProjector.project(fixture, compactState, null, true, false).roots();
                    report.append(ResultsShapeSnapshot.capture(groupBy, sortField, tree, compact, 9).toMarkdown());
                } catch (Throwable t) {
                    report.append("## group=").append(groupBy.name())
                            .append(" sort=").append(sortField.name())
                            .append("\n\n");
                    report.append("Unavailable in JVM explorer: ")
                            .append(t.getClass().getSimpleName())
                            .append(": ")
                            .append(t.getMessage())
                            .append("\n\n");
                }
            }
        }

        Files.writeString(reportPath, report.toString());
        assertTrue(Files.exists(reportPath), "Expected diagnostic report at " + reportPath.toAbsolutePath());
    }

    private static SearchState state(ResultsProcessor.SortField sortField,
                                     ResultsProcessor.GroupBy groupBy,
                                     ResultsToolbar.ViewMode viewMode) {
        SearchState state = new SearchState();
        state.setSortField(sortField);
        state.setGroupBy(groupBy);
        state.setViewMode(viewMode);
        return state;
    }

    private static List<SearchNode> loadVanillaNodes(Path vanillaJar) throws IOException {
        Map<String, String> labels = loadVanillaLabels(vanillaJar);
        List<String> registryOrder = loadRegistryOrder(labels.keySet());
        List<SearchNode> nodes = new ArrayList<>();
        for (String path : registryOrder) {
            String label = labels.get(path);
            if (label == null || "Air".equals(label)) {
                continue;
            }
            nodes.add(node(path, label));
        }
        return nodes;
    }

    private static Map<String, String> loadVanillaLabels(Path vanillaJar) throws IOException {
        Map<String, String> labels = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(vanillaJar.toFile())) {
            var entry = zip.getEntry("assets/minecraft/lang/en_us.json");
            if (entry == null) {
                throw new IOException("Missing assets/minecraft/lang/en_us.json in " + vanillaJar);
            }
            try (Reader reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                for (var langEntry : json.entrySet()) {
                    String key = langEntry.getKey();
                    if (key.startsWith("block.minecraft.")) {
                        labels.putIfAbsent(key.substring("block.minecraft.".length()), langEntry.getValue().getAsString());
                    } else if (key.startsWith("item.minecraft.")) {
                        labels.put(key.substring("item.minecraft.".length()), langEntry.getValue().getAsString());
                    }
                }
            }
        }
        return labels;
    }

    private static List<String> loadRegistryOrder(Set<String> knownPaths) throws IOException {
        Path itemsSource = repoRoot().resolve(Path.of(
                "internal", "reference-sources", "minecraft-1.21.1-neoforge-21.1.228",
                "net", "minecraft", "world", "item", "Items.java"
        ));
        if (!Files.exists(itemsSource)) {
            return knownPaths.stream().sorted().toList();
        }

        List<String> result = new ArrayList<>();
        for (String line : Files.readAllLines(itemsSource)) {
            Matcher matcher = ITEM_FIELD.matcher(line);
            if (!matcher.find()) {
                continue;
            }
            String path = matcher.group(1).toLowerCase(Locale.ROOT);
            if (knownPaths.contains(path)) {
                result.add(path);
            }
        }
        return result;
    }

    private static SearchNode node(String path, String displayName) {
        ResourceLocation id = new ResourceLocation("minecraft:" + path);
        EnumSet<ItemFacet> facets = facets(path);
        Map<String, String> attributes = attributes(path, facets);
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(id, new FacetProfile(facets, attributes));

        Map<String, String> meta = new LinkedHashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, "minecraft");
        meta.put(SearchNodeKeys.ACCESS_LEVEL, "survival");
        meta.put(SearchNodeKeys.MATERIAL_GROUP, materialGroup(path));
        if (!facets.isEmpty()) {
            meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(facets));
        }
        meta.putAll(attributes);
        if (!"misc".equals(assignment.categoryId())) {
            meta.put(SearchNodeKeys.ONTOLOGY_CATEGORY, assignment.categoryId());
            meta.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, assignment.subcategoryId());
        }
        addPresentationGrouping(path, meta);
        return new SearchNode(id, NodeType.ITEM, displayName, 0xFFFFFF, 0, meta);
    }

    private static EnumSet<ItemFacet> facets(String path) {
        EnumSet<ItemFacet> facets = EnumSet.noneOf(ItemFacet.class);
        if (RAW_PROTEINS.contains(path)) {
            facets.add(ItemFacet.FOOD_PROTEIN);
            facets.add(ItemFacet.EDIBLE);
        }
        if (MEALS.contains(path)) facets.add(ItemFacet.FOOD_MEAL);
        if (!MEALS.contains(path) && (path.endsWith("_bottle") || path.equals("potion"))) facets.add(ItemFacet.FOOD_DRINK);
        if (SNACKS.contains(path)) facets.add(ItemFacet.EDIBLE);
        if (path.endsWith("_seeds") || path.equals("nether_wart")) facets.add(ItemFacet.SEED);
        if (path.contains("wheat") || path.contains("carrot") || path.contains("potato") || path.contains("beetroot")) facets.add(ItemFacet.CROP);
        if (path.contains("mushroom") || path.contains("fungus") || path.contains("nylium") || path.contains("mycelium")) facets.add(ItemFacet.FUNGI);
        if (path.contains("flower") || path.contains("tulip") || path.contains("orchid") || path.contains("dandelion") || path.contains("poppy")) facets.add(ItemFacet.FLOWER);
        if (path.endsWith("_log") || path.endsWith("_wood") || path.endsWith("_stem") || path.endsWith("_hyphae")) facets.add(ItemFacet.LOG);
        if (path.contains("leaves")) facets.add(ItemFacet.LEAVES);
        if (path.endsWith("_dye") || path.equals("ink_sac") || path.equals("glow_ink_sac")) facets.add(ItemFacet.INGREDIENT_DYE);
        if (path.endsWith("_ingot")) facets.add(ItemFacet.INGOT);
        if (path.endsWith("_nugget")) facets.add(ItemFacet.NUGGET);
        if (path.equals("raw_copper") || path.equals("raw_gold") || path.equals("raw_iron")) facets.add(ItemFacet.RAW_MATERIAL);
        if (path.endsWith("_dust") || path.equals("redstone") || path.equals("glowstone_dust")) facets.add(ItemFacet.DUST);
        if (path.endsWith("_sword") || path.equals("trident") || path.equals("mace")) facets.add(ItemFacet.MELEE_WEAPON);
        if (path.endsWith("_pickaxe") || path.endsWith("_axe") || path.endsWith("_shovel") || path.endsWith("_hoe")) facets.add(ItemFacet.HARVEST_TOOL);
        if (path.equals("bow") || path.equals("crossbow")) facets.add(ItemFacet.RANGED_WEAPON);
        if (path.equals("arrow") || path.endsWith("_arrow") || path.equals("snowball") || path.equals("egg")) facets.add(ItemFacet.PROJECTILE);
        if (path.endsWith("_helmet")) facets.add(ItemFacet.ARMOR_HEAD);
        if (path.endsWith("_chestplate")) facets.add(ItemFacet.ARMOR_CHEST);
        if (path.endsWith("_leggings")) facets.add(ItemFacet.ARMOR_LEGS);
        if (path.endsWith("_boots")) facets.add(ItemFacet.ARMOR_FEET);
        if (path.endsWith("_spawn_egg")) facets.add(ItemFacet.SPAWN_EGG);
        if (path.endsWith("_bucket")) facets.add(ItemFacet.MOB_BUCKET);
        if (path.contains("rail") || path.contains("minecart") || path.endsWith("_boat")) facets.add(ItemFacet.TRANSPORT);
        if (path.contains("redstone") || path.equals("repeater") || path.equals("comparator") || path.equals("observer")) facets.add(ItemFacet.REDSTONE_LOGIC);
        if (path.contains("chest") || path.contains("barrel") || path.contains("shulker_box")) facets.add(ItemFacet.STORAGE);
        if (path.contains("lantern") || path.contains("torch") || path.contains("candle")) facets.add(ItemFacet.LIGHT_SOURCE);
        if (path.endsWith("_stairs")) facets.add(ItemFacet.STAIRS);
        if (path.endsWith("_slab")) facets.add(ItemFacet.SLAB);
        if (path.endsWith("_wall")) facets.add(ItemFacet.WALL);
        if (path.endsWith("_fence")) facets.add(ItemFacet.FENCE);
        if (path.endsWith("_fence_gate")) facets.add(ItemFacet.FENCE_GATE);
        if (path.endsWith("_door")) facets.add(ItemFacet.DOOR);
        if (path.endsWith("_trapdoor")) facets.add(ItemFacet.TRAPDOOR);
        if (looksPlaceable(path)) facets.add(ItemFacet.PLACEABLE);
        return facets;
    }

    private static Map<String, String> attributes(String path, EnumSet<ItemFacet> facets) {
        Map<String, String> attributes = new LinkedHashMap<>();
        if (path.contains("stone") || path.contains("deepslate") || path.contains("tuff") || path.contains("granite")
                || path.contains("diorite") || path.contains("andesite") || path.contains("netherrack")) {
            facets.add(ItemFacet.STONE_BLOCK);
            attributes.put(SearchNodeKeys.BLOCKS_MATERIAL, "stone");
        }
        if (path.contains("dirt") || path.contains("sand") || path.contains("gravel") || path.contains("mud")
                || path.contains("nylium") || path.contains("mycelium")) {
            facets.add(ItemFacet.SOIL_BLOCK);
            attributes.putIfAbsent(SearchNodeKeys.BLOCKS_MATERIAL, "soil");
        }
        return attributes;
    }

    private static void addPresentationGrouping(String path, Map<String, String> meta) {
        if (path.endsWith("_dye")) {
            meta.put(SearchNodeKeys.SUBTYPE_OF, "minecraft:dye");
            meta.put(SearchNodeKeys.MATERIAL_GROUP, "minecraft:dye");
            meta.put(SearchNodeKeys.COLOR_BUCKET, path.substring(0, path.length() - "_dye".length()));
        } else if (path.equals("candle") || path.endsWith("_candle")) {
            if (!path.equals("candle")) {
                meta.put(SearchNodeKeys.SUBTYPE_OF, "minecraft:candle");
                meta.put(SearchNodeKeys.COLOR_BUCKET, path.substring(0, path.length() - "_candle".length()));
            }
            meta.put(SearchNodeKeys.MATERIAL_GROUP, "minecraft:candle");
        } else if (path.endsWith("_banner")) {
            meta.put(SearchNodeKeys.COLLAPSE_FAMILY, "banners");
            meta.put(SearchNodeKeys.COLLAPSE_LABEL, "Banners");
        } else if (path.contains("mushroom") || path.contains("fungus")) {
            meta.put(SearchNodeKeys.SUBTYPE_OF, "minecraft:mushroom");
            meta.put(SearchNodeKeys.MATERIAL_GROUP, "minecraft:mushroom");
        }
    }

    private static String materialGroup(String path) {
        if (path.endsWith("_ingot")) return "minecraft:" + path.substring(0, path.length() - "_ingot".length());
        if (path.endsWith("_nugget")) return "minecraft:" + path.substring(0, path.length() - "_nugget".length());
        if (path.startsWith("raw_")) return "minecraft:" + path.substring("raw_".length());
        return "minecraft:" + path;
    }

    private static boolean looksPlaceable(String path) {
        return path.contains("block") || path.contains("stone") || path.contains("dirt") || path.contains("wood")
                || path.contains("planks") || path.contains("nylium") || path.contains("mycelium") || path.contains("mushroom")
                || path.contains("fungus") || path.endsWith("_stairs") || path.endsWith("_slab") || path.endsWith("_wall")
                || path.endsWith("_fence") || path.endsWith("_door") || path.endsWith("_trapdoor") || path.contains("leaves")
                || path.contains("flower") || path.contains("torch") || path.contains("lantern") || path.contains("candle");
    }

    private static Optional<Path> locateVanillaJar() {
        String configured = System.getProperty("ami.vanillaJar");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("AMI_VANILLA_JAR");
        }
        if (configured != null && !configured.isBlank()) {
            Path path = Path.of(configured);
            if (Files.exists(path)) return Optional.of(path);
        }

        Path known = Path.of(System.getProperty("user.home"), ".gradle", "caches", "fabric-loom",
                "minecraftMaven", "net", "minecraft", "minecraft-merged-deobf", "1.21.1",
                "minecraft-merged-deobf-1.21.1.jar");
        return Files.exists(known) ? Optional.of(known) : Optional.empty();
    }

    private static Path repoRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("AGENTS.md"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate repository root");
    }
}
