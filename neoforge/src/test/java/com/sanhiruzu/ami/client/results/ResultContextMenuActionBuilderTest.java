package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.api.AmiContextMenuAction;
import com.sanhiruzu.ami.api.AmiItemContext;
import com.sanhiruzu.ami.api.AmiQuestDocument;
import com.sanhiruzu.ami.api.AmiQuestTaskDocument;
import com.sanhiruzu.ami.api.AmiQuestsApi;
import com.sanhiruzu.ami.api.IAmiPlugin;
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
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultContextMenuActionBuilderTest {
    @BeforeEach
    void resetConfig() {
        AmiConfig.resetToDefaults();
        AmiQuestsApi.clearQuestGroups();
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
                "ami.context.wiki",
                "ami.context.start_category_fix"
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

        assertEquals(List.of(
                ResultContextMenuActionBuilder.START_CATEGORY_FIX,
                ResultContextMenuActionBuilder.EDIT_CATEGORY_FIX,
                ResultContextMenuActionBuilder.FILTER_MOD
        ), ids(actions));
    }

    @Test
    void defaultConfigHidesLowValueTechnicalActions() {
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(item("stone", "Stone"), ItemStack.EMPTY, null, ignored -> {
                })
        );

        assertEquals(List.of(
                ResultContextMenuActionBuilder.CHAT,
                ResultContextMenuActionBuilder.WIKI,
                ResultContextMenuActionBuilder.START_CATEGORY_FIX
        ), ids(actions));
    }

    @Test
    void devModeShowsAdvancedCategoryFixEditor() {
        AmiConfig.devMode = true;
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(item("stone", "Stone"), ItemStack.EMPTY, null, ignored -> {
                })
        );

        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.EDIT_CATEGORY_FIX));
    }

    @Test
    void explicitConfigShowsAdvancedCategoryFixEditor() {
        AmiConfig.contextMenuEnabledActions = "ami:edit_category_fix";
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(item("stone", "Stone"), ItemStack.EMPTY, null, ignored -> {
                })
        );

        assertEquals(List.of(ResultContextMenuActionBuilder.EDIT_CATEGORY_FIX), ids(actions));
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
                ResultContextMenuActionBuilder.WIKI,
                ResultContextMenuActionBuilder.START_CATEGORY_FIX
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
    void pluginsCanAppendItemContextMenuActions() {
        AtomicReference<AmiItemContext> seenContext = new AtomicReference<>();
        AtomicBoolean ran = new AtomicBoolean();
        IAmiPlugin plugin = new IAmiPlugin() {
            @Override
            public void addItemContextMenuActions(AmiItemContext context, Consumer<AmiContextMenuAction> actions) {
                seenContext.set(context);
                actions.accept(AmiContextMenuAction.enabled(
                        "example:spawn_debug",
                        Component.literal("Spawn Debug"),
                        'd',
                        () -> ran.set(true)
                ));
            }
        };
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder(
                () -> true,
                stack -> false,
                () -> List.of(plugin)
        );

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(
                        item("diamond", "Diamond", Map.of(SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients")),
                        stack("diamond"),
                        null,
                        ignored -> {
                        }
                )
        );

        ResultContextMenu.Action pluginAction = actions.stream()
                .filter(action -> action.id().equals("example:spawn_debug"))
                .findFirst()
                .orElseThrow();
        assertEquals("ITEM", seenContext.get().type());
        assertTrue(seenContext.get().cheatEnabled());

        pluginAction.onClick().run();
        assertTrue(ran.get());
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
    void questMatchesAddQuestContextActions() {
        AtomicBoolean opened = new AtomicBoolean();
        AmiQuestsApi.registerQuestDocument(AmiQuestDocument.builder(
                        "ftbquests:chapter/basic_power",
                        "ftbquests",
                        "Basic Power")
                .sourceId("ftbquests")
                .chapterTitle("Getting Started")
                .task(AmiQuestTaskDocument.builder(
                                "ftbquests:chapter/basic_power/task/redstone",
                                "ftbquests:chapter/basic_power",
                                AmiQuestTaskDocument.Role.REQUIREMENT)
                        .taskType("item")
                        .itemId(new ResourceLocation("minecraft", "redstone"))
                        .requiredCount(12)
                        .build())
                .openAction(() -> opened.set(true))
                .build());
        AtomicReference<String> token = new AtomicReference<>();
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(
                        item("redstone", "Redstone"),
                        stack("redstone"),
                        null,
                        token::set
                )
        );

        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.QUESTS_FOR_ITEM));
        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.OPEN_QUEST));
        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.COPY_QUEST_MATCHES));

        actions.stream()
                .filter(action -> action.id().equals(ResultContextMenuActionBuilder.QUESTS_FOR_ITEM))
                .findFirst()
                .orElseThrow()
                .onClick()
                .run();
        actions.stream()
                .filter(action -> action.id().equals(ResultContextMenuActionBuilder.OPEN_QUEST))
                .findFirst()
                .orElseThrow()
                .onClick()
                .run();

        assertEquals("quest redstone", token.get());
        assertTrue(opened.get());
    }

    @Test
    void devModeCanCopyFtbItemTaskTemplate() {
        AmiConfig.devMode = true;
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(
                        item("apple", "Apple"),
                        stack("apple"),
                        null,
                        ignored -> {
                        }
                )
        );

        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.COPY_FTB_ITEM_TASK));
        assertEquals("{type:\"item\",item:\"minecraft:apple\",count:1}",
                ResultContextMenuActionBuilder.ftbItemTaskTemplate(new ResourceLocation("minecraft", "apple"), 1));
    }

    @Test
    void devModeCanCopyPackAuthorItemTemplates() {
        AmiConfig.devMode = true;
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(
                        item("apple", "Apple"),
                        stack("apple"),
                        null,
                        ignored -> {
                        }
                )
        );

        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.COPY_FTB_QUEST_SKELETON));
        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.COPY_KUBEJS_RECIPE_STUB));
        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.COPY_GAMESTAGE_CONDITION));
        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.COPY_PACK_AUTHOR_REPORT));

        String quest = ResultContextMenuActionBuilder.ftbQuestSkeleton(item("apple", "Apple"));
        assertTrue(quest.contains("title:\"Apple\""));
        assertTrue(quest.contains("item:\"minecraft:apple\""));

        String kubeJs = ResultContextMenuActionBuilder.kubeJsRecipeStub(new ResourceLocation("minecraft", "apple"));
        assertTrue(kubeJs.contains("event.shaped('minecraft:apple'"));

        String stage = ResultContextMenuActionBuilder.gameStageConditionStub(new ResourceLocation("minecraft", "apple"));
        assertTrue(stage.contains("const stage = 'replace_stage_id'"));
        assertTrue(stage.contains(".stage(stage)"));
    }

    @Test
    void packAuthorModeCanCopyAuthorTemplatesWithoutDevMode() {
        AmiConfig.packAuthorMode = true;
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(
                        item("apple", "Apple"),
                        stack("apple"),
                        null,
                        ignored -> {
                        }
                )
        );

        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.COPY_FTB_ITEM_TASK));
        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.COPY_PACK_AUTHOR_REPORT));
        assertTrue(!ids(actions).contains(ResultContextMenuActionBuilder.EDIT_CATEGORY_FIX));
    }

    @Test
    void packAuthorActionsStayHiddenByDefault() {
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(
                        item("apple", "Apple"),
                        stack("apple"),
                        null,
                        ignored -> {
                        }
                )
        );

        assertTrue(!ids(actions).contains(ResultContextMenuActionBuilder.COPY_FTB_ITEM_TASK));
        assertTrue(!ids(actions).contains(ResultContextMenuActionBuilder.COPY_PACK_AUTHOR_REPORT));
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
    void mekanismDocumentationUsesMekanismWiki() {
        SearchNode node = modItem("mekanism", "jetpack", "Jetpack");

        ResultContextMenuActionBuilder.DocumentationTarget target =
                ResultContextMenuActionBuilder.documentationTargetFor(node);

        assertEquals(ResultContextMenuActionBuilder.DocumentationKind.MEKANISM_WIKI, target.kind());
        assertEquals("ami.context.open_mekanism_wiki", target.label().getString());
        assertEquals(URI.create("https://wiki.aidancbrady.com/wiki/Jetpack"), target.uri());
    }

    @Test
    void unknownModDocumentationUsesWebSearch() {
        SearchNode node = modItem("simpletms", "tr_irontail", "Iron Tail TM");

        ResultContextMenuActionBuilder.DocumentationTarget target =
                ResultContextMenuActionBuilder.documentationTargetFor(node);

        assertEquals(ResultContextMenuActionBuilder.DocumentationKind.WEB_SEARCH, target.kind());
        assertEquals("ami.context.search_web", target.label().getString());
        String uri = target.uri().toString();
        assertTrue(uri.startsWith("https://duckduckgo.com/?q="));
        assertTrue(uri.contains("simpletms%3Atr_irontail"));
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
    void devModeGroupContextCanCopyFtbQuestSkeleton() {
        AmiConfig.devMode = true;
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();
        TreeNode group = new TreeNode("food", Component.literal("Food"));
        group.addChild(new TreeNode(Component.literal("Apple"), item("apple", "Apple")));
        group.addChild(new TreeNode(Component.literal("Bread"), item("bread", "Bread")));

        List<ResultContextMenu.Action> actions = builder.forGroup(
                new ResultContextMenuActionBuilder.GroupContext(group, ignored -> {
                }, () -> {
                })
        );

        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.COPY_FTB_QUEST_SKELETON));
        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.COPY_PACK_AUTHOR_REPORT));

        String skeleton = ResultContextMenuActionBuilder.ftbQuestSkeleton(
                "Food",
                List.of(item("apple", "Apple"), item("bread", "Bread")),
                false
        );
        assertTrue(skeleton.contains("title:\"Food\""));
        assertTrue(skeleton.contains("item:\"minecraft:apple\""));
        assertTrue(skeleton.contains("item:\"minecraft:bread\""));
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

    private static SearchNode modItem(String namespace, String path, String name) {
        return new SearchNode(
                new ResourceLocation(namespace + ":" + path),
                NodeType.ITEM,
                name,
                0,
                0,
                Map.of()
        );
    }

    private static ItemStack stack(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("minecraft:" + path)));
    }
}
