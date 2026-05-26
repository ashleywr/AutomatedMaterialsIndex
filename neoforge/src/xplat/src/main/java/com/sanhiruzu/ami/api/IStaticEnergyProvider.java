package com.sanhiruzu.ami.api;

import net.minecraft.world.level.block.state.BlockState;

import com.sanhiruzu.ami.neoforge.AMI;
/**
 * Optional AMI-facing static energy metadata contract for block classes.
 *
 * <p>Blocks that have a stable, state-derived FE/RF capacity can implement this
 * interface directly to expose that capacity without AMI constructing a
 * {@code BlockEntity}. This is intentionally narrow: it is a startup-indexing
 * hint, not a replacement for NeoForge's runtime energy capabilities.</p>
 */
public interface IStaticEnergyProvider {
    /**
     * Returns the base maximum energy capacity for the supplied block state.
     *
     * @param state block state being indexed, usually the block's default state
     * @return maximum energy capacity in FE/RF; non-positive values are ignored
     */
    int getBaseCapacity(BlockState state);
}
