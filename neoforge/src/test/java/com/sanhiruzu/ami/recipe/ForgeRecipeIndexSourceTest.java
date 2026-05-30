package com.sanhiruzu.ami.recipe;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ForgeRecipeIndexSourceTest {
    @Test
    void forgeBrewingIndexUsesForgeBrewingRegistryInsteadOfMappedPotionFields() throws Exception {
        Path source = Paths.get("../forge/src/main/java/com/sanhiruzu/ami/recipe/AmiRecipeIndex.java");
        assertTrue(Files.exists(source), "Missing Forge AmiRecipeIndex source at " + source);

        String content = Files.readString(source);
        assertTrue(content.contains("BrewingRecipeRegistry.getRecipes()"),
                "Forge brewing indexing should use Forge's brewing registry API");
        assertFalse(content.contains("POTION_MIXES"),
                "Forge brewing indexing must not reflect mapped vanilla PotionBrewing field names");
    }

    @Test
    void forgeRepairIndexGuardsModdedRepairMaterialProbes() throws Exception {
        Path source = Paths.get("../forge/src/main/java/com/sanhiruzu/ami/recipe/AmiRecipeIndex.java");
        assertTrue(Files.exists(source), "Missing Forge AmiRecipeIndex source at " + source);

        String content = Files.readString(source);
        assertTrue(content.contains("private static boolean isValidRepairItem("),
                "Forge repair indexing should use a guarded repair probe helper");
        assertTrue(content.contains("catch (RuntimeException | LinkageError ignored)"),
                "Forge repair indexing should skip modded repair probes that throw");
        assertTrue(content.contains("if (isValidRepairItem(item, stack, materialStack))"),
                "Forge repair indexing should call the guarded helper from indexRepairs");
    }
}
