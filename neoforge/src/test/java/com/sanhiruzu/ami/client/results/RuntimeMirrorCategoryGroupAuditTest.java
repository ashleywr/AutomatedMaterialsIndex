package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.SearchNodeMirrorDump;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RuntimeMirrorCategoryGroupAuditTest {
    private static final List<Path> DEFAULT_DUMPS = List.of(
            Path.of("C:", "Users", "ashle", "AppData", "Roaming", "PrismLauncher", "instances", "Society- Sunlit Valley", "minecraft", "ami_dumps", "search_nodes.jsonl"),
            Path.of("C:", "Users", "ashle", "AppData", "Roaming", "PrismLauncher", "instances", "Ashley Modpack", "minecraft", "ami_dumps", "search_nodes.jsonl"),
            Path.of("C:", "Users", "ashle", "AppData", "Roaming", "PrismLauncher", "instances", "DeceasedCraft - Urban Zombie Apocalypse", "minecraft", "ami_dumps", "search_nodes.jsonl")
    );

    private static final Set<String> CATEGORY_CARDINALITY_ALLOWLIST = Set.of("candle", "mushroom");
    private static final Set<String> MATERIAL_OR_VARIANT_LABEL_TOKENS = Set.of(
            "acacia", "birch", "dark", "jungle", "mangrove", "oak", "spruce", "crimson", "warped",
            "pine", "palm", "maple", "cherry", "willow", "mahogany", "runewood",
            "blue", "brown", "cyan", "gray", "green", "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow",
            "black", "light", "short", "long", "narrow", "wide", "thin", "small", "large",
            "pattern", "patterned", "brick", "bricks", "tile", "tiles", "stage"
    );

    private static final Set<String> STRUCTURAL_CATEGORY_LABELS = Set.of(
            "building", "nature", "wood & logs", "flora & foliage", "seeds", "crops", "food",
            "tech", "tools", "utility", "magic", "bestiary", "misc", "full blocks", "lighting",
            "doors", "windows & panes", "furniture", "storage"
    );

    private static List<Path> configuredDumps() {
        String configured = System.getProperty("ami.categoryGroupAuditDumps");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("AMI_CATEGORY_GROUP_AUDIT_DUMPS");
        }
        if (configured == null || configured.isBlank()) {
            configured = System.getProperty("ami.searchNodesDump");
        }
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("AMI_SEARCH_NODES_DUMP");
        }
        if (configured == null || configured.isBlank()) {
            return DEFAULT_DUMPS;
        }

        List<Path> paths = new ArrayList<>();
        for (String raw : configured.split(java.io.File.pathSeparator)) {
            if (!raw.isBlank()) {
                paths.add(Path.of(raw));
            }
        }
        return paths;
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

    @BeforeEach
    void setUp() {
        GlobalIndex.getInstance().clear();
        GlobalIndex.getInstance().markIndexReady();
    }

    @Test
    void writesCategoryGroupAuditForAvailableRuntimeDumps() throws IOException {
        Path reportPath = repoRoot().resolve(Path.of("neoforge", "build", "reports", "ami-result-shapes", "category-group-audit.md"));
        Files.createDirectories(reportPath.getParent());

        StringBuilder report = new StringBuilder();
        report.append("# AMI Category Group Audit\n\n");
        report.append("Heuristic audit over runtime `search_nodes.jsonl` dumps. It projects the current Category tree, then flags suspicious group shape and item/category conflicts.\n\n");

        int dumpsRead = 0;
        for (Path dump : configuredDumps()) {
            if (!Files.exists(dump)) {
                report.append("## Missing Dump\n\n`").append(dump).append("`\n\n");
                continue;
            }

            dumpsRead++;
            List<SearchNode> runtime = SearchNodeMirrorDump.readJsonl(dump);
            List<SearchNode> fixture = SearchNodeMirrorDump.reclassifyItemOntology(runtime);
            List<TreeNode> categoryTree = categoryTree(fixture);

            List<AuditFinding> groupFindings = suspiciousGroups(categoryTree);
            List<AuditFinding> itemFindings = suspiciousItems(fixture);
            List<ClassificationDiagnostic> diagnostics = classificationDiagnostics(fixture);
            List<BucketSummary> buckets = largestBuckets(categoryTree);

            report.append("## ").append(dump.getParent().getParent().getFileName()).append("\n\n");
            report.append("- Source: `").append(dump).append("`\n");
            report.append("- Nodes: ").append(fixture.size()).append("\n");
            report.append("- Suspicious groups: ").append(groupFindings.size()).append("\n");
            report.append("- Suspicious items: ").append(itemFindings.size()).append("\n\n");

            appendFindings(report, "Suspicious Category Groups", groupFindings, 150);
            appendFindings(report, "Suspicious Items By Category", itemFindings, 250);
            appendDiagnostics(report, "Low Confidence Classifications", diagnostics.stream()
                    .filter(ClassificationDiagnostic::lowConfidence)
                    .limit(250)
                    .toList());
            appendDiagnostics(report, "Conflicting Evidence", diagnostics.stream()
                    .filter(ClassificationDiagnostic::conflicting)
                    .limit(250)
                    .toList());
            appendDiagnostics(report, "Generic Fallback Winners", diagnostics.stream()
                    .filter(ClassificationDiagnostic::genericFallbackWinner)
                    .limit(250)
                    .toList());
            appendBuckets(report, buckets, 80);
        }

        if (dumpsRead == 0) {
            report.append("No configured runtime dumps were found on this machine.\n");
        }

        Files.writeString(reportPath, report.toString());
        assertTrue(Files.exists(reportPath), "Expected diagnostic report at " + reportPath.toAbsolutePath());
    }

    private static List<TreeNode> categoryTree(List<SearchNode> fixture) {
        SearchState state = new SearchState();
        state.setGroupBy(ResultsProcessor.GroupBy.CATEGORY);
        state.setSortField(ResultsProcessor.SortField.COUNT);
        state.setAscending(false);
        state.setViewMode(ResultsToolbar.ViewMode.LIST);
        return ResultsViewProjector.project(fixture, state, null, false, false).roots();
    }

    private static List<AuditFinding> suspiciousGroups(List<TreeNode> roots) {
        List<AuditFinding> findings = new ArrayList<>();
        for (TreeNode root : roots) {
            visitGroups(root, ResultsDumpLabels.label(root), 0, findings);
        }
        findings.sort(Comparator
                .comparingInt(AuditFinding::severity).reversed()
                .thenComparing(AuditFinding::path));
        return findings;
    }

    private static void visitGroups(TreeNode node, String path, int depth, List<AuditFinding> findings) {
        if (!node.isLeaf()) {
            String label = ResultsDumpLabels.label(node);
            int leaves = countLeaves(node);
            String key = node.getKey() == null ? "" : node.getKey();

            if (node.isHighCardinality() && !isAllowedCategoryCardinalityGroup(key) && !isAllowedCategoryCardinalityGroup(label)) {
                findings.add(new AuditFinding(90, path, "category view cardinality group is not on the semantic allowlist", sampleLeaves(node, 4)));
            }
            if (depth >= 2 && looksLikeMaterialOrVariantLabel(label) && !STRUCTURAL_CATEGORY_LABELS.contains(label.toLowerCase(Locale.ROOT))) {
                findings.add(new AuditFinding(70, path, "group label looks like material, color, shape, or variant rather than category taxonomy", sampleLeaves(node, 4)));
            }
            if (depth >= 2 && leaves <= 3 && !node.getChildren().isEmpty()) {
                findings.add(new AuditFinding(30, path, "very small nested group; likely too granular for Category view", sampleLeaves(node, 3)));
            }
        }

        for (TreeNode child : node.getChildren()) {
            if (child.isLeaf()) {
                continue;
            }
            visitGroups(child, path + " > " + ResultsDumpLabels.label(child), depth + 1, findings);
        }
    }

    private static boolean isAllowedCategoryCardinalityGroup(String key) {
        String lowerKey = key.toLowerCase(Locale.ROOT);
        for (String allowed : CATEGORY_CARDINALITY_ALLOWLIST) {
            if (lowerKey.contains(allowed)) {
                return true;
            }
        }
        if (!key.startsWith("cardinality:")) {
            return true;
        }
        String base = key.substring("cardinality:".length());
        int sep = base.indexOf(':');
        if (sep >= 0) {
            base = base.substring(0, sep);
        }
        for (String allowed : CATEGORY_CARDINALITY_ALLOWLIST) {
            if (base.toLowerCase(Locale.ROOT).contains(allowed)) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksLikeMaterialOrVariantLabel(String label) {
        String normalized = label.toLowerCase(Locale.ROOT).replace('&', ' ');
        for (String token : normalized.split("[\\s_-]+")) {
            if (MATERIAL_OR_VARIANT_LABEL_TOKENS.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static List<AuditFinding> suspiciousItems(List<SearchNode> nodes) {
        List<AuditFinding> findings = new ArrayList<>();
        for (SearchNode node : nodes) {
            if (node.type() != NodeType.ITEM) {
                continue;
            }
            String category = node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "");
            String subcategory = node.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "");
            String path = node.id().getPath().toLowerCase(Locale.ROOT);
            String facets = node.meta(SearchNodeKeys.FACETS, "");
            String location = category + "/" + subcategory + " > " + node.displayName() + " [" + node.id() + "]";

            if (("tech".equals(category) || "machines".equals(subcategory))
                    && (hasPathToken(path, "bush", "crop", "sapling", "leaves", "leaf") || hasFacet(facets, ItemFacet.CROP, ItemFacet.LEAVES, ItemFacet.SEED))) {
                findings.add(new AuditFinding(95, location, "nature evidence inside Tech", nodeEvidence(node)));
            }
            if ("tools".equals(category) && "ammo".equals(subcategory)
                    && (hasPathToken(path, "track", "narrow", "shell") || hasFacet(facets, ItemFacet.INGREDIENT_ORGANIC))) {
                findings.add(new AuditFinding(90, location, "ammo placement has known projectile false-positive signals", nodeEvidence(node)));
            }
            if ("magic".equals(category) && path.contains("wood")
                    && !node.meta("classificationEvidence", "").contains("magic_artifact")) {
                findings.add(new AuditFinding(75, location, "wood-like item inside Magic; check rune/essence word bias", nodeEvidence(node)));
            }
            if ("masonry".equals(category) && "full_block".equals(subcategory)
                    && (hasPathToken(path, "cake", "pie", "cheese", "bread", "meat", "fruit", "apple", "berry", "grape", "durian")
                    || hasFacet(facets, ItemFacet.EDIBLE, ItemFacet.PLACEABLE_FOOD, ItemFacet.FOOD_MEAL, ItemFacet.FOOD_DRINK, ItemFacet.FOOD_PROTEIN, ItemFacet.SEED, ItemFacet.CROP, ItemFacet.LEAVES))) {
                findings.add(new AuditFinding(80, location, "Full Blocks item has food or nature evidence", nodeEvidence(node)));
            }
            if ("decoration".equals(category) && "lighting".equals(subcategory)
                    && !hasPathToken(path, "lamp", "lantern", "torch", "candle", "chandelier", "sconce", "brazier", "glowstone", "shroomlight", "froglight", "beacon")) {
                findings.add(new AuditFinding(70, location, "Lighting placement lacks primary lighting name tokens", nodeEvidence(node)));
            }
            if ("misc".equals(category)
                    && (hasFacet(facets, ItemFacet.TECH_COMPONENT, ItemFacet.MECHANICAL_COMPONENT, ItemFacet.SEED, ItemFacet.LEAVES, ItemFacet.CROP, ItemFacet.EDIBLE, ItemFacet.PLACEABLE_FOOD, ItemFacet.FOOD_MEAL, ItemFacet.FOOD_DRINK, ItemFacet.FOOD_PROTEIN)
                    || hasPathToken(path, "circuit", "processor", "gear", "cogwheel", "seed", "sapling", "leaf", "leaves", "crop"))) {
                findings.add(new AuditFinding(85, location, "Misc item has stronger category evidence", nodeEvidence(node)));
            }
        }
        findings.sort(Comparator
                .comparingInt(AuditFinding::severity).reversed()
                .thenComparing(AuditFinding::path));
        return findings;
    }

    private static List<ClassificationDiagnostic> classificationDiagnostics(List<SearchNode> nodes) {
        List<ClassificationDiagnostic> diagnostics = new ArrayList<>();
        for (SearchNode node : nodes) {
            if (node.type() != NodeType.ITEM) {
                continue;
            }
            String mode = node.meta("classificationMode", "");
            if (!"evidence_scoring".equals(mode)) {
                continue;
            }
            List<ScoreEntry> scores = parseScores(node.meta("classificationScores", ""));
            if (scores.isEmpty()) {
                continue;
            }
            String category = node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "");
            String subcategory = node.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "");
            String winnerKey = category + "/" + subcategory;
            int winnerScore = scoreFor(scores, winnerKey);
            if (winnerScore <= 0) {
                winnerScore = parseInt(node.meta("classificationScore", "0"));
            }
            ScoreEntry runnerUp = scores.stream()
                    .filter(score -> !score.key().equals(winnerKey))
                    .findFirst()
                    .orElse(new ScoreEntry("", 0));
            int margin = winnerScore - runnerUp.score();
            String evidence = node.meta("classificationEvidence", "");
            String location = winnerKey + " > " + node.displayName() + " [" + node.id() + "]";
            diagnostics.add(new ClassificationDiagnostic(
                    location,
                    winnerScore,
                    runnerUp,
                    margin,
                    evidence,
                    nodeEvidence(node),
                    genericFallbackEvidence(evidence, winnerScore)
            ));
        }
        diagnostics.sort(Comparator
                .comparingInt(ClassificationDiagnostic::sortRank).reversed()
                .thenComparingInt(ClassificationDiagnostic::margin)
                .thenComparing(ClassificationDiagnostic::location));
        return diagnostics;
    }

    private static List<ScoreEntry> parseScores(String encoded) {
        List<ScoreEntry> scores = new ArrayList<>();
        if (encoded == null || encoded.isBlank()) {
            return scores;
        }
        for (String raw : encoded.split(";")) {
            String part = raw.trim();
            int sep = part.lastIndexOf('=');
            if (sep <= 0 || sep == part.length() - 1) {
                continue;
            }
            scores.add(new ScoreEntry(part.substring(0, sep), parseInt(part.substring(sep + 1))));
        }
        scores.sort(Comparator.comparingInt(ScoreEntry::score).reversed().thenComparing(ScoreEntry::key));
        return scores;
    }

    private static int scoreFor(List<ScoreEntry> scores, String key) {
        for (ScoreEntry score : scores) {
            if (score.key().equals(key)) {
                return score.score();
            }
        }
        return 0;
    }

    private static int parseInt(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static boolean genericFallbackEvidence(String evidence, int winnerScore) {
        if (evidence == null || evidence.isBlank()) {
            return false;
        }
        return winnerScore <= 80
                && (evidence.contains("facet.placeable")
                || evidence.contains("component.max_damage")
                || evidence.contains("component.tool"));
    }

    private static List<BucketSummary> largestBuckets(List<TreeNode> roots) {
        List<BucketSummary> buckets = new ArrayList<>();
        for (TreeNode root : roots) {
            collectBuckets(root, ResultsDumpLabels.label(root), buckets);
        }
        buckets.sort(Comparator
                .comparingInt(BucketSummary::count).reversed()
                .thenComparing(BucketSummary::path));
        return buckets;
    }

    private static void collectBuckets(TreeNode node, String path, List<BucketSummary> buckets) {
        if (!node.isLeaf()) {
            int leaves = countLeaves(node);
            if (leaves >= 75 || path.toLowerCase(Locale.ROOT).contains("misc") || path.toLowerCase(Locale.ROOT).contains("full blocks")) {
                buckets.add(new BucketSummary(path, leaves, sampleLeaves(node, 5)));
            }
        }
        for (TreeNode child : node.getChildren()) {
            if (!child.isLeaf()) {
                collectBuckets(child, path + " > " + ResultsDumpLabels.label(child), buckets);
            }
        }
    }

    private static int countLeaves(TreeNode node) {
        if (node.isLeaf()) {
            return 1;
        }
        int count = 0;
        for (TreeNode child : node.getChildren()) {
            count += countLeaves(child);
        }
        return count;
    }

    private static List<String> sampleLeaves(TreeNode node, int limit) {
        List<String> samples = new ArrayList<>();
        appendSampleLeaves(node, samples, limit);
        return samples;
    }

    private static void appendSampleLeaves(TreeNode node, List<String> samples, int limit) {
        if (samples.size() >= limit) {
            return;
        }
        if (node.isLeaf()) {
            SearchNode entry = node.getEntry();
            samples.add(entry.displayName() + " [" + entry.id() + "]");
            return;
        }
        for (TreeNode child : node.getChildren()) {
            appendSampleLeaves(child, samples, limit);
            if (samples.size() >= limit) {
                return;
            }
        }
    }

    private static List<String> nodeEvidence(SearchNode node) {
        List<String> evidence = new ArrayList<>();
        addEvidence(evidence, "classificationScore", node.meta("classificationScore", ""));
        addEvidence(evidence, "classificationScores", node.meta("classificationScores", ""));
        addEvidence(evidence, "classificationEvidence", node.meta("classificationEvidence", ""));
        addEvidence(evidence, "facets", node.meta(SearchNodeKeys.FACETS, ""));
        addEvidence(evidence, "componentFacts", node.meta(SearchNodeKeys.COMPONENT_FACTS, ""));
        addEvidence(evidence, "itemClass", node.meta(SearchNodeKeys.ITEM_CLASS, ""));
        addEvidence(evidence, "equipmentSlot", node.meta(SearchNodeKeys.EQUIPMENT_SLOT, ""));
        addEvidence(evidence, "tags", node.meta(SearchNodeKeys.TAGS, ""));
        addEvidence(evidence, "blockTags", node.meta(SearchNodeKeys.BLOCK_TAGS, ""));
        addEvidence(evidence, "blockClass", node.meta(SearchNodeKeys.BLOCK_CLASS, ""));
        addEvidence(evidence, "creativeTab", node.meta(SearchNodeKeys.CREATIVE_TAB_LABEL, ""));
        return evidence;
    }

    private static void addEvidence(List<String> evidence, String label, String value) {
        if (value != null && !value.isBlank()) {
            evidence.add(label + "=" + value);
        }
    }

    private static boolean hasPathToken(String path, String... expected) {
        for (String token : path.split("[_/]")) {
            for (String match : expected) {
                if (match.equals(token)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasFacet(String facets, ItemFacet... expected) {
        if (facets == null || facets.isBlank()) {
            return false;
        }
        Map<String, Boolean> facetSet = new HashMap<>();
        for (String raw : facets.split(",")) {
            facetSet.put(raw.trim(), true);
        }
        for (ItemFacet facet : expected) {
            if (facetSet.containsKey(facet.id())) {
                return true;
            }
        }
        return false;
    }

    private static void appendFindings(StringBuilder report, String title, List<AuditFinding> findings, int limit) {
        report.append("### ").append(title).append("\n\n");
        if (findings.isEmpty()) {
            report.append("No findings.\n\n");
            return;
        }

        findings.stream().limit(limit).forEach(finding -> {
            report.append("- `").append(finding.path()).append("`\n");
            report.append("  - reason: ").append(finding.reason()).append("\n");
            for (String sample : finding.samples()) {
                report.append("  - sample: ").append(sample).append("\n");
            }
        });
        if (findings.size() > limit) {
            report.append("- ... ").append(findings.size() - limit).append(" more\n");
        }
        report.append("\n");
    }

    private static void appendDiagnostics(StringBuilder report, String title, List<ClassificationDiagnostic> diagnostics) {
        report.append("### ").append(title).append("\n\n");
        if (diagnostics.isEmpty()) {
            report.append("No findings.\n\n");
            return;
        }

        for (ClassificationDiagnostic diagnostic : diagnostics) {
            report.append("- `").append(diagnostic.location()).append("`\n");
            report.append("  - score: ").append(diagnostic.winnerScore())
                    .append(", runner-up: ");
            if (diagnostic.runnerUp().key().isBlank()) {
                report.append("none");
            } else {
                report.append(diagnostic.runnerUp().key()).append("=").append(diagnostic.runnerUp().score());
            }
            report.append(", margin: ").append(diagnostic.margin()).append("\n");
            if (!diagnostic.evidence().isBlank()) {
                report.append("  - winning evidence: ").append(diagnostic.evidence()).append("\n");
            }
            for (String sample : diagnostic.samples()) {
                report.append("  - sample: ").append(sample).append("\n");
            }
        }
        report.append("\n");
    }

    private static void appendBuckets(StringBuilder report, List<BucketSummary> buckets, int limit) {
        report.append("### Largest Buckets\n\n");
        if (buckets.isEmpty()) {
            report.append("No large buckets found.\n\n");
            return;
        }

        buckets.stream().limit(limit).forEach(bucket -> {
            report.append("- `").append(bucket.path()).append("`: ").append(bucket.count()).append(" items\n");
            for (String sample : bucket.samples()) {
                report.append("  - sample: ").append(sample).append("\n");
            }
        });
        if (buckets.size() > limit) {
            report.append("- ... ").append(buckets.size() - limit).append(" more\n");
        }
        report.append("\n");
    }

    private record AuditFinding(int severity, String path, String reason, List<String> samples) {
    }

    private record BucketSummary(String path, int count, List<String> samples) {
    }

    private record ScoreEntry(String key, int score) {
    }

    private record ClassificationDiagnostic(
            String location,
            int winnerScore,
            ScoreEntry runnerUp,
            int margin,
            String evidence,
            List<String> samples,
            boolean genericFallbackWinner
    ) {
        private boolean lowConfidence() {
            return winnerScore < 100 || margin <= 30;
        }

        private boolean conflicting() {
            return runnerUp.score() >= 70 && margin <= 60;
        }

        private int sortRank() {
            int rank = 0;
            if (lowConfidence()) rank += 3;
            if (conflicting()) rank += 2;
            if (genericFallbackWinner()) rank += 1;
            return rank;
        }
    }
}
