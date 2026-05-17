package com.sanhiruzu.ami.client.icon;

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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(node.displayName()));
        lines.add(Component.literal(node.id().toString()).withStyle(s -> s.withColor(0x666666)));

        String category = node.meta(SearchNodeKeys.ENTITY_CATEGORY, "");
        if (!category.isEmpty()) {
            lines.add(Component.translatable("ami.tooltip.category")
                    .append(": ").append(formatCategoryComponent(category))
                    .withStyle(s -> s.withColor(0x888888)));
        }

        String traits = node.meta(SearchNodeKeys.ENTITY_TRAITS, "");
        if (!traits.isEmpty()) {
            lines.add(Component.literal(formatTraits(traits)).withStyle(s -> s.withColor(0x55FFFF)));
        }

        lines.add(Component.translatable("ami.gui.debug_hint").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        return lines;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(SearchNode node) {
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
            return new StatIconRowTooltipComponent(sword, dmg + " dmg", 0xFFFF5555);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Component formatCategoryComponent(String raw) {
        return switch (raw.toUpperCase()) {
            case "MONSTER"   -> Component.translatable("ami.entity_category.hostile");
            case "CREATURE"  -> Component.translatable("ami.entity_category.passive");
            case "AMBIENT"   -> Component.translatable("ami.entity_category.ambient");
            case "WATER_CREATURE", "WATER_AMBIENT" -> Component.translatable("ami.entity_category.aquatic");
            case "MISC"      -> Component.translatable("ami.entity_category.misc");
            default          -> Component.literal(raw);
        };
    }

    private static String formatTraits(String raw) {
        StringBuilder sb = new StringBuilder();
        for (String token : raw.split(" ")) {
            if (token.startsWith("#")) {
                if (sb.length() > 0) sb.append("  ");
                sb.append(token.substring(1));
            }
        }
        return sb.toString();
    }

    @Override
    public void invalidate() {
        entityCache.clear();
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
                    String cls = e != null ? e.getClass().getSimpleName() : "null";
                    missing.add(base + "  [not LivingEntity: " + cls + "]");
                }
            } catch (Exception ex) {
                missing.add(base + "  [exception: " + ex.getMessage() + "]");
            }
        }

        missing.sort(String::compareTo);
        return missing;
    }
}
