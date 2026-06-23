package com.sanhiruzu.ami.client.sources;

import com.sanhiruzu.ami.index.EdgeType;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ItemSourceResolver {
    private static final int MAX_SPAWN_BIOMES = 6;
    private static final Map<String, ResourceLocation> VANILLA_RECIPE_METHOD_ICONS = Map.ofEntries(
            Map.entry("crafting", ResourceLocation.parse("minecraft:crafting_table")),
            Map.entry("smelting", ResourceLocation.parse("minecraft:furnace")),
            Map.entry("smoking", ResourceLocation.parse("minecraft:smoker")),
            Map.entry("blasting", ResourceLocation.parse("minecraft:blast_furnace")),
            Map.entry("campfire_cooking", ResourceLocation.parse("minecraft:campfire")),
            Map.entry("stonecutting", ResourceLocation.parse("minecraft:stonecutter")),
            Map.entry("smithing", ResourceLocation.parse("minecraft:smithing_table")),
            Map.entry("smithing_transform", ResourceLocation.parse("minecraft:smithing_table")),
            Map.entry("smithing_trim", ResourceLocation.parse("minecraft:smithing_table"))
    );

    private final List<SearchNode> nodes;

    public ItemSourceResolver(Collection<SearchNode> nodes) {
        this.nodes = nodes == null ? List.of() : List.copyOf(nodes);
    }

    public static ItemSourceResolver fromGlobalIndex() {
        GlobalIndex index = GlobalIndex.getInstance();
        List<SearchNode> all = new ArrayList<>();
        for (NodeType type : NodeType.values()) {
            all.addAll(index.getNodes(type));
        }
        return new ItemSourceResolver(all);
    }

    public ItemSourceReport resolve(SearchNode target) {
        if (target == null) {
            return new ItemSourceReport(Component.literal("Sources"), List.of());
        }
        target = canonicalNode(target);

        List<ItemSourceRow> rows = new ArrayList<>();
        Set<String> emitted = new LinkedHashSet<>();
        rows.addAll(directMobDrops(target, emitted));
        rows.addAll(recipeSources(target, emitted));
        return new ItemSourceReport(Component.literal("Sources: " + target.displayName()), rows);
    }

    private SearchNode canonicalNode(SearchNode target) {
        for (SearchNode node : nodes) {
            if (sameNode(node, target)) {
                return node;
            }
        }
        return GlobalIndex.getInstance().getNode(target.id(), target.type()).orElse(target);
    }

    private List<ItemSourceRow> directMobDrops(SearchNode target, Set<String> emitted) {
        List<ItemSourceRow> rows = new ArrayList<>();
        for (SearchNode entity : entities()) {
            if (!drops(entity, target)) continue;
            List<SearchNode> biomes = spawnBiomes(entity);
            String summary = "drops " + target.displayName();
            String text = entity.displayName() + " -> " + summary + spawnSuffix(biomes);
            emit(rows, emitted, new ItemSourceRow(
                    ItemSourceType.MOB_DROP,
                    text,
                    linksWithBiomes(links(entity, target), biomes),
                    link(entity),
                    summary,
                    links(biomes)
            ));
        }
        return rows;
    }

    private List<ItemSourceRow> recipeSources(SearchNode target, Set<String> emitted) {
        List<ItemSourceRow> rows = new ArrayList<>();
        for (SearchNode recipe : recipeSourceNodes(target)) {
            if (recipe == null || recipe.type() != NodeType.RECIPE) continue;
            List<SearchNode> ingredients = edgeNodes(recipe, EdgeType.REQUIRES, NodeType.ITEM);
            if (ingredients.stream().anyMatch(ingredient -> sameNode(ingredient, target))) {
                continue;
            }

            ItemSourceLink method = recipeMethodLink(recipe);
            String methodName = method.label();
            String summary = "makes " + target.displayName();
            String text = methodName + " -> " + target.displayName();
            emit(rows, emitted, new ItemSourceRow(
                    ItemSourceType.RECIPE,
                    text,
                    links(method.node(), recipe, target),
                    method,
                    summary,
                    List.of()
            ));

            for (SearchNode ingredient : ingredients) {
                if (ingredient == null || ingredient.type() != NodeType.ITEM) continue;
                addIndirectMobDropRows(rows, emitted, ingredient, recipe, methodName, target);
            }
        }
        return rows;
    }

    private List<SearchNode> recipeSourceNodes(SearchNode target) {
        Map<String, SearchNode> recipes = new LinkedHashMap<>();
        for (SearchNode recipe : edgeNodes(target, EdgeType.OUTPUT_OF, NodeType.RECIPE)) {
            recipes.putIfAbsent(recipe.type() + ":" + recipe.id(), recipe);
        }
        for (SearchNode recipe : nodes) {
            if (recipe == null || recipe.type() != NodeType.RECIPE) continue;
            boolean producesTarget = edgeNodes(recipe, EdgeType.PRODUCES, NodeType.ITEM).stream()
                    .anyMatch(output -> sameNode(output, target));
            if (producesTarget) {
                recipes.putIfAbsent(recipe.type() + ":" + recipe.id(), recipe);
            }
        }
        return List.copyOf(recipes.values());
    }

    private void addIndirectMobDropRows(List<ItemSourceRow> rows, Set<String> emitted,
                                        SearchNode ingredient, SearchNode recipe, String recipeName, SearchNode target) {
        for (SearchNode entity : entities()) {
            if (!drops(entity, ingredient)) continue;
            if (sameNode(entity, target)) continue;
            List<SearchNode> biomes = spawnBiomes(entity);
            String summary = "drops " + ingredient.displayName()
                    + " -> " + recipeName
                    + " -> " + target.displayName();
            String text = entity.displayName()
                    + " -> drops " + ingredient.displayName()
                    + spawnSuffix(biomes)
                    + " -> " + recipeName
                    + " -> " + target.displayName();
            emit(rows, emitted, new ItemSourceRow(
                    ItemSourceType.INDIRECT_SOURCE,
                    text,
                    linksWithBiomes(links(entity, ingredient, recipe, target), biomes),
                    link(entity),
                    summary,
                    links(biomes)
            ));
        }
    }

    private List<SearchNode> entities() {
        return nodes.stream().filter(node -> node.type() == NodeType.ENTITY).toList();
    }

    private boolean drops(SearchNode entity, SearchNode item) {
        if (entity == null || item == null) return false;
        for (SearchNode dropped : entity.getEdges(EdgeType.DROPS)) {
            if (sameNode(dropped, item)) return true;
        }
        return false;
    }

    private List<SearchNode> spawnBiomes(SearchNode entity) {
        return edgeNodes(entity, EdgeType.SPAWNS_IN, NodeType.BIOME).stream()
                .filter(node -> node.type() == NodeType.BIOME)
                .filter(distinctById())
                .limit(MAX_SPAWN_BIOMES)
                .toList();
    }

    private String spawnSuffix(List<SearchNode> biomes) {
        if (biomes.isEmpty()) return "";
        return " -> spawns in " + biomes.stream()
                .map(ItemSourceResolver::sourceDisplayName)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static String recipeLabel(SearchNode recipe) {
        String type = recipe.meta(SearchNodeKeys.RECIPE_TYPE_ID, "");
        if (!type.isBlank()) return type;
        ResourceLocation id = recipe.id();
        return id == null ? recipe.displayName() : id.toString();
    }

    private ItemSourceLink recipeMethodLink(SearchNode recipe) {
        String iconItemId = recipe.meta(SearchNodeKeys.RECIPE_METHOD_ICON_ITEM_ID, "");
        SearchNode metadataIcon = findItemNode(iconItemId);
        if (metadataIcon != null) return link(metadataIcon);

        String typeId = recipeLabel(recipe);
        SearchNode iconNode = recipeMethodIconNode(typeId);
        if (iconNode != null) return link(iconNode);

        String metadataLabel = recipe.meta(SearchNodeKeys.RECIPE_METHOD_LABEL, "");
        if (!metadataLabel.isBlank()) return new ItemSourceLink(metadataLabel, recipe);
        return new ItemSourceLink(friendlyRecipeType(typeId), recipe);
    }

    private SearchNode findItemNode(String rawId) {
        if (rawId == null || rawId.isBlank()) return null;
        ResourceLocation parsed = parseResourceLocation(rawId.trim().toLowerCase(Locale.ROOT));
        return parsed == null ? null : findNode(parsed, NodeType.ITEM);
    }

    private SearchNode recipeMethodIconNode(String typeId) {
        String normalized = typeId == null ? "" : typeId.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) return null;

        ResourceLocation mapped = VANILLA_RECIPE_METHOD_ICONS.get(normalized);
        if (mapped != null) {
            SearchNode mappedNode = findNode(mapped, NodeType.ITEM);
            if (mappedNode != null) return mappedNode;
        }

        ResourceLocation direct = parseResourceLocation(normalized);
        if (direct != null) {
            SearchNode directNode = findNode(direct, NodeType.ITEM);
            if (directNode != null) return directNode;
        }

        String path = direct == null ? normalized : direct.getPath();
        for (SearchNode node : nodes) {
            if (node.type() == NodeType.ITEM && node.id().getPath().equals(path)) {
                return node;
            }
        }
        return null;
    }

    private SearchNode findNode(ResourceLocation id, NodeType type) {
        for (SearchNode node : nodes) {
            if (node.type() == type && node.id().equals(id)) {
                return node;
            }
        }
        return GlobalIndex.getInstance().getNode(id, type).orElse(null);
    }

    private List<SearchNode> edgeNodes(SearchNode node, EdgeType edgeType, NodeType expectedType) {
        if (node == null || edgeType == null) return List.of();
        Map<String, SearchNode> resolved = new LinkedHashMap<>();
        for (SearchNode edgeNode : node.getEdges(edgeType)) {
            if (edgeNode == null) continue;
            if (expectedType != null && edgeNode.type() != expectedType) continue;
            resolved.putIfAbsent(edgeNode.type() + ":" + edgeNode.id(), edgeNode);
        }
        for (ResourceLocation id : node.getUnresolvedEdgeIds(edgeType)) {
            SearchNode edgeNode = expectedType == null
                    ? findAnyNode(id)
                    : findNode(id, expectedType);
            if (edgeNode == null) continue;
            resolved.putIfAbsent(edgeNode.type() + ":" + edgeNode.id(), edgeNode);
        }
        return List.copyOf(resolved.values());
    }

    private SearchNode findAnyNode(ResourceLocation id) {
        if (id == null) return null;
        for (SearchNode node : nodes) {
            if (node.id().equals(id)) {
                return node;
            }
        }
        return GlobalIndex.getInstance().getNode(id).orElse(null);
    }

    private static ResourceLocation parseResourceLocation(String value) {
        try {
            return value.contains(":")
                    ? ResourceLocation.parse(value)
                    : ResourceLocation.fromNamespaceAndPath("minecraft", value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String friendlyRecipeType(String typeId) {
        if (typeId == null || typeId.isBlank()) return "Recipe";
        String path = typeId;
        int namespace = path.indexOf(':');
        if (namespace >= 0 && namespace + 1 < path.length()) {
            path = path.substring(namespace + 1);
        }
        path = path.replace('/', ' ').replace('_', ' ').trim();
        if (path.isBlank()) return "Recipe";

        StringBuilder out = new StringBuilder(path.length());
        boolean wordStart = true;
        for (int i = 0; i < path.length(); i++) {
            char ch = path.charAt(i);
            if (Character.isWhitespace(ch)) {
                out.append(ch);
                wordStart = true;
            } else if (wordStart) {
                out.append(Character.toUpperCase(ch));
                wordStart = false;
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private static boolean sameNode(SearchNode left, SearchNode right) {
        return left != null && right != null && left.type() == right.type() && left.id().equals(right.id());
    }

    private static List<ItemSourceLink> links(SearchNode... nodes) {
        Map<String, ItemSourceLink> deduped = new LinkedHashMap<>();
        for (SearchNode node : nodes) {
            if (node == null) continue;
            deduped.putIfAbsent(node.type() + ":" + node.id(), new ItemSourceLink(sourceDisplayName(node), node));
        }
        return List.copyOf(deduped.values());
    }

    private static List<ItemSourceLink> links(List<SearchNode> nodes) {
        return links(nodes == null ? new SearchNode[0] : nodes.toArray(SearchNode[]::new));
    }

    private static ItemSourceLink link(SearchNode node) {
        return node == null ? null : new ItemSourceLink(sourceDisplayName(node), node);
    }

    private static List<ItemSourceLink> linksWithBiomes(List<ItemSourceLink> base, List<SearchNode> biomes) {
        List<SearchNode> nodes = new ArrayList<>();
        if (base != null) {
            for (ItemSourceLink link : base) {
                if (link != null && link.node() != null) nodes.add(link.node());
            }
        }
        if (biomes != null) nodes.addAll(biomes);
        return links(nodes);
    }

    private static String sourceDisplayName(SearchNode node) {
        if (node == null) return "";
        String name = node.displayName();
        if (node.type() == NodeType.BIOME && name != null) {
            return name.replaceFirst("(?i)\\s+biome$", "");
        }
        return name == null ? "" : name;
    }

    private static java.util.function.Predicate<SearchNode> distinctById() {
        Set<String> seen = new LinkedHashSet<>();
        return node -> seen.add(node.type() + ":" + node.id());
    }

    private static void emit(List<ItemSourceRow> rows, Set<String> emitted, ItemSourceRow row) {
        String key = row.type() + "\n" + row.text();
        if (emitted.add(key)) rows.add(row);
    }
}
