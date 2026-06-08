package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.index.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Populates the GlobalIndex with structures from the level registry.
 * Handles deferred loading with server-side fallback for singleplayer.
 * Port of WorldAtlasIndexer structure section.
 */
public class StructureProvider implements IAmiDataProvider {
    private static final List<String> VANILLA_STRUCTURE_IDS = List.of(
            "ancient_city",
            "bastion_remnant",
            "buried_treasure",
            "desert_pyramid",
            "end_city",
            "fortress",
            "igloo",
            "jungle_pyramid",
            "mansion",
            "mineshaft",
            "mineshaft_mesa",
            "monument",
            "nether_fossil",
            "ocean_ruin_cold",
            "ocean_ruin_warm",
            "pillager_outpost",
            "ruined_portal",
            "ruined_portal_desert",
            "ruined_portal_jungle",
            "ruined_portal_mountain",
            "ruined_portal_nether",
            "ruined_portal_ocean",
            "ruined_portal_swamp",
            "shipwreck",
            "shipwreck_beached",
            "stronghold",
            "swamp_hut",
            "trail_ruins",
            "trial_chambers",
            "village_desert",
            "village_plains",
            "village_savanna",
            "village_snowy",
            "village_taiga"
    );

    static SearchNode createStructureNode(ResourceLocation id) {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, id.getNamespace());
        meta.put(SearchNodeKeys.ONTOLOGY_CATEGORY, AmiOntology.ENVIRONMENT.id);
        meta.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "structures");
        return new SearchNode(id, NodeType.STRUCTURE,
                RegistryUtils.formatPathWithSuffix(id.getPath(), "Structure"),
                0xFF888888, 0, meta);
    }

    @Override
    public void populate(GlobalIndex index, @Nullable Level level) {
        if (level == null) {
            index.setLoading(NodeType.STRUCTURE, false);
            return;
        }

        List<SearchNode> nodes = new ArrayList<>();
        boolean found = false;

        // Try client registry first
        var optReg = level.registryAccess().registry(Registries.STRUCTURE);
        if (optReg.isPresent()) {
            var reg = optReg.get();
            reg.holders().forEach(holder -> {
                var id = holder.key().location();
                nodes.add(createStructureNode(id));
            });
            found = !nodes.isEmpty();
        }

        // If not found, try server registry (singleplayer only)
        if (!found) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getSingleplayerServer() != null) {
                var serverLevel = mc.getSingleplayerServer().overworld();
                var serverOptReg = serverLevel.registryAccess().registry(Registries.STRUCTURE);
                if (serverOptReg.isPresent()) {
                    var reg = serverOptReg.get();
                    reg.holders().forEach(holder -> {
                        var id = holder.key().location();
                        nodes.add(createStructureNode(id));
                    });
                    found = !nodes.isEmpty();
                    AmiCore.LOGGER.debug("Structure registry found on server with {} entries", nodes.size());
                }
            }
        }

        if (!found) {
            for (String path : VANILLA_STRUCTURE_IDS) {
                nodes.add(createStructureNode(ResourceLocation.fromNamespaceAndPath("minecraft", path)));
            }
            found = true;
            AmiCore.LOGGER.debug("Structure registry not found on client; using {} vanilla structure ids", nodes.size());
        }

        nodes.sort(RegistryUtils.ENTRY_ORDER);
        index.replaceNodes(NodeType.STRUCTURE, nodes);
        index.setLoading(NodeType.STRUCTURE, false);
    }
}
