package com.sanhiruzu.ami.client.icon;

import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RecipeViewerIngredientRenderer implements IIconRenderer {
    private static final Map<ResourceLocation, RenderHandle> HANDLES = new ConcurrentHashMap<>();

    public static void register(ResourceLocation id, RenderHandle handle) {
        if (id == null || handle == null) return;
        HANDLES.put(id, handle);
    }

    public static void clearPersistent() {
        HANDLES.clear();
    }

    @Override
    public void render(GuiGraphics g, SearchNode node, int x, int y, int size, boolean hovered) {
        RenderHandle handle = HANDLES.get(node.id());
        if (handle != null) {
            try {
                if (handle.render(g, x, y, size)) {
                    return;
                }
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
        FallbackTextRenderer.renderFallback(g, node, x, y, size);
    }

    @Override
    public List<Component> getTooltip(SearchNode node) {
        RenderHandle handle = HANDLES.get(node.id());
        if (handle == null) {
            return List.of();
        }
        try {
            return handle.tooltip();
        } catch (RuntimeException | LinkageError ignored) {
            return List.of();
        }
    }

    @Override
    public void invalidate() {
        clearPersistent();
    }

    public interface RenderHandle {
        boolean render(GuiGraphics g, int x, int y, int size);

        List<Component> tooltip();

        static <V> RenderHandle jei(mezz.jei.api.ingredients.IIngredientRenderer<V> renderer, V ingredient, List<Component> tooltip) {
            List<Component> safeTooltip = tooltip == null ? List.of() : List.copyOf(tooltip);
            return new RenderHandle() {
                @Override
                public boolean render(GuiGraphics g, int x, int y, int size) {
                    int width = Math.max(1, renderer.getWidth());
                    int height = Math.max(1, renderer.getHeight());
                    float scale = Math.min((float) size / width, (float) size / height);
                    float drawWidth = width * scale;
                    float drawHeight = height * scale;
                    float dx = x + (size - drawWidth) / 2.0f;
                    float dy = y + (size - drawHeight) / 2.0f;
                    var pose = g.pose();
                    pose.pushPose();
                    pose.translate(dx, dy, com.sanhiruzu.ami.client.overlay.OverlayLayers.SCREEN);
                    if (scale != 1.0f) {
                        pose.scale(scale, scale, 1.0f);
                    }
                    renderer.render(g, ingredient, 0, 0);
                    pose.popPose();
                    return true;
                }

                @Override
                public List<Component> tooltip() {
                    return safeTooltip;
                }
            };
        }
    }
}
