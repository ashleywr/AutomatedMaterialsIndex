package com.sanhiruzu.ami.client.icon;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemIconRenderer implements IIconRenderer {

    /** 
     * Stacks for synthetic nodes (potions, enchanted books) registered during indexing.
     * These are NOT cleared on invalidate because they cannot be recovered from the registry.
     */
    private static final Map<ResourceLocation, ItemStack> persistentStacks = new HashMap<>();
    
    /** Lazy cache for regular items; cleared on resource reload. */
    private static final Map<ResourceLocation, ItemStack> lazyCache = new HashMap<>();

    @Override
    public void render(GuiGraphics g, SearchNode node, int x, int y, int size) {
        ItemStack stack = resolveStack(node.id());
        if (stack.isEmpty()) {
            FallbackTextRenderer.renderFallback(g, node, x, y, size);
            return;
        }

        if (size == 16) {
            // ItemIconCache has a Z-projection bug (near/far=[1000,3000] clips item geometry at Z≈100),
            // producing transparent-black framebuffers. Bypass until projection is fixed.
            g.renderItem(stack, x, y);
            return;
        }

        // Scale item rendering to requested size
        var poses = g.pose();
        poses.pushPose();
        poses.translate(x, y, 0);
        float s = size / 16f;
        poses.scale(s, s, 1f);
        g.renderItem(stack, 0, 0);
        poses.popPose();
    }

    /** Pre-register a custom ItemStack for a synthetic node id (e.g. subtype nodes). */
    public static void registerStack(ResourceLocation id, ItemStack stack) {
        persistentStacks.put(id, stack.copy());
    }

    public static ItemStack resolveStack(ResourceLocation id) {
        ItemStack persistent = persistentStacks.get(id);
        if (persistent != null) return persistent;

        return lazyCache.computeIfAbsent(id,
                k -> BuiltInRegistries.ITEM.getOptional(k).map(ItemStack::new).orElse(ItemStack.EMPTY));
    }

    @Override
    public List<Component> getTooltip(SearchNode node) {
        // Caller should use ItemStack tooltip for richer item data; return null to signal that.
        return null;
    }

    @Override
    public void invalidate() {
        lazyCache.clear();
    }

    /** Clear synthetic stacks; called when the entire index is being rebuilt. */
    public static void clearPersistent() {
        persistentStacks.clear();
    }

    /**
     * Scans all ITEM and ENTITY nodes for missing icons, logs a summary, and writes
     * icon_audit.txt to the game directory for offline triage.
     * Called automatically after indexing when DEV_MODE is on.
     */
    public static void auditMissingIcons() {
        // Items
        List<SearchNode> items = GlobalIndex.getInstance().getNodes(NodeType.ITEM);
        List<String> missingItems = new ArrayList<>();
        List<String> okItems = new ArrayList<>();
        for (SearchNode node : items) {
            String entry = node.id() + "  (" + node.displayName() + ")";
            if (resolveStack(node.id()).isEmpty()) missingItems.add(entry);
            else okItems.add(entry);
        }
        missingItems.sort(String::compareTo);
        okItems.sort(String::compareTo);

        // Entities
        List<String> missingEntities = EntityIconRenderer.collectMissingEntities();
        int entityTotal = GlobalIndex.getInstance().getNodes(NodeType.ENTITY).size();

        AMI.LOGGER.warn("AMI IconAudit: items {}/{} missing, entities {}/{} missing — see icon_audit.txt",
                missingItems.size(), items.size(), missingEntities.size(), entityTotal);
        missingItems.forEach(s -> AMI.LOGGER.warn("  ITEM MISSING    {}", s));
        missingEntities.forEach(s -> AMI.LOGGER.warn("  ENTITY MISSING  {}", s));

        writeAuditReport(missingItems, okItems, missingEntities, entityTotal);
    }

    private static void writeAuditReport(List<String> missingItems, List<String> okItems,
                                         List<String> missingEntities, int entityTotal) {
        Path out = Minecraft.getInstance().gameDirectory.toPath().resolve("icon_audit.txt").toAbsolutePath();
        AMI.LOGGER.info("AMI IconAudit: writing report to {}", out);
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
            w.printf("AMI Icon Audit%n");
            w.printf("  Items:    %d / %d missing%n", missingItems.size(), missingItems.size() + okItems.size());
            w.printf("  Entities: %d / %d missing%n%n", missingEntities.size(), entityTotal);

            w.println("=== ITEMS — MISSING ===");
            if (missingItems.isEmpty()) w.println("  (none)");
            else missingItems.forEach(s -> w.println("  " + s));

            w.println();
            w.println("=== ENTITIES — MISSING ===");
            if (missingEntities.isEmpty()) w.println("  (none)");
            else missingEntities.forEach(s -> w.println("  " + s));

            w.println();
            w.println("=== ITEMS — OK ===");
            okItems.forEach(s -> w.println("  " + s));
        } catch (IOException e) {
            AMI.LOGGER.warn("AMI IconAudit: failed to write report to {}: {}", out, e.getMessage(), e);
        }
    }
}
