package com.sanhiruzu.ami.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.sanhiruzu.ami.client.EntityIconCache;
import com.sanhiruzu.ami.client.results.*;
import com.sanhiruzu.ami.fabric.AmiFabric;
import com.sanhiruzu.ami.index.*;
import com.sanhiruzu.ami.index.providers.RecipeViewerItemAudit;
import com.sanhiruzu.ami.client.icon.RendererRegistry;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Fabric equivalent of NeoForge/Forge's {@code AmiClientCommands}: registers the {@code /ami} client
 * command tree via Fabric API's client-only command dispatcher instead of an FML event.
 */
public final class AmiClientCommands {
    private AmiClientCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                registerAmiCommand(dispatcher));
    }

    private static void registerAmiCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> dumpCmd = ClientCommandManager.literal("dump")
                .executes(context -> {
                    exportRegistryDump(context.getSource());
                    return 1;
                })
                .then(ClientCommandManager.literal("all")
                        .executes(context -> {
                            dumpAll(context.getSource());
                            return 1;
                        })
                )
                .then(ClientCommandManager.literal("tree")
                        .executes(context -> {
                            exportResultsTree(context.getSource(), "");
                            return 1;
                        })
                        .then(ClientCommandManager.argument("query", StringArgumentType.greedyString())
                                .executes(context -> {
                                    exportResultsTree(context.getSource(), StringArgumentType.getString(context, "query"));
                                    return 1;
                                })
                        )
                )
                .then(ClientCommandManager.literal("search-nodes")
                        .executes(context -> {
                            exportSearchNodes(context.getSource());
                            return 1;
                        })
                )
                .then(ClientCommandManager.literal("guide-docs")
                        .executes(context -> {
                            exportGuideDocs(context.getSource());
                            return 1;
                        })
                )
                .then(ClientCommandManager.literal("recipes")
                        .executes(context -> {
                            exportRuntimeRecipes(context.getSource());
                            return 1;
                        })
                )
                .then(ClientCommandManager.literal("loot-tables")
                        .executes(context -> {
                            exportLootTables(context.getSource());
                            return 1;
                        })
                )
                .then(ClientCommandManager.literal("recipe-viewer-items")
                        .executes(context -> {
                            exportRecipeViewerItemAudit(context.getSource());
                            return 1;
                        })
                )
                .then(ClientCommandManager.literal("recipe-viewer-recipes")
                        .executes(context -> {
                            exportRecipeViewerRecipes(context.getSource());
                            return 1;
                        })
                )
                .then(ClientCommandManager.literal("pokemon-coverage")
                        .executes(context -> {
                            exportPokemonCoverage(context.getSource());
                            return 1;
                        })
                );

        LiteralArgumentBuilder<FabricClientCommandSource> cmd = ClientCommandManager.literal("ami")
                .then(dumpCmd)
                .then(ClientCommandManager.literal("reindex")
                        .executes(context -> {
                            invalidateAndRebuildIndex(context.getSource());
                            return 1;
                        })
                );

        dispatcher.register(cmd);
    }

    private static void exportResultsTree(FabricClientCommandSource source, String query) {
        Path dumpDir = dumpDir("results");
        try {
            Files.createDirectories(dumpDir);
            Path out = dumpDir.resolve("results_tree_dump.md");
            Files.writeString(out, buildResultsTreeDump(query == null ? "" : query.trim()));
            source.sendFeedback(Component.literal("AMI results tree dump written to " + out.toAbsolutePath())
                    .withStyle(ChatFormatting.GREEN));
        } catch (Exception e) {
            AmiFabric.LOGGER.error("Failed to export results tree dump", e);
            source.sendFeedback(Component.literal("Failed to export AMI results tree dump: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static void exportSearchNodes(FabricClientCommandSource source) {
        Path dumpDir = dumpDir("search");
        try {
            Files.createDirectories(dumpDir);
            Path out = dumpDir.resolve("search_nodes.jsonl");
            Path meta = dumpDir.resolve("search_nodes.meta.json");
            List<SearchNode> nodes = SearchNodeMirrorDump.runtimeAtlasNodes();
            int count = SearchNodeMirrorDump.writeJsonl(out, nodes);
            SearchNodeMirrorDump.writeMeta(meta);
            source.sendFeedback(Component.literal(
                            "AMI search node mirror written to " + out.toAbsolutePath() +
                                    " (" + count + " nodes) + metadata at " + meta.toAbsolutePath())
                    .withStyle(ChatFormatting.GREEN));
        } catch (Exception e) {
            AmiFabric.LOGGER.error("Failed to export search node mirror", e);
            source.sendFeedback(Component.literal("Failed to export AMI search node mirror: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static void exportGuideDocs(FabricClientCommandSource source) {
        Path dumpDir = dumpDir("guides");
        try {
            Files.createDirectories(dumpDir);
            Path out = dumpDir.resolve("guide_docs.jsonl");
            int count = GuideDocumentMirrorDump.writeJsonl(out,
                    AmiIndexerService.getInstance().getGuideSearchIndex().allDocuments());
            source.sendFeedback(Component.literal("AMI guide docs mirror written to " + out.toAbsolutePath() + " (" + count + " docs)")
                    .withStyle(ChatFormatting.GREEN));
        } catch (Exception e) {
            AmiFabric.LOGGER.error("Failed to export AMI guide docs mirror", e);
            source.sendFeedback(Component.literal("Failed to export AMI guide docs mirror: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static void exportRuntimeRecipes(FabricClientCommandSource source) {
        Path dumpDir = dumpDir("recipes");
        try {
            Files.createDirectories(dumpDir);
            RecipeDumpWriters.RuntimeRecipeDumpOutputs outputs = RecipeDumpWriters.writeRuntimeRecipes(
                    dumpDir,
                    net.minecraft.client.Minecraft.getInstance().level
            );
            source.sendFeedback(Component.literal(
                            "AMI runtime recipe dump written to "
                                    + outputs.dump().toAbsolutePath()
                                    + " ("
                                    + outputs.recipeCount()
                                    + " recipes) + metadata at "
                                    + outputs.meta().toAbsolutePath()
                                    + "; reports at "
                                    + outputs.csv().toAbsolutePath()
                                    + " and "
                                    + outputs.markdown().toAbsolutePath())
                    .withStyle(ChatFormatting.GREEN));
        } catch (Exception e) {
            AmiFabric.LOGGER.error("Failed to export AMI runtime recipe dump", e);
            source.sendFeedback(Component.literal("Failed to export AMI runtime recipe dump: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static void exportLootTables(FabricClientCommandSource source) {
        Path dumpDir = dumpDir("loot_tables");
        try {
            Files.createDirectories(dumpDir);
            RecipeDumpWriters.LootTableDumpOutputs outputs = RecipeDumpWriters.writeLootTables(dumpDir);
            source.sendFeedback(Component.literal(
                            "AMI loot table dump written to "
                                    + outputs.dump().toAbsolutePath()
                                    + " ("
                                    + outputs.tableCount()
                                    + " tables) + metadata at "
                                    + outputs.meta().toAbsolutePath()
                                    + "; reports at "
                                    + outputs.csv().toAbsolutePath()
                                    + " and "
                                    + outputs.markdown().toAbsolutePath())
                    .withStyle(ChatFormatting.GREEN));
        } catch (Exception e) {
            AmiFabric.LOGGER.error("Failed to export AMI loot table dump", e);
            source.sendFeedback(Component.literal("Failed to export AMI loot table dump: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static void exportRecipeViewerItemAudit(FabricClientCommandSource source) {
        Path dumpDir = dumpDir("recipe_viewer");
        try {
            Files.createDirectories(dumpDir);
            RecipeViewerItemAudit.AuditOutputs outputs = RecipeViewerItemAudit.writeDump(
                    dumpDir,
                    net.minecraft.client.Minecraft.getInstance().level
            );
            source.sendFeedback(Component.literal(
                            "AMI recipe viewer item audit written to "
                                    + outputs.reportMarkdown().toAbsolutePath()
                                    + " and "
                                    + outputs.reportJson().toAbsolutePath())
                    .withStyle(ChatFormatting.GREEN));
        } catch (Exception | LinkageError e) {
            AmiFabric.LOGGER.error("Failed to export AMI recipe viewer item audit", e);
            source.sendFeedback(Component.literal("Failed to export AMI recipe viewer item audit: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static void exportRegistryDump(FabricClientCommandSource source) {
        Path dumpDir = dumpDir("registry");
        try {
            Files.createDirectories(dumpDir);
            Path out = dumpDir.resolve("registry-dump.json");
            int count = RegistryDumpWriter.writeJson(out,
                    RegistryDumpWriter.collectFromRuntime(net.minecraft.client.Minecraft.getInstance().level));
            source.sendFeedback(Component.literal(
                    "AMI registry dump written to " + out.toAbsolutePath() + " (" + count + " items)")
                    .withStyle(ChatFormatting.GREEN));
        } catch (Exception e) {
            AmiFabric.LOGGER.error("Failed to export AMI registry dump", e);
            source.sendFeedback(Component.literal("Failed to export AMI registry dump: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static void exportRecipeViewerRecipes(FabricClientCommandSource source) {
        Path dumpDir = dumpDir("recipe_viewer");
        try {
            Files.createDirectories(dumpDir);
            RecipeDumpWriters.ViewerRecipeDumpOutputs outputs = RecipeDumpWriters.writeViewerRecipes(
                    dumpDir,
                    net.minecraft.client.Minecraft.getInstance().level
            );
            source.sendFeedback(Component.literal(
                            "AMI recipe viewer recipe dump written to "
                                    + outputs.meta().toAbsolutePath()
                                    + " ("
                                    + outputs.totalRecipes()
                                    + " recipes across "
                                    + outputs.datasets().size()
                                    + " datasets)")
                    .withStyle(ChatFormatting.GREEN));
        } catch (Exception | LinkageError e) {
            AmiFabric.LOGGER.error("Failed to export AMI recipe viewer recipe dump", e);
            source.sendFeedback(Component.literal("Failed to export AMI recipe viewer recipe dump: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static void exportPokemonCoverage(FabricClientCommandSource source) {
        Path dumpDir = dumpDir("pokemon_coverage");
        try {
            Files.createDirectories(dumpDir);
            PokemonCoverageDumpWriter.Outputs outputs = PokemonCoverageDumpWriter.writeDump(dumpDir);
            source.sendFeedback(Component.literal(
                            "AMI Pokemon coverage written to "
                                    + outputs.html().toAbsolutePath()
                                    + ", "
                                    + outputs.csv().toAbsolutePath()
                                    + ", and "
                                    + outputs.json().toAbsolutePath()
                                    + " ("
                                    + outputs.pokemonCount()
                                    + " Pokemon rows)")
                    .withStyle(ChatFormatting.GREEN));
        } catch (Exception e) {
            AmiFabric.LOGGER.error("Failed to export AMI Pokemon coverage", e);
            source.sendFeedback(Component.literal("Failed to export AMI Pokemon coverage: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static Path dumpDir(String category) {
        return FabricLoader.getInstance().getGameDir().resolve("ami_dumps").resolve(category);
    }

    private static void invalidateAndRebuildIndex(FabricClientCommandSource source) {
        try {
            boolean deleted = GlobalIndexCache.invalidateCurrent();
            boolean accepted = AmiIndexerService.getInstance().rebuild(true);
            if (accepted) {
                invalidateRuntimeCaches();
                source.sendFeedback(Component.literal("AMI index cache "
                                + (deleted ? "deleted" : "was already absent")
                                + "; forced reindex and icon cache reset started")
                        .withStyle(ChatFormatting.GREEN));
            } else {
                source.sendFeedback(Component.literal("AMI index cache "
                                + (deleted ? "deleted" : "was already absent")
                                + ", but a reindex is already running")
                        .withStyle(ChatFormatting.YELLOW));
            }
        } catch (Exception e) {
            AmiFabric.LOGGER.error("Failed to invalidate AMI index cache", e);
            source.sendFeedback(Component.literal("Failed to invalidate AMI index cache: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static void invalidateRuntimeCaches() {
        RendererRegistry.invalidateAll();
        EntityIconCache.invalidateAndPurgePersistentCache();
    }

    private static void dumpAll(FabricClientCommandSource source) {
        exportRegistryDump(source);
        exportSearchNodes(source);
        exportGuideDocs(source);
        exportRuntimeRecipes(source);
        exportLootTables(source);
        exportRecipeViewerItemAudit(source);
        exportRecipeViewerRecipes(source);
        exportPokemonCoverage(source);
        exportResultsTree(source, "");
    }

    private static String buildResultsTreeDump(String query) {
        StringBuilder report = new StringBuilder();
        report.append("# AMI Runtime Results Tree Dump\n\n");
        report.append("AMI version: ").append(AmiDebugSettings.versionLabel()).append("\n");
        report.append("Debug build: ").append(AmiDebugSettings.debugBuild()).append("\n");
        report.append("Index ready: ").append(GlobalIndex.getInstance().isIndexReady()).append("\n\n");
        report.append("Guide docs: ").append(AmiIndexerService.getInstance().getGuideSearchIndex().allDocuments().size()).append("\n\n");

        List<SearchNode> all = new ArrayList<>();
        for (NodeType type : NodeType.atlasValues()) {
            all.addAll(GlobalIndex.getInstance().getNodes(type));
        }
        SearchService searchService = SearchService.buildFrom(GlobalIndex.getInstance());

        appendLivePanels(report);
        appendProjection(report, "runtime-default-list", all, searchService, query, false, ResultsToolbar.ViewMode.LIST);
        appendProjection(report, "runtime-default-grid", all, searchService, query, false, ResultsToolbar.ViewMode.GRID);
        appendProjection(report, "runtime-compact-grid", all, searchService, query, true, ResultsToolbar.ViewMode.GRID);
        return report.toString();
    }

    private static void appendLivePanels(StringBuilder report) {
        List<UniversalResultsPanel> panels = InventoryOverlayHandler.getManager().getDebugVisibleResultPanels();
        if (panels.isEmpty()) {
            report.append("## Live Visible Overlay Panels\n\nnone\n\n");
            return;
        }

        int index = 1;
        for (UniversalResultsPanel panel : panels) {
            appendLivePanel(report, "visible-overlay-panel-" + index, panel);
            index++;
        }
    }

    private static void appendLivePanel(StringBuilder report, String label, UniversalResultsPanel panel) {
        List<TreeNode> roots = panel.getDebugRootNodes();
        report.append("## ").append(label).append("\n\n");
        report.append(panel.getDebugSummary()).append("\n\n");
        appendTreeAndGrid(report, roots);
    }

    private static void appendProjection(StringBuilder report, String label, List<SearchNode> nodes,
                                         SearchService searchService, String query, boolean compact,
                                         ResultsToolbar.ViewMode viewMode) {
        SearchState state = new SearchState();
        state.setQuery(query);
        state.setViewMode(viewMode);
        ResultsViewProjector.Projection projection = ResultsViewProjector.project(
                nodes,
                state,
                searchService,
                AmiIndexerService.getInstance().getGuideSearchIndex(),
                compact,
                false
        );
        report.append("## ").append(label).append("\n\n");
        report.append(projection.summary()).append("\n\n");
        appendGuideRows(report, projection.guideRows());
        appendTreeAndGrid(report, projection.roots());
    }

    private static void appendGuideRows(StringBuilder report, List<GuideResultRow> guideRows) {
        report.append("Guide rows:\n\n```text\n");
        if (guideRows.isEmpty()) {
            report.append("none\n");
        } else {
            for (GuideResultRow row : guideRows) {
                report.append(row.document().id())
                        .append(" | ")
                        .append(row.title())
                        .append(" | ")
                        .append(row.sourceLine())
                        .append(" | open=")
                        .append(row.document().canOpen())
                        .append("\n");
            }
        }
        report.append("```\n\n");
    }

    private static void appendTreeAndGrid(StringBuilder report, List<TreeNode> roots) {
        report.append("Tree:\n\n```text\n")
                .append(ResultsTreeShapeDump.dumpTree(roots))
                .append("```\n\n");
        report.append("Grid:\n\n```text\n")
                .append(ResultsTreeShapeDump.dumpGrid(roots, 9))
                .append("```\n\n");
    }
}
