package com.sanhiruzu.ami.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaCraftablesServiceTest {

    @Test
    void openContainerCraftabilityCountsOnlyUsableNonPlayerSlots() {
        assertTrue(CraftableSlotPolicy.shouldAccountOpenContainerSlot(true, false, false, true));

        assertFalse(CraftableSlotPolicy.shouldAccountOpenContainerSlot(false, false, false, true));
        assertFalse(CraftableSlotPolicy.shouldAccountOpenContainerSlot(true, true, false, true));
        assertFalse(CraftableSlotPolicy.shouldAccountOpenContainerSlot(true, false, true, true));
        assertFalse(CraftableSlotPolicy.shouldAccountOpenContainerSlot(true, false, false, false));
    }
}
