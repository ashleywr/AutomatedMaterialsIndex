package com.sanhiruzu.ami.client.entitydetails;

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

public final class EntityDetailsResolver {
    private static final int MAX_ROWS_PER_SECTION = 18;

    private final List<SearchNode> nodes;

    public EntityDetailsResolver(Collection<SearchNode> nodes) {
        this.nodes = nodes == null ? List.of() : List.copyOf(nodes);
    }

    public static EntityDetailsResolver fromGlobalIndex() {
        GlobalIndex index = GlobalIndex.getInstance();
        List<SearchNode> all = new ArrayList<>();
        for (NodeType type : NodeType.values()) {
            all.addAll(index.getNodes(type));
        }
        return new EntityDetailsResolver(all);
    }

    public EntityDetailsReport resolve(SearchNode target) {
        if (target == null) {
            return new EntityDetailsReport(Component.translatable("ami.entity_details.title"), List.of());
        }
        target = canonicalNode(target);

        List<EntityDetailsRow> rows = new ArrayList<>();
        rows.addAll(statRows(target));
        rows.addAll(spawnRows(target));
        rows.addAll(dropRows(target));
        return new EntityDetailsReport(Component.translatable("ami.entity_details.title.named", target.displayName()), rows);
    }

    private SearchNode canonicalNode(SearchNode target) {
        for (SearchNode node : nodes) {
            if (sameNode(node, target)) {
                return node;
            }
        }
        return GlobalIndex.getInstance().getNode(target.id(), target.type()).orElse(target);
    }

    private List<EntityDetailsRow> statRows(SearchNode entity) {
        List<EntityDetailsRow> rows = new ArrayList<>();
        String health = entity.meta(SearchNodeKeys.ENTITY_HEALTH, "");
        if (!health.isBlank()) rows.add(stat(formatHearts(health), EntityDetailsStatKind.HEALTH));

        String damage = entity.meta(SearchNodeKeys.ENTITY_ATTACK_DAMAGE, "");
        if (!damage.isBlank()) rows.add(stat(formatDamage(damage), EntityDetailsStatKind.DAMAGE));

        if (Boolean.parseBoolean(entity.meta(SearchNodeKeys.FIRE_IMMUNE, "false"))) {
            rows.add(stat("Fire immune", EntityDetailsStatKind.EFFECT));
        }

        for (String trait : friendlyTraits(entity.meta(SearchNodeKeys.ENTITY_TRAITS, ""))) {
            rows.add(stat(trait, EntityDetailsStatKind.TRAIT));
        }
        return rows;
    }

    private static EntityDetailsRow stat(String text, EntityDetailsStatKind kind) {
        return EntityDetailsRow.stat(text, kind);
    }

    private List<EntityDetailsRow> spawnRows(SearchNode entity) {
        List<EntityDetailsRow> rows = new ArrayList<>();
        for (SearchNode biome : edgeNodes(entity, EdgeType.SPAWNS_IN, NodeType.BIOME)) {
            rows.add(new EntityDetailsRow(EntityDetailsSection.SPAWNS, displayName(biome), link(biome)));
            if (rows.size() >= MAX_ROWS_PER_SECTION) break;
        }
        return rows;
    }

    private List<EntityDetailsRow> dropRows(SearchNode entity) {
        List<EntityDetailsRow> rows = new ArrayList<>();
        for (SearchNode item : edgeNodes(entity, EdgeType.DROPS, NodeType.ITEM)) {
            rows.add(new EntityDetailsRow(EntityDetailsSection.DROPS, displayName(item), link(item)));
            if (rows.size() >= MAX_ROWS_PER_SECTION) break;
        }
        return rows;
    }

    private static String formatHearts(String rawHealth) {
        double health = parseDouble(rawHealth);
        if (Double.isNaN(health)) return rawHealth.trim() + " HP";
        double hearts = health / 2.0D;
        return formatNumber(hearts) + " " + (hearts == 1.0D ? "heart" : "hearts");
    }

    private static String formatDamage(String rawDamage) {
        double damage = parseDouble(rawDamage);
        String value = Double.isNaN(damage) ? rawDamage.trim() : formatNumber(damage);
        return value + " damage";
    }

    private static double parseDouble(String value) {
        if (value == null || value.isBlank()) return Double.NaN;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return Double.NaN;
        }
    }

    private static String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
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
            SearchNode edgeNode = expectedType == null ? findAnyNode(id) : findNode(id, expectedType);
            if (edgeNode == null) continue;
            resolved.putIfAbsent(edgeNode.type() + ":" + edgeNode.id(), edgeNode);
        }
        return List.copyOf(resolved.values());
    }

    private SearchNode findNode(ResourceLocation id, NodeType type) {
        for (SearchNode node : nodes) {
            if (node.type() == type && node.id().equals(id)) {
                return node;
            }
        }
        return GlobalIndex.getInstance().getNode(id, type).orElse(null);
    }

    private SearchNode findAnyNode(ResourceLocation id) {
        if (id == null) return null;
        for (SearchNode node : nodes) {
            if (node.id().equals(id)) return node;
        }
        return GlobalIndex.getInstance().getNode(id).orElse(null);
    }

    private static EntityDetailsLink link(SearchNode node) {
        return node == null ? null : new EntityDetailsLink(displayName(node), node);
    }

    private static String displayName(SearchNode node) {
        if (node == null) return "";
        String name = node.displayName();
        if (node.type() == NodeType.BIOME && name != null) {
            return name.replaceFirst("(?i)\\s+biome$", "");
        }
        return name == null ? "" : name;
    }

    private static String friendlyToken(String value) {
        if (value == null || value.isBlank()) return "";
        String raw = value.trim().toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder out = new StringBuilder(raw.length());
        boolean wordStart = true;
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
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

    private static List<String> friendlyTraits(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        Set<String> traits = new LinkedHashSet<>();
        for (String token : raw.split("\\s+")) {
            if (token == null || token.isBlank() || token.contains(":") || token.startsWith("#")) continue;
            if (token.matches(".*\\d.*")) continue;
            traits.add(friendlyToken(token));
        }
        return List.copyOf(traits);
    }

    private static boolean sameNode(SearchNode left, SearchNode right) {
        return left != null && right != null && left.type() == right.type() && left.id().equals(right.id());
    }
}
