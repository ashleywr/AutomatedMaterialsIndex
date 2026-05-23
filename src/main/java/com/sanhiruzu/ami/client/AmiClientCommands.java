package com.sanhiruzu.ami.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.benchmark.AmiOntologyDiagnostics;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;

@EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class AmiClientCommands {

    @SubscribeEvent
    public static void onClientCommandsRegister(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        LiteralArgumentBuilder<CommandSourceStack> cmd = Commands.literal("ami")
            .then(Commands.literal("dump-ontology")
                .executes(context -> {
                    exportOntologyToCsv(context.getSource());
                    return 1;
                })
            )
            .then(Commands.literal("dump-fallback-sample")
                .executes(context -> {
                    exportFallbackSampleToCsv(context.getSource());
                    return 1;
                })
            );
            
        dispatcher.register(cmd);
    }

    private static void exportOntologyToCsv(CommandSourceStack source) {
        Path configDir = FMLPaths.GAMEDIR.get().resolve("ami_dumps");
        try {
            Files.createDirectories(configDir);
            Path csvFile = configDir.resolve("ontology_dump.csv");
            AmiOntologyDiagnostics.exportOntologyCsv(csvFile);
            source.sendSystemMessage(Component.translatable("ami.command.ontology_exported", csvFile.toAbsolutePath()).withStyle(ChatFormatting.GREEN));
        } catch (Exception e) {
            AMI.LOGGER.error("Failed to export ontology", e);
            source.sendSystemMessage(Component.translatable("ami.command.ontology_export_failed", e.getMessage()).withStyle(ChatFormatting.RED));
        }
    }

    private static void exportFallbackSampleToCsv(CommandSourceStack source) {
        Path configDir = FMLPaths.GAMEDIR.get().resolve("ami_dumps");
        try {
            Files.createDirectories(configDir);
            Path csvFile = configDir.resolve("facet_fallback_sample.csv");
            AmiOntologyDiagnostics.FallbackSampleSummary summary = AmiOntologyDiagnostics.exportFacetFallbackCsv(csvFile);
            source.sendSystemMessage(Component.translatable("ami.command.fallback_exported", csvFile.toAbsolutePath())
                    .append(Component.translatable("ami.command.fallback_summary",
                            summary.playerVisibleFacetlessItems(), summary.playerVisibleItems(),
                            summary.playerVisibleUnresolvedFacetfulItems()))
                    .withStyle(ChatFormatting.GREEN));
        } catch (Exception e) {
            AMI.LOGGER.error("Failed to export fallback sample", e);
            source.sendSystemMessage(Component.translatable("ami.command.fallback_export_failed", e.getMessage()).withStyle(ChatFormatting.RED));
        }
    }
}
