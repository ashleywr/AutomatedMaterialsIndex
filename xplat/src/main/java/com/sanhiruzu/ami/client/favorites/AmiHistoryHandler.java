package com.sanhiruzu.ami.client.favorites;

import com.sanhiruzu.ami.platform.Services;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles tracking of item lookup history within AMI.
 * This provides a fallback for recipe viewers that don't expose their own history.
 */
public class AmiHistoryHandler {
    private static final AmiHistoryHandler INSTANCE = new AmiHistoryHandler();
    private static final int MAX_HISTORY = 100;

    private final List<ItemStack> lookupHistory = new ArrayList<>();
    private final List<ItemStack> craftHistory = new ArrayList<>();
    private final AtomicLong revision = new AtomicLong();
    private Runnable onChange;

    private AmiHistoryHandler() {
    }

    public static AmiHistoryHandler getInstance() {
        return INSTANCE;
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public void recordLookup(ItemStack stack) {
        record(lookupHistory, stack);
    }

    public void recordCraft(ItemStack stack) {
        record(craftHistory, stack);
    }

    private void record(List<ItemStack> history, ItemStack stack) {
        if (stack.isEmpty()) return;

        // Remove existing to move to top
        history.removeIf(s -> Services.PLATFORM.sameItemSameComponents(s, stack));

        history.add(0, stack.copy());

        if (history.size() > MAX_HISTORY) {
            history.remove(history.size() - 1);
        }
        revision.incrementAndGet();

        if (onChange != null) {
            onChange.run();
        }
    }

    public List<ItemStack> getLookupHistory() {
        return List.copyOf(lookupHistory);
    }

    public List<ItemStack> getCraftHistory() {
        return List.copyOf(craftHistory);
    }

    public long revision() {
        return revision.get();
    }
}
