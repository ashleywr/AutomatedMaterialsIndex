package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.index.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
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
                Map<String, String> meta = new HashMap<>();
                meta.put(SearchNodeKeys.MOD_ID, id.getNamespace());
                meta.put(SearchNodeKeys.ONTOLOGY_CATEGORY, AmiOntology.ENVIRONMENT.id);
                meta.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "structures");
                nodes.add(new SearchNode(id, NodeType.STRUCTURE,
                        RegistryUtils.formatPathWithSuffix(id.getPath(), "Structure"),
                        0xFF888888, 0, meta));
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
                        Map<String, String> meta = new HashMap<>();
                        meta.put(SearchNodeKeys.MOD_ID, id.getNamespace());
                        meta.put(SearchNodeKeys.ONTOLOGY_CATEGORY, AmiOntology.ENVIRONMENT.id);
                        meta.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "structures");
                        nodes.add(new SearchNode(id, NodeType.STRUCTURE,
                                RegistryUtils.formatPathWithSuffix(id.getPath(), "Structure"),
                                0xFF888888, 0, meta));
                    });
                    found = !nodes.isEmpty();
                    AmiCore.LOGGER.info("Structure registry found on server with {} entries", nodes.size());
                }
            }
        }

        if (!found) {
            AmiCore.LOGGER.warn("Structure registry not found - known NeoForge 1.21.1 issue");
        }

        nodes.sort(RegistryUtils.ENTRY_ORDER);
        index.replaceNodes(NodeType.STRUCTURE, nodes);
        index.setLoading(NodeType.STRUCTURE, false);
    }
}
