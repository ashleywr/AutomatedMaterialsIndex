package com.sanhiruzu.ami.player;

import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

final class DetectedMapWaypointProvider implements PlayerWaypointProvider {
    private static final Logger LOGGER = Logger.getLogger(DetectedMapWaypointProvider.class.getName());
    private final String id;
    private final String label;
    private final List<String> modIds;

    DetectedMapWaypointProvider(String id, String label, List<String> modIds) {
        this.id = id == null ? "" : id;
        this.label = label == null ? this.id : label;
        this.modIds = modIds == null ? List.of() : List.copyOf(modIds);
    }

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
        for (String modId : modIds) {
            if (Services.PLATFORM.isModLoaded(modId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Optional<PlayerWaypointExport> waypointExport(PlayerWaypointContext context) {
        if (context == null || !hasCoordinates(context)) {
            return Optional.empty();
        }
        String payload = "name=" + context.playerName()
                + " dimension=" + context.metadata().getOrDefault(com.sanhiruzu.ami.index.SearchNodeKeys.PLAYER_DIMENSION, "minecraft:overworld")
                + " x=" + context.metadata().getOrDefault(com.sanhiruzu.ami.index.SearchNodeKeys.PLAYER_X, "")
                + " y=" + context.metadata().getOrDefault(com.sanhiruzu.ami.index.SearchNodeKeys.PLAYER_Y, "")
                + " z=" + context.metadata().getOrDefault(com.sanhiruzu.ami.index.SearchNodeKeys.PLAYER_Z, "")
                + " provider=" + id;
        return Optional.of(new PlayerWaypointExport(id, label, payload));
    }

    @Override
    public List<LiveWaypoint> liveWaypoints() {
        if (!"xaero".equals(id)) {
            return List.of();
        }
        Object manager = xaeroWaypointManager().orElse(null);
        if (manager == null) return List.of();
        List<LiveWaypoint> results = new ArrayList<>();
        Object currentWorld = invoke(manager, "getCurrentWorld");
        collectXaeroWorld(results, currentWorld, "current");
        Object currentSet = invoke(manager, "getWaypoints");
        collectXaeroSet(results, currentSet, "current", "minecraft:overworld");
        Object serverWaypoints = invoke(manager, "getServerWaypoints");
        if (serverWaypoints instanceof Collection<?> waypoints) {
            for (Object waypoint : waypoints) {
                toXaeroLiveWaypoint(waypoint, "server", "minecraft:overworld").ifPresent(results::add);
            }
        }
        return List.copyOf(new java.util.LinkedHashSet<>(results));
    }

    @Override
    public List<PlayerWaypointAction> liveWaypointActions(LiveWaypointContext context) {
        if (!"xaero".equals(id) || context == null || context.waypoint() == null) {
            return List.of();
        }
        return List.of(new PlayerWaypointAction(
                "ami:open_xaero_waypoints",
                "Open " + label + " Waypoints",
                'o',
                DetectedMapWaypointProvider::openXaeroWaypoints
        ));
    }

    @Override
    public Optional<PlayerWaypointAction> openLiveWaypointAction(LiveWaypointContext context) {
        if (!"xaero".equals(id)) {
            return Optional.empty();
        }
        return Optional.of(new PlayerWaypointAction(
                "ami:open_xaero_waypoints",
                "Open " + label + " Waypoints",
                'o',
                DetectedMapWaypointProvider::openXaeroWaypoints
        ));
    }

    private static boolean hasCoordinates(PlayerWaypointContext context) {
        return !context.metadata().getOrDefault(com.sanhiruzu.ami.index.SearchNodeKeys.PLAYER_X, "").isBlank()
                && !context.metadata().getOrDefault(com.sanhiruzu.ami.index.SearchNodeKeys.PLAYER_Y, "").isBlank()
                && !context.metadata().getOrDefault(com.sanhiruzu.ami.index.SearchNodeKeys.PLAYER_Z, "").isBlank();
    }

    private static Optional<Object> xaeroWaypointManager() {
        try {
            Object session = Class.forName("xaero.common.XaeroMinimapSession")
                    .getMethod("getCurrentSession")
                    .invoke(null);
            if (session == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(session.getClass().getMethod("getWaypointsManager").invoke(session));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Xaero waypoint manager unavailable", e);
            return Optional.empty();
        }
    }

    private static void collectXaeroWorld(List<LiveWaypoint> results, Object world, String source) {
        if (world == null) return;
        String dimension = dimensionName(invoke(world, "getDimId"));
        Object currentSet = invoke(world, "getCurrentSet");
        collectXaeroSet(results, currentSet, source, dimension);
    }

    private static void collectXaeroSet(List<LiveWaypoint> results, Object set, String source, String dimension) {
        Object list = invoke(set, "getList");
        if (!(list instanceof Collection<?> waypoints)) return;
        for (Object waypoint : waypoints) {
            toXaeroLiveWaypoint(waypoint, source, dimension).ifPresent(results::add);
        }
    }

    private static Optional<LiveWaypoint> toXaeroLiveWaypoint(Object waypoint, String source, String dimension) {
        try {
            String name = stringValue(invoke(waypoint, "getName"), "Waypoint");
            int x = intValue(invoke(waypoint, "getX"), 0);
            int y = intValue(invoke(waypoint, "getY"), 0);
            int z = intValue(invoke(waypoint, "getZ"), 0);
            int color = intValue(invoke(waypoint, "getColor"), 0);
            boolean visible = !booleanValue(invoke(waypoint, "isDisabled"), false);
            Map<String, String> meta = new HashMap<>();
            meta.put(com.sanhiruzu.ami.index.SearchNodeKeys.WAYPOINT_COLOR, Integer.toString(color));
            meta.put(com.sanhiruzu.ami.index.SearchNodeKeys.WAYPOINT_VISIBLE, Boolean.toString(visible));
            meta.put("waypointSource", source);
            return Optional.of(new LiveWaypoint("xaero", "Xaero",
                    source + ":" + dimension + ":" + x + ":" + y + ":" + z + ":" + name,
                    name, dimension, x, y, z, meta));
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Failed to adapt Xaero waypoint", e);
            return Optional.empty();
        }
    }

    private static void openXaeroWaypoints() {
        try {
            Object session = Class.forName("xaero.common.XaeroMinimapSession")
                    .getMethod("getCurrentSession")
                    .invoke(null);
            if (session == null) return;
            Object mod = Class.forName("xaero.common.HudMod").getField("INSTANCE").get(null);
            if (mod == null) {
                Object maybeMod = invoke(session, "getModMain");
                if (maybeMod != null) mod = maybeMod;
            }
            if (mod == null) return;
            Class<?> modClass = Class.forName("xaero.common.IXaeroMinimap");
            Class<?> sessionClass = Class.forName("xaero.common.XaeroMinimapSession");
            Object screen = Class.forName("xaero.common.gui.GuiWaypoints")
                    .getConstructor(modClass, sessionClass, Screen.class)
                    .newInstance(mod, session, Minecraft.getInstance().screen);
            if (screen instanceof Screen minecraftScreen) {
                Minecraft.getInstance().setScreen(minecraftScreen);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Failed to open Xaero waypoints", e);
        }
    }

    private static Object invoke(Object target, String method) {
        if (target == null) return null;
        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static String dimensionName(Object dimensionKey) {
        if (dimensionKey == null) return "minecraft:overworld";
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
}
