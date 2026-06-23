package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Player-facing context-menu settings are intentionally evaluated outside the
 * builder. The builder defines available actions; this policy decides whether a
 * specific action should be shown for a specific target.
 */
public final class ResultContextMenuActionPolicy {
    private static final String LEGACY_DEFAULT_ACTIONS =
            "ami:copy_tooltip,ami:craft_one,ami:craft_stack,ami:recipes,ami:uses,ami:favorite,ami:chat,ami:wiki,"
                    + "ami:locate,ami:cheat_give_one,ami:cheat_give_stack,ami:cheat_spawn_egg,"
                    + "ami:cheat_spawn_egg_stack,ami:cheat_spawn_pokemon,ami:cheat_pokemon_party,"
                    + "ami:group_toggle,ami:filter_category,ami:copy_group_key,ami:start_category_fix,"
                    + "ami:apply_category_fix,ami:clear_item_fix,ami:quests_for_item,ami:open_quest,"
                    + "ami:copy_quest_matches";
    private static final String PRE_QUEST_LEGACY_DEFAULT_ACTIONS =
            "ami:copy_tooltip,ami:craft_one,ami:craft_stack,ami:recipes,ami:uses,ami:favorite,ami:chat,ami:wiki,"
                    + "ami:locate,ami:cheat_give_one,ami:cheat_give_stack,ami:cheat_spawn_egg,"
                    + "ami:cheat_spawn_egg_stack,ami:cheat_spawn_pokemon,ami:cheat_pokemon_party,"
                    + "ami:group_toggle,ami:filter_category,ami:copy_group_key,ami:start_category_fix,"
                    + "ami:apply_category_fix,ami:clear_item_fix";

    private final Set<String> enabledActions;
    private final Map<String, Set<String>> disabledByMod;
    private final Map<String, Set<String>> disabledByType;
    private final Map<String, Set<String>> disabledByCategory;

    private ResultContextMenuActionPolicy(Set<String> enabledActions,
                                          Map<String, Set<String>> disabledByMod,
                                          Map<String, Set<String>> disabledByType,
                                          Map<String, Set<String>> disabledByCategory) {
        this.enabledActions = enabledActions == null ? ResultContextMenuActionBuilder.KNOWN_ACTIONS : enabledActions;
        this.disabledByMod = disabledByMod == null ? Map.of() : disabledByMod;
        this.disabledByType = disabledByType == null ? Map.of() : disabledByType;
        this.disabledByCategory = disabledByCategory == null ? Map.of() : disabledByCategory;
    }

    public static ResultContextMenuActionPolicy fromConfig() {
        return new ResultContextMenuActionPolicy(
                parseEnabledActionIds(AmiConfig.contextMenuEnabledActions),
                parseScopedDisables(AmiConfig.contextMenuDisabledByMod),
                parseScopedDisables(AmiConfig.contextMenuDisabledByType),
                parseScopedDisables(AmiConfig.contextMenuDisabledByCategory)
        );
    }

    public boolean allows(SearchNode node, String actionId) {
        if (!isGloballyEnabled(actionId)) return false;
        return allowsWhenEnabled(node, actionId);
    }

    public boolean allowsDev(SearchNode node, String actionId) {
        return allowsWhenEnabled(node, actionId);
    }

