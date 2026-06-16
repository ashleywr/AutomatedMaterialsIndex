package com.sanhiruzu.ami.player;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

final class WaystonesWaypointProvider implements PlayerWaypointProvider {
    private static final Logger LOGGER = Logger.getLogger(WaystonesWaypointProvider.class.getName());
    private static final String OPEN_WAYSTONES_GUI_ACTION_ID = "ami:open_waystones_gui";
    private static final String ID = "waystones";
    private static final String LABEL = "Waystones";
    private static final String WAYSTONES_API_CLASS = "net.blay09.mods.waystones.api.WaystonesAPI";
    private static final String WAYSTONE_MANAGER_CLASS = "net.blay09.mods.waystones.core.PlayerWaystoneManager";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String label() {
        return LABEL;
    }

    @Override
    public boolean isAvailable() {
        return Services.PLATFORM.isModLoaded("waystones");
    }

    @Override
    public List<LiveWaypoint> liveWaypoints() {
        if (!isAvailable()) {
            return List.of();
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return List.of();
        }

        Collection<?> waystones = apiWaystones(player).orElse(null);
        if (waystones == null || waystones.isEmpty()) {
            return List.of();
        }

        List<LiveWaypoint> results = new ArrayList<>();
        for (Object waystone : waystones) {
            toLiveWaypoint(waystone).ifPresent(results::add);
        }
        return List.copyOf(results);
    }

    @Override
    public Optional<PlayerWaypointAction> openLiveWaypointAction(LiveWaypointContext context) {
        if (AmiConfig.waystonesOpenScreenFromAnywhere) {
            return openMapHandlerAction();
        }

        Optional<PlayerWaypointAction> defaultAction = WaypointMapHandlerRegistry.defaultWaypointOpenAction(context);
        if (defaultAction.isPresent() && !OPEN_WAYSTONES_GUI_ACTION_ID.equals(defaultAction.get().id())) {
            return defaultAction;
        }

        Optional<PlayerWaypointAction> fallbackMapAction = WaypointMapHandlerRegistry
                .waypointMapMenuActions(context, false)
                .stream()
                .filter(action -> !OPEN_WAYSTONES_GUI_ACTION_ID.equals(action.id()))
                .findFirst();
        if (fallbackMapAction.isPresent()) {
            return fallbackMapAction;
        }

        return Optional.of(noCompatibleMapModMessageAction());
    }

    @Override
    public List<PlayerWaypointAction> liveWaypointActions(LiveWaypointContext context) {
        return WaypointMapHandlerRegistry.waypointMapMenuActions(context, true);
    }

    static Optional<PlayerWaypointAction> openMapHandlerAction() {
        return Optional.of(new PlayerWaypointAction(
                "ami:open_waystones_gui",
                "Open Waystones GUI",
                'o',
                WaystonesWaypointProvider::openWaystonesGui
        ));
    }

    private static PlayerWaypointAction noCompatibleMapModMessageAction() {
        return new PlayerWaypointAction(
                "ami:message.no_compatible_waypoint_map_mod",
                Component.translatable("ami.waystones.no-compatible-map-mod").getString(),
                'i',
                WaystonesWaypointProvider::showNoCompatibleMapModMessage
        );
    }

    private static Optional<Collection<?>> apiWaystones(Player player) {
        return apiWaystonesFromPlayerData(player)
                .or(() -> apiWaystonesFromActivated(player));
    }

    private static Optional<Collection<?>> apiWaystonesFromPlayerData(Player player) {
        try {
            Class<?> managerClass = Class.forName(WAYSTONE_MANAGER_CLASS);
            Object data = managerClass
                    .getMethod("getPlayerWaystoneData", net.minecraft.world.level.Level.class)
                    .invoke(null, player.level());
            if (data == null) {
                return Optional.empty();
            }
            Object waystones = data.getClass()
                    .getMethod("getWaystones", Player.class)
                    .invoke(data, player);
            return waystones instanceof Collection<?> waystoneList ? Optional.of(waystoneList) : Optional.empty();
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.log(Level.FINE, "AMI: Unable to read player waystones for sync, falling back to activated list", e);
            return Optional.empty();
        }
    }

    private static Optional<Collection<?>> apiWaystonesFromActivated(Player player) {
        try {
            Class<?> apiClass = Class.forName(WAYSTONES_API_CLASS);
            Object waystones = apiClass
                    .getMethod("getActivatedWaystones", Player.class)
                    .invoke(null, player);
            return waystones instanceof Collection<?> waystoneList ? Optional.of(waystoneList) : Optional.empty();
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.log(Level.FINE, "AMI: Unable to read activated Waystones for runtime waypoints", e);
            return Optional.empty();
        }
    }

    private static Optional<LiveWaypoint> toLiveWaypoint(Object waystone) {
        if (waystone == null) {
            return Optional.empty();
        }

        String id = stringValue(invoke(waystone, "getWaystoneUid"), null);
        if (id == null || id.isBlank()) {
            id = safeIdForUnknown();
        }

        String name = componentToPlainText(invoke(waystone, "getName"));
        BlockPos pos = position(waystone);
        if (pos == null) {
            return Optional.empty();
        }
        String dimension = dimensionName(invoke(waystone, "getDimension"));
        String ownerUid = stringValue(invoke(waystone, "getOwnerUid"), "unknown");

        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.ONTOLOGY_CATEGORY, AmiOntology.ENVIRONMENT.id);
        meta.put(SearchNodeKeys.WAYPOINT_OWNER, ownerUid);
        meta.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "waypoints");
        meta.put("waystonesWaystoneType", stringValue(invoke(waystone, "getWaystoneType"), "unknown"));

        return Optional.of(new LiveWaypoint(ID, LABEL, id, name, dimension, pos.getX(), pos.getY(), pos.getZ(), meta));
    }

    private static String safeIdForUnknown() {
        return UUID.randomUUID().toString();
    }

    private static void openWaystonesGui() {
        try {
            var player = Minecraft.getInstance().player;
            if (player == null || player.connection == null) {
                return;
            }
            player.connection.sendCommand("waystones gui @s");
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "AMI: Failed to open waystones GUI", e);
        }
    }

    private static void showNoCompatibleMapModMessage() {
        try {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.sendSystemMessage(Component.translatable("ami.waystones.no-compatible-map-mod"));
            }
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "AMI: Failed to show waypoints fallback message", e);
        }
    }

    private static BlockPos position(Object waystone) {
        Object pos = invoke(waystone, "getPos");
        return pos instanceof BlockPos blockPos ? blockPos : null;
    }

    private static String dimensionName(Object dimensionKey) {
        Object location = invoke(dimensionKey, "location");
        return location == null ? "minecraft:overworld" : String.valueOf(location);
    }

    private static String componentToPlainText(Object value) {
        return value instanceof Component component ? component.getString() : String.valueOf(value);
    }

    private static String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }

    private static Object invoke(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.log(Level.FINE, "AMI: Waystones reflection call failed: " + methodName, e);
            return null;
        }
    }
}
