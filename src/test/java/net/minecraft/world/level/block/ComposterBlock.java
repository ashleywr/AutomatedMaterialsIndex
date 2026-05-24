package net.minecraft.world.level.block;

import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.world.item.Item;

public final class ComposterBlock {
    public static final Object2FloatMap<Item> COMPOSTABLES = new Object2FloatOpenHashMap<>();

    private ComposterBlock() {
    }
}
