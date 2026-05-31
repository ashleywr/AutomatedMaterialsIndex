package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultContextMenuActionBuilderTest {
    @BeforeEach
    void resetConfig() {
        AmiConfig.resetToDefaults();
    }

    @Test
    void nullItemContextReturnsNoActions() {
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();

        assertTrue(builder.forItem(null).isEmpty());
    }

    @Test
    void itemWithoutStackStillGetsMetadataActions() {
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();
        AtomicReference<String> token = new AtomicReference<>();
        SearchNode node = item("stone", "Stone");

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(node, ItemStack.EMPTY, null, token::set)
        );

        assertEquals(List.of(
                "ami.context.chat",
                "ami.context.wiki"
        ), labels(actions));
    }

    @Test
    void configCanDisableActionsByModTypeAndCategory() {
        AmiConfig.contextMenuEnabledActions = "all";
        AmiConfig.contextMenuDisabledByMod = "minecraft=ami:wiki";
        AmiConfig.contextMenuDisabledByType = "ITEM=ami:chat";
        AmiConfig.contextMenuDisabledByCategory = "building=ami:copy_id";
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();
        SearchNode node = item("stone", "Stone", Map.of(SearchNodeKeys.ONTOLOGY_CATEGORY, "building"));

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(node, ItemStack.EMPTY, null, ignored -> {
                })
        );

        assertEquals(List.of(ResultContextMenuActionBuilder.FILTER_MOD), ids(actions));
    }

    @Test
    void defaultConfigHidesLowValueTechnicalActions() {
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(item("stone", "Stone"), ItemStack.EMPTY, null, ignored -> {
                })
        );

        assertEquals(List.of(ResultContextMenuActionBuilder.CHAT, ResultContextMenuActionBuilder.WIKI), ids(actions));
    }

    @Test
    void recipeAndUseActionsNeedIndexedEvidence() {
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();

        List<ResultContextMenu.Action> noRecipes = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(
                        item("diamond", "Diamond"),
                        stack("diamond"),
                        null,
                        ignored -> {
                        }
                )
        );
        assertEquals(List.of(
                ResultContextMenuActionBuilder.COPY_TOOLTIP,
                ResultContextMenuActionBuilder.CHAT,
                ResultContextMenuActionBuilder.WIKI
        ), ids(noRecipes));

        List<ResultContextMenu.Action> withRecipes = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(
                        item("diamond", "Diamond", Map.of(
                                SearchNodeKeys.RECIPE_OUTPUT_COUNT, "1",
                                SearchNodeKeys.RECIPE_USE_COUNT, "2"
                        )),
                        stack("diamond"),
                        null,
                        ignored -> {
                        }
                )
        );
        assertTrue(ids(withRecipes).contains(ResultContextMenuActionBuilder.RECIPES));
        assertTrue(ids(withRecipes).contains(ResultContextMenuActionBuilder.USES));
    }

    @Test
    void cheatModeAddsItemCheatActions() {
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder(() -> true);

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(
                        item("diamond", "Diamond"),
                        stack("diamond"),
                        null,
                        ignored -> {
                        }
                )
        );

        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.CHEAT_GIVE_ONE));
        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.CHEAT_GIVE_STACK));
    }

    @Test
    void craftActionsOnlyAppearWhenTransferIsAvailable() {
        ResultContextMenuActionBuilder unavailable = new ResultContextMenuActionBuilder(() -> false, stack -> false);
        List<ResultContextMenu.Action> unavailableActions = unavailable.forItem(
                new ResultContextMenuActionBuilder.ItemContext(
                        item("diamond", "Diamond"),
                        stack("diamond"),
                        null,
                        ignored -> {
                        }
                )
        );
        assertTrue(!ids(unavailableActions).contains(ResultContextMenuActionBuilder.CRAFT_ONE));
        assertTrue(!ids(unavailableActions).contains(ResultContextMenuActionBuilder.CRAFT_STACK));

        ResultContextMenuActionBuilder available = new ResultContextMenuActionBuilder(() -> false, stack -> true);
        List<ResultContextMenu.Action> availableActions = available.forItem(
                new ResultContextMenuActionBuilder.ItemContext(
                        item("diamond", "Diamond"),
                        stack("diamond"),
                        null,
                        ignored -> {
                        }
                )
        );

        assertTrue(ids(availableActions).contains(ResultContextMenuActionBuilder.CRAFT_ONE));
        assertTrue(ids(availableActions).contains(ResultContextMenuActionBuilder.CRAFT_STACK));
    }

    @Test
    void minecraftBiomeWikiUsesDirectPageWithoutBiomeSuffix() {
        SearchNode node = new SearchNode(
                new ResourceLocation("minecraft:end_highlands"),
                NodeType.BIOME,
                "End Highlands Biome",
                0,
                0,
                Map.of()
        );

        assertEquals(URI.create("https://minecraft.wiki/w/End_Highlands"),
                ResultContextMenuActionBuilder.wikiUriFor(node).orElseThrow());
    }

    @Test
    void malformedConfigFallsBackToDefaultActions() {
        assertEquals(ResultContextMenuActionBuilder.KNOWN_ACTIONS,
                ResultContextMenuActionPolicy.parseEnabledActionIds("missing nonsense"));
        assertTrue(ResultContextMenuActionPolicy.parseScopedDisables("bad; also bad; =ami:wiki").isEmpty());
    }

    @Test
    void globalConfigCanHideAllActions() {
        AmiConfig.contextMenuEnabledActions = "none";
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(item("stone", "Stone"), ItemStack.EMPTY, null, ignored -> {
                })
        );

        assertTrue(actions.isEmpty());
    }

    @Test
    void groupContextBuildsTreeActionsAndRunsInvalidation() {
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();
        TreeNode group = new TreeNode("building", Component.literal("Building"));
        AtomicReference<String> token = new AtomicReference<>();
        AtomicBoolean invalidated = new AtomicBoolean();

        List<ResultContextMenu.Action> actions = builder.forGroup(
                new ResultContextMenuActionBuilder.GroupContext(group, token::set, () -> invalidated.set(true))
        );

        assertEquals(List.of(
                "ami.context.expand_group",
                "ami.context.filter_category",
                "ami.context.copy_group_key"
        ), labels(actions));

        actions.get(0).onClick().run();
        actions.get(1).onClick().run();

        assertTrue(group.isExpanded());
        assertTrue(invalidated.get());
        assertEquals("$building", token.get());
    }

    @Test
    void leafGroupContextReturnsNoActions() {
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();

        List<ResultContextMenu.Action> actions = builder.forGroup(
                new ResultContextMenuActionBuilder.GroupContext(
                        new TreeNode(Component.literal("Stone"), item("stone", "Stone")),
                        ignored -> {
                        },
                        () -> {
                        }
                )
        );

        assertTrue(actions.isEmpty());
    }

    private static List<String> labels(List<ResultContextMenu.Action> actions) {
        return actions.stream().map(action -> action.label().getString()).toList();
    }

    private static List<String> ids(List<ResultContextMenu.Action> actions) {
        return actions.stream().map(ResultContextMenu.Action::id).toList();
    }

    private static SearchNode item(String path, String name) {
        return item(path, name, Map.of());
    }

    private static SearchNode item(String path, String name, Map<String, String> metadata) {
        return new SearchNode(
                new ResourceLocation("minecraft:" + path),
                NodeType.ITEM,
                name,
                0,
                0,
                metadata
        );
    }

    private static ItemStack stack(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("minecraft:" + path)));
    }
}
