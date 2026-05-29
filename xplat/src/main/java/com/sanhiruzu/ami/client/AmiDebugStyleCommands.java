package com.sanhiruzu.ami.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class AmiDebugStyleCommands {
    private AmiDebugStyleCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("style")
                .then(Commands.literal("grid-band")
                        .executes(context -> showGridBand(context.getSource()))
                        .then(Commands.argument("color", StringArgumentType.word())
                                .executes(context -> setGridBand(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "color")))));
    }

    private static int showGridBand(CommandSourceStack source) {
        Integer override = AMITheme.getDebugGridGroupBandOverride();
        source.sendSystemMessage(Component.literal("AMI grid group band: " + AMITheme.formatColor(AMITheme.GRID_GROUP_BAND)
                + (override == null ? " (theme default)" : " (runtime override)"))
                .withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int setGridBand(CommandSourceStack source, String rawColor) {
        String value = rawColor.trim().toLowerCase(Locale.ROOT);
        if (value.equals("clear") || value.equals("default") || value.equals("reset")) {
            AMITheme.setDebugGridGroupBandOverride(null);
            AMITheme.sync();
            source.sendSystemMessage(Component.literal("AMI grid group band reset to theme default "
                    + AMITheme.formatColor(AMITheme.GRID_GROUP_BAND)).withStyle(ChatFormatting.GREEN));
            return 1;
        }

        if (value.equals("off") || value.equals("none")) {
            AMITheme.setDebugGridGroupBandOverride(0);
            source.sendSystemMessage(Component.literal("AMI grid group band override set to 0x00000000")
                    .withStyle(ChatFormatting.GREEN));
            return 1;
        }

        try {
            int color = parseArgb(value);
            AMITheme.setDebugGridGroupBandOverride(color);
            source.sendSystemMessage(Component.literal("AMI grid group band override set to " + AMITheme.formatColor(color))
                    .withStyle(ChatFormatting.GREEN));
            return 1;
        } catch (IllegalArgumentException e) {
            source.sendSystemMessage(Component.literal("Invalid color. Use AARRGGBB, 0xAARRGGBB, #AARRGGBB, off, or reset.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int parseArgb(String raw) {
        String value = raw;
        if (value.startsWith("#")) {
            value = value.substring(1);
        } else if (value.startsWith("0x")) {
            value = value.substring(2);
        }
        if (value.length() == 6) {
            value = "FF" + value;
        }
        if (value.length() != 8) {
            throw new IllegalArgumentException("Expected 6 or 8 hex digits");
        }
        return (int) Long.parseLong(value, 16);
    }
}
