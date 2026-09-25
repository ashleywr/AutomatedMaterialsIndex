package com.sanhiruzu.ami.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.sanhiruzu.ami.client.RecipeDumpWriters;
import com.sanhiruzu.ami.fabric.AmiFabric;
import com.sanhiruzu.ami.index.RegistryDumpWriter;
import com.sanhiruzu.ami.index.WorldgenDumpWriter;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

/** Server-side {@code /ami dump}, usable from a Fabric dedicated-server console. */
public final class AmiServerDumpCommand {

    private AmiServerDumpCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> dump = Commands.literal("dump")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> dumpAll(context.getSource()))
                .then(Commands.literal("all").executes(context -> dumpAll(context.getSource())))
                .then(Commands.literal("items").executes(context -> dumpItems(context.getSource())))
                .then(Commands.literal("recipes").executes(context -> dumpRecipes(context.getSource())))
                .then(Commands.literal("loot-tables").executes(context -> dumpLootTables(context.getSource())))
                .then(Commands.literal("trades").executes(context -> dumpTrades(context.getSource())))
                .then(Commands.literal("worldgen").executes(context -> dumpWorldgen(context.getSource())));

        dispatcher.register(Commands.literal("ami").then(dump));
    }

    private static int dumpAll(CommandSourceStack source) {
        int written = dumpItems(source)
                + dumpRecipes(source)
                + dumpLootTables(source)
                + dumpTrades(source)
                + dumpWorldgen(source);
        info(source, "AMI dump complete (" + written + "/5 sections written)");
        return written;
    }

    private static int dumpItems(CommandSourceStack source) {
        try {
            Path out = dumpDir("registry").resolve("registry-dump.json");
            java.nio.file.Files.createDirectories(out.getParent());
            int count = RegistryDumpWriter.writeJson(out, RegistryDumpWriter.collectFromRegistry());
            info(source, "items: " + count + " -> " + out.toAbsolutePath());
            return 1;
        } catch (Exception e) {
            return fail(source, "items", e);
        }
    }

    private static int dumpRecipes(CommandSourceStack source) {
        try {
            RecipeDumpWriters.RuntimeRecipeDumpOutputs outputs =
                    RecipeDumpWriters.writeRuntimeRecipes(dumpDir("recipes"), source.getLevel());
            info(source, "recipes: " + outputs.recipeCount() + " -> " + outputs.dump().toAbsolutePath());
            return 1;
        } catch (Exception e) {
            return fail(source, "recipes", e);
        }
    }

    private static int dumpLootTables(CommandSourceStack source) {
        try {
            MinecraftServer server = source.getServer();
            RecipeDumpWriters.LootTableDumpOutputs outputs = RecipeDumpWriters.writeLootTables(
                    dumpDir("loot_tables"), server.getResourceManager());
            info(source, "loot tables: " + outputs.tableCount() + " -> " + outputs.dump().toAbsolutePath());
            return 1;
        } catch (Exception e) {
            return fail(source, "loot-tables", e);
        }
    }

    private static int dumpTrades(CommandSourceStack source) {
        try {
            Path out = dumpDir("trades").resolve("trades_runtime.json");
            TradeDumpWriter.Counts counts = TradeDumpWriter.writeJson(out, source.getLevel());
            info(source, "trades: " + counts.trades() + " candidates, " + counts.unresolved()
                    + " unresolved -> " + out.toAbsolutePath());
            return 1;
        } catch (Exception e) {
            return fail(source, "trades", e);
        }
    }

    private static int dumpWorldgen(CommandSourceStack source) {
        try {
            Path out = dumpDir("worldgen").resolve("worldgen-dump.json");
            WorldgenDumpWriter.Counts counts =
                    WorldgenDumpWriter.writeJson(out, source.getServer().registryAccess());
            info(source, "worldgen: " + counts.biomes() + " biomes, " + counts.structures()
                    + " structures, " + counts.dimensions() + " dimensions -> " + out.toAbsolutePath());
            return 1;
        } catch (Exception e) {
            return fail(source, "worldgen", e);
        }
    }

    private static Path dumpDir(String category) {
        return FabricLoader.getInstance().getGameDir().resolve("ami_dumps").resolve(category);
    }

    private static void info(CommandSourceStack source, String message) {
        source.sendSystemMessage(Component.literal("[AMI] " + message).withStyle(ChatFormatting.GREEN));
        AmiFabric.LOGGER.info("[AMI dump] {}", message);
    }

    private static int fail(CommandSourceStack source, String section, Exception error) {
        AmiFabric.LOGGER.error("Failed to write AMI {} dump", section, error);
        source.sendSystemMessage(Component.literal("[AMI] " + section + " dump FAILED: " + error)
                .withStyle(ChatFormatting.RED));
        return 0;
    }
}
