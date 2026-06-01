package com.sanhiruzu.ami.author;

import com.sanhiruzu.ami.api.AmiQuestDocument;
import com.sanhiruzu.ami.api.AmiQuestItemMatch;
import com.sanhiruzu.ami.api.AmiQuestTaskDocument;
import com.sanhiruzu.ami.api.AmiQuestsApi;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Clipboard-oriented pack author reports. This class intentionally produces
 * diagnostics and starter notes only; it does not mutate quest, recipe, stage,
 * or loot files.
 */
public final class PackAuthorDiagnostics {
    private static final int MAX_DETAIL_ROWS = 24;
    private static final int MAX_MOD_ROWS = 16;
    private static final int MAX_QUEST_ROWS = 16;

    private PackAuthorDiagnostics() {
    }

    public static String itemReport(SearchNode node, List<AmiQuestItemMatch> questMatches) {
        if (node == null || node.id() == null) {
            return "";
        }

        List<AmiQuestItemMatch> matches = questMatches == null ? List.of() : questMatches;
        StringBuilder out = new StringBuilder();
        out.append("# AMI Pack Author Item Report").append(System.lineSeparator()).append(System.lineSeparator());
        out.append("Item: ").append(displayName(node)).append(System.lineSeparator());
        out.append("ID: ").append(node.id()).append(System.lineSeparator());
        out.append("Type: ").append(node.type() == null ? "unknown" : node.type().name()).append(System.lineSeparator());
        out.append("Mod: ").append(node.id().getNamespace()).append(System.lineSeparator());
        out.append("Category: ").append(blankDefault(node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, ""), "unknown"));
        String subcategory = node.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "");
        if (!subcategory.isBlank()) {
            out.append(" / ").append(subcategory);
        }
        out.append(System.lineSeparator());
        out.append("Access: ").append(blankDefault(node.meta(SearchNodeKeys.ACCESS_LEVEL, ""), "unknown")).append(System.lineSeparator());
        out.append("Obtainability: ").append(blankDefault(node.meta(SearchNodeKeys.OBTAINABILITY, ""), "unknown")).append(System.lineSeparator());
        out.append("Recipe Outputs: ").append(recipeCount(node, SearchNodeKeys.RECIPE_OUTPUT_COUNT)).append(System.lineSeparator());
        out.append("Recipe Uses: ").append(recipeCount(node, SearchNodeKeys.RECIPE_USE_COUNT)).append(System.lineSeparator());
        out.append(System.lineSeparator());

        out.append("## Quests").append(System.lineSeparator());
        if (matches.isEmpty()) {
            out.append("- No quest references found.").append(System.lineSeparator());
        } else {
            for (AmiQuestItemMatch match : matches.stream()
                    .filter(match -> match != null && match.quest() != null && match.task() != null)
                    .limit(MAX_QUEST_ROWS)
                    .toList()) {
                out.append("- ").append(questPath(match.quest()))
                        .append(" (").append(match.task().role().name().toLowerCase(Locale.ROOT));
                if (match.task().requiredCount() > 1) {
                    out.append(", x").append(match.task().requiredCount());
                }
                if (match.task().highCardinality()) {
                    out.append(", high-cardinality");
                }
                out.append(")").append(System.lineSeparator());
            }
            if (matches.size() > MAX_QUEST_ROWS) {
                out.append("- ... ").append(matches.size() - MAX_QUEST_ROWS).append(" more quest references")
                        .append(System.lineSeparator());
            }
        }

        List<String> notes = itemNotes(node, matches);
        out.append(System.lineSeparator()).append("## Author Notes").append(System.lineSeparator());
        if (notes.isEmpty()) {
            out.append("- No immediate issues detected from indexed AMI evidence.").append(System.lineSeparator());
        } else {
            for (String note : notes) {
                out.append("- ").append(note).append(System.lineSeparator());
            }
        }
        return out.toString();
    }

    public static String groupReport(String title, List<SearchNode> nodes, List<AmiQuestDocument> questDocuments) {
        List<SearchNode> items = cleanItems(nodes);
        if (items.isEmpty()) {
            return "";
        }

        List<AmiQuestDocument> quests = questDocuments == null ? List.of() : questDocuments;
        Map<ResourceLocation, SearchNode> itemById = new LinkedHashMap<>();
        for (SearchNode item : items) {
            itemById.putIfAbsent(item.id(), item);
        }

        List<SearchNode> unquested = new ArrayList<>();
        List<SearchNode> noRecipe = new ArrayList<>();
        List<SearchNode> restricted = new ArrayList<>();
        Map<String, ModStats> modStats = new LinkedHashMap<>();

        for (SearchNode item : items) {
            List<AmiQuestItemMatch> matches = AmiQuestsApi.getQuestMatchesForItem(item.id());
            boolean quested = !matches.isEmpty();
            boolean recipeDeadEnd = hasNoRecipeOutput(item);
            boolean restrictedAccess = hasRestrictedAccess(item);

            if (!quested) {
                unquested.add(item);
            }
            if (recipeDeadEnd) {
                noRecipe.add(item);
            }
            if (restrictedAccess) {
                restricted.add(item);
            }

            modStats.computeIfAbsent(item.id().getNamespace(), ModStats::new)
                    .add(quested, recipeDeadEnd, restrictedAccess);
        }

        QuestDiagnostics questDiagnostics = questDiagnostics(quests, itemById);

        StringBuilder out = new StringBuilder();
        out.append("# AMI Pack Author Report: ")
                .append(title == null || title.isBlank() ? "Selected Items" : title.trim())
                .append(System.lineSeparator()).append(System.lineSeparator());
        out.append("Items: ").append(items.size()).append(System.lineSeparator());
        out.append("Quest Documents: ").append(quests.size()).append(System.lineSeparator());
        out.append("Quest-linked Items: ").append(items.size() - unquested.size()).append(System.lineSeparator());
        out.append("Unquested Items: ").append(unquested.size()).append(System.lineSeparator());
        out.append("No Recipe Output Evidence: ").append(noRecipe.size()).append(System.lineSeparator());
        out.append("Creative/Cheat/Dev Access: ").append(restricted.size()).append(System.lineSeparator());
        out.append("Missing Quest Item IDs: ").append(questDiagnostics.missingItemIds().size()).append(System.lineSeparator());
        out.append("High-Cardinality Quest Tasks: ").append(questDiagnostics.highCardinalityTasks().size()).append(System.lineSeparator());
        out.append(System.lineSeparator());

        appendModStats(out, modStats);
        appendItems(out, "Unquested Items", unquested, "No quest references found.");
        appendItems(out, "No Recipe Output Evidence", noRecipe, "All selected items have recipe output evidence.");
        appendItems(out, "Creative/Cheat/Dev Access", restricted, "No selected items are marked creative, cheat, or dev access.");
        appendQuestDiagnostics(out, questDiagnostics);
        return out.toString();
    }

    private static QuestDiagnostics questDiagnostics(List<AmiQuestDocument> quests, Map<ResourceLocation, SearchNode> itemById) {
        Set<ResourceLocation> missing = new LinkedHashSet<>();
        List<String> missingRows = new ArrayList<>();
        List<String> noRecipeRows = new ArrayList<>();
        List<String> highCardinalityRows = new ArrayList<>();

        for (AmiQuestDocument quest : quests) {
            if (quest == null) continue;
            if (!questTouchesSelectedItems(quest, itemById)) continue;
            for (AmiQuestTaskDocument task : quest.tasks()) {
                if (task == null) continue;
                if (task.highCardinality()) {
                    highCardinalityRows.add(questPath(quest) + " > " + taskLabel(task));
                }
                for (ResourceLocation itemId : task.itemIds()) {
                    SearchNode item = itemById.get(itemId);
                    if (item == null) {
                        if (missing.add(itemId)) {
                            missingRows.add(itemId + " (" + questPath(quest) + ")");
                        }
                    } else if (task.role() == AmiQuestTaskDocument.Role.REQUIREMENT && hasNoRecipeOutput(item)) {
                        noRecipeRows.add(item.id() + " (" + questPath(quest) + ")");
                    }
                }
            }
        }

        missingRows.sort(String::compareTo);
        noRecipeRows.sort(String::compareTo);
        highCardinalityRows.sort(String::compareTo);
        return new QuestDiagnostics(missingRows, noRecipeRows, highCardinalityRows);
    }

    private static boolean questTouchesSelectedItems(AmiQuestDocument quest, Map<ResourceLocation, SearchNode> itemById) {
        if (quest == null || itemById == null || itemById.isEmpty()) {
            return false;
        }
        for (AmiQuestTaskDocument task : quest.tasks()) {
            if (task == null) continue;
            for (ResourceLocation itemId : task.itemIds()) {
                if (itemById.containsKey(itemId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void appendModStats(StringBuilder out, Map<String, ModStats> stats) {
        out.append("## By Mod").append(System.lineSeparator());
        stats.values().stream()
                .sorted(Comparator.comparingInt(ModStats::items).reversed().thenComparing(ModStats::modId))
                .limit(MAX_MOD_ROWS)
                .forEach(stat -> out.append("- ").append(stat.modId())
                        .append(": ").append(stat.items()).append(" items, ")
                        .append(stat.quested()).append(" quest-linked, ")
                        .append(stat.noRecipe()).append(" no-recipe, ")
                        .append(stat.restricted()).append(" restricted")
                        .append(System.lineSeparator()));
        if (stats.size() > MAX_MOD_ROWS) {
            out.append("- ... ").append(stats.size() - MAX_MOD_ROWS).append(" more mods").append(System.lineSeparator());
        }
        out.append(System.lineSeparator());
    }

    private static void appendItems(StringBuilder out, String title, List<SearchNode> items, String emptyText) {
        out.append("## ").append(title).append(System.lineSeparator());
        if (items.isEmpty()) {
            out.append("- ").append(emptyText).append(System.lineSeparator()).append(System.lineSeparator());
            return;
        }
        items.stream()
                .sorted(Comparator.comparing((SearchNode node) -> node.id().toString()))
                .limit(MAX_DETAIL_ROWS)
                .forEach(node -> out.append("- ").append(node.id())
                        .append(" | ").append(displayName(node))
                        .append(" | access=").append(blankDefault(node.meta(SearchNodeKeys.ACCESS_LEVEL, ""), "unknown"))
                        .append(" | recipes=").append(recipeCount(node, SearchNodeKeys.RECIPE_OUTPUT_COUNT))
                        .append(System.lineSeparator()));
        if (items.size() > MAX_DETAIL_ROWS) {
            out.append("- ... ").append(items.size() - MAX_DETAIL_ROWS).append(" more").append(System.lineSeparator());
        }
        out.append(System.lineSeparator());
    }

    private static void appendQuestDiagnostics(StringBuilder out, QuestDiagnostics diagnostics) {
        out.append("## Quest Diagnostics").append(System.lineSeparator());
        appendRows(out, "Missing indexed item IDs", diagnostics.missingItemIds(),
                "No quest task item IDs are missing from the selected item set.");
        appendRows(out, "Quest requirements with no recipe output evidence", diagnostics.noRecipeRequirements(),
                "No selected quest requirements are recipe dead ends by AMI metadata.");
        appendRows(out, "High-cardinality quest tasks", diagnostics.highCardinalityTasks(),
                "No high-cardinality quest tasks were reported by registered quest providers.");
    }

    private static void appendRows(StringBuilder out, String label, List<String> rows, String emptyText) {
        out.append("### ").append(label).append(System.lineSeparator());
        if (rows.isEmpty()) {
            out.append("- ").append(emptyText).append(System.lineSeparator());
            return;
        }
        rows.stream().limit(MAX_DETAIL_ROWS).forEach(row -> out.append("- ").append(row).append(System.lineSeparator()));
        if (rows.size() > MAX_DETAIL_ROWS) {
            out.append("- ... ").append(rows.size() - MAX_DETAIL_ROWS).append(" more").append(System.lineSeparator());
        }
    }

    private static List<SearchNode> cleanItems(List<SearchNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        Map<ResourceLocation, SearchNode> unique = new LinkedHashMap<>();
        for (SearchNode node : nodes) {
            if (node != null && node.type() == NodeType.ITEM && node.id() != null) {
                unique.putIfAbsent(node.id(), node);
            }
        }
        return List.copyOf(unique.values());
    }

    private static List<String> itemNotes(SearchNode node, List<AmiQuestItemMatch> questMatches) {
        List<String> notes = new ArrayList<>();
        if (questMatches == null || questMatches.isEmpty()) {
            notes.add("No quest coverage found for this item.");
        }
        if (hasNoRecipeOutput(node)) {
            notes.add("No indexed recipe output evidence; verify loot, worldgen, shop, quest reward, or script path.");
        }
        if (hasRestrictedAccess(node)) {
            notes.add("Access is " + node.meta(SearchNodeKeys.ACCESS_LEVEL, "") + "; avoid requiring it unless intentionally gated.");
        }
        if ("dev".equals(node.meta(SearchNodeKeys.ACCESS_LEVEL, ""))) {
            notes.add("Dev-only item is visible because AMI is in author/debug mode or hidden items are enabled.");
        }
        return notes;
    }

    private static boolean hasNoRecipeOutput(SearchNode node) {
        if (node == null) {
            return false;
        }
        String obtainability = node.meta(SearchNodeKeys.OBTAINABILITY, "");
        if ("no_recipe".equals(obtainability)) {
            return true;
        }
        String raw = node.meta(SearchNodeKeys.RECIPE_OUTPUT_COUNT, "");
        if (raw.isBlank()) {
            return false;
        }
        try {
            return Integer.parseInt(raw.trim()) == 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean hasRestrictedAccess(SearchNode node) {
        if (node == null) {
            return false;
        }
        return switch (node.meta(SearchNodeKeys.ACCESS_LEVEL, "")) {
            case "creative", "cheat", "dev" -> true;
            default -> false;
        };
    }

    private static int recipeCount(SearchNode node, String key) {
        if (node == null || key == null) {
            return 0;
        }
        String raw = node.meta(key, "");
        if (raw.isBlank()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String displayName(SearchNode node) {
        if (node == null) {
            return "";
        }
        if (node.displayName() != null && !node.displayName().isBlank()) {
            return node.displayName();
        }
        return node.id() == null ? "" : node.id().getPath().replace('_', ' ');
    }

    private static String questPath(AmiQuestDocument quest) {
        if (quest == null) {
            return "unknown quest";
        }
        String title = quest.title().isBlank() ? quest.id() : quest.title();
        if (!quest.chapterTitle().isBlank()) {
            return quest.chapterTitle() + " > " + title;
        }
        return title;
    }

    private static String taskLabel(AmiQuestTaskDocument task) {
        if (task == null) {
            return "unknown task";
        }
        if (!task.title().isBlank()) {
            return task.title();
        }
        if (!task.taskType().isBlank()) {
            return task.taskType();
        }
        return task.id();
    }

    private static String blankDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record QuestDiagnostics(
            List<String> missingItemIds,
            List<String> noRecipeRequirements,
            List<String> highCardinalityTasks
    ) {
    }

    private static final class ModStats {
        private final String modId;
        private int items;
        private int quested;
        private int noRecipe;
        private int restricted;

        private ModStats(String modId) {
            this.modId = modId;
        }

        private void add(boolean quested, boolean noRecipe, boolean restricted) {
            this.items++;
            if (quested) this.quested++;
            if (noRecipe) this.noRecipe++;
            if (restricted) this.restricted++;
        }

        private String modId() {
            return modId;
        }

        private int items() {
            return items;
        }

        private int quested() {
            return quested;
        }

        private int noRecipe() {
            return noRecipe;
        }

        private int restricted() {
            return restricted;
        }
    }
}
