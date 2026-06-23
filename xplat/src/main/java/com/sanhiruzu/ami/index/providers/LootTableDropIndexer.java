package com.sanhiruzu.ami.index.providers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanhiruzu.ami.index.EdgeType;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class LootTableDropIndexer {
    private LootTableDropIndexer() {
    }

    public static IndexingResult indexEntityLootTables(GlobalIndex index, List<LootTableResource> resources) {
        if (index == null || resources == null || resources.isEmpty()) {
            return new IndexingResult(0, 0, 0);
        }

        int entityTables = 0;
        int itemRefs = 0;
        int edgesAdded = 0;
        Set<String> emittedEdges = new LinkedHashSet<>();

        for (LootTableResource resource : resources) {
            ResourceLocation entityId = entityIdFromTable(resource.tableId());
            if (entityId == null) continue;
            entityTables++;

            SearchNode entity = index.getNode(entityId, NodeType.ENTITY).orElse(null);
            if (entity == null) continue;

            for (ResourceLocation itemId : itemRefs(resource.json())) {
                itemRefs++;
                SearchNode item = index.getNode(itemId, NodeType.ITEM).orElse(null);
                if (item == null) continue;

                String edgeKey = entityId + "->" + itemId;
                if (!emittedEdges.add(edgeKey) || alreadyDrops(entity, item)) continue;

                entity.addUnresolvedEdge(EdgeType.DROPS, itemId);
                entity.addResolvedEdge(EdgeType.DROPS, item);
                edgesAdded++;
            }
        }

        return new IndexingResult(entityTables, itemRefs, edgesAdded);
    }

    static ResourceLocation entityIdFromTable(ResourceLocation tableId) {
        if (tableId == null || !"entities".equals(tableKind(tableId))) return null;
        String path = tableId.getPath();
        String entityPath = path.substring("entities/".length());
        if (entityPath.isBlank() || entityPath.contains("/")) return null;
        return ResourceLocation.fromNamespaceAndPath(tableId.getNamespace(), entityPath);
    }

    private static String tableKind(ResourceLocation tableId) {
        String path = tableId.getPath();
        int split = path.indexOf('/');
        return split <= 0 ? "" : path.substring(0, split);
    }

    private static Set<ResourceLocation> itemRefs(String json) {
        if (json == null || json.isBlank()) return Set.of();
        try {
            JsonElement element = JsonParser.parseString(json);
            Set<ResourceLocation> out = new LinkedHashSet<>();
            collectItemRefs(element, out);
            return out;
        } catch (RuntimeException ignored) {
            return Set.of();
        }
    }

    private static void collectItemRefs(JsonElement element, Set<ResourceLocation> out) {
        if (element == null || element.isJsonNull()) return;
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectItemRefs(child, out);
            }
            return;
        }
        if (!element.isJsonObject()) return;

        JsonObject object = element.getAsJsonObject();
        if (isItemEntry(object)) {
            ResourceLocation itemId = resourceLocation(object.get("name"));
            if (itemId != null) out.add(itemId);
        }
        for (var entry : object.entrySet()) {
            collectItemRefs(entry.getValue(), out);
        }
    }

    private static boolean isItemEntry(JsonObject object) {
        JsonElement type = object.get("type");
        return type != null
                && type.isJsonPrimitive()
                && "minecraft:item".equals(type.getAsString());
    }

    private static ResourceLocation resourceLocation(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) return null;
        String raw = element.getAsString();
        if (raw == null || !raw.contains(":")) return null;
        try {
            return ResourceLocation.parse(raw);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean alreadyDrops(SearchNode entity, SearchNode item) {
        for (SearchNode existing : entity.getEdges(EdgeType.DROPS)) {
            if (existing.type() == item.type() && existing.id().equals(item.id())) {
                return true;
            }
        }
        return false;
    }

    public record LootTableResource(ResourceLocation tableId, String json) {
    }

    public record IndexingResult(int entityTables, int itemRefs, int edgesAdded) {
    }
}
