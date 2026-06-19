package com.sanhiruzu.ami.player;

import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerWaypointProvidersTest {
    @AfterEach
    void resetWaypointConfig() {
        com.sanhiruzu.ami.config.AmiConfig.resetToDefaults();
        PlayerWaypointProviders.resetLiveWaypointStateForTests();
    }

    @Test
    void availabilityFailuresFailClosed() {
        List<PlayerWaypointProvider> providers = List.of(
                provider("good", true),
                provider("bad", false),
                throwingProvider("throws")
        );

        assertEquals(List.of("good"), PlayerWaypointProviders.availableProviderIdsForTests(providers));
    }

    @Test
    void enrichmentKeepsWorkingWhenProviderThrows() {
        PlayerWaypointProvider good = new PlayerWaypointProvider() {
            @Override
            public String id() {
                return "good";
            }

            @Override
            public String label() {
                return "Good Map";
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public void enrich(PlayerWaypointContext context, Map<String, String> metadata) {
                metadata.put("goodProviderTouched", context.playerName());
            }
        };
        Map<String, String> meta = new HashMap<>();

        PlayerWaypointProviders.enrichForTests(List.of(throwingProvider("test_throwing_provider"), good),
                new PlayerWaypointContext("Alex", "uuid", meta), meta);

        assertEquals("Alex", meta.get("goodProviderTouched"));
        String providers = meta.get(SearchNodeKeys.PLAYER_WAYPOINT_PROVIDERS);
        org.junit.jupiter.api.Assertions.assertTrue(providers.contains("good"));
    }

    @Test
    void waypointExportsSkipUnavailableAndThrowingProviders() {
        PlayerWaypointProvider exporter = new PlayerWaypointProvider() {
            @Override
            public String id() {
                return "exporter";
            }

            @Override
            public String label() {
                return "Exporter";
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public Optional<PlayerWaypointExport> waypointExport(PlayerWaypointContext context) {
                return Optional.of(new PlayerWaypointExport(id(), label(), "payload:" + context.playerName()));
            }
        };

        List<PlayerWaypointExport> exports = PlayerWaypointProviders.waypointExportsForTests(
                List.of(exporter, throwingProvider("export_throwing_provider")),
                new PlayerWaypointContext("Alex", "uuid", Map.of()));

        org.junit.jupiter.api.Assertions.assertTrue(exports.stream()
                .anyMatch(export -> export.providerId().equals("exporter") && export.payload().equals("payload:Alex")));
    }

    @Test
    void waypointActionsSkipBrokenProvidersAndWrapActionFailures() {
        PlayerWaypointProvider actionProvider = new PlayerWaypointProvider() {
            @Override
            public String id() {
                return "action_provider";
            }

            @Override
            public String label() {
                return "Action Provider";
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public List<PlayerWaypointAction> waypointActions(PlayerWaypointContext context) {
                return List.of(new PlayerWaypointAction("ami:test_action", "Test Action", 't', () -> {
                    throw new IllegalStateException("click failed");
                }));
            }
        };
        PlayerWaypointProvider brokenBuilder = new PlayerWaypointProvider() {
            @Override
            public String id() {
                return "broken_builder";
            }

            @Override
            public String label() {
                return "Broken Builder";
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public List<PlayerWaypointAction> waypointActions(PlayerWaypointContext context) {
                throw new IllegalStateException("build failed");
            }
        };

        List<PlayerWaypointAction> actions = PlayerWaypointProviders.waypointActionsForTests(
                List.of(brokenBuilder, actionProvider),
                new PlayerWaypointContext("Alex", "uuid", Map.of())
        );

        assertEquals(List.of("ami:test_action"), actions.stream().map(PlayerWaypointAction::id).toList());
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> actions.get(0).action().run());
    }

    @Test
    void manualWaypointExportIsAlwaysAvailableWhenCoordinatesAreKnown() {
        ManualWaypointProvider provider = new ManualWaypointProvider();
        Map<String, String> metadata = Map.of(
                SearchNodeKeys.PLAYER_DIMENSION, "minecraft:overworld",
                SearchNodeKeys.PLAYER_X, "10",
                SearchNodeKeys.PLAYER_Y, "64",
                SearchNodeKeys.PLAYER_Z, "-20"
        );

        Optional<PlayerWaypointExport> export = provider.waypointExport(
                new PlayerWaypointContext("Alex", "uuid", metadata));

        org.junit.jupiter.api.Assertions.assertTrue(export.isPresent());
        assertEquals("manual", export.get().providerId());
        org.junit.jupiter.api.Assertions.assertTrue(export.get().payload().contains("/tp @s 10 64 -20"));
    }

    @Test
    void liveWaypointNodesNormalizeEnvironmentWaypointsMetadata() {
        PlayerWaypointProvider provider = new PlayerWaypointProvider() {
            @Override
            public String id() {
                return "test_provider";
            }

            @Override
            public String label() {
                return "Test Provider";
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public List<LiveWaypoint> liveWaypoints() {
                return List.of(new LiveWaypoint(
                        id(),
                        label(),
                        "demo",
                        "Demo",
                        "minecraft:overworld",
                        10,
                        64,
                        -5,
                        Map.of(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "provider_specific")
                ));
            }
        };

        var nodes = PlayerWaypointProviders.liveWaypointNodesForTests(List.of(provider));

        assertEquals(1, nodes.size());
        assertEquals("environment", nodes.get(0).meta(SearchNodeKeys.ONTOLOGY_CATEGORY, ""));
        assertEquals("waypoints", nodes.get(0).meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, ""));
    }

    @Test
    void liveWaypointNodesMergeEquivalentProviderWaypoints() {
        PlayerWaypointProvider ftb = providerWithWaypoints("ftbchunks", "FTB Chunks", List.of(
                new LiveWaypoint("ftbchunks", "FTB Chunks", "ftb-home", "Home",
                        "minecraft:overworld", 10, 64, 20,
                        Map.of(SearchNodeKeys.WAYPOINT_VISIBLE, "true", "waypointDeathpoint", "false"))
        ));
        PlayerWaypointProvider journey = providerWithWaypoints("journeymap", "JourneyMap", List.of(
                new LiveWaypoint("journeymap", "JourneyMap", "jm-home", "Home",
                        "minecraft:overworld", 10, 64, 20,
                        Map.of("waypoint_origin", "ftbchunks"))
        ));

        var nodes = PlayerWaypointProviders.liveWaypointNodesForTests(List.of(ftb, journey));

        assertEquals(1, nodes.size());
        assertEquals("Home", nodes.get(0).displayName());
        assertEquals("ftbchunks", nodes.get(0).meta("waypointPrimaryProvider", ""));
        assertEquals("ftbchunks,journeymap", nodes.get(0).meta("waypointMergedProviders", ""));
    }

    @Test
    void liveWaypointNodesKeepDistinctNamesSeparateAtSameCoordinates() {
        PlayerWaypointProvider provider = providerWithWaypoints("ftbchunks", "FTB Chunks", List.of(
                new LiveWaypoint("ftbchunks", "FTB Chunks", "home-a", "Home",
                        "minecraft:overworld", 10, 64, 20, Map.of()),
                new LiveWaypoint("ftbchunks", "FTB Chunks", "mine-b", "Mine",
                        "minecraft:overworld", 10, 64, 20, Map.of())
        ));

        var nodes = PlayerWaypointProviders.liveWaypointNodesForTests(List.of(provider));

        assertEquals(2, nodes.size());
    }

    @Test
    void mergedWaypointsPreferConfiguredPrimaryProvider() {
        com.sanhiruzu.ami.config.AmiConfig.waypointOpenProviderPriority = "journeymap,ftbchunks,xaero,manual";
        PlayerWaypointProvider ftb = providerWithWaypoints("ftbchunks", "FTB Chunks", List.of(
                new LiveWaypoint("ftbchunks", "FTB Chunks", "ftb-home", "Home",
                        "minecraft:overworld", 10, 64, 20, Map.of())
        ));
        PlayerWaypointProvider journey = providerWithWaypoints("journeymap", "JourneyMap", List.of(
                new LiveWaypoint("journeymap", "JourneyMap", "jm-home", "Home",
                        "minecraft:overworld", 10, 64, 20, Map.of())
        ));

        SearchNode node = PlayerWaypointProviders.liveWaypointNodesForTests(List.of(ftb, journey)).get(0);

        assertEquals("journeymap", node.meta("waypointPrimaryProvider", ""));
    }

    @Test
    void invalidateLiveWaypointCacheForcesRevisionChange() {
        PlayerWaypointProviders.resetLiveWaypointStateForTests();
        long before = PlayerWaypointProviders.liveWaypointRevisionForTests(List.of(
                providerWithWaypoints("ftbchunks", "FTB Chunks", List.of(
                        new LiveWaypoint("ftbchunks", "FTB Chunks", "home", "Home",
                                "minecraft:overworld", 10, 64, 20, Map.of())
                ))
        ));

        PlayerWaypointProviders.invalidateLiveWaypointCache();
        long after = PlayerWaypointProviders.liveWaypointRevisionForTests(List.of(
                providerWithWaypoints("ftbchunks", "FTB Chunks", List.of(
                        new LiveWaypoint("ftbchunks", "FTB Chunks", "mine", "Mine",
                                "minecraft:overworld", 10, 64, 20, Map.of())
                ))
        ));

        assertTrue(after > before);
    }

    @Test
    void mergedNodesPreserveFtbChunksWaypointStateMetadata() {
        PlayerWaypointProvider ftb = providerWithWaypoints("ftbchunks", "FTB Chunks", List.of(
                new LiveWaypoint("ftbchunks", "FTB Chunks", "home", "Home",
                        "minecraft:overworld", 10, 64, 20,
                        Map.of(
                                SearchNodeKeys.WAYPOINT_VISIBLE, "false",
                                "waypointDeathpoint", "true",
                                "waypointTransient", "true",
                                SearchNodeKeys.WAYPOINT_COLOR, "16711680"
                        ))
        ));

        SearchNode node = PlayerWaypointProviders.liveWaypointNodesForTests(List.of(ftb)).get(0);

        assertEquals("false", node.meta(SearchNodeKeys.WAYPOINT_VISIBLE, ""));
        assertEquals("true", node.meta("waypointDeathpoint", ""));
        assertEquals("true", node.meta("waypointTransient", ""));
    }

    private static PlayerWaypointProvider provider(String id, boolean available) {
        return new PlayerWaypointProvider() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String label() {
                return id;
            }

            @Override
            public boolean isAvailable() {
                return available;
            }
        };
    }

    private static PlayerWaypointProvider throwingProvider(String id) {
        return new PlayerWaypointProvider() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String label() {
                return id;
            }

            @Override
            public boolean isAvailable() {
                throw new IllegalStateException("boom");
            }
        };
    }

    private static PlayerWaypointProvider providerWithWaypoints(String id, String label, List<LiveWaypoint> waypoints) {
        return new PlayerWaypointProvider() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String label() {
                return label;
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public List<LiveWaypoint> liveWaypoints() {
                return waypoints;
            }
        };
    }
}
