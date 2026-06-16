package com.sanhiruzu.ami.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class AmiStructureCommand {
    private static final String COMMAND = "ami";
    private static final String SUBCOMMAND = "what-is-this-structure";
    private static final String DIMENSION_ARG = "dimension";
    private static final String LOCATION_ARG = "location";

    private AmiStructureCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(COMMAND)
                .then(Commands.literal(SUBCOMMAND)
                        .requires(source -> Commands.LEVEL_ALL.check(source.permissions()))
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            Vec3 position = source.getPosition();
                            BlockPos blockPos = BlockPos.containing(position);
                            listStructuresAt(source.getLevel(), blockPos, true, context);
                            return 1;
                        })
                        .then(Commands.argument(DIMENSION_ARG, DimensionArgument.dimension())
                                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                                .then(Commands.argument(LOCATION_ARG, Vec3Argument.vec3())
                                        .executes(context -> {
                                            ServerLevel level = DimensionArgument.getDimension(context, DIMENSION_ARG);
                                            Coordinates coordinates = Vec3Argument.getCoordinates(context, LOCATION_ARG);
                                            listStructuresAt(level, coordinates.getBlockPos(context.getSource()), false, context);
                                            return 1;
                                        })))));
    }

    private static void listStructuresAt(ServerLevel level, BlockPos pos, boolean callerPosition, CommandContext<CommandSourceStack> context) {
        List<Identifier> structureIds = level.structureManager()
                .startsForStructure(new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4), structure -> true)
                .stream()
                .filter(start -> start.getBoundingBox().isInside(pos))
                .map(StructureStart::getStructure)
                .map(structure -> structureId(level, structure))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Identifier::toString))
                .toList();

        CommandSourceStack source = context.getSource();
        if (structureIds.isEmpty()) {
            Component message = Component.translatable(callerPosition
                    ? "ami.command.structure.none_here"
                    : "ami.command.structure.none_at");
            source.sendSuccess(() -> message, !source.isPlayer());
            return;
        }

        Component message = callerPosition
                ? Component.translatable("ami.command.structure.header_here")
                : Component.translatable("ami.command.structure.header_at", pos.toShortString());

        for (Identifier structureId : structureIds) {
            message = message.copy()
                    .append(Component.literal("\n - ").withStyle(ChatFormatting.RESET))
                    .append(Component.literal(structureId.toString()).withStyle(ChatFormatting.GOLD));
        }

        Component finalMessage = message;
        source.sendSuccess(() -> finalMessage, !source.isPlayer());
    }

    private static Identifier structureId(ServerLevel level, Structure structure) {
        return level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(structure);
    }
}
