package com.sanhiruzu.ami.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.sanhiruzu.ami.client.EntityIconCache;
import com.sanhiruzu.ami.client.results.*;
import com.sanhiruzu.ami.forge.AMI;
import com.sanhiruzu.ami.index.*;
import com.sanhiruzu.ami.index.providers.RecipeViewerItemAudit;
import com.sanhiruzu.ami.client.icon.RendererRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class AmiClientCommands {
    @SubscribeEvent
    public static void onClientCommandsRegister(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        LiteralArgumentBuilder<CommandSourceStack> cmd = Commands.literal("ami")
                .then(Commands.literal("dump-search-nodes")
                        .executes(context -> {
                            exportSearchNodes(context.getSource());
                            return 1;
                        })
                )
                .then(Commands.literal("dump-guide-docs")
                        .executes(context -> {
                            exportGuideDocs(context.getSource());
                            return 1;
                        })
                )
                .then(Commands.literal("dump-recipe-viewer-items")
                        .executes(context -> {
                            exportRecipeViewerItemAudit(context.getSource());
                            return 1;
                        })
                )
                .then(Commands.literal("reindex")
                        .executes(context -> {
                            invalidateAndRebuildIndex(context.getSource());
                            return 1;
                        })
                );

        cmd.then(Commands.literal("dump-results-tree")
                .executes(context -> {
                    exportResultsTree(context.getSource(), "");
                    return 1;
                })
                .then(Commands.argument("query", StringArgumentType.greedyString())
                        .executes(context -> {
                            exportResultsTree(context.getSource(), StringArgumentType.getString(context, "query"));
                            return 1;
                        }))
        );

        dispatcher.register(cmd);
    }

    private static void exportResultsTree(CommandSourceStack source, String query) {
        Path dumpDir = FMLPaths.GAMEDIR.get().resolve("ami_dumps");
        try {
            Files.createDirectories(dumpDir);
            Path out = dumpDir.resolve("results_tree_dump.md");
            Files.writeString(out, buildResultsTreeDump(query == null ? "" : query.trim()));
            source.sendSystemMessage(Component.literal("AMI results tree dump written to " + out.toAbsolutePath())
                    .withStyle(ChatFormatting.GREEN));
        } catch (Exception e) {
            AMI.LOGGER.error("Failed to export results tree dump", e);
            source.sendSystemMessage(Component.literal("Failed to export AMI results tree dump: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static void exportSearchNodes(CommandSourceStack source) {
        Path dumpDir = FMLPaths.GAMEDIR.get().resolve("ami_dumps");
        try {
            Files.createDirectories(dumpDir);
            Path out = dumpDir.resolve("search_nodes.jsonl");
            int count = SearchNodeMirrorDump.writeJsonl(out, SearchNodeMirrorDump.runtimeAtlasNodes());
            source.sendSystemMessage(Component.literal("AMI search node mirror written to " + out.toAbsolutePath() + " (" + count + " nodes)")
                    .withStyle(ChatFormatting.GREEN));
        } catch (Exception e) {
            AMI.LOGGER.error("Failed to export search node mirror", e);
            source.sendSystemMessage(Component.literal("Failed to export AMI search node mirror: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static void exportGuideDocs(CommandSourceStack source) {
        Path dumpDir = FMLPaths.GAMEDIR.get().resolve("ami_dumps");
        try {
            Files.createDirectories(dumpDir);
            Path out = dumpDir.resolve("guide_docs.jsonl");
            int count = GuideDocumentMirrorDump.writeJsonl(out,
                    AmiIndexerService.getInstance().getGuideSearchIndex().allDocuments());
            source.sendSystemMessage(Component.literal("AMI guide docs mirror written to " + out.toAbsolutePath() + " (" + count + " docs)")
                    .withStyle(ChatFormatting.GREEN));
        } catch (Exception e) {
            AMI.LOGGER.error("Failed to export AMI guide docs mirror", e);
            source.sendSystemMessage(Component.literal("Failed to export AMI guide docs mirror: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static void exportRecipeViewerItemAudit(CommandSourceStack source) {
        Path dumpDir = FMLPaths.GAMEDIR.get().resolve("ami_dumps");
        try {
            Files.createDirectories(dumpDir);
            RecipeViewerItemAudit.AuditOutputs outputs = RecipeViewerItemAudit.writeDump(
                    dumpDir,
                    net.minecraft.client.Minecraft.getInstance().level
            );
            source.sendSystemMessage(Component.literal(
                            "AMI recipe viewer item audit written to "
                                    + outputs.reportMarkdown().toAbsolutePath()
                                    + " and "
                                    + outputs.reportJson().toAbsolutePath())
                    .withStyle(ChatFormatting.GREEN));
        } catch (Exception e) {
            AMI.LOGGER.error("Failed to export AMI recipe viewer item audit", e);
            source.sendSystemMessage(Component.literal("Failed to export AMI recipe viewer item audit: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static void invalidateAndRebuildIndex(CommandSourceStack source) {
        try {
            boolean deleted = GlobalIndexCache.invalidateCurrent();
            boolean accepted = AmiIndexerService.getInstance().rebuild(true);
            if (accepted) {
                invalidateRuntimeCaches();
                source.sendSystemMessage(Component.literal("AMI index cache "
                                + (deleted ? "deleted" : "was already absent")
                                + "; forced reindex and icon cache reset started")
                        .withStyle(ChatFormatting.GREEN));
            } else {
                source.sendSystemMessage(Component.literal("AMI index cache "
                                + (deleted ? "deleted" : "was already absent")
                                + ", but a reindex is already running")
                        .withStyle(ChatFormatting.YELLOW));
            }
        } catch (Exception e) {
            AMI.LOGGER.error("Failed to invalidate AMI index cache", e);
            source.sendSystemMessage(Component.literal("Failed to invalidate AMI index cache: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static void invalidateRuntimeCaches() {
        RendererRegistry.invalidateAll();
        EntityIconCache.invalidateAndPurgePersistentCache();
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