    private boolean allowsWhenEnabled(SearchNode node, String actionId) {
        if (node == null) return true;

        if (node.id() != null && scopeBlocks(disabledByMod, node.id().getNamespace(), actionId)) {
            return false;
        }
        if (node.type() != null && scopeBlocks(disabledByType, node.type().name(), actionId)) {
            return false;
        }
        if (scopeBlocks(disabledByCategory, node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, ""), actionId)) {
            return false;
        }
        if (scopeBlocks(disabledByCategory, node.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, ""), actionId)) {
            return false;
        }
        return true;
    }

    public boolean allowsGroup(TreeNode node, String actionId) {
        if (!isGloballyEnabled(actionId)) return false;
        return allowsDevGroup(node, actionId);
    }

    public boolean allowsDevGroup(TreeNode node, String actionId) {
        if (node == null) return true;

        String key = node.getKey();
        if (scopeBlocks(disabledByCategory, key, actionId)) {
            return false;
        }
        if (node.isModGroup() && scopeBlocks(disabledByMod, key, actionId)) {
            return false;
        }
        return true;
    }

    private boolean isGloballyEnabled(String actionId) {
        return actionId != null && enabledActions.contains(actionId.toLowerCase(Locale.ROOT));
    }

    static Set<String> parseEnabledActionIds(String raw) {
        if (raw == null || raw.isBlank()) return ResultContextMenuActionBuilder.KNOWN_ACTIONS;
        if (isKnownDefaultActionList(raw)) {
            raw = ResultContextMenuActionBuilder.DEFAULT_ACTIONS;
        }

        Set<String> enabled = new HashSet<>();
        String[] tokens = raw.split("[,;\\s]+");
        for (String token : tokens) {
            String normalized = normalize(token);
            if (normalized.isEmpty()) continue;
            if ("none".equals(normalized)) return Set.of();
            if ("all".equals(normalized) || "*".equals(normalized)) {
                return ResultContextMenuActionBuilder.KNOWN_ACTIONS;
            }
            if (ResultContextMenuActionBuilder.KNOWN_ACTIONS.contains(normalized)) {
                enabled.add(normalized);
            }
        }
        return enabled.isEmpty() ? ResultContextMenuActionBuilder.KNOWN_ACTIONS : Set.copyOf(enabled);
    }

    private static boolean isKnownDefaultActionList(String raw) {
        String canonical = canonicalActionList(raw);
        return canonical.equals(canonicalActionList(LEGACY_DEFAULT_ACTIONS))
                || canonical.equals(canonicalActionList(PRE_QUEST_LEGACY_DEFAULT_ACTIONS))
                || canonical.equals(canonicalActionList(preSourcesDefaultActions()))
                || canonical.equals(canonicalActionList(preEntityDetailsDefaultActions()));
    }

    private static String preSourcesDefaultActions() {
        return ResultContextMenuActionBuilder.DEFAULT_ACTIONS
                .replace(ResultContextMenuActionBuilder.SOURCES + ",", "");
    }

    private static String preEntityDetailsDefaultActions() {
        return ResultContextMenuActionBuilder.DEFAULT_ACTIONS
                .replace(ResultContextMenuActionBuilder.ENTITY_DETAILS + ",", "");
    }

    static Map<String, Set<String>> parseScopedDisables(String raw) {
        if (raw == null || raw.isBlank()) return Map.of();

        Map<String, Set<String>> result = new HashMap<>();
        for (String entry : raw.split(";")) {
            if (entry == null || entry.isBlank()) continue;
            int split = entry.indexOf('=');
            if (split <= 0 || split >= entry.length() - 1) continue;

            String scope = normalizeScope(entry.substring(0, split));
            if (scope.isEmpty()) continue;

            Set<String> actions = parseActionList(entry.substring(split + 1));
            if (!actions.isEmpty()) {
                result.put(scope, actions);
            }
        }
        return result.isEmpty() ? Map.of() : Map.copyOf(result);
    }

    private static Set<String> parseActionList(String raw) {
        Set<String> actions = new HashSet<>();
        if (raw == null || raw.isBlank()) return actions;

        for (String token : raw.split("[,|\\s]+")) {
            String normalized = normalize(token);
            if (normalized.isEmpty()) continue;
            if ("all".equals(normalized) || "*".equals(normalized)) {
                return ResultContextMenuActionBuilder.KNOWN_ACTIONS;
            }
            if (ResultContextMenuActionBuilder.KNOWN_ACTIONS.contains(normalized)) {
                actions.add(normalized);
            }
        }
        return actions;
    }

    private static boolean scopeBlocks(Map<String, Set<String>> scopes, String scope, String actionId) {
        if (scopes == null || scopes.isEmpty() || actionId == null) return false;
        String normalizedScope = normalizeScope(scope);
        if (normalizedScope.isEmpty()) return false;

        Set<String> disabled = scopes.get(normalizedScope);
        return disabled != null && disabled.contains(actionId.toLowerCase(Locale.ROOT));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String canonicalActionList(String raw) {
        if (raw == null || raw.isBlank()) return "";
        Set<String> values = new HashSet<>();
        for (String token : raw.split("[,;\\s]+")) {
            String normalized = normalize(token);
            if (!normalized.isEmpty()) {
                values.add(normalized);
            }
        }
        return String.join(",", values.stream().sorted().toList());
    }

    private static String normalizeScope(String value) {
        return normalize(value).replace(' ', '_');
    }
}
