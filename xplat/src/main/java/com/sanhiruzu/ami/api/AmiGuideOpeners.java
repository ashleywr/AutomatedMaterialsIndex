package com.sanhiruzu.ami.api;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory methods for common guidebook open actions.
 * <p>
 * Use these when building {@link AmiGuideDocument}s via
 * {@link AmiGuideDocument.Builder#openAction(Runnable)}, so AMI can open the
 * guide when the player clicks a guide result.
 * <p>
 * Each factory returns a {@link Runnable} that silently does nothing when the
 * target mod is not installed.
 *
 * <pre>{@code
 * AmiGuideDocument.builder(id, "patchouli", modId, title)
 *     .bookId(bookId)
 *     .pageId(pageId)
 *     .openAction(AmiGuideOpeners.patchouli(bookId, pageId))
 *     .build();
 * }</pre>
 */
public final class AmiGuideOpeners {
    private static final Logger LOGGER = Logger.getLogger(AmiGuideOpeners.class.getName());

    private AmiGuideOpeners() {
    }

    // ── Patchouli ─────────────────────────────────────────────────────────────

    /**
     * Opens a Patchouli-based guidebook to a specific entry page.
     * Falls back to opening the book cover if the entry-specific method is not found.
     *
     * @param bookId the book's ResourceLocation (e.g. {@code botania:lexicon})
     * @param pageId the entry path within the book namespace (e.g. {@code basics/mana_spreader})
     */
    public static Runnable patchouli(ResourceLocation bookId, String pageId) {
        return () -> openPatchouliBook(bookId, pageId);
    }

    /**
     * Opens a Patchouli-based guidebook to its cover or last-visited page.
     *
     * @param bookId the book's ResourceLocation
     */
    public static Runnable patchouli(ResourceLocation bookId) {
        return () -> openPatchouliBook(bookId, null);
    }

    // ── GuideME ───────────────────────────────────────────────────────────────

    /**
     * Opens a GuideME guide (used by Applied Energistics 2 and other mods)
     * to the guide's root page.
     *
     * @param bookId the guide's ResourceLocation (e.g. {@code ae2:guide})
     */
    public static Runnable guideME(ResourceLocation bookId) {
        return () -> openGuideMEBook(bookId, null);
    }

    /**
     * Opens a GuideME guide to a specific page.
     *
     * @param bookId the guide's ResourceLocation
     * @param pageId the page path within the guide namespace
     */
    public static Runnable guideME(ResourceLocation bookId, String pageId) {
        return () -> openGuideMEBook(bookId, pageId);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static void openPatchouliBook(ResourceLocation bookId, String pageId) {
        try {
            Class<?> apiClass = Class.forName("vazkii.patchouli.api.PatchouliAPI");
            Object api = apiClass.getMethod("get").invoke(null);
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            if (pageId != null && !pageId.isBlank()) {
                ResourceLocation entryId = ResourceLocation.fromNamespaceAndPath(bookId.getNamespace(), pageId);
                for (Method m : api.getClass().getMethods()) {
                    if ("openBookGui".equals(m.getName()) && m.getParameterCount() == 4) {
                        m.invoke(api, mc.player, bookId, entryId, 0);
                        return;
                    }
                }
            }

            for (Method m : api.getClass().getMethods()) {
                if ("openBookGui".equals(m.getName()) && m.getParameterCount() == 2) {
                    m.invoke(api, mc.player, bookId);
                    return;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Patchouli unavailable for guide open: " + bookId, e);
        }
    }

    private static void openGuideMEBook(ResourceLocation bookId, String pageId) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            Class<?> guidesCommon = Class.forName("guideme.GuidesCommon");

            if (pageId != null && !pageId.isBlank()) {
                ResourceLocation pageLocation = ResourceLocation.fromNamespaceAndPath(bookId.getNamespace(), pageId);
                for (Method m : guidesCommon.getMethods()) {
                    if ("openGuide".equals(m.getName()) && m.getParameterCount() == 3) {
                        m.invoke(null, mc.player, bookId, pageLocation);
                        return;
                    }
                }
            }

            for (Method m : guidesCommon.getMethods()) {
                if ("openGuide".equals(m.getName()) && m.getParameterCount() == 2) {
                    m.invoke(null, mc.player, bookId);
                    return;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: GuideME unavailable for guide open: " + bookId, e);
        }
    }
}
