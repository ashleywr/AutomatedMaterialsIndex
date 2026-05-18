package net.minecraft.world.item;

import net.minecraft.world.entity.EquipmentSlot;

public class ArmorItem extends Item {
    private final EquipmentSlot slot;

    public ArmorItem(String name, EquipmentSlot slot) {
        super(name);
        this.slot = slot;
    }

    public EquipmentSlot getEquipmentSlot() {
        return slot;
    }
}
