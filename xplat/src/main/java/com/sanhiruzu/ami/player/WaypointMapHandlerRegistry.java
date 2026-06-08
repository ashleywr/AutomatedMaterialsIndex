package com.sanhiruzu.ami.player;

import com.sanhiruzu.ami.config.AmiConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

final class WaypointMapHandlerRegistry {
    private static final Logger LOGGER = Logger.getLogger(WaypointMapHandlerRegistry.class.getName());
    private static final String AUTO = "auto";
    private static final String WAYSTONES_HANDLER = "waystones";
    private static final String JOURNEY_MAP_HANDLER = "journeymap";
    private static final String FTB_CHUNKS_HANDLER = "ftbchunks";
    private static final String XAERO_HANDLER = "xaero";
    private static final List<String> KNOWN_MAP_HANDLERS = List.of(
            WAYSTONES_HANDLER,
            JOURNEY_MAP_HANDLER,
            FTB_CHUNKS_HANDLER,
            XAERO_HANDLER
    );

    private WaypointMapHandlerRegistry() {
    }

    public static Optional<PlayerWaypointAction> defaultWaypointOpenAction(LiveWaypointContext context) {
        String configured = normalizeHandlerId(AmiConfig.defaultWaypointMapHandler);
        return resolveOpenAction(context, candidateHandlers(context, configured));
    }

    public static List<PlayerWaypointAction> waypointMapMenuActions(LiveWaypointContext context) {
        return waypointMapMenuActions(context, false);
    }

    public static List<PlayerWaypointAction> waypointMapMenuActions(LiveWaypointContext context, boolean includeDefaultAction) {
        String defaultActionId = defaultWaypointOpenAction(context).map(PlayerWaypointAction::id).orElse("");
        List<PlayerWaypointAction> actions = new ArrayList<>();
        for (String handlerId : KNOWN_MAP_HANDLERS) {
            openActionFor(handlerId, context).ifPresent(action -> {
                if (includeDefaultAction || !action.id().equals(defaultActionId)) {
                    actions.add(action);
                }
            });
        }
        return List.copyOf(actions);
    }

    private static Optional<PlayerWaypointAction> resolveOpenAction(
            LiveWaypointContext context,
            List<String> candidates
    ) {
        for (String handlerId : candidates) {
            Optional<PlayerWaypointAction> action = openActionFor(handlerId, context);
            if (action.isPresent()) {
                return action;
            }
        }
        return Optional.empty();
    }

    private static List<String> candidateHandlers(LiveWaypointContext context, String configuredHandler) {
        List<String> candidates = new ArrayList<>();
        if (AUTO.equals(configuredHandler)) {
            String providerId = normalizeHandlerId(context != null && context.waypoint() != null
                    ? context.waypoint().providerId()
                    : "");
            if (!providerId.isBlank() && !isUnknownHandler(providerId)) {
                candidates.add(providerId);
            }
        } else if (!isUnknownHandler(configuredHandler)) {
            candidates.add(configuredHandler);
        }
        for (String candidate : KNOWN_MAP_HANDLERS) {
            if (!candidates.contains(candidate)) {
                candidates.add(candidate);
            }
        }
        return candidates;
    }

    private static Optional<PlayerWaypointAction> openActionFor(String handlerId, LiveWaypointContext context) {
        String normalized = normalizeHandlerId(handlerId);
        if (isUnknownHandler(normalized) || context == null || context.waypoint() == null) {
            return Optional.empty();
        }
        PlayerWaypointProvider handler = findWaypointProvider(normalized);
        if (handler == null || !handlerAvailable(handler)) {
            return Optional.empty();
        }
        if (WAYSTONES_HANDLER.equals(normalized)) {
            return WaystonesWaypointProvider.openMapHandlerAction()
                    .filter(PlayerWaypointAction::isUsable)
                    .map(action -> safeAction(handler, action));
        }
        try {
            return handler.openLiveWaypointAction(context)
                    .filter(PlayerWaypointAction::isUsable)
                    .map(action -> safeAction(handler, action));
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Failed to resolve map waypoint action for handler " + normalized, e);
            return Optional.empty();
        }
    }

    private static PlayerWaypointAction safeAction(PlayerWaypointProvider provider, PlayerWaypointAction action) {
        return new PlayerWaypointAction(action.id(), action.label(), action.mnemonic(), () -> {
            try {
                action.action().run();
            } catch (RuntimeException | LinkageError e) {
                LOGGER.log(Level.WARNING, "AMI: Map waypoint action failed for handler " + provider.id(), e);
            }
        });
    }

    private static boolean isUnknownHandler(String handlerId) {
        return handlerId == null || handlerId.isBlank() || !KNOWN_MAP_HANDLERS.contains(handlerId);
    }

    private static PlayerWaypointProvider findWaypointProvider(String handlerId) {
        for (PlayerWaypointProvider provider : PlayerWaypointProviders.providers()) {
            if (handlerId.equals(provider.id())) {
                return provider;
            }
        }
        return null;
    }

    private static boolean handlerAvailable(PlayerWaypointProvider provider) {
        if (provider == null) {
            return false;
        }
        try {
            return provider.isAvailable();
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Map waypoint handler availability check failed: " + provider.id(), e);
            return false;
        }
    }

    private static String normalizeHandlerId(String handlerId) {
        if (handlerId == null) {
            return AUTO;
        }
        String normalized = handlerId.strip().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? AUTO : normalized;
    }
}
