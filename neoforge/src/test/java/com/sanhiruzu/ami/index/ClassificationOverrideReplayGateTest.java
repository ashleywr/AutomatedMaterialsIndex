package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ClassificationOverrideReplayGateTest {

    private static Path locateDump() {
        String configured = System.getProperty("ami.searchNodesDump");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("AMI_SEARCH_NODES_DUMP");
        }
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        Path synesthesia = Path.of(System.getProperty("user.home"),
                "AppData", "Roaming", "PrismLauncher", "instances", "Synesthesia",
                "minecraft", "ami_dumps", "search", "search_nodes.jsonl");
        if (Files.exists(synesthesia)) {
            return synesthesia;
        }
        return repoRoot().resolve(Path.of("run", "neoforge-emi", "ami_dumps", "search", "search_nodes.jsonl"));
    }

    private static Path repoRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle")) && Files.exists(current.resolve("gradle.properties"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate repository root");
    }

    private static Path bundledOverrides() {
        return repoRoot().resolve(Path.of(
                "xplat", "src", "main", "resources", "assets", "ami", "classification_overrides.json"));
    }

    private static List<SearchNode> items(Path dump) throws IOException {
        return SearchNodeMirrorDump.readJsonl(dump).stream()
                .filter(node -> node.type() == NodeType.ITEM)
                .toList();
    }

    private static Map<String, String> signatureById(List<SearchNode> nodes) {
        Map<String, String> result = new LinkedHashMap<>();
        for (SearchNode node : nodes) {
            String[] facets = node.meta(SearchNodeKeys.FACETS, "").split(",");
            java.util.Arrays.sort(facets);
            result.put(node.id().toString(),
                    node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "") + "/"
                  + node.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "") + "|"
                  + String.join(",", facets));
        }
        return result;
    }

    private static boolean explained(SearchNode node) {
        ResourceLocation id = node.id();
        return ClassificationOverrides.forItem(id).isPresent()
                || ClassificationOverrides.patternFor(
                        id.getNamespace(), id.getPath(), node.meta(SearchNodeKeys.ITEM_CLASS, "")).isPresent();
    }

    @Test
    void explainsMatchAllModPatternWhenItemClassMatches() {
        try {
            ClassificationOverrides.clear();
            ClassificationOverrides.parseAndInstall(
                    "{\"items\":{},\"modPatterns\":[{\"mod\":\"minecolonies\",\"match\":\"all\","
                  + "\"pathTokens\":[\"blockhut\"],\"classTokens\":[\"itemblockhut\"],"
                  + "\"addVerbs\":[\"settlement_worksite\"]}]}");
            SearchNode node = new SearchNode(
                    new ResourceLocation("minecolonies:blockhutbuilder"),
                    NodeType.ITEM,
                    "Builder Hut",
                    0xFFFFFF,
                    0,
                    Map.of(SearchNodeKeys.ITEM_CLASS, "com.minecolonies.core.items.ItemBlockHut"));

            assertTrue(explained(node));
        } finally {
            ClassificationOverrides.loadBundledDefaults();
        }
    }

    @Test
    void detectsExplainedChangeAndLeavesOthersUntouched() throws IOException {
        Path dump = locateDump();
        assumeTrue(Files.exists(dump),
                "no search_nodes dump available locally; the real-data test writes the no-data report");
        List<SearchNode> source = items(dump);
        try {
            ClassificationOverrides.clear();
            Map<String, String> baseline = signatureById(SearchNodeMirrorDump.reclassifyItemOntology(source));

            ClassificationOverrides.parseAndInstall(
                    "{\"items\":{\"minecraft:dirt\":{\"category\":\"weapon\",\"subcategory\":\"throwable\"}},\"modPatterns\":[]}");
            List<SearchNode> after = SearchNodeMirrorDump.reclassifyItemOntology(source);
            Map<String, String> afterById = signatureById(after);

            assertTrue(afterById.get("minecraft:dirt").startsWith("weapon/throwable|"),
                    "forced override must win for the targeted item");

            List<String> unexplained = new ArrayList<>();
            for (SearchNode node : after) {
                String id = node.id().toString();
                if (!baseline.get(id).equals(afterById.get(id)) && !explained(node)) {
                    unexplained.add(id);
                }
            }
            assertTrue(unexplained.isEmpty(),
                    "a single-item override changed untargeted items: " + unexplained);
        } finally {
            ClassificationOverrides.loadBundledDefaults();
        }
    }

    @Test
    void writesOverrideReplayGateReport() throws IOException {
        Path reportPath = repoRoot().resolve(Path.of(
                "neoforge", "build", "reports", "ami-classification", "override-replay-gate.md"));
        Files.createDirectories(reportPath.getParent());

        Path dump = locateDump();
        if (!Files.exists(dump)) {
            Files.writeString(reportPath, "# AMI Override Replay Gate\n\nNo dump found at `" + dump + "`.\n");
            assertTrue(Files.exists(reportPath));
            return;
        }

        String json = Files.readString(bundledOverrides(), StandardCharsets.UTF_8);
        List<SearchNode> source = items(dump);
        List<String> unexplained = new ArrayList<>();
        long changed;
        try {
            ClassificationOverrides.clear();
            Map<String, String> baseline = signatureById(SearchNodeMirrorDump.reclassifyItemOntology(source));

            ClassificationOverrides.parseAndInstall(json);
            List<SearchNode> after = SearchNodeMirrorDump.reclassifyItemOntology(source);
            Map<String, String> afterById = signatureById(after);

            changed = 0;
            for (SearchNode node : after) {
                String id = node.id().toString();
                if (!baseline.get(id).equals(afterById.get(id))) {
                    changed++;
                    if (!explained(node)) {
                        unexplained.add(id + ": " + baseline.get(id) + " -> " + afterById.get(id));
                    }
                }
            }
        } finally {
            ClassificationOverrides.loadBundledDefaults();
        }

        StringBuilder report = new StringBuilder();
        report.append("# AMI Override Replay Gate\n\n");
        report.append("Source dump: `").append(dump).append("`\n\n");
        report.append("- Items compared: ").append(source.size()).append("\n");
        report.append("- Changed by overrides: ").append(changed).append("\n");
        report.append("- Unexplained changes: ").append(unexplained.size()).append("\n\n");
        if (!unexplained.isEmpty()) {
            report.append("## Unexplained changes\n\n");
            unexplained.stream().limit(200).forEach(line -> report.append("- `").append(line).append("`\n"));
        }
        Files.writeString(reportPath, report.toString());

        assertTrue(Files.exists(reportPath));
        if (Boolean.getBoolean("ami.overrideGateStrict")) {
            assertFalse(!unexplained.isEmpty(),
                    () -> "Overrides changed untargeted items. See " + reportPath.toAbsolutePath());
        }
    }

    @Test
    void detectsExplainedFacetOnlyChange() throws IOException {
        Path dump = locateDump();
        assumeTrue(Files.exists(dump),
                "no search_nodes dump available locally; the real-data test writes the no-data report");
        List<SearchNode> source = items(dump);
        try {
            ClassificationOverrides.clear();
            Map<String, String> baseline = signatureById(SearchNodeMirrorDump.reclassifyItemOntology(source));

            // facet-only modPattern override: items with path token "pig" (i.e. minecraft:pig_spawn_egg)
            // gain magic_artifact. Spawn eggs are classified by the "spawn eggs and mob buckets"
            // primary rule which fires before the "magic facets" rule, so the category stays
            // bestiary/spawn_eggs. This exercises the modPattern facet-injection path.
            ClassificationOverrides.parseAndInstall(
                    "{\"items\":{},\"modPatterns\":[{\"mod\":\"minecraft\",\"pathTokens\":[\"pig\"],"
                  + "\"addFacets\":[\"magic_artifact\"]}]}");
            List<SearchNode> after = SearchNodeMirrorDump.reclassifyItemOntology(source);
            Map<String, String> afterById = signatureById(after);

            assertTrue(afterById.get("minecraft:pig_spawn_egg").contains(ItemFacet.MAGIC_ARTIFACT.id()),
                    "facet-only override must add the facet to the targeted item");
            assertEquals(baseline.get("minecraft:pig_spawn_egg").split("\\|")[0],
                    afterById.get("minecraft:pig_spawn_egg").split("\\|")[0],
                    "facet-only override must not change the targeted item's category");

            List<String> unexplained = new ArrayList<>();
            for (SearchNode node : after) {
                String id = node.id().toString();
                if (!baseline.get(id).equals(afterById.get(id)) && !explained(node)) {
                    unexplained.add(id);
                }
            }
            assertTrue(unexplained.isEmpty(),
                    "a facet-only override changed unexplained items: " + unexplained);
        } finally {
            ClassificationOverrides.loadBundledDefaults();
        }
    }
}
