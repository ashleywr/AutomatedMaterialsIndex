package com.sanhiruzu.ami.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerTooltipOwnershipPolicyTest {
    @Test
    void containerTooltipMixinDoesNotCancelVanillaTooltipRendering() throws Exception {
        assertDoesNotCancelContainerTooltip(repoRoot().resolve(Path.of("xplat", "src", "main", "java", "com",
                "sanhiruzu", "ami", "mixin", "ContainerTooltipMixin.java")));
        assertDoesNotCancelContainerTooltip(repoRoot().resolve(Path.of("fabric", "src", "main", "java", "com",
                "sanhiruzu", "ami", "mixin", "FabricContainerTooltipMixin.java")));
    }

    @Test
    void statusEffectHoverAlwaysSuppressesAmiHoverForThatFrame() throws Exception {
        assertStatusEffectHoverTakesOwnership(repoRoot().resolve(Path.of("forge", "src", "main", "java", "com",
                "sanhiruzu", "ami", "client", "InventoryOverlayHandler.java")));
        assertStatusEffectHoverTakesOwnership(repoRoot().resolve(Path.of("neoforge", "src", "main", "java", "com",
                "sanhiruzu", "ami", "client", "InventoryOverlayHandler.java")));
    }

    private static void assertDoesNotCancelContainerTooltip(Path sourcePath) throws Exception {
        String source = Files.readString(sourcePath);

        assertFalse(source.contains("renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V"),
                "Container tooltip drawing must stay vanilla-owned; AMI wins only by suppressing its own hover");
        assertFalse(source.contains("suppressSlotTooltipBehindAmi"),
                "Do not cancel AbstractContainerScreen.renderTooltip from the AMI overlay mixin");
    }

    private static void assertStatusEffectHoverTakesOwnership(Path sourcePath) throws Exception {
        String source = Files.readString(sourcePath);

        assertTrue(source.contains("frameStatusEffectsHovered = updateStatusEffectsHoverOwnership(screen, mouseX, mouseY);"),
                "Container frames must compute status-effect hover ownership before AMI top-layer rendering");
        assertFalse(source.contains("&& !isMouseOverAmiOverlay(mouseX, mouseY)"),
                "Status-effect hover must not depend on whether the same point overlaps an AMI panel");
    }

    private static Path repoRoot() {
        Path current = Path.of("").toAbsolutePath();
        if (Files.exists(current.resolve("settings.gradle"))) {
            return current;
        }
        return current.getParent();
    }
}
