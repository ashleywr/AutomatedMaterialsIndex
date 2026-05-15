package com.sanhiruzu.ami.client.icon;

import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders 3D mob miniatures using InventoryScreen.renderEntityInInventory.
 * Falls back to FallbackTextRenderer at small sizes (< 12px) where 3D rendering
 * is imperceptible and expensive.
 */
public class EntityIconRenderer implements IIconRenderer {

    private static final Map<ResourceLocation, LivingEntity> entityCache = new HashMap<>();

    @Override
    public void render(GuiGraphics g, SearchNode node, int x, int y, int size) {
        if (size < 12) {
            FallbackTextRenderer.renderFallback(g, node, x, y, size);
            return;
        }

        LivingEntity entity = resolveEntity(node.id());
        if (entity == null) {
            FallbackTextRenderer.renderFallback(g, node, x, y, size);
            return;
        }

        // Clip to cell so large mobs don't bleed into neighbours
        g.enableScissor(x, y, x + size, y + size);

        int cx    = x + size / 2;
        int cy    = y + size - 1;
        float bbH = entity.getBbHeight();
        int scale = (int) Math.min(size - 4, Math.max(2, (size - 2) / bbH));

        // rotateZ(PI) flips the entity so it faces the viewer (same as vanilla inventory)
        Quaternionf cameraOrient = new Quaternionf().rotateZ((float) Math.PI);
        InventoryScreen.renderEntityInInventory(g, cx, cy, scale,
                new Vector3f(0f, 0f, 0f), cameraOrient, null, entity);

        g.disableScissor();
    }

    private static LivingEntity resolveEntity(ResourceLocation id) {
        // Return cached instance if already created.
        if (entityCache.containsKey(id)) return entityCache.get(id);

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null) {
            entityCache.put(id, null);
            return null;
        }

        LivingEntity entity = null;
        try {
            Entity e = type.create(mc.level);
            if (e instanceof LivingEntity le) entity = le;
        } catch (Exception ignored) {
            // Some entity types require server-side state; skip them gracefully.
        }

        entityCache.put(id, entity); // null entries suppress future attempts
        return entity;
    }

    @Override
    public List<Component> getTooltip(SearchNode node) {
        return List.of(
                Component.literal(node.displayName()),
                Component.literal(node.id().toString()).withStyle(s -> s.withColor(0x666666))
        );
    }

    @Override
    public void invalidate() {
        entityCache.clear();
    }
}
