package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.overlay.OverlayLayers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Small popup menu used by result views. It owns only menu geometry and dispatch;
 * callers decide which actions are available for the clicked result.
 */
public class ResultContextMenu {
    private static final String BACK_ACTION_ID = "ami:context_menu_back";
    private static final int MIN_WIDTH = 104;
    private static final int MAX_WIDTH = 190;
    private static final int PADDING_X = 8;
    private static final int PADDING_Y = 4;
    private static final int ROW_HEIGHT = 16;
    private static final Logger LOGGER = Logger.getLogger(ResultContextMenu.class.getName());

    private final List<Action> actions = new ArrayList<>();
    private final List<List<Action>> actionStack = new ArrayList<>();
    private boolean open;
    private int x;
    private int y;
    private int width;
    private int height;
    private int boundsX;
    private int boundsY;
    private int boundsWidth;
    private int boundsHeight;

    public void open(int mouseX, int mouseY, int boundsX, int boundsY, int boundsWidth, int boundsHeight,
                     List<Action> requestedActions) {
        replaceActions(requestedActions);
        openWithCurrentActions(mouseX, mouseY, boundsX, boundsY, boundsWidth, boundsHeight);
    }

    public boolean isOpen() {
        return open;
    }

    public void close() {
        open = false;
        actions.clear();
        actionStack.clear();
    }

    public boolean contains(double mouseX, double mouseY) {
        return open && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!open || button != 0 || !contains(mouseX, mouseY)) {
            return false;
        }

