package com.sanhiruzu.ami.player;

import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

final class FtbChunksWaypointProvider implements PlayerWaypointProvider {
    private static final Logger LOGGER = Logger.getLogger(FtbChunksWaypointProvider.class.getName());
    private static final String ID = "ftbchunks";
    private static final String LABEL = "FTB Chunks";

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
        return Services.PLATFORM.isModLoaded("ftbchunks");
    }

    @Override
    public Optional<PlayerWaypointExport> waypointExport(PlayerWaypointContext context) {
        if (context == null || !hasCoordinates(context)) {
            return Optional.empty();
        }
        String payload = "name=" + waypointName(context)
                + " dimension=" + context.metadata().getOrDefault(SearchNodeKeys.PLAYER_DIMENSION, "minecraft:overworld")
                + " x=" + context.metadata().getOrDefault(SearchNodeKeys.PLAYER_X, "")
                + " y=" + context.metadata().getOrDefault(SearchNodeKeys.PLAYER_Y, "")
                + " z=" + context.metadata().getOrDefault(SearchNodeKeys.PLAYER_Z, "")
                + " provider=" + ID;
        return Optional.of(new PlayerWaypointExport(ID, LABEL, payload));
    }

    @Override
    public List<PlayerWaypointAction> waypointActions(PlayerWaypointContext context) {
        if (context == null || !hasCoordinates(context)) {
            return List.of();
        }
        return List.of(new PlayerWaypointAction(
                "ami:add_player_waypoint",
                "Add " + LABEL + " Waypoint",
                'a',
                () -> addWaypoint(context)
        ));
    }

    @Override
    public List<LiveWaypoint> liveWaypoints() {
        Object manager = waypointManager().orElse(null);
        if (manager == null) {
            return List.of();
        }
        try {
            Object value = manager.getClass().getMethod("getAllWaypoints").invoke(manager);
            if (!(value instanceof Collection<?> waypoints)) {
                return List.of();
            }
            List<LiveWaypoint> results = new ArrayList<>();
            for (Object waypoint : waypoints) {
                toLiveWaypoint(waypoint).ifPresent(results::add);
            }
            return List.copyOf(results);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Failed to enumerate FTB Chunks waypoints", e);
            return List.of();
        }
    }

    @Override
    public List<PlayerWaypointAction> liveWaypointActions(LiveWaypointContext context) {
        if (context == null || context.waypoint() == null) {
            return List.of();
        }
        List<PlayerWaypointAction> actions = new ArrayList<>();
        actions.add(new PlayerWaypointAction(
                "ami:open_ftbchunks_waypoints",
                "Open " + LABEL + " Waypoints",
                'o',
                FtbChunksWaypointProvider::openWaypointEditor
        ));
        findLiveWaypoint(context.waypoint()).ifPresent(waypoint -> actions.add(new PlayerWaypointAction(
                "ami:delete_ftbchunks_waypoint",
                "Delete " + LABEL + " Waypoint",
                'd',
                () -> deleteWaypoint(waypoint)
        )));
        return List.copyOf(actions);
    }

    @Override
    public Optional<PlayerWaypointAction> openLiveWaypointAction(LiveWaypointContext context) {
        if (context == null || context.waypoint() == null) {
            return Optional.empty();
        }
        return Optional.of(new PlayerWaypointAction(
                "ami:open_ftbchunks_waypoints",
                "Open " + LABEL + " Waypoints",
                'o',
                FtbChunksWaypointProvider::openWaypointEditor
        ));
    }

    private static void addWaypoint(PlayerWaypointContext context) {
        try {
            Object manager = waypointManager().orElse(null);
            if (manager == null) return;
            Method addWaypointAt = manager.getClass().getMethod("addWaypointAt", BlockPos.class, String.class);
            addWaypointAt.invoke(manager, blockPos(context), waypointName(context));
            requestMinimapRefresh();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.WARNING, "AMI: Failed to add FTB Chunks waypoint for " + context.playerName(), e);
        }
    }

    private static Optional<Object> waypointManager() {
        try {
            Object clientApi = clientApi();
            if (clientApi == null) return Optional.empty();
            Object maybeManager = clientApi.getClass().getMethod("getWaypointManager").invoke(clientApi);
            if (maybeManager instanceof Optional<?> optional && optional.isPresent()) {
                return Optional.of(optional.get());
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: FTB Chunks waypoint manager unavailable", e);
        }
        return Optional.empty();
    }

    private static Object clientApi() throws ReflectiveOperationException {
        return Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI")
                .getMethod("clientApi")
                .invoke(null);
    }

    private static Optional<LiveWaypoint> toLiveWaypoint(Object waypoint) {
        try {
            String name = stringValue(invoke(waypoint, "getName"), "Waypoint");
            BlockPos pos = (BlockPos) invoke(waypoint, "getPos");
            String dimension = dimensionName(invoke(waypoint, "getDimension"));
            int color = intValue(invoke(waypoint, "getColor"), 0);
            boolean hidden = booleanValue(invoke(waypoint, "isHidden"), false);
            boolean deathpoint = booleanValue(invoke(waypoint, "isDeathpoint"), false);
            boolean transientWaypoint = booleanValue(invoke(waypoint, "isTransient"), false);
            Map<String, String> meta = new HashMap<>();
            meta.put(SearchNodeKeys.WAYPOINT_COLOR, Integer.toString(color));
            meta.put(SearchNodeKeys.WAYPOINT_VISIBLE, Boolean.toString(!hidden));
            meta.put("waypointDeathpoint", Boolean.toString(deathpoint));
            meta.put("waypointTransient", Boolean.toString(transientWaypoint));
            return Optional.of(new LiveWaypoint(ID, LABEL, liveId(dimension, pos, name), name, dimension,
                    pos.getX(), pos.getY(), pos.getZ(), meta));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Failed to adapt FTB Chunks waypoint", e);
            return Optional.empty();
        }
    }

    private static Optional<Object> findLiveWaypoint(LiveWaypoint target) {
        Object manager = waypointManager().orElse(null);
        if (manager == null) return Optional.empty();
        try {
            Object value = manager.getClass().getMethod("getAllWaypoints").invoke(manager);
            if (!(value instanceof Collection<?> waypoints)) return Optional.empty();
            for (Object waypoint : waypoints) {
                Optional<LiveWaypoint> adapted = toLiveWaypoint(waypoint);
                if (adapted.isPresent() && adapted.get().id().equals(target.id())) {
                    return Optional.of(waypoint);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Failed to find FTB Chunks waypoint " + target.id(), e);
        }
        return Optional.empty();
    }

    private static void deleteWaypoint(Object waypoint) {
        Object manager = waypointManager().orElse(null);
        if (manager == null) return;
        try {
            Class<?> waypointClass = Class.forName("dev.ftb.mods.ftbchunks.api.client.waypoint.Waypoint");
            manager.getClass().getMethod("removeWaypoint", waypointClass).invoke(manager, waypoint);
            requestMinimapRefresh();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.WARNING, "AMI: Failed to delete FTB Chunks waypoint", e);
        }
    }

    private static void openWaypointEditor() {
        try {
            Object screen = Class.forName("dev.ftb.mods.ftbchunks.client.gui.WaypointEditorScreen")
                    .getConstructor()
                    .newInstance();
            if (screen instanceof Screen minecraftScreen) {
                Minecraft.getInstance().setScreen(minecraftScreen);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Failed to open FTB Chunks waypoint editor", e);
        }
    }

    private static void requestMinimapRefresh() {
        try {
            Object clientApi = clientApi();
            if (clientApi != null) clientApi.getClass().getMethod("requestMinimapIconRefresh").invoke(clientApi);
        } catch (ReflectiveOperationException ignored) {
            // Older/newer FTB Chunks builds may persist without an explicit refresh hook.
        }
    }

    private static BlockPos blockPos(PlayerWaypointContext context) {
        return new BlockPos(
                parseCoordinate(context, SearchNodeKeys.PLAYER_X),
                parseCoordinate(context, SearchNodeKeys.PLAYER_Y),
                parseCoordinate(context, SearchNodeKeys.PLAYER_Z)
        );
    }

    private static int parseCoordinate(PlayerWaypointContext context, String key) {
        try {
            return (int) Math.floor(Double.parseDouble(context.metadata().getOrDefault(key, "0")));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean hasCoordinates(PlayerWaypointContext context) {
        return !context.metadata().getOrDefault(SearchNodeKeys.PLAYER_X, "").isBlank()
                && !context.metadata().getOrDefault(SearchNodeKeys.PLAYER_Y, "").isBlank()
                && !context.metadata().getOrDefault(SearchNodeKeys.PLAYER_Z, "").isBlank();
    }

    private static Object invoke(Object target, String method) throws ReflectiveOperationException {
        return target.getClass().getMethod(method).invoke(target);
    }

    private static String dimensionName(Object dimensionKey) {
        try {
            Object location = dimensionKey.getClass().getMethod("location").invoke(dimensionKey);
            return String.valueOf(location);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return String.valueOf(dimensionKey);
        }
    }

    private static String stringValue(Object value, String fallback) {
        String text = value == null ? "" : String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }

    private static int intValue(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static boolean booleanValue(Object value, boolean fallback) {
        return value instanceof Boolean bool ? bool : fallback;
    }

    private static String liveId(String dimension, BlockPos pos, String name) {
        return dimension + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ() + ":" + name;
    }

    private static String waypointName(PlayerWaypointContext context) {
        String playerName = context == null ? "" : context.playerName();
        if (playerName == null || playerName.isBlank()) {
            return "AMI Player";
        }
        return playerName + " (AMI)";
    }
}
