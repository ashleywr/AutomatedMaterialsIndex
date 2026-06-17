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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * Entity icon renderer: spawn egg item icon by default, spinning 3D entity on hover.
 * Falls back to FallbackTextRenderer at small sizes (< 12px).
 */
public class EntityIconRenderer implements IIconRenderer {

    private static final int MAX_ENTITY_INSTANCE_CACHE =
            Math.max(16, Integer.getInteger("ami.entityIconEntityCacheLimit", 128));
    private static final Map<Identifier, LivingEntity> entityCache = new LinkedHashMap<>(MAX_ENTITY_INSTANCE_CACHE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Identifier, LivingEntity> eldest) {
            return size() > MAX_ENTITY_INSTANCE_CACHE;
        }
    };
    private static final Set<Identifier> failedRenderers = new HashSet<>();

    private static void renderSpinningEntity(GuiGraphicsExtractor g, int x, int y, int size, int scale, LivingEntity entity) {
        float spinDeg = (System.currentTimeMillis() % 3000L) / 3000.0f * 360.0f;
        renderEntityWithRotation(g, x, y, size, scale, entity, EntityFacingConstants.STATIC_ENTITY_Y_ROT + spinDeg);
    }

    private static void renderEntityWithRotation(GuiGraphicsExtractor g, int x, int y, int size, int scale, LivingEntity entity, float yRot) {
        float entityScale = entity.getScale();
        float renderScale = scale / entityScale;
        float xAngle = (yRot - 180.0f) / 20.0f;
        float offsetY = 0.0f;
        // GuiGraphicsExtractor.entity() (1.21.5 PIP system) stores x0/y0/x1/y1 as screen-absolute
        // GUI coordinates, unlike fill/blit which capture and apply the 2D pose. The caller pushes
        // translate(cellCenterX, cellCenterY) before calling render(-8,-8,16,...), so we must
        // apply the current pose to the pose-relative bounds before passing them to the PIP call.
        org.joml.Matrix3x2f pose = g.pose();
        float sx0 = pose.m00 * x + pose.m10 * y + pose.m20;
        float sy0 = pose.m01 * x + pose.m11 * y + pose.m21;
        float sx1 = pose.m00 * (x + size) + pose.m10 * (y + size) + pose.m20;
        float sy1 = pose.m01 * (x + size) + pose.m11 * (y + size) + pose.m21;
        int ix0 = (int) Math.min(sx0, sx1);
        int iy0 = (int) Math.min(sy0, sy1);
        int ix1 = (int) Math.ceil(Math.max(sx0, sx1));
        int iy1 = (int) Math.ceil(Math.max(sy0, sy1));
        InventoryScreen.renderEntityInInventoryFollowsAngle(g, ix0, iy0, ix1, iy1, (int) renderScale, offsetY, xAngle, 0.0f, entity);
    }

    private static LivingEntity resolveEntity(Identifier id) {
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
            Entity e = type.create(mc.level, EntitySpawnReason.COMMAND);
            if (e instanceof LivingEntity le) entity = le;
        } catch (Exception ignored) {
            // Some entity types require server-side state; skip them gracefully.
        }

        entityCache.put(id, entity); // null entries suppress future attempts
        return entity;
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
                    .getOptional(Identifier.withDefaultNamespace("iron_sword"))
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

            if (EntityIconFallbacks.proxyItemId(node.id()) != null) {
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
                Entity e = type.create(mc.level, EntitySpawnReason.COMMAND);
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

    public static void tickAtlasWarmup() {
        // Entity icon default view now uses spawn egg item icons rather than atlas-baked
        // 3D renders, so atlas warmup is not needed. No-op until atlas baking is re-enabled.
    }

    @Override
    public void render(GuiGraphicsExtractor g, SearchNode node, int x, int y, int size, boolean hovered) {
        if (EntityIconTooltipSupport.isPokemonSpecies(node)) {
            CobblemonPokemonIconRenderer.render(g, node, x, y, size, hovered);
            return;
        }

        if (size < 12) {
            FallbackTextRenderer.renderFallback(g, node, x, y, size);
            return;
        }

        if (!hovered) {
            // Default: show spawn egg item icon (fast, no 3D PIP rendering).
            EntityIconFallbacks.renderFailure(g, node.id(), x, y, size);
            return;
        }

        // Hovered: show spinning live 3D entity render.
        LivingEntity entity = resolveEntity(node.id());
        if (entity == null || failedRenderers.contains(node.id())) {
            EntityIconFallbacks.renderFailure(g, node.id(), x, y, size);
            return;
        }

        float maxBounds = Math.max(entity.getBbHeight(), entity.getBbWidth());
        int scale = Math.max(1, (int) Math.min(size - 4, (size - 2) / maxBounds));

        try {
            renderSpinningEntity(g, x, y, size, scale, entity);
        } catch (RuntimeException e) {
            if (failedRenderers.add(node.id())) {
                AmiCore.LOGGER.warn("AMI: disabling entity icon renderer for {} after render failure", node.id(), e);
            }
            EntityIconFallbacks.renderFailure(g, node.id(), x, y, size);
        }
    }

    @Override
    public List<Component> getTooltip(SearchNode node) {
        List<Component> lines = new ArrayList<>();
        if (com.sanhiruzu.ami.config.AmiConfig.devMode) {
            lines.add(Component.literal(node.id().toString()).withStyle(s -> s.withColor(AMITheme.ENTITY_ID_COLOR)));
        }

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
        EntityIconFallbacks.clear();
        failedRenderers.clear();
        EntityIconCache.invalidate();
        CobblemonPokemonIconRenderer.invalidate();
    }
}
