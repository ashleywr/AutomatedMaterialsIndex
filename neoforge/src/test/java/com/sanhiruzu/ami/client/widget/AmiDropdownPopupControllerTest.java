package com.sanhiruzu.ami.client.widget;

import net.minecraft.client.gui.GuiGraphics;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmiDropdownPopupControllerTest {
    @Test
    void handledClickSurvivesCallbackClearingTrackedDropdown() {
        AtomicReference<AmiDropdownPopup> openDropdown = new AtomicReference<>();
        FakeDropdown dropdown = new FakeDropdown(() -> openDropdown.set(null), true);
        openDropdown.set(dropdown);

        assertTrue(AmiDropdownPopupController.mouseClicked(
                openDropdown::get,
                openDropdown::set,
                10,
                10,
                0
        ));

        assertNull(openDropdown.get());
        assertFalse(dropdown.isOpen());
    }

    @Test
    void handledClickDoesNotClearReplacementDropdown() {
        AtomicReference<AmiDropdownPopup> openDropdown = new AtomicReference<>();
        FakeDropdown replacement = new FakeDropdown(() -> {
        }, true);
        FakeDropdown dropdown = new FakeDropdown(() -> openDropdown.set(replacement), true);
        openDropdown.set(dropdown);

        assertTrue(AmiDropdownPopupController.mouseClicked(
                openDropdown::get,
                openDropdown::set,
                10,
                10,
                0
        ));

        assertSame(replacement, openDropdown.get());
        assertFalse(dropdown.isOpen());
    }

    @Test
    void unhandledClickClosesOnlyTheTrackedDropdown() {
        AtomicReference<AmiDropdownPopup> openDropdown = new AtomicReference<>();
        FakeDropdown dropdown = new FakeDropdown(() -> {
        }, false);
        openDropdown.set(dropdown);

        assertFalse(AmiDropdownPopupController.mouseClicked(
                openDropdown::get,
                openDropdown::set,
                250,
                250,
                0
        ));

        assertNull(openDropdown.get());
        assertFalse(dropdown.isOpen());
    }

    private static final class FakeDropdown implements AmiDropdownPopup {
        private final Runnable callback;
        private final boolean handlesClick;
        private boolean open = true;

        private FakeDropdown(Runnable callback, boolean handlesClick) {
            this.callback = callback;
            this.handlesClick = handlesClick;
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() {
            open = false;
        }

        @Override
        public void renderDropdownList(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        public boolean isMouseOverPopup(double mouseX, double mouseY) {
            return false;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!handlesClick) {
                return false;
            }
            callback.run();
            close();
            return true;
        }
    }
}
