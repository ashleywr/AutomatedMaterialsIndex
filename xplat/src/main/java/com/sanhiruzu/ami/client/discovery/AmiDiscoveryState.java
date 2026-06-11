package com.sanhiruzu.ami.client.discovery;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Client-local discovery checklist state. The MVP tracks visited biome ids per
 * local world/profile and exposes runtime metadata for result projection.
 */
public final class AmiDiscoveryState {
    public static final String STATE_DISCOVERED = "discovered";
    public static final String STATE_UNDISCOVERED = "undiscovered";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int FORMAT_VERSION = 1;
    private static final int SAMPLE_INTERVAL_TICKS = 20;
    private static final AmiDiscoveryState INSTANCE = new AmiDiscoveryState();

    private static boolean persistenceEnabled = true;
    private static Path testRoot = null;

    private final Set<ResourceLocation> discoveredBiomes = new HashSet<>();
    private final Set<ResourceLocation> discoveredStructures = new HashSet<>();
    private final Set<ResourceLocation> tastedFoods = new HashSet<>();
    private String loadedScope = "";
    private int tickCounter = 0;
    private long revision = 0;

    private AmiDiscoveryState() {
    }

    public static AmiDiscoveryState getInstance() {
        return INSTANCE;
    }

    public static void disablePersistenceForTests() {
        persistenceEnabled = false;
        testRoot = null;
        resetForTests();
    }

    public static void usePersistenceRootForTests(Path root) {
        persistenceEnabled = true;
        testRoot = root;
        resetForTests();
    }

    public static void resetForTests() {
        INSTANCE.discoveredBiomes.clear();
        INSTANCE.discoveredStructures.clear();
        INSTANCE.tastedFoods.clear();
        INSTANCE.loadedScope = "";
        INSTANCE.tickCounter = 0;
        INSTANCE.revision = 0;
    }

    public static void markBiomeDiscoveredForTests(ResourceLocation id) {
        INSTANCE.discoveredBiomes.add(id);
        INSTANCE.revision++;
    }

    public static void markStructureDiscoveredForTests(ResourceLocation id) {
        INSTANCE.discoveredStructures.add(id);
        INSTANCE.revision++;
    }

    public static void markFoodTastedForTests(ResourceLocation id) {
        INSTANCE.tastedFoods.add(id);
        INSTANCE.revision++;
    }

    public long revision() {
        return revision;
    }

