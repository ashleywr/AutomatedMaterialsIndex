package com.sanhiruzu.ami.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.sanhiruzu.ami.client.RecipeDumpWriters;
import com.sanhiruzu.ami.index.RegistryDumpWriter;
import com.sanhiruzu.ami.index.WorldgenDumpWriter;
import com.sanhiruzu.ami.neoforge.AMI;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

/**
 * Server-side {@code /ami dump}, usable from a dedicated server console.
 *
 * <p>{@link com.sanhiruzu.ami.client.AmiClientCommands} registers through
 * {@code RegisterClientCommandsEvent}, which does not exist on a dedicated server, so
 * none of its dumps could be produced headlessly. That matters because a headless server
 * is the natural place to ask "what is actually in this pack" - it loads the full mod set
 * with no GPU, no resource packs and no human clicking.
 *
 * <p>This registers the subset that is genuinely server-side. Deliberately omitted:
 * search nodes, guide docs, the results tree and the recipe-viewer dumps all read the
 * client index or a recipe-viewer plugin, and have no meaning without a client.
 *
 * <p>Registered ungated but at permission level 2, unlike
 * {@link AmiStructureCommand} which sits behind {@code -Dami.debugCommands}. A dump is an
 * operator tool rather than a debug toy, and needing a JVM flag on a server you did not
 * launch yourself makes it unusable in exactly the case it exists for. The console is
 * always level 4.
 */
public final class AmiServerDumpCommand {

    private static final String COMMAND = "ami";
    private static final String SUBCOMMAND = "dump";

    private AmiServerDumpCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> dump = Commands.literal(SUBCOMMAND)
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> dumpAll(context.getSource()))
                .then(Commands.literal("all").executes(context -> dumpAll(context.getSource())))
                .then(Commands.literal("items").executes(context -> dumpItems(context.getSource())))
                .then(Commands.literal("recipes").executes(context -> dumpRecipes(context.getSource())))
                .then(Commands.literal("loot-tables").executes(context -> dumpLootTables(context.getSource())))
                .then(Commands.literal("trades").executes(context -> dumpTrades(context.getSource())))
                .then(Commands.literal("worldgen").executes(context -> dumpWorldgen(context.getSource())));

        // Brigadier merges same-named literal roots, so this coexists with the
        // what-is-this-structure branch rather than replacing it.
        dispatcher.register(Commands.literal(COMMAND).then(dump));
    }

    private static int dumpAll(CommandSourceStack source) {
        int written = 0;
        written += dumpItems(source);
        written += dumpRecipes(source);
        written += dumpLootTables(source);
        written += dumpTrades(source);
        written += dumpWorldgen(source);
        info(source, "AMI dump complete (" + written + "/5 sections written)");
        return written;
    }

    private static int dumpItems(CommandSourceStack source) {
        try {
            Path out = dumpDir("registry").resolve("registry-dump.json");
            java.nio.file.Files.createDirectories(out.getParent());
            // collectFromRuntime() reads AMI's client index, which does not exist here and
            // silently yields zero rows. Walk the real item registry instead.
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
            // Pass the server's ResourceManager explicitly: the no-arg overload falls back
            // to Minecraft.getInstance(), which is not loadable on a dedicated server.
            RecipeDumpWriters.LootTableDumpOutputs outputs = RecipeDumpWriters.writeLootTables(
                    dumpDir("loot_tables"), server.getResourceManager());
            info(source, "loot tables: " + outputs.tableCount() + " -> " + outputs.dump().toAbsolutePath());
            return 1;
        } catch (Exception e) {
            return fail(source, "loot-tables", e);
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

    private static Path dumpDir(String category) {
        return FMLPaths.GAMEDIR.get().resolve("ami_dumps").resolve(category);
    }

    private static void info(CommandSourceStack source, String message) {
        source.sendSystemMessage(Component.literal("[AMI] " + message).withStyle(ChatFormatting.GREEN));
        AMI.LOGGER.info("[AMI dump] {}", message);
    }

    private static int fail(CommandSourceStack source, String section, Exception error) {
        AMI.LOGGER.error("Failed to write AMI {} dump", section, error);
        source.sendSystemMessage(Component.literal(
                        "[AMI] " + section + " dump FAILED: " + error)
                .withStyle(ChatFormatting.RED));
        return 0;
    }
}
