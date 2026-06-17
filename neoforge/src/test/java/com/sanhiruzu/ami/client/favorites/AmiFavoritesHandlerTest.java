package com.sanhiruzu.ami.client.favorites;

import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AmiFavoritesHandlerTest {
    @TempDir
    Path tempDir;

    @BeforeEach
    void resetHandler() {
        AmiFavoritesHandler.disablePersistenceForTests();
        AmiFavoritesHandler.clearFileOverrideForTests();
        AmiFavoritesHandler.clearForTests();
    }

    @Test
    void testLocalFavoritesPersistence() {
        AmiFavoritesHandler handler = AmiFavoritesHandler.getInstance();

        // Mock a biome node (not supported by EMI)
        SearchNode biome = new SearchNode(
                new ResourceLocation("minecraft:plains"),
                NodeType.BIOME,
                "Plains",
                0, 0, Map.of()
        );
        com.sanhiruzu.ami.index.GlobalIndex.getInstance().addNode(biome);

        assertFalse(handler.isFavorite(biome));
        handler.addFavorite(biome);
        assertTrue(handler.isFavorite(biome));

        // Check if it appears in the list
        List<SearchNode> favorites = handler.getFavorites();
        assertTrue(favorites.stream().anyMatch(n -> n.id().equals(biome.id())));

        handler.removeFavorite(biome);
        assertFalse(handler.isFavorite(biome));
    }

    @Test
    void testLocalStackFavoriteCreatesRenderableSyntheticNode() {
        AmiFavoritesHandler handler = AmiFavoritesHandler.getInstance();
        ItemStack stack = new ItemStack(Items.APPLE);

        handler.removeFavorite(stack);
        assertFalse(handler.isFavorite(stack));

        handler.addFavorite(stack);
        assertTrue(handler.isFavorite(stack));
        assertTrue(handler.getFavorites().stream().anyMatch(node ->
                "item".equals(node.meta(FavoriteEntry.META_KIND)) &&
                        "minecraft:apple".equals(node.meta(FavoriteEntry.META_BASE_ID))));

        handler.removeFavorite(stack);
        assertFalse(handler.isFavorite(stack));
    }

    @Test
    void testStackFavoriteToggleRemovesLocalFavorite() {
        AmiFavoritesHandler handler = AmiFavoritesHandler.getInstance();
        ItemStack stack = new ItemStack(Items.REDSTONE);

        handler.removeFavorite(stack);
        assertFalse(handler.isFavorite(stack));

        handler.toggleFavorite(stack);
        assertTrue(handler.isFavorite(stack));

        handler.toggleFavorite(stack);
        assertFalse(handler.isFavorite(stack));
    }

    @Test
    void testRecipeFavoriteIsDistinctFromItemFavorite() {
        AmiFavoritesHandler handler = AmiFavoritesHandler.getInstance();
        ItemStack stack = new ItemStack(Items.CAKE);
        ResourceLocation recipeId = new ResourceLocation("minecraft:diamond_from_blasting");

        handler.removeFavorite(stack);
        handler.removeRecipeFavorite(recipeId, stack);
        assertFalse(handler.isRecipeFavorite(recipeId, stack));

        handler.addRecipeFavorite(recipeId, stack);
        assertTrue(handler.isRecipeFavorite(recipeId, stack));
        assertFalse(handler.isFavorite(stack));
        assertTrue(handler.getFavorites().stream().anyMatch(node ->
                recipeId.toString().equals(node.meta(FavoriteEntry.META_RECIPE_ID))));

        handler.removeRecipeFavorite(recipeId, stack);
        assertFalse(handler.isRecipeFavorite(recipeId, stack));
    }

    @Test
    void runtimeWaypointFavoriteSurvivesOutsideGlobalIndex() {
        AmiFavoritesHandler handler = AmiFavoritesHandler.getInstance();
        SearchNode waypoint = new SearchNode(
                new ResourceLocation("ami:waypoint/manual/home"),
                NodeType.WAYPOINT,
                "Home",
                0,
                0,
                Map.of("waypointDimension", "minecraft:overworld")
        );

        handler.removeFavorite(waypoint);
        assertFalse(handler.isFavorite(waypoint));

        handler.addFavorite(waypoint);

        assertTrue(handler.isFavorite(waypoint));
        SearchNode favorite = handler.getFavorites().stream()
                .filter(node -> node.id().equals(waypoint.id()) && node.type() == NodeType.WAYPOINT)
                .findFirst()
                .orElseThrow();
        assertEquals("stale", favorite.meta(SearchNodeKeys.RUNTIME_FAVORITE_STATE, ""));
        assertTrue(favorite.displayName().endsWith("(Unavailable)"));

        handler.removeFavorite(waypoint);
        assertFalse(handler.isFavorite(waypoint));
    }

    @Test
    void offlinePlayerFavoriteIsMarkedStale() {
        AmiFavoritesHandler handler = AmiFavoritesHandler.getInstance();
        SearchNode player = new SearchNode(
                new ResourceLocation("ami:player/123456781234123412341234567890ab"),
                NodeType.PLAYER,
                "Alex",
                0,
                0,
                Map.of(
                        SearchNodeKeys.PLAYER_NAME, "Alex",
                        SearchNodeKeys.PLAYER_UUID, "12345678-1234-1234-1234-1234567890ab",
                        SearchNodeKeys.PLAYER_ONLINE, "true"
                )
        );

        handler.removeFavorite(player);
        handler.addFavorite(player);

        SearchNode favorite = handler.getFavorites().stream()
                .filter(node -> node.id().equals(player.id()) && node.type() == NodeType.PLAYER)
                .findFirst()
                .orElseThrow();
        assertEquals("stale", favorite.meta(SearchNodeKeys.RUNTIME_FAVORITE_STATE, ""));
        assertEquals("false", favorite.meta(SearchNodeKeys.PLAYER_ONLINE, ""));
        assertTrue(favorite.displayName().endsWith("(Offline)"));

        handler.removeFavorite(player);
    }

    @Test
    void moveFavoriteReordersMixedFavorites() {
        AmiFavoritesHandler handler = AmiFavoritesHandler.getInstance();
        SearchNode player = new SearchNode(
                new ResourceLocation("ami:player/123456781234123412341234567890ab"),
                NodeType.PLAYER,
                "Alex",
                0,
                0,
                Map.of(
                        SearchNodeKeys.PLAYER_NAME, "Alex",
                        SearchNodeKeys.PLAYER_UUID, "12345678-1234-1234-1234-1234567890ab"
                )
        );
        ItemStack apple = new ItemStack(Items.APPLE);

        handler.addFavorite(player);
        handler.addFavorite(apple);
        SearchNode itemFavorite = handler.getFavorites().stream()
                .filter(node -> "item".equals(node.meta(FavoriteEntry.META_KIND, "")))
                .findFirst()
                .orElseThrow();

        handler.moveFavorite(itemFavorite, 0);

        List<SearchNode> favorites = handler.getFavorites();
        assertEquals(itemFavorite.id(), favorites.get(0).id());
        assertEquals(player.id(), favorites.get(1).id());
    }

    @Test
    void itemFavoritePersistenceWritesCanonicalStore() throws Exception {
        Path favoritesFile = tempDir.resolve("ami").resolve("favorites.json");
        AmiFavoritesHandler.setFileOverrideForTests(favoritesFile);
        AmiFavoritesHandler.enablePersistenceForTests();
        AmiFavoritesHandler.clearForTests();

        AmiFavoritesHandler handler = AmiFavoritesHandler.getInstance();
        ItemStack stack = new ItemStack(Items.APPLE);

        handler.addFavorite(stack);

        assertTrue(Files.exists(favoritesFile));
        String json = Files.readString(favoritesFile, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"records\""));
        assertTrue(json.contains("\"itemId\": \"minecraft:apple\""));
    }

    @Test
    void temporaryFavoritesInfoDiagnosticsAreNotPresentInSource() throws Exception {
        Path source = Path.of("").toAbsolutePath().getParent().resolve(Path.of(
                "xplat", "src", "main", "java", "com", "sanhiruzu", "ami", "client", "favorites", "AmiFavoritesHandler.java"));
        String content = Files.readString(source, StandardCharsets.UTF_8);

        assertFalse(content.contains("AMI favorites persistState: wrote"),
                "Temporary favorites persistState info diagnostics should not ship.");
        assertFalse(content.contains("AMI favorites getFavorites: {} records -> {} nodes"),
                "Temporary favorites getFavorites info diagnostics should not ship.");
        assertFalse(content.contains("AMI favorites loadState: ENTER"),
                "Temporary favorites loadState info diagnostics should not ship.");
        assertFalse(content.contains("AMI favorites loadState: file="),
                "Temporary favorites loadState file info diagnostics should not ship.");
        assertFalse(content.contains("AMI favorites loadState: loaded {} records"),
                "Temporary favorites loaded-records info diagnostics should not ship.");
    }
}
