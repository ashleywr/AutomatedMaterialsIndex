package com.sanhiruzu.ami.client.results;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DropdownClickAwayTest {
    @Test
    void singleSelectClickAwayIsUnhandledAndDoesNotSelect() {
        AtomicReference<String> selectedByCallback = new AtomicReference<>();
        SingleSelectDropdown<String> dropdown = new SingleSelectDropdown<>(
                Component.literal("Sort"),
                List.of("alpha", "beta", "gamma"),
                Component::literal,
                "alpha",
                selectedByCallback::set
        );
        dropdown.updatePosition(10, 10, 80);

        assertTrue(dropdown.mouseClicked(12, 12, 0));
        assertTrue(dropdown.isOpen());

        assertFalse(dropdown.mouseClicked(250, 250, 0));
        assertTrue(dropdown.isOpen(), "The owner closes click-away after the dropdown reports it unhandled");
        assertEquals("alpha", dropdown.getSelected());
        assertNull(selectedByCallback.get());
    }

    @Test
    void singleSelectInsidePanelButOutsideItemStaysHandled() {
        SingleSelectDropdown<String> dropdown = new SingleSelectDropdown<>(
                Component.literal("Sort"),
                List.of("alpha", "beta", "gamma"),
                Component::literal,
                "alpha",
                ignored -> {
                }
        );
        dropdown.updatePosition(10, 10, 80);

        assertTrue(dropdown.mouseClicked(12, 12, 0));

        assertTrue(dropdown.mouseClicked(12, 26, 0));
        assertTrue(dropdown.isOpen());
        assertEquals("alpha", dropdown.getSelected());
    }

    @Test
    void singleSelectItemClickStillSelectsAndCloses() {
        AtomicReference<String> selectedByCallback = new AtomicReference<>();
        SingleSelectDropdown<String> dropdown = new SingleSelectDropdown<>(
                Component.literal("Sort"),
                List.of("alpha", "beta", "gamma"),
                Component::literal,
                "alpha",
                selectedByCallback::set
        );
        dropdown.updatePosition(10, 10, 80);

        assertTrue(dropdown.mouseClicked(12, 12, 0));
        assertTrue(dropdown.mouseClicked(12, 40, 0));

        assertFalse(dropdown.isOpen());
        assertEquals("beta", dropdown.getSelected());
        assertEquals("beta", selectedByCallback.get());
    }

    @Test
    void multiSelectClickAwayIsUnhandledAndDoesNotToggleSelection() {
        MultiSelectDropdown<String> dropdown = new MultiSelectDropdown<>(
                List.of("alpha", "beta", "gamma"),
                value -> value
        );
        dropdown.updatePosition(10, 10, 80);

        assertTrue(dropdown.mouseClicked(12, 12, 0));
        assertTrue(dropdown.isOpen());

        assertFalse(dropdown.mouseClicked(250, 250, 0));
        assertTrue(dropdown.isOpen(), "The owner closes click-away after the dropdown reports it unhandled");
        assertEquals(3, dropdown.getSelected().size());
    }
}
