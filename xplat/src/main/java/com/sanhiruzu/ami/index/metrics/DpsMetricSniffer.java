package com.sanhiruzu.ami.index.metrics;

import com.sanhiruzu.ami.platform.IPlatformHelper;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
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
        return estimateStats(stack).map(stats -> OptionalDouble.of(stats.dps())).orElseGet(OptionalDouble::empty);
    }

    public static OptionalDouble estimateDamage(ItemStack stack) {
        return estimateStats(stack).map(stats -> OptionalDouble.of(stats.damage())).orElseGet(OptionalDouble::empty);
    }

    public static Optional<DpsStats> estimateStats(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();

        MutableAttribute damage = new MutableAttribute(PLAYER_BASE_DAMAGE);
        MutableAttribute speed = new MutableAttribute(PLAYER_BASE_ATTACK_SPEED);
        boolean hasMainHandAttackModifier = false;

        for (IPlatformHelper.ItemAttributeModifier modifier : Services.PLATFORM.getMainHandAttackModifiers(stack)) {
            if (modifier.kind() == IPlatformHelper.ItemAttributeKind.ATTACK_DAMAGE) {
                hasMainHandAttackModifier = true;
                damage.apply(modifier);
            } else if (modifier.kind() == IPlatformHelper.ItemAttributeKind.ATTACK_SPEED) {
                hasMainHandAttackModifier = true;
                speed.apply(modifier);
            }
        }

        double finalDamage = Math.max(0.0D, damage.value());
        double finalSpeed = Math.max(0.0D, speed.value());
        double dps = finalDamage * finalSpeed;

        // Do not annotate harmless/default items as weapons, but keep slow weapons
        // like the mace whose baseline DPS can be lower than an empty hand.
        if (!hasMainHandAttackModifier || dps <= 0.0D) {
            return Optional.empty();
        }
        return Optional.of(new DpsStats(finalDamage, dps));
    }

    private static final class MutableAttribute {
        private final double base;
        private double value;

        private MutableAttribute(double base) {
            this.base = base;
            this.value = base;
        }

        private void apply(IPlatformHelper.ItemAttributeModifier modifier) {
            if (modifier.operation() == IPlatformHelper.ItemAttributeOperation.ADD_VALUE) {
                value += modifier.amount();
            } else if (modifier.operation() == IPlatformHelper.ItemAttributeOperation.ADD_MULTIPLIED_BASE) {
                value += base * modifier.amount();
            } else if (modifier.operation() == IPlatformHelper.ItemAttributeOperation.ADD_MULTIPLIED_TOTAL) {
                value *= 1.0D + modifier.amount();
            }
        }

        private double value() {
            return value;
        }
    }

    public record DpsStats(double damage, double dps) {
    }
}
