package com.sanhiruzu.ami.client.results;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultContextMenuTest {
    @BeforeEach
    void resetConfig() {
        com.sanhiruzu.ami.config.AmiConfig.resetToDefaults();
    }

    @Test
    void nullAndMalformedActionsDoNotOpenMenu() {
        ResultContextMenu menu = new ResultContextMenu();

        menu.open(10, 10, 0, 0, 100, 100, null);
        assertFalse(menu.isOpen());

        menu.open(10, 10, 0, 0, 100, 100, List.of(new ResultContextMenu.Action(null, true, () -> {
        })));
        assertFalse(menu.isOpen());
    }

    @Test
    void invalidBoundsCloseMenuWithoutDispatching() {
        AtomicInteger calls = new AtomicInteger();
        ResultContextMenu menu = new ResultContextMenu();

        menu.open(10, 10, 0, 0, 0, 100, List.of(
                ResultContextMenu.Action.enabled(Component.literal("Copy ID"), calls::incrementAndGet)
        ));

        assertFalse(menu.isOpen());
        assertEquals(0, calls.get());
    }

    @Test
    void opensClampedInsideBounds() {
        ResultContextMenu menu = new ResultContextMenu();

        menu.open(500, 500, 10, 20, 120, 80, List.of(
                ResultContextMenu.Action.enabled(Component.literal("Copy ID"), () -> {
                })
        ));

        assertTrue(menu.isOpen());
        assertTrue(menu.getX() >= 10);
        assertTrue(menu.getY() >= 20);
        assertTrue(menu.getX() + menu.getWidth() <= 130);
        assertTrue(menu.getY() + menu.getHeight() <= 100);
        assertEquals(1, menu.actionCount());
    }

    @Test
    void disabledActionConsumesClickWithoutRunning() {
        AtomicInteger calls = new AtomicInteger();
        ResultContextMenu menu = new ResultContextMenu();
        menu.open(10, 10, 0, 0, 100, 100, List.of(
                new ResultContextMenu.Action(Component.literal("Disabled"), false, calls::incrementAndGet)
        ));

        assertTrue(menu.mouseClicked(12, 16, 0));

        assertFalse(menu.isOpen());
        assertEquals(0, calls.get());
    }

    @Test
    void actionFailureClosesMenuAndDoesNotPropagate() {
        ResultContextMenu menu = new ResultContextMenu();
        menu.open(10, 10, 0, 0, 100, 100, List.of(
                ResultContextMenu.Action.enabled(Component.literal("Broken"), () -> {
                    throw new IllegalStateException("boom");
                })
        ));

        assertTrue(menu.mouseClicked(12, 16, 0));
        assertFalse(menu.isOpen());
    }

    @Test
    void mnemonicRunsMatchingEnabledAction() {
        AtomicInteger calls = new AtomicInteger();
        ResultContextMenu menu = new ResultContextMenu();
        menu.open(10, 10, 0, 0, 140, 100, List.of(
                ResultContextMenu.Action.enabled("ami:test", Component.literal("Copy ID"), 'i', calls::incrementAndGet)
        ));

        assertTrue(menu.keyPressed(73, 0, 0)); // GLFW_KEY_I

        assertFalse(menu.isOpen());
        assertEquals(1, calls.get());
    }

    @Test
    void typedMnemonicRunsMatchingEnabledAction() {
        AtomicInteger calls = new AtomicInteger();
        ResultContextMenu menu = new ResultContextMenu();
        menu.open(10, 10, 0, 0, 140, 100, List.of(
                ResultContextMenu.Action.enabled("ami:test", Component.literal("Show Recipes"), 'r', calls::incrementAndGet)
        ));

        assertTrue(menu.charTyped('r', 0));

        assertFalse(menu.isOpen());
        assertEquals(1, calls.get());
    }

    @Test
    void submenuClickOpensChildrenAndBackReturnsToParent() {
        AtomicInteger calls = new AtomicInteger();
        ResultContextMenu menu = new ResultContextMenu();
        menu.open(10, 10, 0, 0, 180, 120, List.of(
                ResultContextMenu.Action.submenu("ami:more", Component.literal("More"), 'm', List.of(
                        ResultContextMenu.Action.enabled("ami:child", Component.literal("Child"), 'c', calls::incrementAndGet)
                ))
        ));

        assertTrue(menu.mouseClicked(12, 16, 0));
        assertTrue(menu.isOpen());
        assertEquals(2, menu.actionCount());
        assertEquals("ami:context_menu_back", menu.actionForTests(0).id());
        assertEquals("ami:child", menu.actionForTests(1).id());

        assertTrue(menu.mouseClicked(12, 16, 0));
        assertTrue(menu.isOpen());
        assertEquals(1, menu.actionCount());
        assertEquals("ami:more", menu.actionForTests(0).id());

        assertTrue(menu.mouseClicked(12, 16, 0));
        assertTrue(menu.isOpen());
        assertEquals("ami:child", menu.actionForTests(1).id());

        assertTrue(menu.mouseClicked(12, 32, 0));
        assertFalse(menu.isOpen());
        assertEquals(1, calls.get());
    }
}
