package com.sanhiruzu.ami.index.metrics;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.OptionalDouble;

/**
 * Best-effort baseline DPS estimator for item cards.
 * <p>
 * This intentionally evaluates only main-hand attack damage/speed attributes.
 * Conditional mod logic, procs, enchantments, and target-specific effects are
 * outside the indexer's scope.
 */
public final class DpsMetricSniffer {
    private static final double PLAYER_BASE_DAMAGE = 1.0D;
    private static final double PLAYER_BASE_ATTACK_SPEED = 4.0D;

    private DpsMetricSniffer() {
    }

    public static OptionalDouble estimate(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return OptionalDouble.empty();

        ItemAttributeModifiers modifiers = stack.getAttributeModifiers();
        MutableAttribute damage = new MutableAttribute(PLAYER_BASE_DAMAGE);
        MutableAttribute speed = new MutableAttribute(PLAYER_BASE_ATTACK_SPEED);

        modifiers.forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (sameAttribute(attribute, Attributes.ATTACK_DAMAGE)) {
                damage.apply(modifier);
            } else if (sameAttribute(attribute, Attributes.ATTACK_SPEED)) {
                speed.apply(modifier);
            }
        });

        double finalDamage = Math.max(0.0D, damage.value());
        double finalSpeed = Math.max(0.0D, speed.value());
        double dps = finalDamage * finalSpeed;

        // Do not annotate harmless/default items as weapons.
        if (dps <= PLAYER_BASE_DAMAGE * PLAYER_BASE_ATTACK_SPEED) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(dps);
    }

    private static boolean sameAttribute(Holder<Attribute> left, Holder<Attribute> right) {
        return left == right || left.equals(right);
    }

    private static final class MutableAttribute {
        private final double base;
        private double value;

        private MutableAttribute(double base) {
            this.base = base;
            this.value = base;
        }

        private void apply(AttributeModifier modifier) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                value += modifier.amount();
            } else if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                value += base * modifier.amount();
            } else if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                value *= 1.0D + modifier.amount();
            }
        }

        private double value() {
            return value;
        }
    }
}
