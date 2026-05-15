package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.index.EdgeType;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.IAmiDataProvider;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Scans loot tables and records DROPS edges (entity -> item) asynchronously to avoid blocking.
 * Uses reflection fallbacks to tolerate mapping differences across environments.
 */
public class LootTableProvider implements IAmiDataProvider {

    @Override
    public void populate(GlobalIndex index, @Nullable ClientLevel level) {
        if (level == null) return;

        AMI.LOGGER.info("LootTableProvider: scheduling async scan of loot tables...");

        CompletableFuture.runAsync(() -> {
            try {
                level.registryAccess().registry(Registries.LOOT_TABLE).ifPresent(reg ->
                    reg.holders().forEach(holder -> {
                        ResourceLocation ltId = holder.key().location();
                        try {
                            String path = ltId.getPath();
                            // Entity loot tables are under 'entities/<entity>' in vanilla
                            if (!path.startsWith("entities/")) return;

                            String entityPath = path.substring("entities/".length());
                            ResourceLocation entityId = ResourceLocation.fromNamespaceAndPath(ltId.getNamespace(), entityPath);

                            index.getNode(entityId).ifPresent(entityNode -> {
                                Object lootTable = holder.value();

                                // Try method getPools(), else try field 'pools'
                                List<?> pools = null;
                                try {
                                    Method m = lootTable.getClass().getMethod("getPools");
                                    pools = (List<?>) m.invoke(lootTable);
                                } catch (NoSuchMethodException ignored) {
                                    try {
                                        Field f = lootTable.getClass().getDeclaredField("pools");
                                        f.setAccessible(true);
                                        pools = (List<?>) f.get(lootTable);
                                    } catch (NoSuchFieldException | IllegalAccessException ignored2) {
                                    }
                                } catch (Throwable t) {
                                    // ignore
                                }

                                if (pools == null) return;

                                for (Object pool : pools) {
                                    List<?> entries = null;
                                    try {
                                        Method gm = pool.getClass().getMethod("getEntries");
                                        entries = (List<?>) gm.invoke(pool);
                                    } catch (NoSuchMethodException ignored) {
                                        try {
                                            Field cf = pool.getClass().getDeclaredField("children");
                                            cf.setAccessible(true);
                                            entries = (List<?>) cf.get(pool);
                                        } catch (NoSuchFieldException | IllegalAccessException ignored2) {
                                        }
                                    } catch (Throwable t) {
                                        // ignore
                                    }

                                    if (entries == null) continue;

                                    for (Object entry : entries) {
                                        try {
                                            Object maybeItem = null;
                                            try {
                                                Method gim = entry.getClass().getMethod("getItem");
                                                maybeItem = gim.invoke(entry);
                                            } catch (NoSuchMethodException e) {
                                                try {
                                                    Field ifld = entry.getClass().getDeclaredField("item");
                                                    ifld.setAccessible(true);
                                                    maybeItem = ifld.get(entry);
                                                } catch (NoSuchFieldException | IllegalAccessException ignored3) {
                                                }
                                            }

                                            if (maybeItem instanceof Item) {
                                                Item it = (Item) maybeItem;
                                                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(it);
                                                if (itemId != null) {
                                                    entityNode.addUnresolvedEdge(EdgeType.DROPS, itemId);
                                                }
                                            } else if (maybeItem instanceof ResourceLocation) {
                                                entityNode.addUnresolvedEdge(EdgeType.DROPS, (ResourceLocation) maybeItem);
                                            }
                                        } catch (Throwable t) {
                                            // ignore individual entry failures
                                        }
                                    }
                                }
                            });
                        } catch (Throwable t) {
                            // Protect the scan from throwing
                        }
                    })
                );
            } catch (Throwable t) {
                AMI.LOGGER.warn("LootTableProvider async scan failed: {}", t.toString());
            }
        });
    }
}
