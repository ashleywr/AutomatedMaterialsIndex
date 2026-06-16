package com.sanhiruzu.ami.client.results;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;
import java.util.function.IntSupplier;

final class GeneratedGuiSprite {
    private final Identifier texture;
    private final int width;
    private final int height;
    private final IntSupplier signatureSupplier;
    private final Consumer<Canvas> painter;
    private int registeredSignature = Integer.MIN_VALUE;

    GeneratedGuiSprite(Identifier texture, int width, int height,
                       IntSupplier signatureSupplier, Consumer<Canvas> painter) {
        this.texture = texture;
        this.width = width;
        this.height = height;
        this.signatureSupplier = signatureSupplier;
        this.painter = painter;
    }

    void blit(GuiGraphicsExtractor g, int x, int y) {
        ensureRegistered();
        g.blit(texture, x, y, width, height, 0.0f, 0.0f, (float) width, (float) height);
    }

    void blit(GuiGraphicsExtractor g, int x, int y, int targetWidth, int targetHeight) {
        if (targetWidth <= 0 || targetHeight <= 0) {
            return;
        }
        ensureRegistered();
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0f, 0.0f, targetWidth, targetHeight, width, height, width, height);
    }

    private void ensureRegistered() {
        int signature = signatureSupplier.getAsInt();
        if (registeredSignature == signature) {
            return;
        }
        NativeImage image = new NativeImage(width, height, true);
        painter.accept(new Canvas(image, width, height));
        Minecraft.getInstance().getTextureManager().register(texture, new DynamicTexture(() -> "ami:generated_sprite", image));
        registeredSignature = signature;
    }

    static final class Canvas {
        private final NativeImage image;
        private final int width;
        private final int height;

        private Canvas(NativeImage image, int width, int height) {
            this.image = image;
            this.width = width;
            this.height = height;
        }

        void fill(int x1, int y1, int x2, int y2, int argb) {
            if ((argb >>> 24) == 0) {
                return;
            }
            int rgba = argbToNativeRgba(argb);
            for (int y = Math.max(0, y1); y < Math.min(height, y2); y++) {
                for (int x = Math.max(0, x1); x < Math.min(width, x2); x++) {
                    image.setPixelABGR(x, y, rgba);
                }
            }
        }

        void border(int x, int y, int w, int h, int argb) {
            fill(x, y, x + w, y + 1, argb);
            fill(x, y + h - 1, x + w, y + h, argb);
            fill(x, y + 1, x + 1, y + h - 1, argb);
            fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
        }

        void diagonal(int x1, int y1, int x2, int y2, int argb) {
            int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
            if (steps <= 0) {
                fill(x1, y1, x1 + 1, y1 + 1, argb);
                return;
            }
            for (int i = 0; i <= steps; i++) {
                int px = x1 + Math.round((x2 - x1) * (i / (float) steps));
                int py = y1 + Math.round((y2 - y1) * (i / (float) steps));
                fill(px, py, px + 1, py + 1, argb);
            }
        }

        private static int argbToNativeRgba(int argb) {
            int a = (argb >>> 24) & 0xFF;
            int r = (argb >>> 16) & 0xFF;
            int g = (argb >>> 8) & 0xFF;
            int b = argb & 0xFF;
            return (a << 24) | (b << 16) | (g << 8) | r;
        }
    }
}
