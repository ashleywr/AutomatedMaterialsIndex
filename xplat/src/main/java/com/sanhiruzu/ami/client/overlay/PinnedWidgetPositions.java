package com.sanhiruzu.ami.client.overlay;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.config.AmiConfigStore;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages pinned widget positions stored as JSON in config.
 */
public class PinnedWidgetPositions {
    private static final Gson GSON = new Gson();
    private static final int UNSET = Integer.MIN_VALUE;
    private Map<String, Position> positions = new HashMap<>();

    public static class Position {
        public int x;
        public int y;
        public int w;
        public int h;

        public Position(int x, int y) {
            this(x, y, UNSET, UNSET);
        }

        public Position(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public boolean isPinned() {
            return x != UNSET || y != UNSET || w != UNSET || h != UNSET;
        }

        public int getX(int defaultX) {
            return x == Integer.MIN_VALUE ? defaultX : x;
        }

        public int getY(int defaultY) {
            return y == UNSET ? defaultY : y;
        }

        public int getW(int defaultW) {
            return w == UNSET ? defaultW : w;
        }

        public int getH(int defaultH) {
            return h == UNSET ? defaultH : h;
        }
    }

    /**
     * Load positions from config JSON.
     */
    public static PinnedWidgetPositions load() {
        PinnedWidgetPositions positions = new PinnedWidgetPositions();
        try {
            JsonObject json = GSON.fromJson(AmiConfig.pinnedPositionsJson, JsonObject.class);
            if (json != null) {
                for (String key : json.keySet()) {
                    JsonElement elem = json.get(key);
                    if (elem.isJsonObject()) {
                        JsonObject pos = elem.getAsJsonObject();
                        int x = pos.has("x") ? pos.get("x").getAsInt() : UNSET;
                        int y = pos.has("y") ? pos.get("y").getAsInt() : UNSET;
                        int w = pos.has("w") ? pos.get("w").getAsInt() : UNSET;
                        int h = pos.has("h") ? pos.get("h").getAsInt() : UNSET;
                        positions.positions.put(key, new Position(x, y, w, h));
                    }
                }
            }
        } catch (Exception ignored) {
            // Invalid JSON, use defaults
        }
        return positions;
    }

    /**
     * Get position for a widget, or default if not pinned.
     */
    public Position get(String widgetId, int defaultX, int defaultY) {
        Position pos = positions.get(widgetId);
            if (pos != null && pos.isPinned()) {
                return new Position(pos.getX(defaultX), pos.getY(defaultY));
            }
            return new Position(defaultX, defaultY, Integer.MIN_VALUE, Integer.MIN_VALUE);
        }

    public Position get(String widgetId, int defaultX, int defaultY, int defaultW, int defaultH) {
        Position pos = positions.get(widgetId);
        if (pos != null && pos.isPinned()) {
            return new Position(pos.getX(defaultX), pos.getY(defaultY), pos.getW(defaultW), pos.getH(defaultH));
        }
        return new Position(defaultX, defaultY, defaultW, defaultH);
    }

    public boolean isPinned(String widgetId) {
        Position pos = positions.get(widgetId);
        return pos != null && pos.isPinned();
    }

    /**
     * Set position for a widget.
     */
    public void set(String widgetId, int x, int y) {
        positions.put(widgetId, new Position(x, y));
    }

    /**
     * Clear position for a widget.
     */
    public void clear(String widgetId) {
        positions.remove(widgetId);
    }

    /**
     * Clear all positions.
     */
    public void clearAll() {
        positions.clear();
    }

    /**
     * Save positions to config and persist to disk.
     */
    public void save() {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, Position> entry : positions.entrySet()) {
            JsonObject pos = new JsonObject();
            pos.addProperty("x", entry.getValue().x);
            pos.addProperty("y", entry.getValue().y);
            if (entry.getValue().w != UNSET) {
                pos.addProperty("w", entry.getValue().w);
            }
            if (entry.getValue().h != UNSET) {
                pos.addProperty("h", entry.getValue().h);
            }
            json.add(entry.getKey(), pos);
        }
        AmiConfig.pinnedPositionsJson = GSON.toJson(json);
        AmiConfigStore.save();
    }

    public void set(String widgetId, int x, int y, int w, int h) {
        positions.put(widgetId, new Position(x, y, w, h));
    }
}
