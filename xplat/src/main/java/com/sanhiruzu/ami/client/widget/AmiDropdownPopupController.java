package com.sanhiruzu.ami.client.widget;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class AmiDropdownPopupController {
    private AmiDropdownPopupController() {
    }

    public static boolean mouseClicked(
            Supplier<AmiDropdownPopup> openDropdown,
            Consumer<AmiDropdownPopup> setOpenDropdown,
            double mouseX,
            double mouseY,
            int button
    ) {
        AmiDropdownPopup dropdown = openDropdown.get();
        if (dropdown == null || !dropdown.isOpen()) {
            return false;
        }

        if (dropdown.handlePopupClick(mouseX, mouseY, button)) {
            if (openDropdown.get() == dropdown && !dropdown.isOpen()) {
                setOpenDropdown.accept(null);
            }
            return true;
        }

        if (openDropdown.get() == dropdown) {
            dropdown.close();
            setOpenDropdown.accept(null);
        }
        return false;
    }

    public static boolean blocksUnderlyingHover(AmiDropdownPopup dropdown, double mouseX, double mouseY) {
        return dropdown != null && dropdown.isOpen() && dropdown.isMouseOverPopup(mouseX, mouseY);
    }
}
