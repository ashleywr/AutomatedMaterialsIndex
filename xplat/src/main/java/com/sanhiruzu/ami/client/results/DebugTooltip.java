package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.FacetCodec;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sanhiruzu.ami.AmiCore;
/**
 * Builds an AMI-internal debug tooltip shown when Left Control is held while hovering an entry.
 * Reveals: registry ID, ontology classification (precomputed vs runtime), metadata keys, and tags.
 */
public final class DebugTooltip {

    private static final int MAX_TAGS_SHOWN = 12;

    public static List<Component> build(SearchNode entry) {
        if (!AmiConfig.devMode) {
            return List.of(Component.literal(entry.displayName()));
        }

        List<Component> lines = new ArrayList<>();

        // Header
        lines.add(Component.translatable("ami.debug.header")
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        lines.add(Component.literal(entry.displayName())
                .withStyle(ChatFormatting.WHITE));
        lines.add(Component.empty()
                .append(Component.literal(entry.id().toString()).withStyle(ChatFormatting.GRAY))
                .append(Component.literal("  "))
                .append(Component.translatable("ami.debug.type").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(entry.type().name()).withStyle(ChatFormatting.DARK_GRAY)));

        // Ontology
        AmiOntology.Category classified = AmiOntology.classifyNode(entry);
        String precat = entry.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "");
        String presub = entry.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "");

        lines.add(Component.empty());
        lines.add(Component.translatable("ami.debug.ontology").withStyle(ChatFormatting.GOLD));
        lines.add(Component.empty()
                .append(Component.literal("  "))
                .append(Component.translatable("ami.debug.classified").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("  "))
                .append(Component.literal(classified.displayName().getString()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" (" + classified.id + ")").withStyle(ChatFormatting.DARK_GRAY)));
        if (!precat.isEmpty() || !presub.isEmpty()) {
            String both = precat.isEmpty() ? presub : precat + (presub.isEmpty() ? "" : " / " + presub);
            lines.add(Component.empty()
                    .append(Component.literal("  "))
                    .append(Component.translatable("ami.debug.precomputed").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" "))
                    .append(Component.literal(both).withStyle(ChatFormatting.GREEN)));
        } else {
            lines.add(Component.empty()
                    .append(Component.literal("  "))
                    .append(Component.translatable("ami.debug.precomputed").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" "))
                    .append(Component.translatable("ami.debug.runtime_only").withStyle(ChatFormatting.RED)));
        }

        // Other metadata (sorted, excludes ontology + tags shown separately)
        List<Map.Entry<String, String>> otherMeta = entry.metadata().entrySet().stream()
                .filter(e -> !e.getKey().equals(SearchNodeKeys.TAGS)
                        && !e.getKey().equals(SearchNodeKeys.ONTOLOGY_CATEGORY)
                        && !e.getKey().equals(SearchNodeKeys.ONTOLOGY_SUBCATEGORY))
                .sorted(Map.Entry.comparingByKey())
                .toList();

        if (!otherMeta.isEmpty()) {
            lines.add(Component.empty());
            lines.add(Component.translatable("ami.debug.metadata").withStyle(ChatFormatting.GOLD));
            for (var kv : otherMeta) {
                lines.add(Component.empty()
                        .append(Component.literal("  ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(kv.getKey()).withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(" "))
                        .append(Component.literal(kv.getValue()).withStyle(ChatFormatting.WHITE)));
            }
        }

        String encodedFacets = entry.meta(SearchNodeKeys.FACETS, "");
        var facets = FacetCodec.decode(encodedFacets);
        if (!facets.isEmpty()) {
            lines.add(Component.empty());
            lines.add(Component.translatable("ami.debug.facets").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(" (" + facets.size() + ")").withStyle(ChatFormatting.DARK_GRAY)));
            for (var facet : facets) {
                lines.add(Component.empty()
                        .append(Component.literal("  "))
                        .append(Component.literal(facet.id()).withStyle(ChatFormatting.GRAY)));
            }
        }

        // Tags
        String tags = entry.meta(SearchNodeKeys.TAGS, "");
        if (!tags.isEmpty()) {
            String[] tagArr = tags.split(",");
            lines.add(Component.empty());
            lines.add(Component.translatable("ami.debug.tags").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(" (" + tagArr.length + ")").withStyle(ChatFormatting.DARK_GRAY)));
            int shown = Math.min(tagArr.length, MAX_TAGS_SHOWN);
            for (int i = 0; i < shown; i++) {
                lines.add(Component.empty()
                        .append(Component.literal("  "))
                        .append(Component.literal(tagArr[i].trim()).withStyle(ChatFormatting.GRAY)));
            }
            if (tagArr.length > MAX_TAGS_SHOWN) {
                int remaining = tagArr.length - MAX_TAGS_SHOWN;
                lines.add(Component.literal("  ")
                        .append(Component.translatable("ami.debug.more", remaining).withStyle(ChatFormatting.DARK_GRAY)));
            }
        } else {
            lines.add(Component.empty());
            lines.add(Component.translatable("ami.debug.no_tags").withStyle(ChatFormatting.DARK_GRAY));
        }

        // Block tags (for BlockItem entries, separate from item tags shown above)
        List<String> blockTags = collectBlockTags(entry);
        if (!blockTags.isEmpty()) {
            lines.add(Component.empty());
            lines.add(Component.translatable("ami.debug.block_tags").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(" (" + blockTags.size() + ")").withStyle(ChatFormatting.DARK_GRAY)));
            int shown = Math.min(blockTags.size(), MAX_TAGS_SHOWN);
            for (int i = 0; i < shown; i++) {
                lines.add(Component.empty()
                        .append(Component.literal("  "))
                        .append(Component.literal(blockTags.get(i)).withStyle(ChatFormatting.GRAY)));
            }
            if (blockTags.size() > MAX_TAGS_SHOWN) {
                int remaining = blockTags.size() - MAX_TAGS_SHOWN;
                lines.add(Component.literal("  ")
                        .append(Component.translatable("ami.debug.more", remaining).withStyle(ChatFormatting.DARK_GRAY)));
            }
        }

        return lines;
    }

    private static List<String> collectBlockTags(SearchNode entry) {
        Item item = BuiltInRegistries.ITEM.get(entry.id());
        if (!(item instanceof BlockItem blockItem)) {
            return List.of();
        }

        @SuppressWarnings("deprecation")
        var holder = blockItem.getBlock().builtInRegistryHolder();
        return holder.tags()
                .map(tagKey -> {
                    ResourceLocation loc = tagKey.location();
                    return "#" + loc.getNamespace() + ":" + loc.getPath();
                })
                .sorted()
                .toList();
    }

    private DebugTooltip() {
    }
}
