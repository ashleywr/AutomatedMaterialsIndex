package com.sanhiruzu.ami.player;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listens to JourneyMap waypoint events to keep AMI's waypoint index in sync.
 * When waypoints are added/edited/deleted in JourneyMap directly (not through AMI),
 * this listener invalidates the cached waypoint list immediately so AMI sees the
 * changes on the next search/display refresh instead of waiting 5 seconds.
 */
final class JourneyMapEventListener {
    private static final Logger LOGGER = Logger.getLogger(JourneyMapEventListener.class.getName());
    private static boolean registered = false;

    private JourneyMapEventListener() {
    }

    /**
     * Attempt to register for JourneyMap waypoint change events.
     * Uses reflection to avoid hard dependency on JourneyMap API structure.
     */
    static void registerIfAvailable() {
        if (registered) return;
        registered = true;

        try {
            Object eventBus = getJourneyMapEventBus();
            if (eventBus == null) {
                LOGGER.log(Level.FINE, "AMI: JourneyMap EventBus not available");
                return;
            }

            registerWaypointListener(eventBus);
            LOGGER.log(Level.FINE, "AMI: Registered JourneyMap waypoint event listener");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Failed to register JourneyMap event listener", e);
        }
    }

    private static Object getJourneyMapEventBus() throws ReflectiveOperationException {
        try {
            Class<?> servicesClass = Class.forName("journeymap.api.services.Services");
            Method getService = servicesClass.getMethod("getService", Class.class);
            Class<?> eventBusClass = Class.forName("journeymap.api.services.EventBus");
            return getService.invoke(null, eventBusClass);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static void registerWaypointListener(Object eventBus) throws ReflectiveOperationException {
        try {
            Method registerMethod = eventBus.getClass().getMethod("register", Object.class);
            Object listener = new WaypointChangeListener();
            registerMethod.invoke(eventBus, listener);
        } catch (ReflectiveOperationException e) {
            LOGGER.log(Level.FINE, "AMI: Could not register with JourneyMap EventBus", e);
        }
    }

    /**
     * Inner class that listens for waypoint events and invalidates the cache.
     * JourneyMap's EventBus uses reflection to call methods matching event signatures.
     */
    private static class WaypointChangeListener {
        @SuppressWarnings("unused")
        public void onWaypointUpdate(Object event) {
            PlayerWaypointProviders.invalidateLiveWaypointCache();
        }

        @SuppressWarnings("unused")
        public void onWaypointAdd(Object event) {
            PlayerWaypointProviders.invalidateLiveWaypointCache();
        }

        @SuppressWarnings("unused")
        public void onWaypointRemove(Object event) {
            PlayerWaypointProviders.invalidateLiveWaypointCache();
        }

        @SuppressWarnings("unused")
        public void onWaypointChange(Object event) {
            PlayerWaypointProviders.invalidateLiveWaypointCache();
        }
    }
}
