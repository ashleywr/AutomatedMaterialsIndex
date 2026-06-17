package com.sanhiruzu.ami.client.icon;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeEntityRenderContractTest {
    private static final Path ENTITY_RENDERER = repoRoot().resolve(Path.of(
            "forge", "src", "main", "java", "com", "sanhiruzu", "ami", "client", "icon", "EntityIconRenderer.java"));
    private static final Path PLAYER_RENDERER = repoRoot().resolve(Path.of(
            "forge", "src", "main", "java", "com", "sanhiruzu", "ami", "client", "icon", "PlayerModelRenderer.java"));

    @Test
    void forgeEntityRendererUsesForgeSpecificInventoryHelper() throws Exception {
        String source = Files.readString(ENTITY_RENDERER);

        assertTrue(source.contains("InventoryScreen.renderEntityInInventoryFollowsAngle("),
                "Forge entity icons must use Forge's renderEntityInInventoryFollowsAngle helper. " +
                "Forge patches InventoryScreen with angle-component semantics that differ from NeoForge.");
        assertTrue(source.contains("cacheG -> renderStaticEntity(cacheG"),
                "Forge atlas warmup/cache lambdas must flow through the static Forge helper path, not bypass it.");
    }

    @Test
    void forgePlayerRendererUsesForgeSpecificInventoryHelperAndFacing() throws Exception {
        String source = Files.readString(PLAYER_RENDERER);

        assertTrue(source.contains("InventoryScreen.renderEntityInInventoryFollowsAngle("),
                "Forge player models must use Forge's renderEntityInInventoryFollowsAngle helper too.");
        assertTrue(source.contains("EntityFacingConstants.STATIC_ENTITY_Y_ROT"),
                "Forge player hover spin should still anchor on the Forge-specific facing constant rather than a copied NeoForge value.");
    }

    private static Path repoRoot() {
        return Path.of("").toAbsolutePath().getParent();
    }
}
