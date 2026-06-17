package com.sanhiruzu.ami.util;

import com.sanhiruzu.ami.client.AMICheatMode;
import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.AmiKeybindHandler;
import com.sanhiruzu.ami.client.favorites.FavoriteEntry;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.client.icon.RendererRegistry;
import com.sanhiruzu.ami.client.results.DebugTooltip;
import com.sanhiruzu.ami.client.results.QuestItemEvidence;
import com.sanhiruzu.ami.client.results.QuestItemEvidenceProjector;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.providers.RegistryUtils;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.util.tooltip.AmiTooltipFacts;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AmiTooltipComposer {
    private AmiTooltipComposer() {
    }

    public static List<Component> buildTooltip(SearchNode entry) {
        if (AmiConfig.devMode && AmiKeybindHandler.isDebugTooltipsActive()) {
            return normalizeTooltipLines(DebugTooltip.build(entry));
        }

        List<Component> lines = new ArrayList<>();

        // 1. Initial Content (Native tooltips or Specialized renderer body)
        if (entry.type() == NodeType.ITEM) {
            ItemStack stack = ItemIconRenderer.resolveStack(entry.id());
            if (!stack.isEmpty()) {
                lines.addAll(Screen.getTooltipFromItem(Minecraft.getInstance(), stack));
            } else {
                lines.add(Component.literal(entry.displayName()));
            }
            appendModNameIfMissing(lines, entry);
        } else {
            // Header: Name and Type Label
            // Entity/player type labels are omitted when the renderer body already provides that subtitle.
            lines.add(Component.literal(entry.displayName()));
            if (shouldShowTypeLabel(entry.type())) {
                lines.add(Component.translatable(entry.type().tooltipKey())
                        .withStyle(s -> s.withColor(AMITheme.TEXT_SUBTLE)));
            }

            // Body: Delegate to specialized renderer
            var renderer = RendererRegistry.get(entry.type());
            if (renderer != null) {
                List<Component> body = safeRendererTooltip(renderer, entry);
                if (body != null) lines.addAll(body);
            }
        }

        // 2. Details Section (Common metadata: Storage, FE, DPS, etc.)
        appendAmiDetailsSection(lines, entry);

        // 3. Branding & Identification (Unified mod name highlight)
        if (entry.type() != NodeType.ITEM) {
            // Item nodes already get mod branding via vanilla logic.
            // For all other types, we provide it here to match.
            String modId = entry.meta(SearchNodeKeys.MOD_ID, entry.id().getNamespace());
            String modName = RegistryUtils.modDisplayName(modId);
            lines.add(Component.literal(modName).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
        }

        // 4. Quest provenance for item hits
        appendQuestEvidence(lines, entry);

        // 5. Interaction Hints (Footer)
        appendHints(lines, entry);

        return normalizeTooltipLines(lines);
    }

    public static List<Component> buildItemTooltipFooter(SearchNode entry) {
        if (entry == null || entry.type() != NodeType.ITEM) {
            return List.of();
        }

        List<Component> lines = new ArrayList<>();
        appendAmiDetailsSection(lines, entry);
        appendQuestEvidence(lines, entry);
        appendHints(lines, entry);
        return normalizeTooltipLines(lines);
    }

    public static List<Component> normalizeTooltipLines(List<Component> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }

        List<Component> normalized = new ArrayList<>(lines.size());
        for (Component line : lines) {
            if (line == null) {
                continue;
            }
            String text = normalizeNewlineEscapes(line.getString());
            if (text.indexOf('\n') < 0) {
                normalized.add(line);
                continue;
            }

            String[] split = text.split("\n", -1);
            for (String part : split) {
                normalized.add(Component.literal(part));
            }
        }
        return normalized;
    }

    private static String normalizeNewlineEscapes(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\\n", "\n");
    }

    public static void appendModNameIfMissing(List<Component> lines, SearchNode entry) {
        ResourceLocation resolvedId = FavoriteEntry.resolvedId(entry);
        String namespace = resolvedId != null ? resolvedId.getNamespace() : "";
        String modId = entry.meta(SearchNodeKeys.MOD_ID, namespace);
        String modName = RegistryUtils.modDisplayName(modId).trim();
        if (modName.isEmpty() || TooltipLineMatcher.containsLine(lines, modName)) {
            return;
        }

        lines.add(Component.literal(modName).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
    }

    private static List<Component> safeRendererTooltip(
            com.sanhiruzu.ami.client.icon.IIconRenderer renderer,
            SearchNode entry
    ) {
        try {
            return renderer.getTooltip(entry);
        } catch (LinkageError | RuntimeException e) {
            return List.of();
        }
    }

    /**
     * Backwards-compatibility bridge for older code expecting buildItemTooltip.
     */
    public static List<Component> buildItemTooltip(SearchNode entry, ItemStack stack) {
        return buildTooltip(entry);
    }

    static boolean shouldShowTypeLabel(NodeType type) {
        return type != NodeType.ENTITY && type != NodeType.PLAYER;
    }

    public static Optional<TooltipComponent> getTooltipImage(SearchNode node) {
        if (node.type() == NodeType.ITEM) {
            return Optional.empty();
        } else {
            var renderer = RendererRegistry.get(node.type());
            return renderer != null ? renderer.getTooltipImage(node) : Optional.empty();
        }
    }

    /**
     * Legacy bridge for older code expecting getItemTooltipImage(ItemStack).
     */
    public static Optional<TooltipComponent> getItemTooltipImage(ItemStack stack) {
        return stack.isEmpty() ? Optional.empty() : stack.getTooltipImage();
    }

    private static List<Component> buildAmiDetails(SearchNode entry) {
        return AmiTooltipFacts.build(entry);
    }

    private static void appendAmiDetailsSection(List<Component> lines, SearchNode entry) {
        List<Component> amiDetails = buildAmiDetails(entry);
        if (!amiDetails.isEmpty()) {
            lines.add(Component.empty());
            lines.addAll(amiDetails);
        }
    }

    private static void appendQuestEvidence(List<Component> lines, SearchNode entry) {
        QuestItemEvidence questEvidence = QuestItemEvidenceProjector.project(entry);
        if (questEvidence.hasMatches()) {
            lines.add(Component.empty());
            for (int i = 0; i < questEvidence.tooltipLines().size(); i++) {
                String line = questEvidence.tooltipLines().get(i);
                int color = i == 0 ? AMITheme.TEXT_HIGHLIGHT : AMITheme.TEXT_SUBTLE;
                lines.add(Component.literal(line).withStyle(style -> style.withColor(color)));
            }
        }
    }

    private static void appendHints(List<Component> lines, SearchNode entry) {
        List<Component> hints = new ArrayList<>();

        if ((entry.type() == NodeType.BIOME || entry.type() == NodeType.STRUCTURE) && AMICheatMode.isEnabled()) {
            hints.add(Component.translatable("ami.tooltip.cheat_locate").withStyle(ChatFormatting.GOLD));
        }

        appendCheatGiveHints(hints, entry);

        if (AmiConfig.devMode && Services.PLATFORM.supportsDebugTooltipToggle()) {
            String keybindName = Services.PLATFORM.keyMappings().debugTooltips().getTranslatedKeyMessage().getString();
            if (AmiKeybindHandler.isDebugTooltipsActive()) {
                hints.add(Component.translatable("ami.gui.debug_hint_active", keybindName)
                        .withStyle(s -> s.withColor(AMITheme.TEXT_SUBTLE)));
            } else {
                hints.add(hintLine(Component.literal(keybindName), Component.translatable("ami.gui.hint.action.debug_info")));
            }
        }

        if (!hints.isEmpty()) {
            lines.add(Component.empty());
            lines.addAll(hints);
        }
    }

    private static Component hintLine(String inputI18n, String actionI18n) {
        return hintLine(Component.translatable(inputI18n), Component.translatable(actionI18n));
    }

    private static Component hintLine(Component input, Component action) {
        return Component.empty()
                .append(input.copy().withStyle(ChatFormatting.GRAY))
                .append(Component.literal("  "))
                .append(action.copy().withStyle(s -> s.withColor(AMITheme.TEXT_SUBTLE)));
    }

    private static void appendCheatGiveHints(List<Component> lines, SearchNode entry) {
        if (!AMICheatMode.isEnabled()) return;
        var keys = Services.PLATFORM.keyMappings();
        String giveOne = keys.cheatGiveOne().getTranslatedKeyMessage().getString();
        String giveStack = keys.cheatGiveStack().getTranslatedKeyMessage().getString();

        if (entry.type() == NodeType.ITEM) {
            lines.add(cheatHintLine(giveOne, "ami.tooltip.cheat_give_one"));
            lines.add(cheatHintLine(giveStack, "ami.tooltip.cheat_give_stack"));
        } else if (entry.type() == NodeType.ENTITY) {
            boolean isPokemon = "pokemon_species".equals(entry.meta(com.sanhiruzu.ami.index.SearchNodeKeys.ENTITY_CATEGORY, ""));
            if (isPokemon) {
                lines.add(cheatHintLine(giveOne, "ami.tooltip.cheat_pokemon_spawn"));
                lines.add(cheatHintLine(giveStack, "ami.tooltip.cheat_pokemon_party"));
            } else {
                lines.add(cheatHintLine(giveOne, "ami.tooltip.cheat_spawn_egg"));
                lines.add(cheatHintLine(giveStack, "ami.tooltip.cheat_spawn_egg_stack"));
            }
        }
        // Biomes and structures use the locate hint instead — no give action there
    }

    private static Component cheatHintLine(String keyText, String actionI18n) {
        return Component.empty()
                .append(Component.literal("[" + keyText + "]").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("  "))
                .append(Component.translatable(actionI18n).withStyle(ChatFormatting.GOLD));
    }

}
