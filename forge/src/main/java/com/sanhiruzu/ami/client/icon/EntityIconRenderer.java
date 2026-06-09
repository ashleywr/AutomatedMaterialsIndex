package com.sanhiruzu.ami.client.icon;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.EntityIconCache;
import com.sanhiruzu.ami.client.tooltip.CompositeTooltipComponent;
import com.sanhiruzu.ami.client.tooltip.HeartBarTooltipComponent;
import com.sanhiruzu.ami.client.tooltip.StatIconRowTooltipComponent;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;

import java.util.*;

/**
 * Renders 3D mob miniatures using InventoryScreen.renderEntityInInventory.
 * Falls back to FallbackTextRenderer at small sizes (< 12px) where 3D rendering
 * is imperceptible and expensive.
 */
public class EntityIconRenderer implements IIconRenderer {

    private static final boolean ENTITY_ICON_CACHE_ENABLED = Boolean.getBoolean("ami.entityIconCache");
    private static final Map<ResourceLocation, LivingEntity> entityCache = new HashMap<>();
    private static final Set<ResourceLocation> failedRenderers = new HashSet<>();

    private static void renderEntity(GuiGraphics g, int x, int y, int scale, float angleX, float angleY, LivingEntity entity) {
        IconRenderState.render3dIcon(g, () ->
                InventoryScreen.renderEntityInInventory(
                        g,
                        x,
                        y,
                        scale,
                        new Quaternionf().rotateZ((float) Math.PI),
                        new Quaternionf(),
                        entity
                )
        );
    }

