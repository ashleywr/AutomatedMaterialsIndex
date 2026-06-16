package com.sanhiruzu.ami.api;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Method;
import java.util.*;
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
     * @param bookId the book's Identifier (e.g. {@code botania:lexicon})
     * @param pageId the entry path within the book namespace (e.g. {@code basics/mana_spreader})
     */
    public static Runnable patchouli(Identifier bookId, String pageId) {
        return () -> openPatchouliBook(bookId, pageId);
    }

    /**
     * Opens a Patchouli book by trying each candidate book ID until one opens.
     * This handles module split/version drift where the book ID changes (for example,
     * {@code apotheosis:apoth_chronicle} vs potential legacy values).
     *
     * @param bookIds candidate book IDs to try in order
     * @param pageId  the entry path within the book namespace (e.g. {@code basics/mana_spreader})
     */
    public static Runnable patchouli(Collection<Identifier> bookIds, String pageId) {
        return () -> openPatchouliBook(bookIds, pageId);
    }

    /**
     * Opens a Patchouli-based guidebook to its cover or last-visited page.
     *
     * @param bookId the book's Identifier
     */
    public static Runnable patchouli(Identifier bookId) {
        return () -> openPatchouliBook(bookId == null ? List.of() : List.of(bookId), null);
    }

    // ── GuideME ───────────────────────────────────────────────────────────────

    /**
     * Opens a GuideME guide (used by Applied Energistics 2 and other mods)
     * to the guide's root page.
     *
     * @param bookId the guide's Identifier (e.g. {@code ae2:guide})
     */
    public static Runnable guideME(Identifier bookId) {
        return () -> tryOpenGuideME(bookId, null);
    }

    /**
     * Opens a GuideME guide to a specific page.
     *
     * @param bookId the guide's Identifier
     * @param pageId the page path within the guide namespace
     */
    public static Runnable guideME(Identifier bookId, String pageId) {
        return () -> tryOpenGuideME(bookId, pageId);
    }

    /**
     * Attempts to open a GuideME book or page immediately.
     *
     * @return {@code true} when a compatible GuideME open method was invoked
     */
    public static boolean tryOpenGuideME(Identifier bookId, String pageId) {
        return openGuideMEBook(bookId, pageId);
    }

    // ── Modonomicon ──────────────────────────────────────────────────────────

    /**
     * Opens a Modonomicon book to a specific category/entry page.
     *
     * @param bookId     the book's Identifier (e.g. {@code spectrum:guidebook})
     * @param categoryId the category id within the book namespace
     * @param entryId    the entry id within the book namespace
     * @param page       zero-based page number
     */
    public static Runnable modonomicon(Identifier bookId, Identifier categoryId,
                                       Identifier entryId, int page) {
        return () -> openModonomiconBook(bookId, categoryId, entryId, page);
    }

    /**
     * Opens a Modonomicon book to its default screen.
     *
     * @param bookId the book's Identifier
     */
    public static Runnable modonomicon(Identifier bookId) {
        return () -> openModonomiconBook(bookId, null, null, 0);
    }

    // ── Silent Gear ──────────────────────────────────────────────────────────

    /**
     * Opens Silent Gear's material book to a specific material detail page.
     *
     * @param materialId the Silent Gear material id, or {@code null} for the material book home
     */
    public static Runnable silentGearMaterialBook(Identifier materialId) {
        return () -> openSilentGearMaterialBook(materialId);
    }

    /**
     * Attempts to open Silent Gear's material book immediately.
     *
     * @return {@code true} when a compatible Silent Gear material-book screen was created
     */
    public static boolean tryOpenSilentGearMaterialBook(Identifier materialId) {
        return openSilentGearMaterialBook(materialId);
    }

    // ── Malum ─────────────────────────────────────────────────────────────────

    /**
     * Opens the Malum codex to a specific entry by identifier.
     * <p>
     * Tries to locate the {@code BookEntry} via the progression screen's entry list
     * and calls {@code CodexEntryScreen.openScreen(entry)}. Falls back to opening
     * the codex overview if the entry cannot be resolved.
     *
     * @param identifier the entry identifier (e.g. {@code "spirit_collection"})
     */
    public static Runnable malumCodexEntry(String identifier) {
        return () -> openMalumCodexEntry(identifier);
    }

    // ── Critters n' Crawlers ─────────────────────────────────────────────────

    /**
     * Opens the Critters n' Crawlers Field Guide to a specific creature entry.
     * <p>
     * In singleplayer the creature screen is opened by running the mod's
     * {@code FieldGuide{Creature}OpenProcedure} on the integrated server thread
     * (required because CnC uses Minecraft's container menu system).
     * In multiplayer the guide opens to its cover screen as a fallback.
     *
     * @param creatureClassSuffix PascalCase creature suffix (e.g. {@code "BlackBear"})
     */
    public static Runnable cncFieldGuideCreature(String creatureClassSuffix) {
        return () -> openCncCreatureScreen(creatureClassSuffix);
    }

    // ── Hexerei ───────────────────────────────────────────────────────────────

    /**
     * Opens a Hexerei guide book screen.
     * <p>
     * {@code bookId} selects the book (e.g. {@code hexerei:book_of_shadows}).
     * {@code pageId} is the page_location string from the book's chapter listing
     * (e.g. {@code hexerei:book_of_shadows/book_pages/entities/entities_crow_page_1}).
     * Precise page navigation is attempted via reflection; if the constructor does
     * not accept a page argument the book opens at its default page.
     *
     * @param bookId the Hexerei book item id
     * @param pageId the page_location string, or {@code null} to open the cover
     */
    public static Runnable hexereiBook(Identifier bookId, String pageId) {
        return () -> openHexereiBook(bookId, pageId);
    }

    // ── Resource-backed custom books ─────────────────────────────────────────

    /**
     * Opens a Mantle/Tinkers-style book to a named page when the book data is available.
     */
    public static Runnable mantleBook(Identifier bookId, String pageId) {
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
    public static Runnable alexsCavesBook(Identifier pageJson) {
        return () -> openAlexsCavesBook(pageJson);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static void openPatchouliBook(Iterable<Identifier> bookIds, String pageId) {
        if (bookIds == null) {
            return;
        }
        LinkedHashSet<Identifier> deduped = new LinkedHashSet<>();
        for (Identifier bookId : bookIds) {
            if (bookId == null) {
                continue;
            }
            deduped.add(bookId);
        }
        List<Identifier> candidates = patchouliBookCandidates(deduped);

        if (pageId != null && !pageId.isBlank()) {
            for (Identifier bookId : candidates) {
                if (openPatchouliBookEntry(bookId, pageId)) {
                    return;
                }
            }
            for (Identifier bookId : candidates) {
                if (openPatchouliBook(bookId)) {
                    return;
                }
            }
            return;
        }

        for (Identifier bookId : candidates) {
            if (openPatchouliBook(bookId)) {
                return;
            }
        }
    }

    private static void openPatchouliBook(Identifier bookId, String pageId) {
        if (bookId == null) {
            return;
        }
        if (pageId == null || pageId.isBlank()) {
            openPatchouliBook(bookId);
            return;
        }
        openPatchouliBook(List.of(bookId), pageId);
    }

    private static boolean openPatchouliBook(Identifier bookId) {
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

    private static boolean openPatchouliBookEntry(Identifier bookId, String pageId) {
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
            for (Identifier entryId : patchouliEntryCandidates(bookId, pageId)) {
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

    static Identifier patchouliEntryId(Identifier bookId, String pageId) {
        if (pageId != null && pageId.contains(":")) {
            Identifier parsed = Identifier.tryParse(pageId);
            if (parsed != null) {
                return parsed;
            }
        }
        return Identifier.fromNamespaceAndPath(bookId.getNamespace(), pageId == null ? "" : pageId);
    }

    static List<Identifier> patchouliEntryCandidates(Identifier bookId, String pageId) {
        if (bookId == null || pageId == null || pageId.isBlank()) {
            return List.of();
        }

        Identifier requested = patchouliEntryId(bookId, pageId);
        LinkedHashSet<Identifier> candidates = new LinkedHashSet<>();
        candidates.add(requested);

        Map<?, ?> entries = patchouliBookEntries(bookId);
        if (entries.isEmpty() || entries.containsKey(requested)) {
            return List.copyOf(candidates);
        }

        String requestedPath = requested.getPath();
        entries.keySet().stream()
                .filter(Identifier.class::isInstance)
                .map(Identifier.class::cast)
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

    public static boolean patchouliEntryVisible(Identifier bookId, String pageId) {
        if (bookId == null || pageId == null || pageId.isBlank()) {
            return true;
        }
        try {
            Map<?, ?> entries = patchouliBookEntries(bookId);
            if (entries.isEmpty()) {
                return true;
            }
            for (Identifier entryId : patchouliEntryCandidates(bookId, pageId)) {
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

    private static List<Identifier> patchouliBookCandidates(Iterable<Identifier> requestedBooks) {
        LinkedHashSet<Identifier> candidates = new LinkedHashSet<>();
        if (requestedBooks != null) {
            for (Identifier requestedBook : requestedBooks) {
                if (requestedBook == null) {
                    continue;
                }
                candidates.add(requestedBook);
                candidates.addAll(installedPatchouliBookCandidates(requestedBook));
            }
        }
        return List.copyOf(candidates);
    }

    private static List<Identifier> installedPatchouliBookCandidates(Identifier requestedBook) {
        if (requestedBook == null) {
            return List.of();
        }
        Map<?, ?> books = patchouliBookRegistryBooks();
        if (books.isEmpty()) {
            return List.of();
        }

        List<Identifier> exactNamespace = new ArrayList<>();
        List<Identifier> guideLike = new ArrayList<>();
        for (Object key : books.keySet()) {
            if (!(key instanceof Identifier bookId)) {
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

    private static Map<?, ?> patchouliBookEntries(Identifier bookId) {
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

    private static boolean tryInvokePatchouliOpen(Object api, String methodName, Identifier expectedBook, Object... args) {
        if (!tryInvokePatchouli(api, methodName, args)) {
            return false;
        }
        Boolean open = isPatchouliBookOpen(api, expectedBook);
        return open == null || open;
    }

    private static Boolean isPatchouliBookOpen(Object api, Identifier expectedBook) {
        if (api == null || expectedBook == null) {
            return null;
        }
        try {
            Method method = api.getClass().getMethod("getOpenBookGui");
            Object result = method.invoke(api);
            if (result instanceof Identifier openBook) {
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

    private static boolean openGuideMEBook(Identifier bookId, String pageId) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return false;

            Class<?> guidesCommon = Class.forName("guideme.GuidesCommon");

            if (pageId != null && !pageId.isBlank()) {
                Identifier pageLocation = guideMEPageLocation(bookId, pageId);
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

    private static Object guideMEPageAnchor(Identifier pageLocation) {
        if (pageLocation == null) {
            return null;
        }
        try {
            Class<?> pageAnchorClass = Class.forName("guideme.PageAnchor");
            return pageAnchorClass.getMethod("page", Identifier.class).invoke(null, pageLocation);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: GuideME page anchor unavailable for " + pageLocation, e);
            return null;
        }
    }

    static Identifier guideMEPageLocation(Identifier bookId, String pageId) {
        Identifier requested = Identifier.fromNamespaceAndPath(bookId.getNamespace(), pageId);
        Identifier runtimeMatch = guideMERuntimePageLocation(bookId, pageId);
        return runtimeMatch == null ? requested : runtimeMatch;
    }

    private static Identifier guideMERuntimePageLocation(Identifier bookId, String pageId) {
        if (bookId == null || pageId == null || pageId.isBlank()) {
            return null;
        }
        try {
            Class<?> proxyClass = Class.forName("guideme.internal.GuideMEProxy");
            Object proxy = proxyClass.getMethod("instance").invoke(null);
            Method method = proxyClass.getMethod("getAvailablePages", Identifier.class);
            Object result = method.invoke(proxy, bookId);
            if (!(result instanceof Stream<?> stream)) {
                return null;
            }
            try (stream) {
                String normalizedPageId = normalizeGuideMEPagePath(pageId);
                return stream
                        .filter(Identifier.class::isInstance)
                        .map(Identifier.class::cast)
                        .filter(candidate -> isGuideMEPageMatch(candidate, normalizedPageId))
                        .findFirst()
                        .orElse(null);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: GuideME available-page lookup failed for " + bookId + " / " + pageId, e);
            return null;
        }
    }

    static boolean isGuideMEPageMatch(Identifier candidate, String normalizedPageId) {
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

    private static Object guideMEPageArgument(Class<?> parameterType, Identifier pageLocation, Object pageAnchor) {
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

    private static void openModonomiconBook(Identifier bookId, Identifier categoryId,
                                            Identifier entryId, int page) {
        if (bookId == null) {
            return;
        }
        try {
            Class<?> addressClass = Class.forName("com.klikli_dev.modonomicon.client.gui.book.BookAddress");
            Object address;
            if (categoryId != null && entryId != null) {
                address = addressClass.getMethod(
                                "ignoreSaved",
                                Identifier.class,
                                Identifier.class,
                                Identifier.class,
                                int.class
                        )
                        .invoke(null, bookId, categoryId, entryId, Math.max(0, page));
            } else {
                address = addressClass.getMethod("defaultFor", Identifier.class).invoke(null, bookId);
            }

            Class<?> managerClass = Class.forName("com.klikli_dev.modonomicon.client.gui.BookGuiManager");
            Object manager = managerClass.getMethod("get").invoke(null);
            managerClass.getMethod("openBook", addressClass).invoke(manager, address);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Modonomicon unavailable for guide open: " + bookId, e);
        }
    }

    private static boolean openSilentGearMaterialBook(Identifier materialId) {
        try {
            Minecraft mc = Minecraft.getInstance();
            Object screen;
            if (materialId != null) {
                Class<?> registriesClass = Class.forName("net.silentchaos512.gear.setup.SgRegistries");
                Object materialManager = registriesClass.getField("MATERIAL").get(null);
                Object material = resolveSilentGearMaterial(materialManager, materialId);
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
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Silent Gear material book unavailable for guide open: " + materialId, e);
            return false;
        }
    }

    private static Object resolveSilentGearMaterial(Object materialManager, Identifier materialId) throws ReflectiveOperationException {
        if (materialManager == null || materialId == null) {
            return null;
        }
        Method get = materialManager.getClass().getMethod("get", Identifier.class);
        Object result = get.invoke(materialManager, materialId);
        if (result instanceof Optional<?> optional) {
            return optional.orElse(null);
        }
        return result;
    }

    private static Object newSilentGearMaterialBookScreen() throws ReflectiveOperationException {
        Class<?> screenClass = Class.forName("net.silentchaos512.gear.client.gui.book.MaterialBookScreen");
        return screenClass.getConstructor().newInstance();
    }

    private static void openMantleBook(Identifier bookId, String pageId) {
        if (bookId == null) {
            return;
        }
        try {
            Object book = mantleBookData(bookId);
            if (book == null) {
                return;
            }
            // Component is an always-present vanilla class with a stable Component.literal(String)
            // API across MC 1.20.1/1.21.1. Direct references are remapped by Loom for the
            // intermediary-named Fabric runtime, where the old Class.forName(Mojmap-name) failed.
            Object title = net.minecraft.network.chat.Component.literal(bookId.toString());
            for (Method method : book.getClass().getMethods()) {
                if (!"openGui".equals(method.getName()) || method.getParameterCount() != 3) {
                    continue;
                }
                Class<?>[] types = method.getParameterTypes();
                if (net.minecraft.network.chat.Component.class.isAssignableFrom(types[0]) && types[1] == String.class) {
                    method.invoke(book, title, pageId == null ? "" : pageId, (java.util.function.Consumer<String>) ignored -> {
                    });
                    return;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Mantle book unavailable for guide open: " + bookId, e);
        }
    }

    private static Object mantleBookData(Identifier bookId) throws ReflectiveOperationException {
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

    private static void openMalumCodexEntry(String identifier) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            // Prefer opening the specific entry via CodexEntryScreen.openScreen(BookEntry).
            java.util.Optional<Object> entry = com.sanhiruzu.ami.compat.MalumCodexGuideSource.findCodexEntry(identifier);
            if (entry.isPresent()) {
                Class<?> entryScreenClass = Class.forName(
                        "com.sammy.malum.client.screen.codex.screens.CodexEntryScreen");
                Class<?> bookEntryClass = Class.forName(
                        "com.sammy.malum.client.screen.codex.BookEntry");
                Method openScreen = entryScreenClass.getMethod("openScreen", bookEntryClass);
                openScreen.invoke(null, entry.get());
                return;
            }
            // Fall back to the Arcana codex overview.
            Class<?> arcanaClass = Class.forName(
                    "com.sammy.malum.client.screen.codex.screens.progression.ArcanaProgressionScreen");
            Object holder = arcanaClass.getField("SCREEN").get(null);
            Class<?> holderClass = holder.getClass();
            holderClass.getMethod("openCodexViaItem", boolean.class).invoke(holder, false);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Malum codex entry open failed for " + identifier, e);
        }
    }

    private static void openCncCreatureScreen(String creatureClassSuffix) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            // Try singleplayer integrated server first — CnC uses AbstractContainerScreen
            // which requires server-side openMenu() to synchronize the menu.
            Object server = cncSingleplayerServer(mc);
            if (server != null) {
                String playerName = mc.player.getName().getString();
                double x = mc.player.getX();
                double y = mc.player.getY();
                double z = mc.player.getZ();
                String procedureName = com.sanhiruzu.ami.compat.CrittersCrawlersGuideSource
                        .procedureClassName(creatureClassSuffix);
                if (procedureName == null) {
                    // Unknown creature; open the cover screen instead.
                    procedureName = "net.imasillylittleguy.cnc.procedures.FieldGuideOpenProcedure";
                }
                final String resolvedProcedure = procedureName;
                final Object finalServer = server;
                Runnable task = () -> {
                    try {
                        Object playerList = finalServer.getClass().getMethod("getPlayerList").invoke(finalServer);
                        Object sp = playerList.getClass().getMethod("getPlayerByName", String.class).invoke(playerList, playerName);
                        if (sp == null) {
                            return;
                        }
                        // LevelAccessor / Entity are always-present vanilla classes with stable names
                        // across MC 1.20.1/1.21.1; direct references are remapped by Loom for the
                        // intermediary-named Fabric runtime (Class.forName(Mojmap-name) failed there).
                        // procClass stays reflective: it is a third-party (CnC) mod class.
                        Class<?> procClass = Class.forName(resolvedProcedure);
                        java.lang.reflect.Method execute = procClass.getMethod("execute",
                                net.minecraft.world.level.LevelAccessor.class,
                                double.class, double.class, double.class,
                                net.minecraft.world.entity.Entity.class);
                        Object level = ((net.minecraft.world.entity.Entity) sp).level();
                        execute.invoke(null, level, x, y, z, sp);
                    } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
                        LOGGER.log(Level.FINE, "AMI: CnC field guide open failed for "
                                + creatureClassSuffix, ex);
                    }
                };
                try {
                    server.getClass().getMethod("execute", Runnable.class).invoke(server, task);
                } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                    LOGGER.log(Level.FINE, "AMI: CnC server execute failed for " + creatureClassSuffix, e);
                }
                return;
            }
            // Multiplayer: fall back to the cover screen via its open procedure.
            // (Multiplayer cannot access the server player directly.)
            LOGGER.log(Level.FINE, "AMI: CnC field guide requires singleplayer to open "
                    + creatureClassSuffix + "; no action in multiplayer");
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: CnC field guide unavailable for " + creatureClassSuffix, e);
        }
    }

    private static Object cncSingleplayerServer(Minecraft mc) {
        try {
            return Minecraft.class.getMethod("getSingleplayerServer").invoke(mc);
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: getSingleplayerServer unavailable", e);
            return null;
        }
    }

    private static void openHexereiBook(Identifier bookId, String pageId) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            // All hexerei books share BookOfShadowsScreen as the unified reader.
            Class<?> screenClass = Class.forName("net.joefoxe.hexerei.screen.BookOfShadowsScreen");
            Object screen = null;
            if (pageId != null && !pageId.isBlank()) {
                Identifier pageLocation = Identifier.tryParse(pageId);
                if (pageLocation != null) {
                    try {
                        screen = screenClass.getConstructor(Identifier.class).newInstance(pageLocation);
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
                if (screen == null) {
                    try {
                        screen = screenClass.getConstructor(String.class).newInstance(pageId);
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
            }
            if (screen == null) {
                screen = screenClass.getConstructor().newInstance();
            }
            final Object finalScreen = screen;
            mc.execute(() -> mc.setScreen((net.minecraft.client.gui.screens.Screen) finalScreen));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Hexerei book unavailable for guide open: " + bookId + " / " + pageId, e);
        }
    }

    private static void openAlexsMobsAnimalDictionary(String pageJson) {
        try {
            Minecraft mc = Minecraft.getInstance();
            Class<?> screenClass = Class.forName("com.github.alexthe666.alexsmobs.client.gui.GUIAnimalDictionary");
            net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    Identifier.fromNamespaceAndPath("alexsmobs", "animal_dictionary")
            ).map(net.minecraft.core.Holder::value).orElse(net.minecraft.world.item.Items.AIR);
            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item);
            Object screen = pageJson == null || pageJson.isBlank()
                    ? screenClass.getConstructor(net.minecraft.world.item.ItemStack.class).newInstance(stack)
                    : screenClass.getConstructor(net.minecraft.world.item.ItemStack.class, String.class).newInstance(stack, pageJson);
            mc.execute(() -> mc.setScreen((net.minecraft.client.gui.screens.Screen) screen));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Alex's Mobs dictionary unavailable for guide open: " + pageJson, e);
        }
    }

    private static void openAlexsCavesBook(Identifier pageJson) {
        try {
            Minecraft mc = Minecraft.getInstance();
            Class<?> screenClass = Class.forName("com.github.alexmodguy.alexscaves.client.gui.book.CaveBookScreen");
            Object screen;
            if (pageJson != null) {
                // CaveBookScreen(String) sets the initial page before init() is called.
                // Data files live under "books/" which alexPagePath strips, so prepend it back.
                String path = "books/" + pageJson.getPath();
                try {
                    screen = screenClass.getConstructor(String.class).newInstance(path);
                } catch (ReflectiveOperationException ignored) {
                    screen = screenClass.getConstructor().newInstance();
                }
            } else {
                screen = screenClass.getConstructor().newInstance();
            }
            final Object finalScreen = screen;
            mc.execute(() -> mc.setScreen((net.minecraft.client.gui.screens.Screen) finalScreen));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Alex's Caves book unavailable for guide open: " + pageJson, e);
        }
    }

    private record ScoredEntryCandidate(Identifier entryId, int score) {
    }
}
