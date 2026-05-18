package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.index.FacetCodec;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds an AMI-internal debug tooltip shown when Left Control is held while hovering an entry.
 * Reveals: registry ID, ontology classification (precomputed vs runtime), metadata keys, and tags.
 */
public final class DebugTooltip {

    private static final int MAX_TAGS_SHOWN = 12;

    public static List<Component> build(SearchNode entry) {
        List<Component> lines = new ArrayList<>();

        // Header
        lines.add(Component.literal("§e§l[AMI Debug]"));
        lines.add(Component.literal("§f" + entry.displayName()));
        lines.add(Component.literal("§7" + entry.id() + "  §8type:" + entry.type().name()));

        // Ontology
        AmiOntology.Category classified = AmiOntology.classifyNode(entry);
        String precat = entry.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "");
        String presub = entry.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "");

        lines.add(Component.literal(""));
        lines.add(Component.literal("§6Ontology"));
        lines.add(Component.literal("  §7classified  §f" + classified.displayName().getString() + " §8(" + classified.id + ")"));
        if (!precat.isEmpty()) {
            String both = precat + (presub.isEmpty() ? "" : " / " + presub);
            lines.add(Component.literal("  §7precomputed §a" + both));
        } else {
            lines.add(Component.literal("  §7precomputed §cruntime only"));
        }

        // Other metadata (sorted, excludes ontology + tags shown separately)
        List<Map.Entry<String, String>> otherMeta = entry.metadata().entrySet().stream()
                .filter(e -> !e.getKey().equals(SearchNodeKeys.TAGS)
                          && !e.getKey().equals(SearchNodeKeys.ONTOLOGY_CATEGORY)
                          && !e.getKey().equals(SearchNodeKeys.ONTOLOGY_SUBCATEGORY))
                .sorted(Map.Entry.comparingByKey())
                .toList();

        if (!otherMeta.isEmpty()) {
            lines.add(Component.literal(""));
            lines.add(Component.literal("§6Metadata"));
            for (var kv : otherMeta) {
                lines.add(Component.literal("  §7" + kv.getKey() + " §f" + kv.getValue()));
            }
        }

        String encodedFacets = entry.meta(SearchNodeKeys.FACETS, "");
        var facets = FacetCodec.decode(encodedFacets);
        if (!facets.isEmpty()) {
            lines.add(Component.literal(""));
            lines.add(Component.literal("§6Facets §8(" + facets.size() + ")"));
            for (var facet : facets) {
                lines.add(Component.literal("  §7" + facet.id()));
            }
        }

        // Tags
        String tags = entry.meta(SearchNodeKeys.TAGS, "");
        if (!tags.isEmpty()) {
            String[] tagArr = tags.split(",");
            lines.add(Component.literal(""));
            lines.add(Component.literal("§6Tags §8(" + tagArr.length + ")"));
            int shown = Math.min(tagArr.length, MAX_TAGS_SHOWN);
            for (int i = 0; i < shown; i++) {
                lines.add(Component.literal("  §7" + tagArr[i].trim()));
            }
            if (tagArr.length > MAX_TAGS_SHOWN) {
                lines.add(Component.literal("  §8… +" + (tagArr.length - MAX_TAGS_SHOWN) + " more"));
            }
        } else {
            lines.add(Component.literal(""));
            lines.add(Component.literal("§8(no tags)"));
        }

        // Block tags (for BlockItem entries, separate from item tags shown above)
        List<String> blockTags = collectBlockTags(entry);
        if (!blockTags.isEmpty()) {
            lines.add(Component.literal(""));
            lines.add(Component.literal("§6Block Tags §8(" + blockTags.size() + ")"));
            int shown = Math.min(blockTags.size(), MAX_TAGS_SHOWN);
            for (int i = 0; i < shown; i++) {
                lines.add(Component.literal("  §7" + blockTags.get(i)));
            }
            if (blockTags.size() > MAX_TAGS_SHOWN) {
                lines.add(Component.literal("  §8… +" + (blockTags.size() - MAX_TAGS_SHOWN) + " more"));
            }
        }

        return lines;
    }

    private static List<String> collectBlockTags(SearchNode entry) {
        Item item = BuiltInRegistries.ITEM.get(entry.id());
        if (!(item instanceof BlockItem blockItem)) {
            return List.of();
        }

        return blockItem.getBlock().builtInRegistryHolder().tags()
                .map(tagKey -> {
                    ResourceLocation loc = tagKey.location();
                    return "#" + loc.getNamespace() + ":" + loc.getPath();
                })
                .sorted()
                .toList();
    }

    private DebugTooltip() {}
}
