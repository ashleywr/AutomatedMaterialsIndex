package com.sanhiruzu.ami.fabric.compat;

import com.sanhiruzu.ami.fabric.AmiFabric;
import me.shedaniel.rei.api.client.REIRuntime;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Keeps REI's overlay (item list + search field) hidden while AMI is the active layer, and restores
 * REI's prior overlay visibility when AMI is no longer active.
 *
 * <p>REI exists only on Fabric, so this controller — like {@link ReiRecipeBridge} — lives in the Fabric
 * module and is only entered behind an {@code isModLoaded("roughlyenoughitems")} guard (see the
 * {@code InventoryOverlayHandler} call sites), so the {@code me.shedaniel.rei.*} types are never linked
 * when REI is absent.
 *
 * <p>The {@link me.shedaniel.rei.api.client.registry.screen.OverlayDecider} in {@link AmiReiPlugin}
 * already denies REI's overlay on foreign (inventory-like) screens, but REI force-renders its overlay on
 * its <b>own</b> {@code DisplayScreen}. Toggling REI's overlay invisible via the public
 * {@link REIRuntime#isOverlayVisible()} / {@link REIRuntime#toggleOverlayVisible()} API hides the
 * item-list/search there too while leaving the recipe content (the {@code DisplayScreen} itself)
 * rendering — exactly the desired result, and with no mixin into REI internals.
 */
@Environment(EnvType.CLIENT)
public final class ReiOverlayController {

    /** Whether AMI currently owns REI's overlay-visibility (i.e. has forced it invisible). */
    private static boolean amiOwnsOverlay = false;

    /** REI's overlay-visible state captured the moment AMI took ownership, restored on release. */
    private static boolean savedOverlayVisible = true;

    private ReiOverlayController() {
    }

    /**
     * Syncs REI's overlay visibility to AMI's state: REI overlay visible == {@code !amiActive}.
     *
     * @param amiActive {@code true} when AMI is the active layer on an AMI screen (REI overlay must hide);
     *                  {@code false} otherwise (restore REI's prior overlay state)
     */
    public static void setAmiActive(boolean amiActive) {
        try {
            REIRuntime runtime = REIRuntime.getInstance();
            if (runtime == null) {
                return;
            }
            if (amiActive) {
                if (!amiOwnsOverlay) {
                    // First time AMI is taking over — remember what REI's overlay was so we can restore it.
                    savedOverlayVisible = runtime.isOverlayVisible();
                    amiOwnsOverlay = true;
                }
                if (runtime.isOverlayVisible()) {
                    runtime.toggleOverlayVisible();
                }
            } else if (amiOwnsOverlay) {
                // Restore REI to whatever it was before AMI suppressed it.
                if (runtime.isOverlayVisible() != savedOverlayVisible) {
                    runtime.toggleOverlayVisible();
                }
                amiOwnsOverlay = false;
            }
        } catch (RuntimeException | LinkageError e) {
            // Never let REI internal changes break AMI's layer transitions.
            AmiFabric.LOGGER.warn("REI overlay sync failed", e);
        }
    }

    /**
     * Releases AMI's ownership of REI's overlay, restoring REI's prior visibility. Equivalent to
     * {@code setAmiActive(false)}; provided as a clearer call site for screen-close / session reset.
     */
    public static void release() {
        setAmiActive(false);
    }
}
