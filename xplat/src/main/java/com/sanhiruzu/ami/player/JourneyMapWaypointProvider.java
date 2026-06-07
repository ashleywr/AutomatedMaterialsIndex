package com.sanhiruzu.ami.player;

import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

final class JourneyMapWaypointProvider implements PlayerWaypointProvider {
    private static final Logger LOGGER = Logger.getLogger(JourneyMapWaypointProvider.class.getName());
    private static final String ID = "journeymap";
    private static final String LABEL = "JourneyMap";

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
        return Services.PLATFORM.isModLoaded("journeymap");
    }

    @Override
    public Optional<PlayerWaypointExport> waypointExport(PlayerWaypointContext context) {
        if (context == null || !hasCoordinates(context)) {
            return Optional.empty();
        }
        String payload = "name=" + waypointName(context)
                + " dimension=" + dimension(context)
                + " x=" + context.metadata().getOrDefault(SearchNodeKeys.PLAYER_X, "")
                + " y=" + context.metadata().getOrDefault(SearchNodeKeys.PLAYER_Y, "")
                + " z=" + context.metadata().getOrDefault(SearchNodeKeys.PLAYER_Z, "")
                + " provider=" + ID;
        return Optional.of(new PlayerWaypointExport(ID, LABEL, payload));
    }

    @Override
    public List<PlayerWaypointAction> waypointActions(PlayerWaypointContext context) {
        if (context == null || !hasCoordinates(context) || !hasNativeWaypointApi()) {
            return List.of();
        }
        return List.of(new PlayerWaypointAction(
                "ami:add_player_waypoint",
                "Add " + LABEL + " Waypoint",
                'j',
                () -> addWaypoint(context)
        ));
    }

    @Override
    public List<LiveWaypoint> liveWaypoints() {
        Object store = waypointStore().orElse(null);
        if (store == null) return List.of();
        try {
            Object value = store.getClass().getMethod("getAll").invoke(store);
            if (!(value instanceof Collection<?> waypoints)) return List.of();
            List<LiveWaypoint> results = new ArrayList<>();
            for (Object waypoint : waypoints) {
                toLiveWaypoint(waypoint).ifPresent(results::add);
            }
            return List.copyOf(results);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Failed to enumerate JourneyMap waypoints", e);
            return List.of();
        }
    }

    @Override
    public List<PlayerWaypointAction> liveWaypointActions(LiveWaypointContext context) {
        if (context == null || context.waypoint() == null) return List.of();
        List<PlayerWaypointAction> actions = new ArrayList<>();
        actions.add(new PlayerWaypointAction(
                "ami:open_journeymap_waypoint",
                "Open " + LABEL + " Waypoint",
                'o',
                () -> openWaypoint(context.waypoint())
        ));
        findLiveWaypoint(context.waypoint()).ifPresent(waypoint -> actions.add(new PlayerWaypointAction(
                "ami:delete_journeymap_waypoint",
                "Delete " + LABEL + " Waypoint",
                'd',
                () -> deleteWaypoint(waypoint)
        )));
        return List.copyOf(actions);
    }

    @Override
    public Optional<PlayerWaypointAction> openLiveWaypointAction(LiveWaypointContext context) {
        if (context == null || context.waypoint() == null) return Optional.empty();
        return Optional.of(new PlayerWaypointAction(
                "ami:open_journeymap_waypoint",
                "Open " + LABEL + " Waypoint",
                'o',
                () -> openWaypoint(context.waypoint())
        ));
    }

    private static void addWaypoint(PlayerWaypointContext context) {
        try {
            Class<?> factoryClass = Class.forName("journeymap.api.client.waypoint.ClientWaypointFactoryImpl");
            Method createWaypoint = factoryClass.getMethod("createWaypoint",
                    String.class, BlockPos.class, String.class, String.class, boolean.class, boolean.class);
            Object waypoint = createWaypoint.invoke(null,
                    waypointName(context),
                    blockPos(context),
                    dimension(context),
                    "AMI",
                    true,
                    true);
            if (waypoint == null) {
                return;
            }

            Class<?> storeClass = Class.forName("journeymap.common.waypoint.WaypointStore");
            Object store = storeClass.getMethod("getInstance").invoke(null);
            if (store == null) {
                return;
            }
            Object selfScope = Class.forName("journeymap.common.waypoint.WaypointScope")
                    .getMethod("self")
                    .invoke(null);
            Class<?> clientWaypointClass = Class.forName("journeymap.client.waypoint.ClientWaypointImpl");
            Class<?> waypointScopeClass = Class.forName("journeymap.common.waypoint.WaypointScope");
            storeClass.getMethod("save", clientWaypointClass, boolean.class, waypointScopeClass)
                    .invoke(store, waypoint, false, selfScope);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.WARNING, "AMI: Failed to add JourneyMap waypoint for " + context.playerName(), e);
        }
    }

    private static boolean hasNativeWaypointApi() {
        try {
            Class.forName("journeymap.api.client.waypoint.ClientWaypointFactoryImpl");
            Class.forName("journeymap.common.waypoint.WaypointStore");
            Class.forName("journeymap.common.waypoint.WaypointScope");
            Class.forName("journeymap.client.waypoint.ClientWaypointImpl");
            return true;
        } catch (RuntimeException | LinkageError | ClassNotFoundException e) {
            LOGGER.log(Level.FINE, "AMI: JourneyMap native waypoint API unavailable", e);
            return false;
        }
    }

    private static Optional<Object> waypointStore() {
        try {
            return Optional.ofNullable(Class.forName("journeymap.common.waypoint.WaypointStore")
                    .getMethod("getInstance")
                    .invoke(null));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: JourneyMap waypoint store unavailable", e);
            return Optional.empty();
        }
    }

    private static Optional<LiveWaypoint> toLiveWaypoint(Object waypoint) {
        try {
            String id = stringValue(invokeFirst(waypoint, "getUuid", "getGuid", "getId"), "");
            String name = stringValue(invokeFirst(waypoint, "getDisplayName", "getName", "getPrettyName"), "Waypoint");
            int x = intValue(invokeFirst(waypoint, "getX"), 0);
            int y = intValue(invokeFirst(waypoint, "getY"), 0);
            int z = intValue(invokeFirst(waypoint, "getZ"), 0);
            String dimension = stringValue(invokeFirst(waypoint, "getPrimaryDimension"), "minecraft:overworld");
            int color = intValue(invokeFirst(waypoint, "getColor", "getIconColor"), 0);
            boolean visible = booleanValue(invokeFirst(waypoint, "isEnabled", "showOnMap"), true);
            Map<String, String> meta = new HashMap<>();
            meta.put(SearchNodeKeys.WAYPOINT_COLOR, Integer.toString(color));
            meta.put(SearchNodeKeys.WAYPOINT_VISIBLE, Boolean.toString(visible));
            if (id.isBlank()) {
                id = dimension + ":" + x + ":" + y + ":" + z + ":" + name;
            }
            return Optional.of(new LiveWaypoint(ID, LABEL, id, name, dimension, x, y, z, meta));
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Failed to adapt JourneyMap waypoint", e);
            return Optional.empty();
        }
    }

    private static Optional<Object> findLiveWaypoint(LiveWaypoint target) {
        Object store = waypointStore().orElse(null);
        if (store == null) return Optional.empty();
        try {
            try {
                Object waypoint = store.getClass().getMethod("get", String.class).invoke(store, target.id());
                if (waypoint != null) return Optional.of(waypoint);
            } catch (ReflectiveOperationException ignored) {
            }
            Object value = store.getClass().getMethod("getAll").invoke(store);
            if (!(value instanceof Collection<?> waypoints)) return Optional.empty();
            for (Object waypoint : waypoints) {
                Optional<LiveWaypoint> adapted = toLiveWaypoint(waypoint);
                if (adapted.isPresent() && adapted.get().id().equals(target.id())) {
                    return Optional.of(waypoint);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Failed to find JourneyMap waypoint " + target.id(), e);
        }
        return Optional.empty();
    }

    private static void openWaypoint(LiveWaypoint target) {
        findLiveWaypoint(target).ifPresentOrElse(waypoint -> {
            try {
                Class<?> waypointClass = Class.forName("journeymap.client.waypoint.ClientWaypointImpl");
                Object screen = Class.forName("journeymap.client.ui.waypointmanager.WaypointManager")
                        .getConstructor(waypointClass, Screen.class)
                        .newInstance(waypoint, Minecraft.getInstance().screen);
                if (screen instanceof Screen minecraftScreen) {
                    Minecraft.getInstance().setScreen(minecraftScreen);
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                LOGGER.log(Level.FINE, "AMI: Failed to open JourneyMap waypoint " + target.id(), e);
            }
        }, JourneyMapWaypointProvider::openWaypointManager);
    }

    private static void openWaypointManager() {
        try {
            Object screen = Class.forName("journeymap.client.ui.waypointmanager.WaypointManager")
                    .getConstructor(Screen.class)
                    .newInstance(Minecraft.getInstance().screen);
            if (screen instanceof Screen minecraftScreen) {
                Minecraft.getInstance().setScreen(minecraftScreen);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Failed to open JourneyMap waypoint manager", e);
        }
    }

    private static void deleteWaypoint(Object waypoint) {
        Object store = waypointStore().orElse(null);
        if (store == null) return;
        try {
            Class<?> waypointClass = Class.forName("journeymap.client.waypoint.ClientWaypointImpl");
            Object selfScope = Class.forName("journeymap.common.waypoint.WaypointScope")
                    .getMethod("self")
                    .invoke(null);
            Class<?> waypointScopeClass = Class.forName("journeymap.common.waypoint.WaypointScope");
            store.getClass().getMethod("remove", waypointClass, boolean.class, waypointScopeClass)
                    .invoke(store, waypoint, false, selfScope);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.WARNING, "AMI: Failed to delete JourneyMap waypoint", e);
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

    private static String dimension(PlayerWaypointContext context) {
        return context.metadata().getOrDefault(SearchNodeKeys.PLAYER_DIMENSION, "minecraft:overworld");
    }

    private static String waypointName(PlayerWaypointContext context) {
        String playerName = context == null ? "" : context.playerName();
        if (playerName == null || playerName.isBlank()) {
            return "AMI Player";
        }
        return playerName + " (AMI)";
    }

    private static Object invokeFirst(Object target, String... methods) {
        for (String method : methods) {
            try {
                return target.getClass().getMethod(method).invoke(target);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
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
