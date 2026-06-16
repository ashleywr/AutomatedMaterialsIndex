package com.sanhiruzu.ami.index.providers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sanhiruzu.ami.client.AmiDebugSettings;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.compat.JeiRuntimeAccessor;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.Services;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.registry.EmiStackList;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IIngredientVisibility;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class RecipeViewerItemAudit {
    private static final Gson GSON = new Gson();
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String AMI_ITEMS_FILE = "recipe_viewer_items_ami.jsonl";
    private static final String REPORT_JSON_FILE = "recipe_viewer_item_audit.json";
    private static final String REPORT_MD_FILE = "recipe_viewer_item_audit.md";

    private RecipeViewerItemAudit() {
    }

    public static AuditOutputs writeDump(Path dumpDir, Level level) throws IOException {
        Files.createDirectories(dumpDir);

        List<AmiItemSnapshot> amiItems = collectAmiItems(level);
        CoverageIndex coverage = buildCoverageIndex(amiItems);
        List<DatasetCapture> datasets = collectViewerDatasets(level);

        List<DatasetReport> reports = new ArrayList<>();
        for (DatasetCapture dataset : datasets) {
            Path dumpFile = dumpDir.resolve(dataset.dumpFileName());
            writeJsonl(dumpFile, dataset.items());
            reports.add(compareDataset(dataset.source(), dataset.scope(), dataset.items(), coverage, dataset.dumpFileName()));
        }

        Path amiItemsPath = dumpDir.resolve(AMI_ITEMS_FILE);
        writeJsonl(amiItemsPath, amiItems);

        AuditReport report = new AuditReport(
                Instant.now().toString(),
                AmiDebugSettings.versionLabel(),
                AmiDebugSettings.debugBuild(),
                GlobalIndex.getInstance().isIndexReady(),
                amiItems.size(),
                coverage.baseItemIds().size(),
                coverage.exactKeys().size(),
                AMI_ITEMS_FILE,
                reports
        );

        Path reportJson = dumpDir.resolve(REPORT_JSON_FILE);
        Files.writeString(reportJson, PRETTY_GSON.toJson(report), StandardCharsets.UTF_8);

        Path reportMd = dumpDir.resolve(REPORT_MD_FILE);
        Files.writeString(reportMd, renderMarkdown(report), StandardCharsets.UTF_8);

        return new AuditOutputs(reportJson, reportMd, amiItemsPath, reports.stream()
                .map(DatasetReport::dumpFile)
                .map(dumpDir::resolve)
                .toList());
    }

    static CoverageIndex buildCoverageIndex(List<AmiItemSnapshot> amiItems) {
        Set<String> baseItemIds = new LinkedHashSet<>();
        Set<String> exactKeys = new LinkedHashSet<>();
        for (AmiItemSnapshot item : amiItems) {
            if (!item.itemId().isBlank()) {
                baseItemIds.add(item.itemId());
            }
            if (!item.exactKey().isBlank()) {
                exactKeys.add(item.exactKey());
            }
        }
        return new CoverageIndex(Set.copyOf(baseItemIds), Set.copyOf(exactKeys));
    }

    static DatasetReport compareDataset(String source, String scope, List<ViewerItemSnapshot> items,
                                        CoverageIndex coverage, String dumpFile) {
        Set<String> uniqueBaseItems = new LinkedHashSet<>();
        Set<String> missingBaseItems = new LinkedHashSet<>();
        List<MissingExactStack> missingExactStacks = new ArrayList<>();

        for (ViewerItemSnapshot item : items) {
            uniqueBaseItems.add(item.itemId());
            if (!coverage.baseItemIds().contains(item.itemId())) {
                missingBaseItems.add(item.itemId());
            }
            if (!item.exactKey().isBlank() && !coverage.exactKeys().contains(item.exactKey())) {
                missingExactStacks.add(new MissingExactStack(item.itemId(), item.displayName(), item.exactHash()));
            }
        }

        List<String> sortedMissingBaseItems = missingBaseItems.stream()
                .sorted()
                .toList();
        List<MissingExactStack> sortedMissingExactStacks = missingExactStacks.stream()
                .sorted(Comparator.comparing(MissingExactStack::itemId)
                        .thenComparing(MissingExactStack::displayName)
                        .thenComparing(MissingExactStack::exactHash))
                .toList();

        return new DatasetReport(
                source,
                scope,
                items.size(),
                uniqueBaseItems.size(),
                items.size(),
                sortedMissingBaseItems.size(),
                sortedMissingExactStacks.size(),
                dumpFile,
                sortedMissingBaseItems,
                sortedMissingExactStacks
        );
    }

    private static List<AmiItemSnapshot> collectAmiItems(Level level) {
        List<AmiItemSnapshot> snapshots = new ArrayList<>();
        for (SearchNode node : GlobalIndex.getInstance().getNodes(NodeType.ITEM)) {
            ItemStack stack = ItemIconRenderer.resolveStack(node.id());
            String itemId = itemIdOf(stack);
            if (itemId.isBlank() && BuiltInRegistries.ITEM.containsKey(node.id())) {
                itemId = node.id().toString();
            }
            if (itemId.isBlank()) {
                String subtypeOf = node.meta(SearchNodeKeys.SUBTYPE_OF, "");
                Identifier subtypeId = Identifier.tryParse(subtypeOf);
                if (subtypeId != null && BuiltInRegistries.ITEM.containsKey(subtypeId)) {
                    itemId = subtypeId.toString();
                }
            }
            String exactHash = itemId.isBlank() || stack.isEmpty() ? "" : exactHash(stack, level);
            if (exactHash.isBlank() && !itemId.isBlank()) {
                exactHash = variantHashFromNodeId(node.id());
            }
            String exactKey = exactKey(itemId, exactHash);
            snapshots.add(new AmiItemSnapshot(
                    node.id().toString(),
                    itemId,
                    node.displayName(),
                    node.meta("accessLevel", ""),
                    node.meta("variantSource", ""),
                    node.meta("subtypeOf", ""),
                    exactHash,
                    exactKey
            ));
        }
        snapshots.sort(Comparator.comparing(AmiItemSnapshot::itemId)
                .thenComparing(AmiItemSnapshot::nodeId));
        return snapshots;
    }

    private static List<DatasetCapture> collectViewerDatasets(Level level) {
        List<DatasetCapture> datasets = new ArrayList<>();

        if (Services.PLATFORM.isModLoaded("emi")) {
            List<ViewerItemSnapshot> emiAll = dedupeViewerStacks("emi", "all", EmiApi.getIndexStacks(), level);
            if (!emiAll.isEmpty()) {
                datasets.add(new DatasetCapture("emi", "all", emiAll, dumpFileName("emi", "all")));
            }

            List<ViewerItemSnapshot> emiVisible = dedupeViewerStacks("emi", "visible", EmiStackList.filteredStacks, level);
            if (!emiVisible.isEmpty()) {
                datasets.add(new DatasetCapture("emi", "visible", emiVisible, dumpFileName("emi", "visible")));
            }
        }

        JeiRuntimeAccessor.withRuntime(runtime -> {
            Collection<ItemStack> allStacks = runtime.getIngredientManager().getAllItemStacks();
            List<ViewerItemSnapshot> jeiAll = dedupeViewerStacks("jei", "all", allStacks, level);
            if (!jeiAll.isEmpty()) {
                datasets.add(new DatasetCapture("jei", "all", jeiAll, dumpFileName("jei", "all")));
            }

            IIngredientVisibility visibility = runtime.getJeiHelpers().getIngredientVisibility();
            List<ItemStack> visibleStacks = new ArrayList<>();
            for (ItemStack stack : allStacks) {
                try {
                    if (visibility.isIngredientVisible(VanillaTypes.ITEM_STACK, stack)) {
                        visibleStacks.add(stack);
                    }
                } catch (RuntimeException ignored) {
                }
            }
            List<ViewerItemSnapshot> jeiVisible = dedupeViewerStacks("jei", "visible", visibleStacks, level);
            if (!jeiVisible.isEmpty()) {
                datasets.add(new DatasetCapture("jei", "visible", jeiVisible, dumpFileName("jei", "visible")));
            }
        });

        return datasets;
    }

    private static List<ViewerItemSnapshot> dedupeViewerStacks(String source, String scope,
                                                               Collection<?> rawStacks, Level level) {
        Map<String, ViewerItemSnapshot> unique = new LinkedHashMap<>();
        if (rawStacks == null) {
            return List.of();
        }
        for (Object raw : rawStacks) {
            ItemStack stack = toItemStack(raw);
            if (stack.isEmpty()) {
                continue;
            }
            String itemId = itemIdOf(stack);
            if (itemId.isBlank()) {
                continue;
            }
            String exactHash = exactHash(stack, level);
            String exactKey = exactKey(itemId, exactHash);
            unique.putIfAbsent(exactKey, new ViewerItemSnapshot(
                    source,
                    scope,
                    itemId,
                    stack.getHoverName().getString(),
                    exactHash,
                    exactKey
            ));
        }
        return unique.values().stream()
                .sorted(Comparator.comparing(ViewerItemSnapshot::itemId)
                        .thenComparing(ViewerItemSnapshot::displayName)
                        .thenComparing(ViewerItemSnapshot::exactHash))
                .toList();
    }

    private static ItemStack toItemStack(Object raw) {
        if (raw instanceof ItemStack stack) {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            return copy;
        }
        if (raw instanceof dev.emi.emi.api.stack.EmiStack emiStack) {
            ItemStack stack = emiStack.getItemStack();
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack copy = stack.copy();
            copy.setCount(1);
            return copy;
        }
        return ItemStack.EMPTY;
    }

    private static String itemIdOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId == null ? "" : itemId.toString();
    }

    private static String exactHash(ItemStack stack, Level level) {
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return "";
        }
        return CreativeStackVariantExpander.stackIdentityHash(itemId, stack, level);
    }

    private static String exactKey(String itemId, String exactHash) {
        if (itemId == null || itemId.isBlank() || exactHash == null || exactHash.isBlank()) {
            return "";
        }
        return itemId + "|" + exactHash;
    }

    private static String variantHashFromNodeId(Identifier nodeId) {
        if (nodeId == null) {
            return "";
        }
        String path = nodeId.getPath();
        int marker = path.indexOf("/variant/");
        if (marker < 0) {
            return "";
        }
        String variantPath = path.substring(marker + "/variant/".length());
        int underscore = variantPath.lastIndexOf('_');
        if (underscore < 0 || underscore + 1 >= variantPath.length()) {
            return "";
        }
        String candidate = variantPath.substring(underscore + 1);
        if (candidate.length() < 8 || candidate.length() > 32) {
            return "";
        }
        for (int i = 0; i < candidate.length(); i++) {
            char c = candidate.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
            if (!hex) {
                return "";
            }
        }
        return candidate;
    }

    private static String dumpFileName(String source, String scope) {
        return "recipe_viewer_items_" + source.toLowerCase(Locale.ROOT) + "_" + scope.toLowerCase(Locale.ROOT) + ".jsonl";
    }

    private static void writeJsonl(Path path, List<?> rows) throws IOException {
        Files.createDirectories(path.getParent());
        List<String> lines = new ArrayList<>(rows.size());
        for (Object row : rows) {
            lines.add(GSON.toJson(row));
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private static String renderMarkdown(AuditReport report) {
        StringBuilder out = new StringBuilder();
        out.append("# AMI Recipe Viewer Item Audit\n\n");
        out.append("- Generated: ").append(report.generatedAtUtc()).append('\n');
        out.append("- AMI version: ").append(report.amiVersion()).append('\n');
        out.append("- Debug build: ").append(report.debugBuild()).append('\n');
        out.append("- Index ready: ").append(report.indexReady()).append('\n');
        out.append("- AMI item nodes: ").append(report.amiItemNodes()).append('\n');
        out.append("- AMI represented base items: ").append(report.amiRepresentedBaseItems()).append('\n');
        out.append("- AMI represented exact stacks: ").append(report.amiRepresentedExactStacks()).append('\n');
        out.append("- AMI dump: `").append(report.amiItemsDumpFile()).append("`\n\n");

        for (DatasetReport dataset : report.datasets()) {
            out.append("## ").append(dataset.source()).append(" / ").append(dataset.scope()).append("\n\n");
            out.append("- Unique exact stacks: ").append(dataset.uniqueExactStacks()).append('\n');
            out.append("- Unique base items: ").append(dataset.uniqueBaseItems()).append('\n');
            out.append("- Missing base items in AMI: ").append(dataset.missingBaseItems()).append('\n');
            out.append("- Missing exact stacks in AMI: ").append(dataset.missingExactStacks()).append('\n');
            out.append("- Dump: `").append(dataset.dumpFile()).append("`\n\n");

            if (!dataset.missingBaseItemIds().isEmpty()) {
                out.append("Missing base items (first 25):\n\n```text\n");
                dataset.missingBaseItemIds().stream().limit(25).forEach(id -> out.append(id).append('\n'));
                out.append("```\n\n");
            }

            if (!dataset.missingExactStackEntries().isEmpty()) {
                out.append("Missing exact stacks (first 25):\n\n```text\n");
                dataset.missingExactStackEntries().stream().limit(25).forEach(entry ->
                        out.append(entry.itemId()).append(" | ")
                                .append(entry.displayName()).append(" | ")
                                .append(entry.exactHash()).append('\n'));
                out.append("```\n\n");
            }
        }
        return out.toString();
    }

    public record AuditOutputs(Path reportJson, Path reportMarkdown, Path amiItemsDump, List<Path> viewerDumps) {
    }

    record CoverageIndex(Set<String> baseItemIds, Set<String> exactKeys) {
    }

    record DatasetCapture(String source, String scope, List<ViewerItemSnapshot> items, String dumpFileName) {
    }

    public record AmiItemSnapshot(
            String nodeId,
            String itemId,
            String displayName,
            String accessLevel,
            String variantSource,
            String subtypeOf,
            String exactHash,
            String exactKey
    ) {
    }

    public record ViewerItemSnapshot(
            String source,
            String scope,
            String itemId,
            String displayName,
            String exactHash,
            String exactKey
    ) {
    }

    public record MissingExactStack(String itemId, String displayName, String exactHash) {
    }

    public record DatasetReport(
            String source,
            String scope,
            int rawStackCount,
            int uniqueBaseItems,
            int uniqueExactStacks,
            int missingBaseItems,
            int missingExactStacks,
            String dumpFile,
            List<String> missingBaseItemIds,
            List<MissingExactStack> missingExactStackEntries
    ) {
    }

    public record AuditReport(
            String generatedAtUtc,
            String amiVersion,
            boolean debugBuild,
            boolean indexReady,
            int amiItemNodes,
            int amiRepresentedBaseItems,
            int amiRepresentedExactStacks,
            String amiItemsDumpFile,
            List<DatasetReport> datasets
    ) {
    }
}
