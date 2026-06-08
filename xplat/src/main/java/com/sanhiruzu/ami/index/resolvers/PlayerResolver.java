package com.sanhiruzu.ami.index.resolvers;

import com.mojang.authlib.GameProfile;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.*;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.player.PlayerWaypointContext;
import com.sanhiruzu.ami.player.PlayerWaypointProviders;
import com.sanhiruzu.ami.util.AmiColors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * Side-safe resolver for online player names.
 * Online player lookup is only available on the client distribution.
 */
public final class PlayerResolver implements IQueryResolver {
    private static final long LIVE_STATE_POLL_INTERVAL_MS = 5_000L;

    private static boolean isClientRuntimeAvailable() {
        try {
            return Services.PLATFORM.isClient();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    public static List<String> suggestNames(String query, int limit) {
        if (!isClientRuntimeAvailable()) return List.of();
        try {
            return ClientPlayerResolver.suggestNames(query, limit);
        } catch (RuntimeException | LinkageError e) {
            return List.of();
        }
    }

    public static List<SearchNode> livePlayerNodes() {
        if (!isClientRuntimeAvailable()) return List.of();
        try {
            return ClientPlayerResolver.livePlayerNodes("");
        } catch (RuntimeException | LinkageError e) {
            return List.of();
        }
    }

    public static long liveStateRevision() {
        if (!isClientRuntimeAvailable()) return 0L;
        try {
            return ClientPlayerResolver.liveStateRevision();
        } catch (RuntimeException | LinkageError e) {
            return 0L;
        }
    }

    public static boolean livePlayerNodesTruncated() {
        if (!isClientRuntimeAvailable()) return false;
        try {
            return ClientPlayerResolver.livePlayerNodesTruncated();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    @Override
    public Map<NodeType, List<SearchNode>> resolve(String query) {
        if (!isClientRuntimeAvailable()) return Map.of();

        return ClientPlayerResolver.resolve(query);
    }

    private static class ClientPlayerResolver {
        private static long lastLiveStatePollMs = Long.MIN_VALUE;
        private static long liveStateRevision = 0L;
        private static String lastLiveStateSnapshot = "";
        private static boolean lastLivePlayerNodesTruncated = false;

        private static long liveStateRevision() {
            long now = System.currentTimeMillis();
            if (now - lastLiveStatePollMs < LIVE_STATE_POLL_INTERVAL_MS) {
                return liveStateRevision;
            }
            lastLiveStatePollMs = now;

            String snapshot = liveStateSnapshot();
            if (!snapshot.equals(lastLiveStateSnapshot)) {
                lastLiveStateSnapshot = snapshot;
                liveStateRevision++;
            }
            return liveStateRevision;
        }

        private static String liveStateSnapshot() {
            var mc = net.minecraft.client.Minecraft.getInstance();
            var conn = mc.getConnection();
            if (conn == null) return "";

            List<String> entries = new ArrayList<>();
            for (var info : conn.getOnlinePlayers()) {
                GameProfile profile = info.getProfile();
                String name = profile.getName();
                UUID uuid = profile.getId();
                if (name == null || uuid == null) {
                    continue;
                }

                StringBuilder entry = new StringBuilder();
                String uuidStr = uuid.toString();
                entry.append(name.toLowerCase(Locale.ROOT)).append('|').append(uuidStr);
                nearbyPlayer(uuidStr).ifPresent(player -> entry
                        .append('|').append(player.level().dimension().location())
                        .append('|').append(player.blockPosition().getX())
                        .append('|').append(player.blockPosition().getY())
                        .append('|').append(player.blockPosition().getZ()));
                entries.add(entry.toString());
            }
            entries.sort(String::compareTo);
            return String.join(";", entries);
        }

        private static Map<NodeType, List<SearchNode>> resolve(String query) {
            String typed = query == null ? "" : query.trim();
            String lower = typed.toLowerCase(Locale.ROOT);
            if (typed.isBlank()) {
                return Map.of();
            }
            if (!PlayerHeadHistory.isValidName(typed)) {
                return Map.of();
            }

            Map<String, GameProfile> headCandidates = playerHeadCandidates(typed, lower, playerHeadResultsLimit());
            List<SearchNode> headNodes = new ArrayList<>();
            for (Map.Entry<String, GameProfile> entry : headCandidates.entrySet()) {
                headNodes.add(playerHeadNode(entry.getKey(), entry.getValue(), lower));
            }

            List<SearchNode> playerNodes = livePlayerNodes(lower);
            if (headNodes.isEmpty() && playerNodes.isEmpty()) return Map.of();

            Map<NodeType, List<SearchNode>> results = new HashMap<>();
            if (!headNodes.isEmpty()) results.put(NodeType.ITEM, headNodes);
            if (!playerNodes.isEmpty()) results.put(NodeType.PLAYER, playerNodes);
            return results;
        }

        private static List<String> suggestNames(String query, int limit) {
            String typed = query == null ? "" : query.trim();
            String lower = typed.toLowerCase(Locale.ROOT);
            if (typed.isBlank() || limit <= 0) return List.of();
            if (!PlayerHeadHistory.isValidName(typed)) return List.of();
            return List.copyOf(playerHeadCandidates(typed, lower, Math.min(limit, playerHeadSuggestionsLimit())).keySet());
        }

        private static int playerHeadResultsLimit() {
            return Math.max(1, Math.min(24, AmiConfig.playerHeadResultsLimit));
        }

        private static int playerHeadSuggestionsLimit() {
            return Math.max(1, Math.min(24, AmiConfig.playerHeadSuggestionsLimit));
        }

        private static List<SearchNode> livePlayerNodes(String lower) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            var conn = mc.getConnection();
            lastLivePlayerNodesTruncated = false;
            if (conn == null) return List.of();

            int maxResults = Math.max(1, AmiConfig.livePlayerResultsLimit);

            List<SearchNode> matches = new ArrayList<>();
            for (var info : conn.getOnlinePlayers()) {
                GameProfile profile = info.getProfile();
                String name = profile.getName();
                if (name == null || !name.toLowerCase(Locale.ROOT).contains(lower)) {
                    continue;
                }
                UUID uuid = profile.getId();
                if (uuid == null) {
                    continue;
                }
                String uuidStr = uuid.toString();
                ResourceLocation id = Services.PLATFORM.rl("ami", "player/" + uuidStr.replace("-", ""));

                matches.add(new SearchNode(
                        id,
                        NodeType.PLAYER,
                        name,
                        AmiColors.PLAYER_NAME_COLOR,
                        name.toLowerCase(Locale.ROOT).startsWith(lower) ? 120 : 100,
                        playerMeta(name, uuidStr)
                ));
                if (matches.size() >= maxResults) {
                    lastLivePlayerNodesTruncated = true;
                    break;
                }
            }

            matches.sort(Comparator
                    .comparingInt((SearchNode node) -> -node.searchWeight())
                    .thenComparing(SearchNode::displayName, String.CASE_INSENSITIVE_ORDER));
            return matches;
        }

        private static boolean livePlayerNodesTruncated() {
            return lastLivePlayerNodesTruncated;
        }

        private static Map<String, GameProfile> playerHeadCandidates(String typed, String lower, int limit) {
            Map<String, GameProfile> candidates = new LinkedHashMap<>();
            collectOnlinePlayerHeads(candidates, lower, limit);
            collectHistory(candidates, lower, limit);
            if (candidates.size() < limit
                    && candidates.keySet().stream().noneMatch(name -> name.equalsIgnoreCase(typed))) {
                candidates.put(typed, null);
            }
            return candidates;
        }

        private static void collectOnlinePlayerHeads(Map<String, GameProfile> out, String lower, int limit) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            var conn = mc.getConnection();
            if (conn == null) return;
            for (boolean prefixPass : List.of(true, false)) {
                for (var info : conn.getOnlinePlayers()) {
                    if (out.size() >= limit) return;
                    GameProfile profile = info.getProfile();
                    String name = profile.getName();
                    if (name == null || containsName(out, name)) continue;
                    String normalized = name.toLowerCase(Locale.ROOT);
                    boolean matches = prefixPass
                            ? normalized.startsWith(lower)
                            : !normalized.startsWith(lower) && normalized.contains(lower);
                    if (matches) {
                        out.put(name, profile);
                    }
                }
            }
        }

        private static void collectHistory(Map<String, GameProfile> out, String lower, int limit) {
            for (boolean prefixPass : List.of(true, false)) {
                for (String name : PlayerHeadHistory.load()) {
                    if (out.size() >= limit) return;
                    if (containsName(out, name)) continue;
                    String normalized = name.toLowerCase(Locale.ROOT);
                    boolean matches = prefixPass
                            ? normalized.startsWith(lower)
                            : !normalized.startsWith(lower) && normalized.contains(lower);
                    if (matches) {
                        out.put(name, null);
                    }
                }
            }
        }

        private static boolean containsName(Map<String, GameProfile> out, String name) {
            return out.keySet().stream().anyMatch(existing -> existing.equalsIgnoreCase(name));
        }

        private static SearchNode playerHeadNode(String name, GameProfile profile, String query) {
            ResourceLocation id = Services.PLATFORM.rl("ami", "player_head/" + safePlayerPath(name));
            if (profile != null) {
                ItemIconRenderer.registerStack(id, Services.PLATFORM.createPlayerHeadStack(profile));
            } else {
                ItemIconRenderer.registerStack(id, Services.PLATFORM.createPlayerHeadStack(name));
            }

            Map<String, String> meta = new HashMap<>();
            meta.put(SearchNodeKeys.MOD_ID, "minecraft");
            meta.put(SearchNodeKeys.ACCESS_LEVEL, ItemFilter.ACCESS_CREATIVE);
            meta.put(SearchNodeKeys.PLAYER_HEAD_NAME, name);
            meta.put(SearchNodeKeys.PLAYER_HEAD_SOURCE, profile == null ? "typed_or_history" : "online_player");
            if (profile != null && profile.getId() != null) {
                meta.put(SearchNodeKeys.PLAYER_UUID, profile.getId().toString());
                meta.put(SearchNodeKeys.PLAYER_ONLINE, "true");
                enrichPlayerMeta(meta, name, profile.getId().toString());
            }
            String lowerName = name.toLowerCase(Locale.ROOT);
            int weight = lowerName.equals(query) ? 160 : lowerName.startsWith(query) ? 145 : 125;
            return new SearchNode(id, NodeType.ITEM, name + " Head", AmiColors.PLAYER_NAME_COLOR, weight, meta);
        }

        private static String safePlayerPath(String name) {
            return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        }

        private static Map<String, String> playerMeta(String name, String uuidStr) {
            Map<String, String> meta = new HashMap<>();
            meta.put(SearchNodeKeys.MOD_ID, "ami");
            meta.put(SearchNodeKeys.PLAYER_UUID, uuidStr);
            meta.put(SearchNodeKeys.PLAYER_NAME, name);
            meta.put(SearchNodeKeys.PLAYER_ONLINE, "true");
            meta.put(SearchNodeKeys.ONTOLOGY_CATEGORY, "social");
            meta.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "players");
            enrichPlayerMeta(meta, name, uuidStr);
            return meta;
        }

        private static void enrichPlayerMeta(Map<String, String> meta, String playerName, String uuidStr) {
            if (uuidStr == null || uuidStr.isBlank()) {
                return;
            }
            nearbyPlayer(uuidStr).ifPresent(player -> {
                meta.put(SearchNodeKeys.PLAYER_X, Integer.toString(player.blockPosition().getX()));
                meta.put(SearchNodeKeys.PLAYER_Y, Integer.toString(player.blockPosition().getY()));
                meta.put(SearchNodeKeys.PLAYER_Z, Integer.toString(player.blockPosition().getZ()));
                meta.put(SearchNodeKeys.PLAYER_DIMENSION, player.level().dimension().location().toString());
                meta.put(SearchNodeKeys.PLAYER_COORD_SOURCE, "client_entity");
            });
            PlayerWaypointProviders.enrich(new PlayerWaypointContext(playerName, uuidStr, meta), meta);
        }

        private static Optional<Player> nearbyPlayer(String uuidStr) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.level == null) return Optional.empty();
                return mc.level.players().stream()
                        .filter(player -> player != null && uuid.equals(player.getUUID()))
                        .map(player -> (Player) player)
                        .findFirst();
            } catch (RuntimeException | LinkageError e) {
                return Optional.empty();
            }
        }

    }
}
