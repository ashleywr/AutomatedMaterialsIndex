package com.sanhiruzu.ami.client.icon;

import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class FluidIconRenderer implements IIconRenderer {

    @Override
    public void render(GuiGraphicsExtractor g, SearchNode node, int x, int y, int size, boolean hovered) {
        Optional<Fluid> maybeFluid = BuiltInRegistries.FLUID.getOptional(node.id());
        if (maybeFluid.isPresent()) {
            Identifier texture = Services.PLATFORM.getFluidStillTexture(maybeFluid.get());
            if (texture != null) {
                TextureAtlasSprite sprite = Minecraft.getInstance()
                        .getAtlasManager()
                        .getAtlasOrThrow(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)
                        .getSprite(texture);
                int tintColor = Services.PLATFORM.getFluidTintColor(maybeFluid.get());
                Services.PLATFORM.renderFluidSprite(g, sprite, tintColor, x, y, size);
                return;
            }
        }
        // Fallback: bucket item icon registered by FluidProvider
        ItemStack bucket = ItemIconRenderer.resolveStack(node.id());
        if (!bucket.isEmpty()) {
            g.item(bucket, x, y);
        } else {
            new FallbackTextRenderer().render(g, node, x, y, size, hovered);
        }
    }

    @Override
    public @Nullable List<Component> getTooltip(SearchNode node) {
        return List.of();
    }
}