    private static ResourceLocation resolveProxyItemId(ResourceLocation entityId) {
        // experience_orb has no item form; proxy to experience_bottle for the icon.
        if ("experience_orb".equals(entityId.getPath())) return ResourceLocation.fromNamespaceAndPath("minecraft", "experience_bottle");
        return null;
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean hasRenderableTexture(LivingEntity entity) {
        try {
            var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
            ResourceLocation texture = renderer.getTextureLocation(entity);
            if (texture == null) return false;
            String path = texture.getPath();
            return path != null && !path.isBlank() && !".png".equals(path) && !path.endsWith("/.png");
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static HeartBarTooltipComponent buildHeartBar(SearchNode node) {
        String s = node.meta(SearchNodeKeys.ENTITY_HEALTH, "");
        if (s.isEmpty()) return null;
        try {
            return new HeartBarTooltipComponent(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static StatIconRowTooltipComponent buildAttackRow(SearchNode node) {
        String s = node.meta(SearchNodeKeys.ENTITY_ATTACK_DAMAGE, "");
        if (s.isEmpty()) return null;
        try {
            int dmg = Integer.parseInt(s);
            ItemStack sword = new ItemStack(BuiltInRegistries.ITEM
                    .getOptional(ResourceLocation.fromNamespaceAndPath("minecraft", "iron_sword"))
                    .orElse(Items.AIR));
            return new StatIconRowTooltipComponent(sword, Component.translatable("ami.tooltip.entity.damage", dmg).getString(), AMITheme.ENTITY_DAMAGE_COLOR);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    /**
     * Scans all ENTITY nodes and returns those that can't be rendered as a LivingEntity.
     * Each entry is "id  (displayName)  [reason]". Requires an active level to instantiate types.
     */
    static List<String> collectMissingEntities() {
        Minecraft mc = Minecraft.getInstance();
        List<String> missing = new ArrayList<>();

        for (SearchNode node : GlobalIndex.getInstance().getNodes(NodeType.ENTITY)) {
            String base = node.id() + "  (" + node.displayName() + ")";

            if (EntityIconTooltipSupport.isPokemonSpecies(node)) {
                continue;
            }

            if (resolveProxyItemId(node.id()) != null) {
                continue;
            }

            if (mc.level == null) {
                missing.add(base + "  [no level]");
                continue;
            }

            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(node.id()).orElse(null);
            if (type == null) {
                missing.add(base + "  [type not in registry]");
                continue;
            }

            try {
                Entity e = type.create(mc.level);
                if (!(e instanceof LivingEntity)) {
                    continue;
                }
            } catch (Exception ex) {
                missing.add(base + "  [exception: " + ex.getMessage() + "]");
            }
        }

        missing.sort(String::compareTo);
        return missing;
    }

    @Override
    public void render(GuiGraphics g, SearchNode node, int x, int y, int size, boolean hovered) {
        if (EntityIconTooltipSupport.isPokemonSpecies(node)) {
            CobblemonPokemonIconRenderer.render(g, node, x, y, size, hovered);
            return;
        }

        if (size < 12) {
            FallbackTextRenderer.renderFallback(g, node, x, y, size);
            return;
        }

        LivingEntity entity = resolveEntity(node.id());
        if (entity == null) {
            ResourceLocation proxyId = resolveProxyItemId(node.id());
            if (proxyId != null) {
                ItemStack proxy = BuiltInRegistries.ITEM.getOptional(proxyId).map(ItemStack::new).orElse(ItemStack.EMPTY);
                if (!proxy.isEmpty()) {
                    IconRenderState.render3dIcon(g, () -> {
                        g.pose().pushPose();
                        try {
                            g.pose().translate(x + size / 2.0, y + size / 2.0, com.sanhiruzu.ami.client.overlay.OverlayLayers.SCREEN);
                            float s = size / 16.0f;
                            g.pose().scale(s, s, 1f);
                            g.renderItem(proxy, -8, -8);
                        } finally {
                            g.pose().popPose();
                        }
                    });
                    return;
                }
            }
            FallbackTextRenderer.renderFallback(g, node, x, y, size);
            return;
        }

        float maxBounds = Math.max(entity.getBbHeight(), entity.getBbWidth());
        int scale = Math.max(1, (int) Math.min(size - 4, (size - 2) / maxBounds));

        if (failedRenderers.contains(node.id()) || !hasRenderableTexture(entity)) {
            FallbackTextRenderer.renderFallback(g, node, x, y, size);
            return;
        }

        // Hovered: live render with spin — no scissor so the full entity shows.
        int cx = x + size / 2;
        int cy = y + size - 1;
        float yRot = 180f;
        if (hovered) {
            yRot += (System.currentTimeMillis() % 3000) / 3000f * 360f;
        }
        float savedBodyRot = entity.yBodyRot;
        float savedYRot = entity.getYRot();
        float savedXRot = entity.getXRot();
        float savedHeadRotO = entity.yHeadRotO;
        float savedHeadRot = entity.yHeadRot;

        entity.yBodyRot = yRot;
        entity.setYRot(yRot);
        entity.setXRot(0f);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();

        try {
            if (!hovered && ENTITY_ICON_CACHE_ENABLED && EntityIconCache.blitCached(g, node.id(), size, x, y,
                    cacheG -> renderEntity(cacheG, size / 2, size - 1, scale, 0f, 0f, entity))) {
                return;
            }
            renderEntity(g, cx, cy, scale, 0f, 0f, entity);
        } catch (RuntimeException e) {
            if (failedRenderers.add(node.id())) {
                AmiCore.LOGGER.warn("AMI: disabling entity icon renderer for {} after render failure", node.id(), e);
            }
            FallbackTextRenderer.renderFallback(g, node, x, y, size);
        } finally {
            entity.yBodyRot = savedBodyRot;
            entity.setYRot(savedYRot);
            entity.setXRot(savedXRot);
            entity.yHeadRotO = savedHeadRotO;
            entity.yHeadRot = savedHeadRot;
        }
    }

    @Override
    public List<Component> getTooltip(SearchNode node) {
        List<Component> lines = new ArrayList<>();

        String category = node.meta(SearchNodeKeys.ENTITY_CATEGORY, "");
        if (!category.isEmpty()) {
            lines.add(Component.translatable("ami.tooltip.category")
                    .append(": ").append(EntityIconTooltipSupport.formatCategoryComponent(category))
                    .withStyle(s -> s.withColor(AMITheme.ENTITY_CATEGORY_COLOR)));
        }

        String traits = node.meta(SearchNodeKeys.ENTITY_TRAITS, "");
        if (!traits.isEmpty()) {
            lines.add(Component.literal(EntityIconTooltipSupport.formatTraits(traits))
                    .withStyle(s -> s.withColor(AMITheme.ENTITY_TRAITS_COLOR)));
        }

        if (EntityIconTooltipSupport.isPokemonSpecies(node)) {
            EntityIconTooltipSupport.appendPokemonTextLines(lines, node);
        }

        return lines;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(SearchNode node) {
        if (EntityIconTooltipSupport.isPokemonSpecies(node)) {
            return EntityIconTooltipSupport.buildPokemonVisuals(node);
        }
        HeartBarTooltipComponent heartBar = buildHeartBar(node);
        StatIconRowTooltipComponent attackRow = buildAttackRow(node);
        if (heartBar != null && attackRow != null) {
            return Optional.of(new CompositeTooltipComponent(List.of(heartBar, attackRow)));
        } else if (heartBar != null) {
            return Optional.of(heartBar);
        } else if (attackRow != null) {
            return Optional.of(attackRow);
        }
        return Optional.empty();
    }

    @Override
    public void invalidate() {
        entityCache.clear();
        failedRenderers.clear();
        EntityIconCache.invalidate();
        CobblemonPokemonIconRenderer.invalidate();
    }
}
