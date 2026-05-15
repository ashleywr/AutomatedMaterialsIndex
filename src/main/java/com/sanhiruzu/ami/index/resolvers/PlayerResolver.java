package com.sanhiruzu.ami.index.resolvers;

import com.sanhiruzu.ami.index.IQueryResolver;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Resolves online player names in multiplayer sessions.
 * Creates transient SearchNodes — these are NEVER added to GlobalIndex.
 *
 * Multiplayer-only guards:
 *  - hasSingleplayerServer() == true → return empty
 *  - getConnection() == null → return empty
 *  - query doesn't match any player name → return empty
 */
public final class PlayerResolver implements IQueryResolver {

    @Override
    public Map<NodeType, List<SearchNode>> resolve(String query) {
        Minecraft mc = Minecraft.getInstance();

        // Multiplayer-only guard
        if (mc.hasSingleplayerServer()) return Map.of();
        var conn = mc.getConnection();
        if (conn == null) return Map.of();

        String lower = query.toLowerCase();
        List<SearchNode> matches = new ArrayList<>();

        for (var info : conn.getOnlinePlayers()) {
            String name = info.getProfile().getName();
            if (name.toLowerCase().contains(lower)) {
                // Transient node: use player UUID as ResourceLocation path
                String uuidStr = info.getProfile().getId().toString().replace("-", "");
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath("ami", "player/" + uuidStr);

                matches.add(new SearchNode(
                    id,
                    NodeType.PLAYER,
                    name,
                    0xFF4488FF,   // blue tint for players
                    100,          // high searchWeight so players surface first
                    Map.of(SearchNodeKeys.MOD_ID, "ami", SearchNodeKeys.PLAYER_UUID, uuidStr)
                ));
            }
        }

        if (matches.isEmpty()) return Map.of();
        return Map.of(NodeType.PLAYER, matches);
    }
}