        int row = ((int) mouseY - y - PADDING_Y) / ROW_HEIGHT;
        if (row >= 0 && row < actions.size()) {
            Action action = actions.get(row);
            dispatch(action);
            return true;
        }

        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!open) return false;

        return runMnemonic(mnemonicFromKeyCode(keyCode));
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (!open) return false;

        return runMnemonic(codePoint);
    }

    private boolean runMnemonic(Character pressed) {
        if (pressed == null) return false;

        char normalized = Character.toLowerCase(pressed);
        for (Action action : actions) {
            if (!action.enabled() || action.mnemonic() == null) continue;
            if (Character.toLowerCase(action.mnemonic()) == normalized) {
                close();
                dispatch(action);
                return true;
            }
        }
        return false;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        if (!open || g == null) return;

        var font = Minecraft.getInstance().font;
        g.flush();
        g.pose().pushPose();
        g.pose().translate(0, 0, OverlayLayers.CONTEXT_MENU);
        AMITheme.fillPixelPopup(g, x, y, width, height,
                AMITheme.DROPDOWN_LIST_BG, AMITheme.SECTION_SEP, AMITheme.GRADIENT_SHADOW, 0);

        for (int i = 0; i < actions.size(); i++) {
            int rowY = y + PADDING_Y + i * ROW_HEIGHT;
            Action action = actions.get(i);
            boolean hovered = action.enabled()
                    && mouseX >= x && mouseX < x + width
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (hovered) {
                g.fill(x + 1, rowY, x + width - 1, rowY + ROW_HEIGHT, AMITheme.DROPDOWN_BG_ACTIVE);
            }
            int color = action.enabled() ? AMITheme.TEXT_HEADER : AMITheme.TEXT_SUBTLE;
            String label = action.label().getString();
            int textX = x + PADDING_X;
            int textY = rowY + (ROW_HEIGHT - font.lineHeight) / 2;
            g.drawString(font, label, textX, textY, color, false);
            underlineMnemonic(g, font, label, action.mnemonic(), textX, textY, color);
            if (action.isSubmenu()) {
                String arrow = ">";
                g.drawString(font, arrow, x + width - PADDING_X - font.width(arrow), textY, color, false);
            }
        }
        g.pose().popPose();
        g.flush();
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    int getWidth() {
        return width;
    }

    int getHeight() {
        return height;
    }

    int actionCount() {
        return actions.size();
    }

    Action actionForTests(int index) {
        return index >= 0 && index < actions.size() ? actions.get(index) : null;
    }

    private void replaceActions(List<Action> requestedActions) {
        actions.clear();
        actionStack.clear();
        if (requestedActions == null) return;

        for (Action action : requestedActions) {
            if (isUsable(action)) {
                actions.add(sanitize(action));
            }
        }
    }

    private void openWithCurrentActions(int mouseX, int mouseY, int boundsX, int boundsY, int boundsWidth, int boundsHeight) {
        if (actions.isEmpty()) {
            close();
            return;
        }

        if (boundsWidth <= 0 || boundsHeight <= 0) {
            LOGGER.warning("AMI: Ignoring context menu open request with invalid bounds " + boundsWidth + "x" + boundsHeight);
            close();
            return;
        }

        this.boundsX = boundsX;
        this.boundsY = boundsY;
        this.boundsWidth = boundsWidth;
        this.boundsHeight = boundsHeight;
        this.width = Math.min(boundsWidth, measureWidth(actions));
        this.height = Math.min(boundsHeight, PADDING_Y * 2 + actions.size() * ROW_HEIGHT);
        int maxX = boundsX + Math.max(0, boundsWidth - width);
        int maxY = boundsY + Math.max(0, boundsHeight - height);
        this.x = clamp(mouseX, boundsX, maxX);
        this.y = clamp(mouseY, boundsY, maxY);
        this.open = true;
    }

    private void dispatch(Action action) {
        if (action == null || !action.enabled()) {
            close();
            return;
        }
        if (BACK_ACTION_ID.equals(action.id())) {
            openParentMenu();
            return;
        }
        if (action.isSubmenu()) {
            openSubmenu(action.children());
            return;
        }
        close();
        runAction(action);
    }

    private void openSubmenu(List<Action> children) {
        if (children == null || children.isEmpty()) {
            return;
        }
        actionStack.add(List.copyOf(actions));
        actions.clear();
        actions.add(Action.enabled(BACK_ACTION_ID, Component.translatable("ami.context.back"), 'b', this::openParentMenu));
        actions.addAll(children);
        resizeForCurrentActions();
    }

    private void openParentMenu() {
        if (actionStack.isEmpty()) {
            close();
            return;
        }
        List<Action> parent = actionStack.remove(actionStack.size() - 1);
        actions.clear();
        actions.addAll(parent);
        resizeForCurrentActions();
    }

    private void resizeForCurrentActions() {
        this.width = Math.min(boundsWidth, measureWidth(actions));
        this.height = Math.min(boundsHeight, PADDING_Y * 2 + actions.size() * ROW_HEIGHT);
        int maxX = boundsX + Math.max(0, boundsWidth - width);
        int maxY = boundsY + Math.max(0, boundsHeight - height);
        this.x = clamp(x, boundsX, maxX);
        this.y = clamp(y, boundsY, maxY);
    }

    private static void runAction(Action action) {
        if (action == null || !action.enabled() || action.onClick() == null) return;

        try {
            action.onClick().run();
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "AMI: Context menu action failed: " + action.label().getString(), e);
        }
    }

    private static boolean isUsable(Action action) {
        return action != null
                && action.label() != null
                && action.label().getString() != null
                && (!action.isSubmenu() || !sanitizeChildren(action.children()).isEmpty());
    }

    private static Action sanitize(Action action) {
        if (action == null || !action.isSubmenu()) {
            return action;
        }
        return new Action(action.id(), action.label(), action.mnemonic(), action.enabled(), action.onClick(),
                sanitizeChildren(action.children()));
    }

    private static List<Action> sanitizeChildren(List<Action> children) {
        if (children == null || children.isEmpty()) {
            return List.of();
        }
        List<Action> usable = new ArrayList<>();
        for (Action child : children) {
            if (isUsable(child)) {
                usable.add(sanitize(child));
            }
        }
        return List.copyOf(usable);
    }

    private static int measureWidth(List<Action> actions) {
        if (actions == null || actions.isEmpty()) return MIN_WIDTH;

        var font = Minecraft.getInstance().font;
        int measured = MIN_WIDTH;
        for (Action action : actions) {
            int submenuPadding = action.isSubmenu() ? font.width(">") + PADDING_X : 0;
            measured = Math.max(measured, font.width(action.label().getString()) + PADDING_X * 2 + submenuPadding);
        }
        return Math.min(MAX_WIDTH, measured);
    }

    private static int clamp(int value, int min, int max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }

    private static Character mnemonicFromKeyCode(int keyCode) {
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            return (char) ('a' + keyCode - GLFW.GLFW_KEY_A);
        }
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            return (char) ('0' + keyCode - GLFW.GLFW_KEY_0);
        }
        if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            return (char) ('0' + keyCode - GLFW.GLFW_KEY_KP_0);
        }
        return null;
    }

    private static void underlineMnemonic(GuiGraphics g, net.minecraft.client.gui.Font font, String label,
                                          Character mnemonic, int textX, int textY, int color) {
        if (g == null || font == null || label == null || label.isEmpty() || mnemonic == null) return;

        int index = mnemonicIndex(label, mnemonic);
        if (index < 0) return;

        int startX = textX + font.width(label.substring(0, index));
        int endX = startX + Math.max(1, font.width(label.substring(index, index + 1)));
        int underlineY = textY + font.lineHeight;
        g.fill(startX, underlineY, endX, underlineY + 1, color);
    }

    private static int mnemonicIndex(String label, char mnemonic) {
        char target = Character.toLowerCase(mnemonic);
        for (int i = 0; i < label.length(); i++) {
            if (Character.toLowerCase(label.charAt(i)) == target) {
                return i;
            }
        }
        return -1;
    }

    public record Action(String id, Component label, Character mnemonic, boolean enabled, Runnable onClick,
                         List<Action> children) {
        public Action {
            children = children == null ? List.of() : List.copyOf(children);
        }

        public Action(String id, Component label, Character mnemonic, boolean enabled, Runnable onClick) {
            this(id, label, mnemonic, enabled, onClick, List.of());
        }

        public Action(Component label, boolean enabled, Runnable onClick) {
            this("", label, null, enabled, onClick);
        }

        public boolean isSubmenu() {
            return !children.isEmpty();
        }

        public static Action enabled(Component label, Runnable onClick) {
            return new Action("", label, null, true, onClick);
        }

        public static Action enabled(String id, Component label, char mnemonic, Runnable onClick) {
            return new Action(id, label, mnemonic, true, onClick);
        }

        public static Action disabled(String id, Component label, char mnemonic) {
            return new Action(id, label, mnemonic, false, null);
        }

        public static Action submenu(String id, Component label, char mnemonic, List<Action> children) {
            return new Action(id, label, mnemonic, true, null, children);
        }
    }
}
