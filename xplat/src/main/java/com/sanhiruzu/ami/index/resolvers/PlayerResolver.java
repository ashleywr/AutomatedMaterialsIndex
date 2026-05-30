package com.sanhiruzu.ami.index.resolvers;

import com.sanhiruzu.ami.index.IQueryResolver;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.util.AmiColors;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Side-safe resolver for online player names.
 * Online player lookup is only available on the client distribution.
 */
public final class PlayerResolver implements IQueryResolver {

    @Override
    public Map<NodeType, List<SearchNode>> resolve(String query) {
        if (!Services.PLATFORM.isClient()) return Map.of();

        return ClientPlayerResolver.resolve(query);
    }

    private static class ClientPlayerResolver {
        private static Map<NodeType, List<SearchNode>> resolve(String query) {
            var mc = net.minecraft.client.Minecraft.getInstance();

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
                    ResourceLocation id = Services.PLATFORM.rl("ami", "player/" + uuidStr);

                    matches.add(new SearchNode(
                            id,
                            NodeType.PLAYER,
                            name,
                            AmiColors.PLAYER_NAME_COLOR,   // blue tint for players
                            100,          // high searchWeight so players surface first
                            Map.of(SearchNodeKeys.MOD_ID, "ami", SearchNodeKeys.PLAYER_UUID, uuidStr)
                    ));
                }
            }

            if (matches.isEmpty()) return Map.of();
            return Map.of(NodeType.PLAYER, matches);
        }
    }
}
