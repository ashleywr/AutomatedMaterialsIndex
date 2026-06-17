package com.sanhiruzu.ami.client.tooltip;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AmiTooltipRendererStateTest {
    @Test
    void amiTooltipsResetRenderStateBeforeVanillaTooltipDraw() throws Exception {
        String source = Files.readString(Path.of("..", "xplat", "src", "main", "java", "com", "sanhiruzu",
                "ami", "client", "tooltip", "AmiTooltipRenderer.java"));

        assertTrue(source.contains("RenderStateSnapshot.capture()"),
                "AMI tooltips should restore the caller's render state after drawing");
        assertTrue(source.contains("RenderSystem.setShaderColor(1f, 1f, 1f, 1f)"),
                "AMI tooltips should clear stale shader tint/alpha before drawing vanilla tooltips");
        assertTrue(source.contains("RenderSystem.defaultBlendFunc()"),
                "AMI tooltips should clear stale blend functions before drawing vanilla tooltips");
    }
}
