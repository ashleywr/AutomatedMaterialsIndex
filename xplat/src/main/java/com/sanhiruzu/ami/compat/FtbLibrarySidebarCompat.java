package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.client.overlay.WidgetBounds;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/**
 * Optional FTB Library sidebar integration.
 *
 * FTB Library draws inventory sidebar buttons in the screen margin. Its
 * SidebarGroupGuiButton.lastDrawnArea is a broad recipe-viewer exclusion and
 * can include non-button space, so AMI computes the visible button grid from
 * FTB's runtime state.
 */
public final class FtbLibrarySidebarCompat {
    private static final int BUTTON_SPACING = 17;
    private static final int EDGE_PADDING = 2;

    private static boolean resolved;
    private static Method areButtonsVisible;
    private static Field sidebarPositionField;
    private static Field sidebarManagerInstanceField;
    private static Method valueGet;
    private static Method positionIsBottom;
    private static Method positionIsRight;
    private static Method getEnabledButtonList;
    private static Method buttonGetGridLocation;
    private static Method gridX;
    private static Method gridY;

    private FtbLibrarySidebarCompat() {
    }

    public static Optional<WidgetBounds> sidebarBounds(Screen screen) {
        if (screen == null) return Optional.empty();
        Optional<WidgetBounds> lastDrawnArea = buttonsVisible(screen) ? lastDrawnAreaBounds() : Optional.empty();
        if (lastDrawnArea.isPresent()) {
            return lastDrawnArea;
        }

        try {
            resolve();
            if (areButtonsVisible == null) return Optional.empty();
            if (!Boolean.TRUE.equals(areButtonsVisible.invoke(null, screen))) return Optional.empty();

            Object positionValue = sidebarPositionField.get(null);
            Object position = valueGet.invoke(positionValue);
            boolean bottom = Boolean.TRUE.equals(positionIsBottom.invoke(position));
            boolean right = Boolean.TRUE.equals(positionIsRight.invoke(position));

            GridSize grid = visibleButtonGridSize();
            int width = grid.width() * BUTTON_SPACING + EDGE_PADDING;
            int height = grid.height() * BUTTON_SPACING + EDGE_PADDING;
            int x = right ? screen.width - width : 0;
            int y = bottom ? screen.height - height : 0;
            return Optional.of(new WidgetBounds(x, y, width, height));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return lastDrawnAreaBounds();
        }
    }

    private static Optional<WidgetBounds> lastDrawnAreaBounds() {
        try {
            Class<?> groupButtonClass = Class.forName("dev.ftb.mods.ftblibrary.sidebar.SidebarGroupGuiButton");
            Object value = groupButtonClass.getField("lastDrawnArea").get(null);
            if (!(value instanceof Rect2i area)) {
                return Optional.empty();
            }
            if (area.getWidth() <= 0 || area.getHeight() <= 0) {
                return Optional.empty();
            }
            return Optional.of(new WidgetBounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return Optional.empty();
        }
    }

    private static boolean buttonsVisible(Screen screen) {
        try {
            Class<?> clientClass = Class.forName("dev.ftb.mods.ftblibrary.FTBLibraryClient");
            Method method = clientClass.getMethod("areButtonsVisible", Screen.class);
            return Boolean.TRUE.equals(method.invoke(null, screen));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return true;
        }
    }

    private static GridSize visibleButtonGridSize() throws ReflectiveOperationException {
        Object manager = sidebarManagerInstanceField.get(null);
        @SuppressWarnings("unchecked")
        List<Object> buttons = (List<Object>) getEnabledButtonList.invoke(manager, false);
        if (buttons.isEmpty()) {
            return new GridSize(1, 1);
        }

        int maxX = 0;
        int maxY = 0;
        for (Object button : buttons) {
            Object location = buttonGetGridLocation.invoke(button);
            int x = ((Number) gridX.invoke(location)).intValue();
            int y = ((Number) gridY.invoke(location)).intValue();
            if (x < 0 || y < 0) continue;
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        return new GridSize(maxX + 1, maxY + 1);
    }

    private static void resolve() throws ReflectiveOperationException {
        if (resolved) return;
        resolved = true;

        Class<?> clientClass = Class.forName("dev.ftb.mods.ftblibrary.FTBLibraryClient");
        Class<?> configClass = Class.forName("dev.ftb.mods.ftblibrary.config.FTBLibraryClientConfig");
        Class<?> managerClass = Class.forName("dev.ftb.mods.ftblibrary.sidebar.SidebarButtonManager");
        Class<?> buttonClass = Class.forName("dev.ftb.mods.ftblibrary.sidebar.SidebarGuiButton");
        Class<?> gridClass = Class.forName("dev.ftb.mods.ftblibrary.sidebar.GridLocation");

        areButtonsVisible = clientClass.getMethod("areButtonsVisible", Screen.class);
        sidebarPositionField = configClass.getField("SIDEBAR_POSITION");
        sidebarManagerInstanceField = managerClass.getField("INSTANCE");
        valueGet = sidebarPositionField.getType().getMethod("get");
        getEnabledButtonList = managerClass.getMethod("getEnabledButtonList", boolean.class);
        buttonGetGridLocation = buttonClass.getMethod("getGridLocation");
        gridX = gridClass.getMethod("x");
        gridY = gridClass.getMethod("y");

        Class<?> positionClass = Class.forName("dev.ftb.mods.ftblibrary.config.FTBLibraryClientConfig$SidebarPosition");
        positionIsBottom = positionClass.getMethod("isBottom");
        positionIsRight = positionClass.getMethod("isRight");
    }

    private record GridSize(int width, int height) {
    }
}