    public void clientTick() {
        if (!AmiConfig.enableDiscoveryChecklist) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        if (++tickCounter < SAMPLE_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        String scope = currentScope(mc);
        ensureLoaded(scope);
        mc.level.getBiome(mc.player.blockPosition())
                .unwrapKey()
                .filter(key -> key.isFor(Registries.BIOME))
                .ifPresent(key -> markBiomeDiscovered(scope, key.location()));

        if (mc.getSingleplayerServer() != null) {
            ServerLevel serverLevel = mc.getSingleplayerServer().getLevel(mc.level.dimension());
            if (serverLevel != null) {
                serverLevel.structureManager().getAllStructuresAt(mc.player.blockPosition()).keySet()
                        .forEach(structure -> markStructureDiscovered(scope, structureId(serverLevel, structure)));
            }
        }
    }

    public void markFoodTasted(ItemStack stack) {
        if (!AmiConfig.enableDiscoveryChecklist || stack == null || stack.isEmpty() || !Services.PLATFORM.hasFood(stack)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        markFoodTasted(currentScope(mc), BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public SearchNode decorate(SearchNode node) {
        if (!AmiConfig.enableDiscoveryChecklist || node == null || !isDiscoverable(node)) {
            return node;
        }
        if (persistenceEnabled) {
            Minecraft mc = Minecraft.getInstance();
            ensureLoaded(currentScope(mc));
        }

        String state = discoveryState(node);
        Map<String, String> metadata = new HashMap<>(node.metadata());
        metadata.put(SearchNodeKeys.DISCOVERY_STATE, state);
        metadata.put(SearchNodeKeys.DISCOVERY_SOURCE, discoverySource(node));
        return node.withMetadata(metadata);
    }

    public boolean isDiscovered(SearchNode node) {
        return STATE_DISCOVERED.equals(discoveryState(node));
    }

    private String discoveryState(SearchNode node) {
        if (node.type() == NodeType.BIOME && discoveredBiomes.contains(node.id())) {
            return STATE_DISCOVERED;
        }
        if (node.type() == NodeType.STRUCTURE && discoveredStructures.contains(node.id())) {
            return STATE_DISCOVERED;
        }
        if (node.type() == NodeType.ITEM && isFoodNode(node) && tastedFoods.contains(node.id())) {
            return STATE_DISCOVERED;
        }
        return STATE_UNDISCOVERED;
    }

    private static boolean isDiscoverable(SearchNode node) {
        return node.type() == NodeType.BIOME || node.type() == NodeType.STRUCTURE || isFoodNode(node);
    }

    private static boolean isFoodNode(SearchNode node) {
        return node != null && node.type() == NodeType.ITEM && !node.meta(SearchNodeKeys.FOOD_NUTRITION, "").isBlank();
    }

    private static String discoverySource(SearchNode node) {
        if (node.type() == NodeType.BIOME) {
            return "visited_biome";
        }
        if (node.type() == NodeType.STRUCTURE) {
            return "entered_structure";
        }
        if (isFoodNode(node)) {
            return "tasted_food";
        }
        return "untracked";
    }

    private void markBiomeDiscovered(String scope, ResourceLocation biomeId) {
        if (biomeId == null) {
            return;
        }
        ensureLoaded(scope);
        if (discoveredBiomes.add(biomeId)) {
            revision++;
            save(scope);
        }
    }

    private void markStructureDiscovered(String scope, ResourceLocation structureId) {
        if (structureId == null) {
            return;
        }
        ensureLoaded(scope);
        if (discoveredStructures.add(structureId)) {
            revision++;
            save(scope);
        }
    }

    private void markFoodTasted(String scope, ResourceLocation foodId) {
        if (foodId == null) {
            return;
        }
        ensureLoaded(scope);
        if (tastedFoods.add(foodId)) {
            revision++;
            save(scope);
        }
    }

    private void ensureLoaded(String scope) {
        String normalized = scope == null || scope.isBlank() ? "unknown" : scope;
        if (normalized.equals(loadedScope)) {
            return;
        }
        discoveredBiomes.clear();
        discoveredStructures.clear();
        tastedFoods.clear();
        loadedScope = normalized;
        load(normalized);
        revision++;
    }

    private void load(String scope) {
        if (!persistenceEnabled) {
            return;
        }
        Path file = file(scope);
        if (file == null || !Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray biomes = root.has("biomes") && root.get("biomes").isJsonArray()
                    ? root.getAsJsonArray("biomes")
                    : new JsonArray();
            for (JsonElement element : biomes) {
                ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
                if (id != null) {
                    discoveredBiomes.add(id);
                }
            }
            JsonArray structures = root.has("structures") && root.get("structures").isJsonArray()
                    ? root.getAsJsonArray("structures")
                    : new JsonArray();
            for (JsonElement element : structures) {
                ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
                if (id != null) {
                    discoveredStructures.add(id);
                }
            }
            JsonArray foods = root.has("foods") && root.get("foods").isJsonArray()
                    ? root.getAsJsonArray("foods")
                    : new JsonArray();
            for (JsonElement element : foods) {
                ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
                if (id != null) {
                    tastedFoods.add(id);
                }
            }
        } catch (IOException | RuntimeException e) {
            AmiCore.LOGGER.warn("AMI: Failed to load discovery checklist state: {}", e.getMessage());
        }
    }

    private void save(String scope) {
        if (!persistenceEnabled) {
            return;
        }
        Path file = file(scope);
        if (file == null) {
            return;
        }
        JsonObject root = new JsonObject();
        root.addProperty("version", FORMAT_VERSION);
        JsonArray biomes = new JsonArray();
        discoveredBiomes.stream()
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(biomes::add);
        root.add("biomes", biomes);
        JsonArray structures = new JsonArray();
        discoveredStructures.stream()
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(structures::add);
        root.add("structures", structures);
        JsonArray foods = new JsonArray();
        tastedFoods.stream()
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(foods::add);
        root.add("foods", foods);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            AmiCore.LOGGER.warn("AMI: Failed to save discovery checklist state: {}", e.getMessage());
        }
    }

    private static Path file(String scope) {
        try {
            Path root = testRoot != null ? testRoot : Services.PLATFORM.getConfigDir().resolve("ami").resolve("discovery");
            return root.resolve(sanitizeScope(scope) + ".json");
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static String currentScope(Minecraft mc) {
        if (mc == null) {
            return "unknown";
        }
        String world = "unknown_world";
        try {
            if (mc.getSingleplayerServer() != null && mc.getSingleplayerServer().getWorldData() != null) {
                world = mc.getSingleplayerServer().getWorldData().getLevelName();
            } else if (mc.getCurrentServer() != null && mc.getCurrentServer().ip != null) {
                world = mc.getCurrentServer().ip;
            }
        } catch (RuntimeException ignored) {
        }

        String dimension = mc.level == null ? "unknown_dimension" : mc.level.dimension().location().toString();
        UUID playerId = mc.player == null ? null : mc.player.getUUID();
        return world + "|" + dimension + "|" + (playerId == null ? "unknown_player" : playerId);
    }

    private static ResourceLocation structureId(ServerLevel level, Structure structure) {
        if (level == null || structure == null) {
            return null;
        }
        return level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure);
    }

    private static String sanitizeScope(String scope) {
        String normalized = scope == null ? "unknown" : scope.toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9._-]+", "_");
        if (normalized.isBlank()) {
            return "unknown";
        }
        return normalized.length() > 160 ? normalized.substring(0, 160) : normalized;
    }
}
