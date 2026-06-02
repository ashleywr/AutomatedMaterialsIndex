package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.client.overlay.WidgetBounds;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Optional FTB Library sidebar integration.
 *
 * FTB Library draws inventory sidebar buttons in the screen margin. AMI needs
 * to reserve that grid before FTB's widget has rendered, so this computes the
 * same top/bottom/left/right button area from FTB's runtime state instead of
 * reading SidebarGroupGuiButton.lastDrawnArea after the fact.
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
        try {
            resolve();
            if (areButtonsVisible == null) return Optional.empty();
            if (!Boolean.TRUE.equals(areButtonsVisible.invoke(null, screen))) return Optional.empty();

            Object positionValue = sidebarPositionField.get(null);
            Object position = valueGet.invoke(positionValue);
            boolean bottom = Boolean.TRUE.equals(positionIsBottom.invoke(position));
            boolean right = Boolean.TRUE.equals(positionIsRight.invoke(position));

            GridSize grid = gridSize();
            int width = Math.max(1, grid.width()) * BUTTON_SPACING + EDGE_PADDING;
            int height = Math.max(1, grid.height()) * BUTTON_SPACING + EDGE_PADDING;
            int x = right ? screen.width - width : 0;
            int y = bottom ? screen.height - height : 0;
            return Optional.of(new WidgetBounds(x, y, width, height));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return Optional.empty();
        }
    }

    private static GridSize gridSize() throws ReflectiveOperationException {
        Object manager = sidebarManagerInstanceField.get(null);
        @SuppressWarnings("unchecked")
        List<Object> buttons = (List<Object>) getEnabledButtonList.invoke(manager, false);
        if (buttons.isEmpty()) {
            return new GridSize(1, 1);
        }

        TreeMap<Integer, Integer> rowWidths = new TreeMap<>();
        for (Object button : buttons) {
            Object location = buttonGetGridLocation.invoke(button);
            int x = ((Number) gridX.invoke(location)).intValue();
            int y = ((Number) gridY.invoke(location)).intValue();
            if (x < 0 || y < 0) continue;
            rowWidths.merge(y, 1, Integer::sum);
        }

        int width = 1;
        for (int rowWidth : rowWidths.values()) {
            width = Math.max(width, rowWidth);
        }
        return new GridSize(width, Math.max(1, rowWidths.size()));
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
