package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.api.AmiContextMenuAction;
import com.sanhiruzu.ami.api.AmiGuideDocument;
import com.sanhiruzu.ami.api.AmiItemContext;
import com.sanhiruzu.ami.api.AmiQuestDocument;
import com.sanhiruzu.ami.api.AmiQuestTaskDocument;
import com.sanhiruzu.ami.api.AmiQuestsApi;
import com.sanhiruzu.ami.api.IAmiPlugin;
import com.sanhiruzu.ami.client.favorites.FavoriteEntry;
import com.sanhiruzu.ami.compat.GregTechCompat;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.AmiGuideSearchIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.player.LiveWaypointContext;
import com.sanhiruzu.ami.player.PlayerWaypointAction;
import com.sanhiruzu.ami.player.PlayerWaypointProvider;
import com.sanhiruzu.ami.player.PlayerWaypointProviders;
import com.sanhiruzu.searchableitems.api.SearchableItemAction;
import com.sanhiruzu.searchableitems.api.SearchableItemActionContext;
import com.sanhiruzu.searchableitems.api.SearchableItemActionProvider;
import com.sanhiruzu.searchableitems.api.SearchableItemActionProviders;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        SearchableItemActionProviders.clearForTests();
        ResultContextMenuActionBuilder.clearPendingCategoryFixForTests();
        com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.disablePersistenceForTests();
        com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.clearForTests();
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
    void itemNodesExposeSourcesActionWhenPanelCanOpenSourcesView() {
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();
        AtomicReference<SearchNode> opened = new AtomicReference<>();
        SearchNode leather = item("leather", "Leather");

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(
                        leather,
                        ItemStack.EMPTY,
                        null,
                        ignored -> {
                        },
                        null,
                        opened::set
                )
        );

        ResultContextMenu.Action sources = firstAction(actions, ResultContextMenuActionBuilder.SOURCES);
        assertEquals("ami.context.sources", sources.label().getString());

        sources.onClick().run();

        assertEquals(leather, opened.get());
    }

    @Test
    void playerNodesUsePlayerSpecificActionsAndGateAdminCommands() {
        ResultContextMenuActionBuilder regularBuilder = new ResultContextMenuActionBuilder(() -> false);
        com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler favorites =
                com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.getInstance();
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

        List<ResultContextMenu.Action> regularActions = regularBuilder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(player, ItemStack.EMPTY, favorites, ignored -> {
                })
        );

        assertEquals(List.of(
                ResultContextMenuActionBuilder.FAVORITE,
                ResultContextMenuActionBuilder.COPY_PLAYER_NAME,
                ResultContextMenuActionBuilder.CHAT
        ), ids(regularActions));

        ResultContextMenuActionBuilder adminBuilder = new ResultContextMenuActionBuilder(() -> true);
        List<ResultContextMenu.Action> adminActions = adminBuilder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(player, ItemStack.EMPTY, favorites, ignored -> {
                })
        );

        ResultContextMenu.Action adminMenu = firstAction(adminActions, ResultContextMenuActionBuilder.PLAYER_ADMIN_MENU);
        assertEquals(List.of(
                ResultContextMenuActionBuilder.CHEAT_GIVE_PLAYER_HEAD,
                ResultContextMenuActionBuilder.TELEPORT_TO_PLAYER,
                ResultContextMenuActionBuilder.TELEPORT_PLAYER_HERE
        ), ids(adminMenu.children()));

        favorites.removeFavorite(player);
    }

    @Test
    void waypointNodesExposeFavoriteAction() {
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder(() -> false);
        com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler favorites =
                com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.getInstance();
        SearchNode waypoint = new SearchNode(
                new ResourceLocation("ami:waypoint/waystones/home"),
                NodeType.WAYPOINT,
                "Home",
                0,
                0,
                Map.of(
                        SearchNodeKeys.WAYPOINT_PROVIDER, "waystones",
                        SearchNodeKeys.WAYPOINT_DIMENSION, "minecraft:overworld",
                        SearchNodeKeys.WAYPOINT_X, "10",
                        SearchNodeKeys.WAYPOINT_Y, "64",
                        SearchNodeKeys.WAYPOINT_Z, "-20"
                )
        );

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(waypoint, ItemStack.EMPTY, favorites, ignored -> {
                })
        );

        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.FAVORITE));

        favorites.removeFavorite(waypoint);
    }

    @Test
    void mergedWaypointContextMenuShowsProviderSpecificActions() {
        PlayerWaypointProviders.register(new PlayerWaypointProvider() {
            @Override
            public String id() {
                return "mockftb";
            }

            @Override
            public String label() {
                return "Mock FTB";
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public java.util.Optional<PlayerWaypointAction> openLiveWaypointAction(LiveWaypointContext context) {
                return java.util.Optional.of(new PlayerWaypointAction("ami:open_mock_ftb", "Open Mock FTB", 'o', () -> {
                }));
            }

            @Override
            public List<PlayerWaypointAction> liveWaypointActions(LiveWaypointContext context) {
                return List.of(new PlayerWaypointAction("ami:delete_mock_ftb", "Delete Mock FTB", 'd', () -> {
                }));
            }
        });
        PlayerWaypointProviders.register(new PlayerWaypointProvider() {
            @Override
            public String id() {
                return "mockjourney";
            }

            @Override
            public String label() {
                return "Mock Journey";
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public java.util.Optional<PlayerWaypointAction> openLiveWaypointAction(LiveWaypointContext context) {
                return java.util.Optional.of(new PlayerWaypointAction("ami:open_mock_journey", "Open Mock Journey", 'j', () -> {
                }));
            }

            @Override
            public List<PlayerWaypointAction> liveWaypointActions(LiveWaypointContext context) {
                return List.of(new PlayerWaypointAction("ami:edit_mock_journey", "Edit Mock Journey", 'e', () -> {
                }));
            }
        });
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder(() -> false);
        com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler favorites =
                com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.getInstance();
        SearchNode waypoint = new SearchNode(
                new ResourceLocation("ami:waypoint/merged/home"),
                NodeType.WAYPOINT,
                "Home",
                0,
                0,
                Map.ofEntries(
                        Map.entry(SearchNodeKeys.WAYPOINT_PROVIDER, "mockftb"),
                        Map.entry(SearchNodeKeys.WAYPOINT_PROVIDER_LABEL, "Mock FTB"),
                        Map.entry(SearchNodeKeys.WAYPOINT_ID, "ftb-home"),
                        Map.entry(SearchNodeKeys.WAYPOINT_NAME, "Home"),
                        Map.entry(SearchNodeKeys.WAYPOINT_DIMENSION, "minecraft:overworld"),
                        Map.entry(SearchNodeKeys.WAYPOINT_X, "10"),
                        Map.entry(SearchNodeKeys.WAYPOINT_Y, "64"),
                        Map.entry(SearchNodeKeys.WAYPOINT_Z, "-20"),
                        Map.entry("waypointPrimaryProvider", "mockftb"),
                        Map.entry("waypointPrimaryProviderLabel", "Mock FTB"),
                        Map.entry("waypointMergedProviders", "mockftb,mockjourney"),
                        Map.entry("waypointMergedProviderLabels", "Mock FTB,Mock Journey"),
                        Map.entry("waypointMergedProviderIds", "ftb-home,jm-home")
                )
        );

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(waypoint, ItemStack.EMPTY, favorites, ignored -> {
                })
        );

        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.OPEN_WAYPOINT));
        assertTrue(labels(actions).stream().anyMatch(label -> label.contains("Mock FTB")));

        favorites.removeFavorite(waypoint);
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
    void externalRecipeViewerShowsRecipeAndUseActionsWithoutIndexedEvidence() {
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder(
                () -> false,
                stack -> false,
                List::of,
                () -> true
        );

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(
                        item("diamond", "Diamond", Map.of(
                                SearchNodeKeys.RECIPE_OUTPUT_COUNT, "0",
                                SearchNodeKeys.RECIPE_USE_COUNT, "0"
                        )),
                        stack("diamond"),
                        null,
                        ignored -> {
                        }
                )
        );

        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.RECIPES));
        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.USES));
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
    void sharedItemActionProvidersCanAppendItemContextMenuActionsWithoutAmiPlugin() {
        AtomicReference<SearchableItemActionContext> seenContext = new AtomicReference<>();
        AtomicBoolean ran = new AtomicBoolean();
        SearchableItemActionProviders.register(new SearchableItemActionProvider() {
            @Override
            public String id() {
                return "example:actions";
            }

            @Override
            public void addItemActions(SearchableItemActionContext context, Consumer<SearchableItemAction> actions) {
                seenContext.set(context);
                actions.accept(SearchableItemAction.enabled(
                        "example:shared_action",
                        Component.literal("Shared Action"),
                        's',
                        () -> ran.set(true)
                ));
            }
        });
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder(
                () -> true,
                stack -> false,
                List::of
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

        ResultContextMenu.Action sharedAction = actions.stream()
                .filter(action -> action.id().equals("example:shared_action"))
                .findFirst()
                .orElseThrow();
        assertEquals("ITEM", seenContext.get().type());
        assertTrue(seenContext.get().cheatEnabled());

        sharedAction.onClick().run();
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

        firstAction(actions, ResultContextMenuActionBuilder.QUESTS_FOR_ITEM).onClick().run();
        firstAction(actions, ResultContextMenuActionBuilder.OPEN_QUEST).onClick().run();

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
    void ae2GuideDocumentForNodePrefersExactReferencedItemPage() {
        SearchNode node = modItem("ae2", "singularity", "Singularity");
        AmiGuideSearchIndex guideIndex = new AmiGuideSearchIndex(List.of(
                guideMeDocument("guide/guideme/ae2/searchable_singularity", "appendix/searchable_singularity",
                        "Searching for Singularity", List.of()),
                guideMeDocument("guide/guideme/ae2/singularity", "items-blocks-machines/singularity",
                        "Singularity", List.of(new ResourceLocation("ae2", "singularity")))
        ), AmiGuideSearchIndex.GuideIndexingMode.TITLES);

        AmiGuideDocument document = ResultContextMenuActionBuilder.ae2GuideDocumentForNode(node, guideIndex)
                .orElseThrow();

        assertEquals("items-blocks-machines/singularity", document.pageId());
    }

    @Test
    void ae2GuideDocumentForNodeFallsBackToAe2GuideSearch() {
        SearchNode node = modItem("ae2", "annihilation_plane", "Annihilation Plane");
        AmiGuideSearchIndex guideIndex = new AmiGuideSearchIndex(List.of(
                guideMeDocument("guide/guideme/ae2/annihilation_plane", "items-blocks-machines/annihilation_plane",
                        "Annihilation Plane", List.of())
        ), AmiGuideSearchIndex.GuideIndexingMode.TITLES);

        AmiGuideDocument document = ResultContextMenuActionBuilder.ae2GuideDocumentForNode(node, guideIndex)
                .orElseThrow();

        assertEquals("items-blocks-machines/annihilation_plane", document.pageId());
    }

    @Test
    void ae2GuideDocumentForNodeIgnoresNonAe2Items() {
        SearchNode node = modItem("mekanism", "jetpack", "Jetpack");
        AmiGuideSearchIndex guideIndex = new AmiGuideSearchIndex(List.of(
                guideMeDocument("guide/guideme/ae2/jetpack", "items-blocks-machines/jetpack",
                        "Jetpack", List.of(new ResourceLocation("ae2", "jetpack")))
        ), AmiGuideSearchIndex.GuideIndexingMode.TITLES);

        assertTrue(ResultContextMenuActionBuilder.ae2GuideDocumentForNode(node, guideIndex).isEmpty());
    }

    @Test
    void cobblemonPokemonDocumentationUsesCobblemonToolsPokedexPage() {
        SearchNode node = new SearchNode(
                new ResourceLocation("cobblemon", "species/mr_mime"),
                NodeType.ENTITY,
                "Mr. Mime",
                0,
                0,
                Map.of(
                        SearchNodeKeys.ENTITY_CATEGORY, "pokemon_species",
                        SearchNodeKeys.POKEMON_SPECIES, "cobblemon:mr_mime"
                )
        );

        ResultContextMenuActionBuilder.DocumentationTarget target =
                ResultContextMenuActionBuilder.documentationTargetFor(node);

        assertEquals(ResultContextMenuActionBuilder.DocumentationKind.COBBLEMON_TOOLS, target.kind());
        assertEquals("ami.context.open_cobblemon_tools", target.label().getString());
        assertEquals(URI.create("https://cobblemon.tools/pokedex/pokemon/mr_mime"), target.uri());
    }

    @Test
    void cobblemonPokemonDocumentationUsesSpeciesIdWhenNameMissing() {
        SearchNode node = new SearchNode(
                new ResourceLocation("cobblemon", "species/mr_mime"),
                NodeType.ENTITY,
                "",
                0,
                0,
                Map.of(
                        SearchNodeKeys.ENTITY_CATEGORY, "pokemon_species",
                        SearchNodeKeys.POKEMON_SPECIES, "cobblemon:mr_mime"
                )
        );

        ResultContextMenuActionBuilder.DocumentationTarget target =
                ResultContextMenuActionBuilder.documentationTargetFor(node);

        assertEquals(ResultContextMenuActionBuilder.DocumentationKind.COBBLEMON_TOOLS, target.kind());
        assertEquals(URI.create("https://cobblemon.tools/pokedex/pokemon/mr_mime"), target.uri());
    }

    @Test
    void pokemonContextActionsAddClientSideFiltersAndDropRoutes() {
        AtomicReference<String> token = new AtomicReference<>();
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();
        SearchNode node = new SearchNode(
                new ResourceLocation("cobblemon", "species/bulbasaur"),
                NodeType.ENTITY,
                "Bulbasaur",
                0,
                0,
                Map.of(
                        SearchNodeKeys.ENTITY_CATEGORY, "pokemon_species",
                        SearchNodeKeys.POKEMON_SPECIES, "cobblemon:bulbasaur",
                        SearchNodeKeys.POKEMON_DEX_NUMBER, "1",
                        SearchNodeKeys.POKEMON_GENERATION, "1",
                        SearchNodeKeys.POKEMON_PRIMARY_TYPE, "grass",
                        SearchNodeKeys.POKEMON_SECONDARY_TYPE, "poison",
                        SearchNodeKeys.POKEMON_EGG_GROUPS, "monster,grass",
                        SearchNodeKeys.POKEMON_ABILITIES, "overgrow,chlorophyll",
                        SearchNodeKeys.POKEMON_DROP_ITEM, "minecraft:apple,minecraft:redstone"
                )
        );

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(node, ItemStack.EMPTY, null, token::set)
        );

        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.FILTER_POKEMON_GENERATION));
        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.FILTER_POKEMON_EGG_GROUP));
        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.FILTER_POKEMON_ABILITY));
        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.SEARCH_POKEMON_DROP_ITEM));
        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.RECIPES_POKEMON_DROP_ITEM));
        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.COPY_POKEMON_DEX_NUMBER));

        firstAction(actions, ResultContextMenuActionBuilder.FILTER_POKEMON_GENERATION).onClick().run();
        assertEquals("?generation:1", token.get());
        firstAction(actions, ResultContextMenuActionBuilder.FILTER_POKEMON_EGG_GROUP).onClick().run();
        assertEquals("%egg:monster", token.get());
        firstAction(actions, ResultContextMenuActionBuilder.FILTER_POKEMON_ABILITY).onClick().run();
        assertEquals("?ability:overgrow", token.get());
        firstAction(actions, ResultContextMenuActionBuilder.SEARCH_POKEMON_DROP_ITEM).onClick().run();
        assertEquals("apple", token.get());
    }

    @Test
    void pokemonDropContextActionsIgnoreInvalidDropItems() {
        AtomicReference<String> token = new AtomicReference<>();
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();
        SearchNode node = new SearchNode(
                new ResourceLocation("cobblemon", "species/bulbasaur"),
                NodeType.ENTITY,
                "Bulbasaur",
                0,
                0,
                Map.of(
                        SearchNodeKeys.ENTITY_CATEGORY, "pokemon_species",
                        SearchNodeKeys.POKEMON_DROP_ITEM, "minecraft:,minecraft:air,minecraft:missing_item,minecraft:apple"
                )
        );

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(node, ItemStack.EMPTY, null, token::set)
        );

        assertEquals(1, ids(actions).stream()
                .filter(id -> id.equals(ResultContextMenuActionBuilder.SEARCH_POKEMON_DROP_ITEM))
                .count());
        assertEquals(1, ids(actions).stream()
                .filter(id -> id.equals(ResultContextMenuActionBuilder.RECIPES_POKEMON_DROP_ITEM))
                .count());

        firstAction(actions, ResultContextMenuActionBuilder.SEARCH_POKEMON_DROP_ITEM).onClick().run();
        assertEquals("apple", token.get());
    }

    @Test
    void gregTechContextActionsAddTierKindAndFactFilters() {
        AtomicReference<String> token = new AtomicReference<>();
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();
        SearchNode node = new SearchNode(
                new ResourceLocation("gtceu", "lv_macerator"),
                NodeType.ITEM,
                "LV Macerator",
                0,
                0,
                Map.of(
                        SearchNodeKeys.COMPAT_FAMILIES, "gregtech",
                        SearchNodeKeys.GREGTECH_ITEM_KIND, "machines",
                        SearchNodeKeys.GREGTECH_TIER, "lv",
                        SearchNodeKeys.GREGTECH_FACTS, "machine,power"
                )
        );

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(node, ItemStack.EMPTY, null, token::set)
        );

        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.FILTER_GREGTECH_TIER));
        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.FILTER_GREGTECH_KIND));
        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.FILTER_GREGTECH_FACT));

        firstAction(actions, ResultContextMenuActionBuilder.FILTER_GREGTECH_TIER).onClick().run();
        assertEquals("?gregtechTier:lv", token.get());
        firstAction(actions, ResultContextMenuActionBuilder.FILTER_GREGTECH_KIND).onClick().run();
        assertEquals("?gregtechKind:machines", token.get());
        firstAction(actions, ResultContextMenuActionBuilder.FILTER_GREGTECH_FACT).onClick().run();
        assertEquals("?gregtechFact:power", token.get());
    }

    @Test
    void gregTechCircuitContextActionsUseTierAndGradeWithoutDuplicateCircuitFact() {
        AtomicReference<String> token = new AtomicReference<>();
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();
        SearchNode node = new SearchNode(
                new ResourceLocation("gtceu", "basic_electronic_circuit"),
                NodeType.ITEM,
                "Basic Electronic Circuit",
                0,
                0,
                Map.of(
                        SearchNodeKeys.COMPAT_FAMILIES, "gregtech",
                        SearchNodeKeys.GREGTECH_ITEM_KIND, "circuits",
                        SearchNodeKeys.GREGTECH_TIER, "lv",
                        SearchNodeKeys.GREGTECH_CIRCUIT_GRADE, "basic",
                        SearchNodeKeys.GREGTECH_FACTS, "circuit"
                )
        );

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(node, ItemStack.EMPTY, null, token::set)
        );

        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.FILTER_GREGTECH_TIER));
        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.FILTER_GREGTECH_KIND));
        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.FILTER_GREGTECH_CIRCUIT_GRADE));
        assertTrue(!ids(actions).contains(ResultContextMenuActionBuilder.FILTER_GREGTECH_FACT));

        firstAction(actions, ResultContextMenuActionBuilder.FILTER_GREGTECH_TIER).onClick().run();
        assertEquals("?gregtechTier:lv", token.get());
        firstAction(actions, ResultContextMenuActionBuilder.FILTER_GREGTECH_CIRCUIT_GRADE).onClick().run();
        assertEquals("?gregtechCircuit:basic", token.get());
    }

    @Test
    void gregTechCircuitContextActionsUseIndexedFallbackTierAndGrade() {
        AtomicReference<String> token = new AtomicReference<>();
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();
        ResourceLocation id = new ResourceLocation("gtceu", "good_electronic_circuit");
        Map<String, String> metadata = new HashMap<>();
        metadata.put(SearchNodeKeys.MOD_ID, "gtceu");
        GregTechCompat.enrichItem(id, metadata);
        SearchNode node = new SearchNode(
                id,
                NodeType.ITEM,
                "Good Electronic Circuit",
                0,
                0,
                Map.copyOf(metadata)
        );

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(node, ItemStack.EMPTY, null, token::set)
        );

        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.FILTER_GREGTECH_TIER));
        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.FILTER_GREGTECH_CIRCUIT_GRADE));

        firstAction(actions, ResultContextMenuActionBuilder.FILTER_GREGTECH_TIER).onClick().run();
        assertEquals("?gregtechTier:mv", token.get());
        firstAction(actions, ResultContextMenuActionBuilder.FILTER_GREGTECH_CIRCUIT_GRADE).onClick().run();
        assertEquals("?gregtechCircuit:good", token.get());
    }

    @Test
    void unknownModDocumentationUsesWebSearch() {
        SearchNode node = modItem("simpletms", "tr_irontail", "Iron Tail TM");

        ResultContextMenuActionBuilder.DocumentationTarget target =
                ResultContextMenuActionBuilder.documentationTargetFor(node);

        assertEquals(ResultContextMenuActionBuilder.DocumentationKind.WEB_SEARCH, target.kind());
        assertEquals("ami.context.search_web", target.label().getString());
        String uri = target.uri().toString();
        assertEquals("https://duckduckgo.com/?q=Iron+Tail+TM+simpletms", uri);
        assertTrue(!uri.contains("simpletms%3Atr_irontail"));
        assertTrue(!uri.contains("minecraft+mod"));
    }

    @Test
    void gregTechDocumentationSearchUsesItemNameAndModNameOnly() {
        SearchNode node = new SearchNode(
                new ResourceLocation("gtceu", "wetware_printed_circuit_board"),
                NodeType.ITEM,
                "Wetware Printed Circuit Board",
                0,
                0,
                Map.of(SearchNodeKeys.COMPAT_FAMILIES, "gregtech")
        );

        ResultContextMenuActionBuilder.DocumentationTarget target =
                ResultContextMenuActionBuilder.documentationTargetFor(node);

        assertEquals(ResultContextMenuActionBuilder.DocumentationKind.WEB_SEARCH, target.kind());
        assertEquals(URI.create("https://duckduckgo.com/?q=Wetware+Printed+Circuit+Board+GregTech"), target.uri());
    }

    @Test
    void silentGearMaterialBookUsesNativeDocumentationTarget() {
        SearchNode node = new SearchNode(
                new ResourceLocation("silentgear", "material_book"),
                NodeType.ITEM,
                "Material Book",
                0,
                0,
                Map.of(
                        SearchNodeKeys.ITEM_CLASS, "net.silentchaos512.gear.item.MaterialBookItem",
                        SearchNodeKeys.FACETS, "book,guide_book"
                )
        );

        ResultContextMenuActionBuilder.DocumentationTarget target =
                ResultContextMenuActionBuilder.documentationTargetFor(node);

        assertEquals(ResultContextMenuActionBuilder.DocumentationKind.SILENT_GEAR_MATERIAL_BOOK, target.kind());
        assertEquals("ami.context.open_silentgear_material_book", target.label().getString());
    }

    @Test
    void malformedConfigFallsBackToDefaultActions() {
        assertEquals(ResultContextMenuActionBuilder.KNOWN_ACTIONS,
                ResultContextMenuActionPolicy.parseEnabledActionIds("missing nonsense"));
        assertTrue(ResultContextMenuActionPolicy.parseScopedDisables("bad; also bad; =ami:wiki").isEmpty());
    }

    @Test
    void legacyDefaultContextMenuConfigPicksUpNewDefaultActions() {
        String legacyDefault = "ami:copy_tooltip,ami:craft_one,ami:craft_stack,ami:recipes,ami:uses,ami:favorite,"
                + "ami:chat,ami:wiki,ami:locate,ami:cheat_give_one,ami:cheat_give_stack,ami:cheat_spawn_egg,"
                + "ami:cheat_spawn_egg_stack,ami:cheat_spawn_pokemon,ami:cheat_pokemon_party,ami:group_toggle,"
                + "ami:filter_category,ami:copy_group_key,ami:start_category_fix,ami:apply_category_fix,"
                + "ami:clear_item_fix,ami:quests_for_item,ami:open_quest,ami:copy_quest_matches";

        Set<String> enabled = ResultContextMenuActionPolicy.parseEnabledActionIds(legacyDefault);

        assertTrue(enabled.contains(ResultContextMenuActionBuilder.SOURCES));
        assertTrue(enabled.contains(ResultContextMenuActionBuilder.FILTER_POKEMON_GENERATION));
        assertTrue(enabled.contains(ResultContextMenuActionBuilder.RECIPES_POKEMON_DROP_ITEM));
        assertTrue(enabled.contains(ResultContextMenuActionBuilder.FILTER_GREGTECH_TIER));
        assertTrue(enabled.contains(ResultContextMenuActionBuilder.FILTER_GREGTECH_CIRCUIT_GRADE));
    }

    @Test
    void preSourcesDefaultContextMenuConfigPicksUpSourcesAction() {
        String preSourcesDefault = ResultContextMenuActionBuilder.DEFAULT_ACTIONS
                .replace(ResultContextMenuActionBuilder.SOURCES + ",", "");

        assertTrue(ResultContextMenuActionPolicy.parseEnabledActionIds(preSourcesDefault)
                .contains(ResultContextMenuActionBuilder.SOURCES));
    }

    @Test
    void preQuestLegacyDefaultContextMenuConfigPicksUpNewDefaultActions() {
        String legacyDefault = "ami:copy_tooltip,ami:craft_one,ami:craft_stack,ami:recipes,ami:uses,ami:favorite,"
                + "ami:chat,ami:wiki,ami:locate,ami:cheat_give_one,ami:cheat_give_stack,ami:cheat_spawn_egg,"
                + "ami:cheat_spawn_egg_stack,ami:cheat_spawn_pokemon,ami:cheat_pokemon_party,ami:group_toggle,"
                + "ami:filter_category,ami:copy_group_key,ami:start_category_fix,ami:apply_category_fix,"
                + "ami:clear_item_fix";

        Set<String> enabled = ResultContextMenuActionPolicy.parseEnabledActionIds(legacyDefault);

        assertTrue(enabled.contains(ResultContextMenuActionBuilder.SOURCES));
        assertTrue(enabled.contains(ResultContextMenuActionBuilder.OPEN_POKEDEX));
        assertTrue(enabled.contains(ResultContextMenuActionBuilder.SEARCH_POKEMON_DROP_ITEM));
        assertTrue(enabled.contains(ResultContextMenuActionBuilder.RECIPES_POKEMON_DROP_ITEM));
        assertTrue(enabled.contains(ResultContextMenuActionBuilder.COPY_POKEMON_DEX_NUMBER));
        assertTrue(enabled.contains(ResultContextMenuActionBuilder.FILTER_GREGTECH_KIND));
        assertTrue(enabled.contains(ResultContextMenuActionBuilder.FILTER_GREGTECH_CIRCUIT_GRADE));
        assertTrue(enabled.contains(ResultContextMenuActionBuilder.COPY_QUEST_MATCHES));
    }

    @Test
    void customContextMenuAllowListDoesNotPickUpNewDefaultActions() {
        Set<String> enabled = ResultContextMenuActionPolicy.parseEnabledActionIds("ami:wiki,ami:chat");

        assertTrue(enabled.contains(ResultContextMenuActionBuilder.WIKI));
        assertTrue(enabled.contains(ResultContextMenuActionBuilder.CHAT));
        assertTrue(!enabled.contains(ResultContextMenuActionBuilder.SOURCES));
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
    void startCategoryFixEnablesMoveHereOnCategoryGroup() {
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();
        SearchNode stone = item("stone", "Stone");
        List<ResultContextMenu.Action> itemActions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(stone, ItemStack.EMPTY, null, ignored -> {
                })
        );

        itemActions.stream()
                .filter(action -> ResultContextMenuActionBuilder.START_CATEGORY_FIX.equals(action.id()))
                .findFirst()
                .orElseThrow()
                .onClick()
                .run();

        TreeNode group = new TreeNode("masonry/full_block", Component.literal("Full Blocks"));
        group.addChild(new TreeNode(Component.literal("Dirt"), item("dirt", "Dirt")));
        List<ResultContextMenu.Action> groupActions = builder.forGroup(
                new ResultContextMenuActionBuilder.GroupContext(group, ignored -> {
                }, () -> {
                })
        );

        assertTrue(ids(groupActions).contains(ResultContextMenuActionBuilder.APPLY_CATEGORY_FIX));
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

        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.CREATE_PACK_FILES));
        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.COPY_FTB_QUEST_SKELETON));
        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.COPY_TAXONOMY_TEMPLATE));
        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.COPY_KUBEJS_RECIPE_REMOVAL_SCRIPT));
        assertTrue(ids(actions).contains(ResultContextMenuActionBuilder.COPY_PACK_AUTHOR_REPORT));

        String skeleton = ResultContextMenuActionBuilder.ftbQuestSkeleton(
                "Food",
                List.of(item("apple", "Apple"), item("bread", "Bread")),
                false
        );
        assertTrue(skeleton.contains("title:\"Food\""));
        assertTrue(skeleton.contains("item:\"minecraft:apple\""));
        assertTrue(skeleton.contains("item:\"minecraft:bread\""));

        String taxonomy = ResultContextMenuActionBuilder.customTaxonomyTemplate(
                group,
                List.of(item("apple", "Apple"), item("bread", "Bread")),
                false
        );
        assertTrue(taxonomy.contains("\"ids\""));
        assertTrue(taxonomy.contains("\"category\": \"food\""));
        assertTrue(taxonomy.contains("\"label\": \"Food\""));
    }

    @Test
    void taxonomyTemplateCarriesCommonGroupingMetadata() {
        TreeNode group = new TreeNode("cardinality:family:colors:tintable/yellow/buttons", Component.literal("Yellow Buttons"));
        SearchNode first = item("yellow_stone_button", "Yellow Stone Button", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "redstone",
                SearchNodeKeys.COLLAPSE_FAMILY, "colors:tintable/yellow/buttons",
                SearchNodeKeys.COLLAPSE_LABEL, "Yellow Buttons",
                SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed"
        ));
        SearchNode second = item("yellow_calcite_button", "Yellow Calcite Button", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "redstone",
                SearchNodeKeys.COLLAPSE_FAMILY, "colors:tintable/yellow/buttons",
                SearchNodeKeys.COLLAPSE_LABEL, "Yellow Buttons",
                SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed"
        ));

        String taxonomy = ResultContextMenuActionBuilder.customTaxonomyTemplate(group, List.of(first, second), false);

        assertTrue(taxonomy.contains("\"category\": \"masonry\""));
        assertTrue(taxonomy.contains("\"subcategory\": \"redstone\""));
        assertTrue(taxonomy.contains("\"collapseFamily\": \"colors:tintable/yellow/buttons\""));
        assertTrue(taxonomy.contains("\"collapseLabel\": \"Yellow Buttons\""));
        assertTrue(taxonomy.contains("\"variantCollapseMode\": \"default_collapsed\""));
    }

    @Test
    void kubeJsRecipeRemovalScriptListsGroupOutputs() {
        String script = ResultContextMenuActionBuilder.kubeJsRecipeRemovalScript(
                "Food",
                List.of(item("apple", "Apple"), item("bread", "Bread")),
                false
        );

        assertTrue(script.contains("// AMI export: remove recipes for Food"));
        assertTrue(script.contains("event.remove({ output: 'minecraft:apple' })"));
        assertTrue(script.contains("event.remove({ output: 'minecraft:bread' })"));
    }

    @Test
    void writePackAuthorGroupFilesCreatesReadyToDropFiles(@TempDir Path tempDir) throws Exception {
        TreeNode group = new TreeNode("food", Component.literal("Food"));
        SearchNode apple = item("apple", "Apple");
        SearchNode bread = item("bread", "Bread");

        List<Path> written = ResultContextMenuActionBuilder.writePackAuthorGroupFiles(
                group,
                List.of(apple, bread),
                false,
                tempDir
        );

        assertEquals(3, written.size());
        assertTrue(Files.exists(tempDir.resolve(Path.of("ami", "ami-export-food-README.md"))));
        assertTrue(Files.exists(tempDir.resolve(Path.of("ami", "taxonomy", "ami-export-food.json"))));
        assertTrue(Files.exists(tempDir.resolve(Path.of("kubejs", "server_scripts", "ami", "ami-export-food.js"))));

        String readme = Files.readString(tempDir.resolve(Path.of("ami", "ami-export-food-README.md")));
        assertTrue(readme.contains("# AMI Pack Export: Food"));
        assertTrue(readme.contains("ami/taxonomy/ami-export-food.json"));
        assertTrue(readme.contains("kubejs/server_scripts/ami/ami-export-food.js"));
        assertTrue(readme.contains("`minecraft:apple`"));

        String taxonomy = Files.readString(tempDir.resolve(Path.of("ami", "taxonomy", "ami-export-food.json")));
        assertTrue(taxonomy.contains("\"category\": \"food\""));
        assertTrue(taxonomy.contains("\"ids\""));

        String script = Files.readString(tempDir.resolve(Path.of("kubejs", "server_scripts", "ami", "ami-export-food.js")));
        assertTrue(script.contains("event.remove({ output: 'minecraft:apple' })"));
        assertTrue(script.contains("event.remove({ output: 'minecraft:bread' })"));

        Method read = Class.forName("com.sanhiruzu.ami.config.AmiCustomTaxonomy")
                .getDeclaredMethod("read", Path.class, String.class);
        read.setAccessible(true);
        Object profile = read.invoke(null, tempDir.resolve(Path.of("ami", "taxonomy", "ami-export-food.json")), "test");

        Method categoriesAccessor = profile.getClass().getDeclaredMethod("categories");
        categoriesAccessor.setAccessible(true);
        Map<?, ?> categories = (Map<?, ?>) categoriesAccessor.invoke(profile);
        assertTrue(categories.containsKey("food"));

        Method rulesAccessor = profile.getClass().getDeclaredMethod("rules");
        rulesAccessor.setAccessible(true);
        List<?> rules = (List<?>) rulesAccessor.invoke(profile);
        assertEquals(1, rules.size());
    }

    @Test
    void documentationTargetForFavoriteMinecraftItemUsesMinecraftWiki() {
        SearchNode favoriteNode = new SearchNode(
                ResourceLocation.tryParse("ami:favorite/item/abc123"),
                NodeType.ITEM,
                "Stone",
                0, 0,
                Map.of(FavoriteEntry.META_BASE_ID, "minecraft:stone", FavoriteEntry.META_KIND, "item")
        );

        ResultContextMenuActionBuilder.DocumentationTarget target =
                ResultContextMenuActionBuilder.documentationTargetFor(favoriteNode);

        assertEquals(ResultContextMenuActionBuilder.DocumentationKind.MINECRAFT_WIKI, target.kind());
    }

    @Test
    void documentationTargetForFavoriteModItemUsesRealModNamespace() {
        SearchNode favoriteNode = new SearchNode(
                ResourceLocation.tryParse("ami:favorite/item/abc123"),
                NodeType.ITEM,
                "Iron Tail TM",
                0, 0,
                Map.of(FavoriteEntry.META_BASE_ID, "simpletms:tr_irontail", FavoriteEntry.META_KIND, "item")
        );

        ResultContextMenuActionBuilder.DocumentationTarget target =
                ResultContextMenuActionBuilder.documentationTargetFor(favoriteNode);

        assertEquals(ResultContextMenuActionBuilder.DocumentationKind.WEB_SEARCH, target.kind());
        assertEquals("https://duckduckgo.com/?q=Iron+Tail+TM+simpletms", target.uri().toString());
    }

    @Test
    void categoryFixOnFavoriteNodeResolvesToRealItemId() {
        ResultContextMenuActionBuilder builder = new ResultContextMenuActionBuilder();
        SearchNode favoriteNode = new SearchNode(
                ResourceLocation.tryParse("ami:favorite/item/abc123"),
                NodeType.ITEM,
                "Diamond",
                0, 0,
                Map.of(
                        FavoriteEntry.META_BASE_ID, "minecraft:diamond",
                        FavoriteEntry.META_KIND, "item"
                )
        );

        List<ResultContextMenu.Action> actions = builder.forItem(
                new ResultContextMenuActionBuilder.ItemContext(favoriteNode, ItemStack.EMPTY, null, ignored -> {})
        );

        firstAction(actions, ResultContextMenuActionBuilder.START_CATEGORY_FIX).onClick().run();

        assertEquals(ResourceLocation.tryParse("minecraft:diamond"),
                ResultContextMenuActionBuilder.pendingCategoryFixNodeForTests().id());
    }

    @Test
    void chatTextOnFavoriteNodeUsesRealItemId() {
        SearchNode favoriteNode = new SearchNode(
                ResourceLocation.tryParse("ami:favorite/item/abc123"),
                NodeType.ITEM,
                "Diamond",
                0, 0,
                Map.of(
                        FavoriteEntry.META_BASE_ID, "minecraft:diamond",
                        FavoriteEntry.META_KIND, "item"
                )
        );

        assertEquals("minecraft:diamond", ResultContextMenuActionBuilder.chatText(favoriteNode));
    }

    @Test
    void chatTextOnRegularNodeUsesNodeId() {
        SearchNode node = item("stone", "Stone");

        assertEquals("minecraft:stone", ResultContextMenuActionBuilder.chatText(node));
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
        return actions.stream().flatMap(ResultContextMenuActionBuilderTest::flatten).map(ResultContextMenu.Action::id).toList();
    }

    private static ResultContextMenu.Action firstAction(List<ResultContextMenu.Action> actions, String id) {
        return actions.stream()
                .flatMap(ResultContextMenuActionBuilderTest::flattenIncludingSubmenus)
                .filter(action -> action.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static java.util.stream.Stream<ResultContextMenu.Action> flattenIncludingSubmenus(ResultContextMenu.Action action) {
        if (action == null) {
            return java.util.stream.Stream.empty();
        }
        if (!action.isSubmenu()) {
            return java.util.stream.Stream.of(action);
        }
        return java.util.stream.Stream.concat(
                java.util.stream.Stream.of(action),
                action.children().stream().flatMap(ResultContextMenuActionBuilderTest::flattenIncludingSubmenus)
        );
    }

    private static java.util.stream.Stream<ResultContextMenu.Action> flatten(ResultContextMenu.Action action) {
        if (action == null) {
            return java.util.stream.Stream.empty();
        }
        if (!action.isSubmenu()) {
            return java.util.stream.Stream.of(action);
        }
        return action.children().stream().flatMap(ResultContextMenuActionBuilderTest::flatten);
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

    private static AmiGuideDocument guideMeDocument(String idPath, String pageId, String title,
                                                    List<ResourceLocation> referencedItems) {
        return AmiGuideDocument.builder(new ResourceLocation("ami", idPath), "guideme", "ae2", title)
                .bookId(new ResourceLocation("ae2", "guide"))
                .pageId(pageId)
                .referencedItems(referencedItems)
                .build();
    }

    private static ItemStack stack(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("minecraft:" + path)));
    }
}
