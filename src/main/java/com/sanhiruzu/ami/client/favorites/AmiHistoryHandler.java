package com.sanhiruzu.ami.client.favorites;

import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles tracking of item lookup history within AMI.
 * This provides a fallback for recipe viewers that don't expose their own history.
 */
public class AmiHistoryHandler {
    private static final AmiHistoryHandler INSTANCE = new AmiHistoryHandler();
    private static final int MAX_HISTORY = 100;

    private final List<ItemStack> lookupHistory = new ArrayList<>();
    private Runnable onChange;

    private AmiHistoryHandler() {}

    public static AmiHistoryHandler getInstance() {
        return INSTANCE;
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public void recordLookup(ItemStack stack) {
        if (stack.isEmpty()) return;
        
        // Remove existing to move to top
        lookupHistory.removeIf(s -> ItemStack.isSameItemSameComponents(s, stack));
        
        lookupHistory.add(0, stack.copy());
        
        if (lookupHistory.size() > MAX_HISTORY) {
            lookupHistory.remove(lookupHistory.size() - 1);
        }

        if (onChange != null) {
            onChange.run();
        }
    }

    public List<ItemStack> getLookupHistory() {
        return List.copyOf(lookupHistory);
    }
}
