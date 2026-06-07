package com.sanhiruzu.ami.api;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

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
        return () -> openPatchouliBook(bookId == null ? List.of() : List.of(bookId), null);
    }

    // ── GuideME ───────────────────────────────────────────────────────────────

    /**
     * Opens a GuideME guide (used by Applied Energistics 2 and other mods)
     * to the guide's root page.
     *
     * @param bookId the guide's ResourceLocation (e.g. {@code ae2:guide})
     */
    public static Runnable guideME(ResourceLocation bookId) {
        return () -> tryOpenGuideME(bookId, null);
    }

    /**
     * Opens a GuideME guide to a specific page.
     *
     * @param bookId the guide's ResourceLocation
     * @param pageId the page path within the guide namespace
     */
    public static Runnable guideME(ResourceLocation bookId, String pageId) {
        return () -> tryOpenGuideME(bookId, pageId);
    }

    /**
     * Attempts to open a GuideME book or page immediately.
     *
     * @return {@code true} when a compatible GuideME open method was invoked
     */
    public static boolean tryOpenGuideME(ResourceLocation bookId, String pageId) {
        return openGuideMEBook(bookId, pageId);
    }

    // ── Modonomicon ──────────────────────────────────────────────────────────

    /**
     * Opens a Modonomicon book to a specific category/entry page.
     *
     * @param bookId the book's ResourceLocation (e.g. {@code spectrum:guidebook})
     * @param categoryId the category id within the book namespace
     * @param entryId the entry id within the book namespace
     * @param page zero-based page number
     */
    public static Runnable modonomicon(ResourceLocation bookId, ResourceLocation categoryId,
                                       ResourceLocation entryId, int page) {
        return () -> openModonomiconBook(bookId, categoryId, entryId, page);
    }

    /**
     * Opens a Modonomicon book to its default screen.
     *
     * @param bookId the book's ResourceLocation
     */
    public static Runnable modonomicon(ResourceLocation bookId) {
        return () -> openModonomiconBook(bookId, null, null, 0);
    }

    // ── Silent Gear ──────────────────────────────────────────────────────────

    /**
     * Opens Silent Gear's material book to a specific material detail page.
     *
     * @param materialId the Silent Gear material id, or {@code null} for the material book home
     */
    public static Runnable silentGearMaterialBook(ResourceLocation materialId) {
        return () -> openSilentGearMaterialBook(materialId);
    }

    // ── Resource-backed custom books ─────────────────────────────────────────

    /**
     * Opens a Mantle/Tinkers-style book to a named page when the book data is available.
     */
    public static Runnable mantleBook(ResourceLocation bookId, String pageId) {
        return () -> openMantleBook(bookId, pageId);
    }

    /**
     * Opens Alex's Mobs' animal dictionary to the requested page JSON.
     */
    public static Runnable alexsMobsAnimalDictionary(String pageJson) {
        return () -> openAlexsMobsAnimalDictionary(pageJson);
    }

    /**
     * Opens Alex's Caves' cave book to the requested entry JSON.
     */
    public static Runnable alexsCavesBook(ResourceLocation pageJson) {
        return () -> openAlexsCavesBook(pageJson);
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
        List<ResourceLocation> candidates = patchouliBookCandidates(deduped);

        if (pageId != null && !pageId.isBlank()) {
            for (ResourceLocation bookId : candidates) {
                if (openPatchouliBookEntry(bookId, pageId)) {
                    return;
                }
            }
            for (ResourceLocation bookId : candidates) {
                if (openPatchouliBook(bookId)) {
                    return;
                }
            }
            return;
        }

        for (ResourceLocation bookId : candidates) {
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
        openPatchouliBook(List.of(bookId), pageId);
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

            if (tryInvokePatchouliOpen(api, "openBookGUI", bookId, bookId)) {
                return true;
            }
            if (tryInvokePatchouliOpen(api, "openBookGUI", bookId, mc.player, bookId)) {
                return true;
            }
            if (tryInvokePatchouliOpen(api, "openBookGui", bookId, mc.player, bookId)) {
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
            for (ResourceLocation entryId : patchouliEntryCandidates(bookId, pageId)) {
                if (tryInvokePatchouliOpen(api, "openBookEntry", bookId, mc.player, bookId, entryId, 0) ||
                        tryInvokePatchouliOpen(api, "openBookEntry", bookId, bookId, entryId, 0) ||
                        tryInvokePatchouliOpen(api, "openBookGui", bookId, mc.player, bookId, entryId, 0) ||
                        tryInvokePatchouliOpen(api, "openBookGUI", bookId, mc.player, bookId, entryId, 0) ||
                        tryInvokePatchouliOpen(api, "openBookGui", bookId, bookId, entryId, 0) ||
                        tryInvokePatchouliOpen(api, "openBookGUI", bookId, bookId, entryId, 0)) {
                    return true;
                }
            }
            return false;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Patchouli unavailable for guide entry open: " + bookId, e);
        }
        return false;
    }

    static ResourceLocation patchouliEntryId(ResourceLocation bookId, String pageId) {
        if (pageId != null && pageId.contains(":")) {
            ResourceLocation parsed = ResourceLocation.tryParse(pageId);
            if (parsed != null) {
                return parsed;
            }
        }
        return ResourceLocation.fromNamespaceAndPath(bookId.getNamespace(), pageId == null ? "" : pageId);
    }

    static List<ResourceLocation> patchouliEntryCandidates(ResourceLocation bookId, String pageId) {
        if (bookId == null || pageId == null || pageId.isBlank()) {
            return List.of();
        }

        ResourceLocation requested = patchouliEntryId(bookId, pageId);
        LinkedHashSet<ResourceLocation> candidates = new LinkedHashSet<>();
        candidates.add(requested);

        Map<?, ?> entries = patchouliBookEntries(bookId);
        if (entries.isEmpty() || entries.containsKey(requested)) {
            return List.copyOf(candidates);
        }

        String requestedPath = requested.getPath();
        entries.keySet().stream()
                .filter(ResourceLocation.class::isInstance)
                .map(ResourceLocation.class::cast)
                .map(entryId -> new ScoredEntryCandidate(entryId, scorePatchouliEntryCandidate(requestedPath, entryId.getPath())))
                .filter(candidate -> candidate.score() > 0)
                .sorted((a, b) -> {
                    int byScore = Integer.compare(b.score(), a.score());
                    return byScore != 0 ? byScore : a.entryId().toString().compareTo(b.entryId().toString());
                })
                .map(ScoredEntryCandidate::entryId)
                .forEach(candidates::add);

        return List.copyOf(candidates);
    }

    public static boolean patchouliEntryVisible(ResourceLocation bookId, String pageId) {
        if (bookId == null || pageId == null || pageId.isBlank()) {
            return true;
        }
        try {
            Map<?, ?> entries = patchouliBookEntries(bookId);
            if (entries.isEmpty()) {
                return true;
            }
            for (ResourceLocation entryId : patchouliEntryCandidates(bookId, pageId)) {
                Object entry = entries.get(entryId);
                if (entry != null) {
                    return patchouliEntryVisible(entry);
                }
            }
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Patchouli entry visibility unavailable for " + bookId + " / " + pageId, e);
        }
        return true;
    }

    private static boolean patchouliEntryVisible(Object entry) {
        try {
            tryInvokeNoArg(entry, "updateLockStatus");
            Boolean hidden = booleanNoArg(entry, "shouldHide");
            if (Boolean.TRUE.equals(hidden)) {
                return false;
            }
            Boolean locked = booleanNoArg(entry, "isLocked");
            if (Boolean.TRUE.equals(locked)) {
                return false;
            }
            Boolean canAdd = booleanNoArg(entry, "canAdd");
            return !Boolean.FALSE.equals(canAdd);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Patchouli entry visibility check failed", e);
            return true;
        }
    }

    private static void tryInvokeNoArg(Object target, String methodName) throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(methodName);
        method.invoke(target);
    }

    private static Boolean booleanNoArg(Object target, String methodName) throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(methodName);
        Object value = method.invoke(target);
        return value instanceof Boolean bool ? bool : null;
    }

    static int scorePatchouliEntryCandidate(String requestedPath, String entryPath) {
        if (requestedPath == null || requestedPath.isBlank() || entryPath == null || entryPath.isBlank()) {
            return 0;
        }
        String requested = requestedPath.toLowerCase(java.util.Locale.ROOT);
        String entry = entryPath.toLowerCase(java.util.Locale.ROOT);
        if (entry.equals(requested)) return 1000;
        if (entry.endsWith("/" + requested)) return 900;
        if (requested.endsWith("/" + entry)) return 850;

        String requestedLeaf = leafPathSegment(requested);
        String entryLeaf = leafPathSegment(entry);
        if (!requestedLeaf.isBlank() && entryLeaf.equals(requestedLeaf)) return 700;
        if (!requestedLeaf.isBlank() && entry.contains(requestedLeaf)) return 300;

        int score = 0;
        for (String token : requested.split("[_/.-]+")) {
            if (token.length() < 4 || "guide".equals(token) || "page".equals(token)) {
                continue;
            }
            if (entry.contains(token)) {
                score += 25;
            }
        }
        return score;
    }

    private static String leafPathSegment(String path) {
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    private static List<ResourceLocation> patchouliBookCandidates(Iterable<ResourceLocation> requestedBooks) {
        LinkedHashSet<ResourceLocation> candidates = new LinkedHashSet<>();
        if (requestedBooks != null) {
            for (ResourceLocation requestedBook : requestedBooks) {
                if (requestedBook == null) {
                    continue;
                }
                candidates.add(requestedBook);
                candidates.addAll(installedPatchouliBookCandidates(requestedBook));
            }
        }
        return List.copyOf(candidates);
    }

    private static List<ResourceLocation> installedPatchouliBookCandidates(ResourceLocation requestedBook) {
        if (requestedBook == null) {
            return List.of();
        }
        Map<?, ?> books = patchouliBookRegistryBooks();
        if (books.isEmpty()) {
            return List.of();
        }

        List<ResourceLocation> exactNamespace = new ArrayList<>();
        List<ResourceLocation> guideLike = new ArrayList<>();
        for (Object key : books.keySet()) {
            if (!(key instanceof ResourceLocation bookId)) {
                continue;
            }
            if (!bookId.getNamespace().equals(requestedBook.getNamespace())) {
                continue;
            }
            if (bookId.equals(requestedBook)) {
                exactNamespace.add(bookId);
                continue;
            }
            String path = bookId.getPath();
            if (path.contains("guide") || path.contains("book") || path.contains("chronicle")) {
                guideLike.add(bookId);
            }
        }

        exactNamespace.addAll(guideLike);
        return exactNamespace;
    }

    private static Map<?, ?> patchouliBookRegistryBooks() {
        try {
            Class<?> registryClass = Class.forName("vazkii.patchouli.common.book.BookRegistry");
            Object registry = registryClass.getField("INSTANCE").get(null);
            Object books = registryClass.getField("books").get(registry);
            if (books instanceof Map<?, ?> map) {
                return map;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Patchouli book registry unavailable", e);
        }
        return Map.of();
    }

    private static Map<?, ?> patchouliBookEntries(ResourceLocation bookId) {
        Object book = patchouliBookRegistryBooks().get(bookId);
        if (book == null) {
            return Map.of();
        }
        try {
            Object contents = book.getClass().getMethod("getContents").invoke(book);
            Object entries = contents.getClass().getField("entries").get(contents);
            if (entries instanceof Map<?, ?> map) {
                return map;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Patchouli book entries unavailable for " + bookId, e);
        }
        return Map.of();
    }

    private static boolean tryInvokePatchouliOpen(Object api, String methodName, ResourceLocation expectedBook, Object... args) {
        if (!tryInvokePatchouli(api, methodName, args)) {
            return false;
        }
        Boolean open = isPatchouliBookOpen(api, expectedBook);
        return open == null || open;
    }

    private record ScoredEntryCandidate(ResourceLocation entryId, int score) {
    }

    private static Boolean isPatchouliBookOpen(Object api, ResourceLocation expectedBook) {
        if (api == null || expectedBook == null) {
            return null;
        }
        try {
            Method method = api.getClass().getMethod("getOpenBookGui");
            Object result = method.invoke(api);
            if (result instanceof ResourceLocation openBook) {
                return expectedBook.equals(openBook);
            }
            return false;
        } catch (NoSuchMethodException e) {
            return null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Patchouli open book check failed", e);
            return null;
        }
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

    private static boolean openGuideMEBook(ResourceLocation bookId, String pageId) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return false;

            Class<?> guidesCommon = Class.forName("guideme.GuidesCommon");

            if (pageId != null && !pageId.isBlank()) {
                ResourceLocation pageLocation = guideMEPageLocation(bookId, pageId);
                Object pageAnchor = guideMEPageAnchor(pageLocation);
                for (Method m : guidesCommon.getMethods()) {
                    if ("openGuide".equals(m.getName()) && m.getParameterCount() == 3) {
                        Object pageArg = guideMEPageArgument(m.getParameterTypes()[2], pageLocation, pageAnchor);
                        if (pageArg != null) {
                            m.invoke(null, mc.player, bookId, pageArg);
                            return true;
                        }
                    }
                }
            }

            for (Method m : guidesCommon.getMethods()) {
                if ("openGuide".equals(m.getName()) && m.getParameterCount() == 2) {
                    m.invoke(null, mc.player, bookId);
                    return true;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: GuideME unavailable for guide open: " + bookId, e);
        }
        return false;
    }

    private static Object guideMEPageAnchor(ResourceLocation pageLocation) {
        if (pageLocation == null) {
            return null;
        }
        try {
            Class<?> pageAnchorClass = Class.forName("guideme.PageAnchor");
            return pageAnchorClass.getMethod("page", ResourceLocation.class).invoke(null, pageLocation);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: GuideME page anchor unavailable for " + pageLocation, e);
            return null;
        }
    }

    static ResourceLocation guideMEPageLocation(ResourceLocation bookId, String pageId) {
        ResourceLocation requested = ResourceLocation.fromNamespaceAndPath(bookId.getNamespace(), pageId);
        ResourceLocation runtimeMatch = guideMERuntimePageLocation(bookId, pageId);
        return runtimeMatch == null ? requested : runtimeMatch;
    }

    private static ResourceLocation guideMERuntimePageLocation(ResourceLocation bookId, String pageId) {
        if (bookId == null || pageId == null || pageId.isBlank()) {
            return null;
        }
        try {
            Class<?> proxyClass = Class.forName("guideme.internal.GuideMEProxy");
            Object proxy = proxyClass.getMethod("instance").invoke(null);
            Method method = proxyClass.getMethod("getAvailablePages", ResourceLocation.class);
            Object result = method.invoke(proxy, bookId);
            if (!(result instanceof Stream<?> stream)) {
                return null;
            }
            try (stream) {
                String normalizedPageId = normalizeGuideMEPagePath(pageId);
                return stream
                        .filter(ResourceLocation.class::isInstance)
                        .map(ResourceLocation.class::cast)
                        .filter(candidate -> isGuideMEPageMatch(candidate, normalizedPageId))
                        .findFirst()
                        .orElse(null);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: GuideME available-page lookup failed for " + bookId + " / " + pageId, e);
            return null;
        }
    }

    static boolean isGuideMEPageMatch(ResourceLocation candidate, String normalizedPageId) {
        if (candidate == null || normalizedPageId == null || normalizedPageId.isBlank()) {
            return false;
        }
        String candidatePath = normalizeGuideMEPagePath(candidate.getPath());
        return candidatePath.equals(normalizedPageId)
                || candidatePath.equals("ae2guide/" + normalizedPageId)
                || candidatePath.endsWith("/" + normalizedPageId);
    }

    private static String normalizeGuideMEPagePath(String raw) {
        String value = raw == null ? "" : raw.replace('\\', '/').trim();
        if (value.endsWith(".md")) {
            value = value.substring(0, value.length() - ".md".length());
        }
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        return value;
    }

    private static Object guideMEPageArgument(Class<?> parameterType, ResourceLocation pageLocation, Object pageAnchor) {
        if (parameterType == null) {
            return null;
        }
        if (pageAnchor != null && parameterType.isInstance(pageAnchor)) {
            return pageAnchor;
        }
        if (pageLocation != null && parameterType.isInstance(pageLocation)) {
            return pageLocation;
        }
        return null;
    }

    private static void openModonomiconBook(ResourceLocation bookId, ResourceLocation categoryId,
                                            ResourceLocation entryId, int page) {
        if (bookId == null) {
            return;
        }
        try {
            Class<?> addressClass = Class.forName("com.klikli_dev.modonomicon.client.gui.book.BookAddress");
            Object address;
            if (categoryId != null && entryId != null) {
                address = addressClass.getMethod(
                                "ignoreSaved",
                                ResourceLocation.class,
                                ResourceLocation.class,
                                ResourceLocation.class,
                                int.class
                        )
                        .invoke(null, bookId, categoryId, entryId, Math.max(0, page));
            } else {
                address = addressClass.getMethod("defaultFor", ResourceLocation.class).invoke(null, bookId);
            }

            Class<?> managerClass = Class.forName("com.klikli_dev.modonomicon.client.gui.BookGuiManager");
            Object manager = managerClass.getMethod("get").invoke(null);
            managerClass.getMethod("openBook", addressClass).invoke(manager, address);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Modonomicon unavailable for guide open: " + bookId, e);
        }
    }

    private static void openSilentGearMaterialBook(ResourceLocation materialId) {
        try {
            Minecraft mc = Minecraft.getInstance();
            Object screen;
            if (materialId != null) {
                Class<?> registriesClass = Class.forName("net.silentchaos512.gear.setup.SgRegistries");
                Object materialManager = registriesClass.getField("MATERIAL").get(null);
                Object material = materialManager.getClass().getMethod("get", ResourceLocation.class).invoke(materialManager, materialId);
                if (material == null) {
                    screen = newSilentGearMaterialBookScreen();
                } else {
                    Class<?> materialClass = Class.forName("net.silentchaos512.gear.api.material.Material");
                    Class<?> detailsScreenClass = Class.forName("net.silentchaos512.gear.client.gui.book.MaterialDetailsBookScreen");
                    screen = detailsScreenClass
                            .getConstructor(net.minecraft.client.gui.screens.Screen.class, materialClass)
                            .newInstance(null, material);
                }
            } else {
                screen = newSilentGearMaterialBookScreen();
            }
            mc.execute(() -> mc.setScreen((net.minecraft.client.gui.screens.Screen) screen));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Silent Gear material book unavailable for guide open: " + materialId, e);
        }
    }

    private static Object newSilentGearMaterialBookScreen() throws ReflectiveOperationException {
        Class<?> screenClass = Class.forName("net.silentchaos512.gear.client.gui.book.MaterialBookScreen");
        return screenClass.getConstructor().newInstance();
    }

    private static void openMantleBook(ResourceLocation bookId, String pageId) {
        if (bookId == null) {
            return;
        }
        try {
            Object book = mantleBookData(bookId);
            if (book == null) {
                return;
            }
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
            Object title = componentClass.getMethod("literal", String.class).invoke(null, bookId.toString());
            for (Method method : book.getClass().getMethods()) {
                if (!"openGui".equals(method.getName()) || method.getParameterCount() != 3) {
                    continue;
                }
                Class<?>[] types = method.getParameterTypes();
                if (componentClass.isAssignableFrom(types[0]) && types[1] == String.class) {
                    method.invoke(book, title, pageId == null ? "" : pageId, (java.util.function.Consumer<String>) ignored -> {
                    });
                    return;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Mantle book unavailable for guide open: " + bookId, e);
        }
    }

    private static Object mantleBookData(ResourceLocation bookId) throws ReflectiveOperationException {
        String path = bookId.getPath();
        if ("tconstruct".equals(bookId.getNamespace())) {
            String fieldName = switch (path) {
                case "materials_and_you" -> "MATERIALS_AND_YOU";
                case "puny_smelting" -> "PUNY_SMELTING";
                case "mighty_smelting" -> "MIGHTY_SMELTING";
                case "tinkers_gadgetry" -> "TINKERS_GADGETRY";
                case "fantastic_foundry" -> "FANTASTIC_FOUNDRY";
                case "encyclopedia" -> "ENCYCLOPEDIA";
                default -> "";
            };
            if (!fieldName.isBlank()) {
                Class<?> tinkerBookClass = Class.forName("slimeknights.tconstruct.library.client.book.TinkerBook");
                tryInvokeStaticNoArg(tinkerBookClass, "initBook");
                return tinkerBookClass.getField(fieldName).get(null);
            }
        }
        if ("tinkers_reforged".equals(bookId.getNamespace()) && "reforging_guide".equals(path)) {
            Class<?> bookClass = Class.forName("mrthomas20121.tinkers_reforged.client.TinkersReforgedBook");
            tryInvokeStaticNoArg(bookClass, "initBook");
            return bookClass.getField("BOOK").get(null);
        }
        return null;
    }

    private static void tryInvokeStaticNoArg(Class<?> target, String methodName) {
        try {
            target.getMethod(methodName).invoke(null);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            LOGGER.log(Level.FINE, "AMI: Optional static guide init failed for " + target.getName(), ignored);
        }
    }

    private static void openAlexsMobsAnimalDictionary(String pageJson) {
        try {
            Minecraft mc = Minecraft.getInstance();
            Class<?> screenClass = Class.forName("com.github.alexthe666.alexsmobs.client.gui.GUIAnimalDictionary");
            net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath("alexsmobs", "animal_dictionary")
            );
            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item);
            Object screen = pageJson == null || pageJson.isBlank()
                    ? screenClass.getConstructor(net.minecraft.world.item.ItemStack.class).newInstance(stack)
                    : screenClass.getConstructor(net.minecraft.world.item.ItemStack.class, String.class).newInstance(stack, pageJson);
            mc.execute(() -> mc.setScreen((net.minecraft.client.gui.screens.Screen) screen));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Alex's Mobs dictionary unavailable for guide open: " + pageJson, e);
        }
    }

    private static void openAlexsCavesBook(ResourceLocation pageJson) {
        try {
            Minecraft mc = Minecraft.getInstance();
            Class<?> screenClass = Class.forName("com.github.alexmodguy.alexscaves.client.gui.book.CaveBookScreen");
            Object screen = screenClass.getConstructor().newInstance();
            if (pageJson != null) {
                try {
                    screenClass.getMethod("attemptChangePage", ResourceLocation.class, boolean.class)
                            .invoke(screen, pageJson, false);
                } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                    LOGGER.log(Level.FINE, "AMI: Alex's Caves exact page selection failed for " + pageJson, ignored);
                }
            }
            mc.execute(() -> mc.setScreen((net.minecraft.client.gui.screens.Screen) screen));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Alex's Caves book unavailable for guide open: " + pageJson, e);
        }
    }
}
