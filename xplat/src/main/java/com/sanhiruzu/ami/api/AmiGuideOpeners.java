package com.sanhiruzu.ami.api;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
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
     * Opens a Patchouli book by trying each candidate book ID until one opens.
     * This handles module split/version drift where the book ID changes (for example,
     * {@code apotheosis:apoth_chronicle} vs potential legacy values).
     *
     * @param bookIds candidate book IDs to try in order
     * @param pageId the entry path within the book namespace (e.g. {@code basics/mana_spreader})
     */
    public static Runnable patchouli(Collection<ResourceLocation> bookIds, String pageId) {
        return () -> openPatchouliBook(bookIds, pageId);
    }

    /**
     * Opens a Patchouli-based guidebook to its cover or last-visited page.
     *
     * @param bookId the book's ResourceLocation
     */
    public static Runnable patchouli(ResourceLocation bookId) {
        return () -> openPatchouliBook(bookId);
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

    private static void openPatchouliBook(Iterable<ResourceLocation> bookIds, String pageId) {
        if (bookIds == null) {
            return;
        }
        LinkedHashSet<ResourceLocation> deduped = new LinkedHashSet<>();
        for (ResourceLocation bookId : bookIds) {
            if (bookId == null) {
                continue;
            }
            deduped.add(bookId);
        }

        if (pageId != null && !pageId.isBlank()) {
            for (ResourceLocation bookId : deduped) {
                if (openPatchouliBookEntry(bookId, pageId)) {
                    return;
                }
            }
            for (ResourceLocation bookId : deduped) {
                if (openPatchouliBook(bookId)) {
                    return;
                }
            }
            return;
        }

        for (ResourceLocation bookId : deduped) {
            if (openPatchouliBook(bookId)) {
                return;
            }
        }
    }

    private static void openPatchouliBook(ResourceLocation bookId, String pageId) {
        if (bookId == null) {
            return;
        }
        if (pageId == null || pageId.isBlank()) {
            openPatchouliBook(bookId);
            return;
        }
        if (openPatchouliBookEntry(bookId, pageId)) {
            return;
        }
        openPatchouliBook(bookId);
    }

    private static boolean openPatchouliBook(ResourceLocation bookId) {
        if (bookId == null) {
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("vazkii.patchouli.api.PatchouliAPI");
            Object api = apiClass.getMethod("get").invoke(null);
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return false;

            if (tryInvokePatchouli(api, "openBookGUI", bookId)) {
                return true;
            }
            if (tryInvokePatchouli(api, "openBookGUI", mc.player, bookId)) {
                return true;
            }
            if (tryInvokePatchouli(api, "openBookGui", mc.player, bookId)) {
                return true;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Patchouli unavailable for guide open: " + bookId, e);
        }
        return false;
    }

    private static boolean openPatchouliBookEntry(ResourceLocation bookId, String pageId) {
        if (bookId == null || pageId == null || pageId.isBlank()) {
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("vazkii.patchouli.api.PatchouliAPI");
            Object api = apiClass.getMethod("get").invoke(null);
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return false;
            }
            ResourceLocation entryId = ResourceLocation.tryParse(pageId);
            if (entryId == null) {
                entryId = ResourceLocation.fromNamespaceAndPath(bookId.getNamespace(), pageId);
            }
            return tryInvokePatchouli(api, "openBookEntry", mc.player, bookId, entryId, 0) ||
                    tryInvokePatchouli(api, "openBookEntry", bookId, entryId, 0) ||
                    tryInvokePatchouli(api, "openBookGui", mc.player, bookId, entryId, 0) ||
                    tryInvokePatchouli(api, "openBookGUI", mc.player, bookId, entryId, 0) ||
                    tryInvokePatchouli(api, "openBookGui", bookId, entryId, 0) ||
                    tryInvokePatchouli(api, "openBookGUI", bookId, entryId, 0);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Patchouli unavailable for guide entry open: " + bookId, e);
        }
        return false;
    }

    private static boolean tryInvokePatchouli(Object api, String methodName, Object... args) {
        outer:
        for (Method method : api.getClass().getMethods()) {
            if (!methodName.equals(method.getName())) {
                continue;
            }
            if (method.getParameterCount() != args.length) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (!isPatchouliArgAssignable(parameterTypes[i], arg)) {
                    continue outer;
                }
            }
            try {
                method.invoke(api, args);
                return true;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                LOGGER.log(Level.FINE, "AMI: Patchouli method invocation failed for " + methodName, ignored);
            }
        }
        return false;
    }

    private static boolean isPatchouliArgAssignable(Class<?> parameterType, Object arg) {
        if (arg == null) {
            return !parameterType.isPrimitive();
        }
        if (!parameterType.isPrimitive()) {
            return parameterType.isInstance(arg);
        }
        if (parameterType == boolean.class && arg instanceof Boolean) {
            return true;
        }
        if (parameterType == byte.class && arg instanceof Byte) {
            return true;
        }
        if (parameterType == short.class && arg instanceof Short) {
            return true;
        }
        if (parameterType == int.class && arg instanceof Integer) {
            return true;
        }
        if (parameterType == long.class && arg instanceof Long) {
            return true;
        }
        if (parameterType == float.class && arg instanceof Float) {
            return true;
        }
        if (parameterType == double.class && arg instanceof Double) {
            return true;
        }
        if (parameterType == char.class && arg instanceof Character) {
            return true;
        }
        return false;
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
