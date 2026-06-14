package com.sanhiruzu.ami.client.icon;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.EntityIconCache;
import com.sanhiruzu.ami.client.EntityIconWarmupMetrics;
import com.sanhiruzu.ami.client.tooltip.CompositeTooltipComponent;
import com.sanhiruzu.ami.client.tooltip.HeartBarTooltipComponent;
import com.sanhiruzu.ami.client.tooltip.StatIconRowTooltipComponent;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.AmiIndexerService;
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
import org.joml.Vector3f;

import java.util.*;

/**
 * Renders 3D mob miniatures using InventoryScreen.renderEntityInInventory.
 * Falls back to FallbackTextRenderer at small sizes (< 12px) where 3D rendering
 * is imperceptible and expensive.
 */
public class EntityIconRenderer implements IIconRenderer {

    private static final boolean ENTITY_ICON_ATLAS_ENABLED =
            Boolean.parseBoolean(System.getProperty("ami.entityIconAtlas", "true"));
    private static final boolean ENTITY_ICON_ATLAS_WARMUP_ENABLED =
            Boolean.parseBoolean(System.getProperty("ami.entityIconAtlasWarmup", "true"));
    private static final int ENTITY_ICON_ATLAS_WARMUP_PER_TICK =
            Math.max(0, Integer.getInteger("ami.entityIconAtlasWarmupPerTick", 8));
    private static final int ENTITY_ICON_ATLAS_BAKE_PER_TICK =
            Math.max(0, Integer.getInteger("ami.entityIconAtlasBakePerTick", 4));
    private static final long ENTITY_ICON_ATLAS_BAKE_BUDGET_NANOS =
            Math.max(1L, Long.getLong("ami.entityIconAtlasBakeBudgetMs", 8L)) * 1_000_000L;
    private static final int ENTITY_ICON_ATLAS_WARMUP_SIZE = 16;
    private static final int MAX_ENTITY_INSTANCE_CACHE =
            Math.max(16, Integer.getInteger("ami.entityIconEntityCacheLimit", 128));
    private static final Map<ResourceLocation, LivingEntity> entityCache = new LinkedHashMap<>(MAX_ENTITY_INSTANCE_CACHE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ResourceLocation, LivingEntity> eldest) {
            return size() > MAX_ENTITY_INSTANCE_CACHE;
        }
    };
    private static final Set<ResourceLocation> failedRenderers = new HashSet<>();
    private static List<SearchNode> warmupQueue = List.of();
    private static long warmupRevision = -1L;
    private static int warmupIndex = 0;

    private static void renderStaticEntity(GuiGraphics g, int x, int y, int size, int scale, LivingEntity entity) {
        renderEntityWithRotation(g, x, y, size, scale, entity, EntityFacingConstants.STATIC_ENTITY_Y_ROT);
    }

    private static void renderSpinningEntity(GuiGraphics g, int x, int y, int size, int scale, LivingEntity entity) {
        float spinDeg = (System.currentTimeMillis() % 3000L) / 3000.0f * 360.0f;
        renderEntityWithRotation(g, x, y, size, scale, entity, EntityFacingConstants.STATIC_ENTITY_Y_ROT + spinDeg);
    }

    private static void renderEntityWithRotation(GuiGraphics g, int x, int y, int size, int scale, LivingEntity entity, float yRot) {
        float savedBodyRot = entity.yBodyRot;
        float savedYRot = entity.getYRot();
        float savedXRot = entity.getXRot();
        float savedHeadRotO = entity.yHeadRotO;
        float savedHeadRot = entity.yHeadRot;

        entity.yBodyRot = yRot;
        entity.setYRot(yRot);
        entity.setXRot(0.0f);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();

        float entityScale = entity.getScale();
        float renderScale = scale / entityScale;
        float centerX = x + size / 2.0f;
        float centerY = y + size / 2.0f;
        Vector3f translate = new Vector3f(0.0f, entity.getBbHeight() / 2.0f, 0.0f);
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        g.pose().pushPose();
        try {
            try {
                IconRenderState.render3dIcon(g, () ->
                        InventoryScreen.renderEntityInInventory(g, centerX, centerY, renderScale, translate, pose, new Quaternionf(), entity)
                );
            } catch (RuntimeException e) {
                // renderEntityInInventory pushes before dispatching to entity renderers; if a modded renderer
                // throws, vanilla never pops that frame. Pop the leaked vanilla frame before unwinding ours.
                g.pose().popPose();
                throw e;
            }
        } finally {
            g.pose().popPose();
            entity.yBodyRot = savedBodyRot;
            entity.setYRot(savedYRot);
            entity.setXRot(savedXRot);
            entity.yHeadRotO = savedHeadRotO;
            entity.yHeadRot = savedHeadRot;
        }
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
                    .getOptional(ResourceLocation.withDefaultNamespace("iron_sword"))
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

    public static void tickAtlasWarmup() {
        if (!ENTITY_ICON_ATLAS_ENABLED) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        if (AmiIndexerService.getInstance().isBusy()) {
            return;
        }

        if (ENTITY_ICON_ATLAS_BAKE_PER_TICK > 0) {
            EntityIconCache.processPendingBakesAdaptive(ENTITY_ICON_ATLAS_BAKE_PER_TICK,
                    ENTITY_ICON_ATLAS_BAKE_BUDGET_NANOS);
        }

        if (!ENTITY_ICON_ATLAS_WARMUP_ENABLED || ENTITY_ICON_ATLAS_WARMUP_PER_TICK <= 0
                || !GlobalIndex.getInstance().isIndexReady()) {
            return;
        }

        long revision = GlobalIndex.getInstance().revision();
        if (revision != warmupRevision) {
            warmupRevision = revision;
            warmupQueue = GlobalIndex.getInstance().getNodes(NodeType.ENTITY).stream()
                    .filter(node -> !EntityIconTooltipSupport.isPokemonSpecies(node))
                    .toList();
            warmupIndex = 0;
            EntityIconWarmupMetrics.reset(revision, warmupQueue.size());
        }

        int warmed = 0;
        while (warmupIndex < warmupQueue.size() && warmed < ENTITY_ICON_ATLAS_WARMUP_PER_TICK) {
            SearchNode node = warmupQueue.get(warmupIndex);
            LivingEntity entity = resolveEntity(node.id());
            if (entity == null || failedRenderers.contains(node.id())) {
                warmupIndex++;
                EntityIconWarmupMetrics.recordSkipped();
                continue;
            }

            float maxBounds = Math.max(entity.getBbHeight(), entity.getBbWidth());
            int scale = Math.max(1, (int) Math.min(
                    ENTITY_ICON_ATLAS_WARMUP_SIZE - 4,
                    (ENTITY_ICON_ATLAS_WARMUP_SIZE - 2) / maxBounds));
            try {
                EntityIconCache.BakeRequestResult result = EntityIconCache.warmCached(node.id(), ENTITY_ICON_ATLAS_WARMUP_SIZE,
                        cacheG -> renderStaticEntity(cacheG, 0, 0, ENTITY_ICON_ATLAS_WARMUP_SIZE, scale, entity));
                if (result == EntityIconCache.BakeRequestResult.QUEUE_FULL) {
                    break;
                }
                warmupIndex++;
                if (result == EntityIconCache.BakeRequestResult.FAILED) {
                    EntityIconWarmupMetrics.recordSkipped();
                    continue;
                }
                EntityIconWarmupMetrics.recordQueuedOrCached();
            } catch (RuntimeException e) {
                warmupIndex++;
                if (failedRenderers.add(node.id())) {
                    AmiCore.LOGGER.warn("AMI: disabling entity icon renderer for {} after warmup failure", node.id(), e);
                }
                EntityIconWarmupMetrics.recordRenderFailure();
            }
            warmed++;
        }
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
            EntityIconFallbacks.renderFailure(g, node.id(), x, y, size);
            return;
        }

        float maxBounds = Math.max(entity.getBbHeight(), entity.getBbWidth());
        int scale = Math.max(1, (int) Math.min(size - 4, (size - 2) / maxBounds));

        if (failedRenderers.contains(node.id())) {
            EntityIconFallbacks.renderFailure(g, node.id(), x, y, size);
            return;
        }

        try {
            if (!hovered) {
                if (ENTITY_ICON_ATLAS_ENABLED) {
                    if (EntityIconCache.blitCached(g, node.id(), size, x, y,
                        cacheG -> renderStaticEntity(cacheG, 0, 0, size, scale, entity))) {
                        return;
                    }
                    if (EntityIconCache.isFailed(node.id(), size)) {
                        EntityIconFallbacks.renderFailure(g, node.id(), x, y, size);
                        return;
                    }
                    // Cache miss: atlas bake pending — fall through to live render
                }
                renderStaticEntity(g, x, y, size, scale, entity);
                return;
            }

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
        warmupQueue = List.of();
        warmupRevision = -1L;
        warmupIndex = 0;
        EntityIconCache.invalidate();
        CobblemonPokemonIconRenderer.invalidate();
    }
}
