package com.sanhiruzu.ami.compat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.api.AmiGuideDocument;
import com.sanhiruzu.ami.api.AmiGuideOpeners;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Guide source for the Malum Encyclopedia (Arcana/Esoterica codex).
 * <p>
 * Malum stores all codex entry text in compiled Java classes rather than data
 * files, so this source reads entry titles and descriptions from the Malum
 * language file at guide-index time and opens entries by identifier via
 * reflection at click time.
 */
public final class MalumCodexGuideSource {
    private static final Logger LOGGER = Logger.getLogger(MalumCodexGuideSource.class.getName());
    private static final String MOD_ID = "malum";
    private static final String ENTRY_PREFIX = "malum.gui.book.entry.";
    private static final String DESCRIPTION_SUFFIX = ".description";

    private MalumCodexGuideSource() {
    }

    public static void registerGuideDocuments(Consumer<AmiGuideDocument> documents) {
        if (documents == null) {
            return;
        }
        Map<String, String> lang = readMalumLang();
        if (lang.isEmpty()) {
            return;
        }
        // Identifiers are lang keys that match "malum.gui.book.entry.{id}" where
        // {id} has no dots (top-level entries only). Sub-section keys like
        // "malum.gui.book.entry.foo.bar" are skipped — they are pages within foo.
        Set<String> identifiers = new LinkedHashSet<>();
        for (String key : lang.keySet()) {
            if (!key.startsWith(ENTRY_PREFIX)) {
                continue;
            }
            String rest = key.substring(ENTRY_PREFIX.length());
            if (rest.isEmpty() || rest.contains(".")) {
                continue;
            }
            identifiers.add(rest);
        }

        // Try to enumerate entries from the progression screens to discover isVoid.
        // Maps identifier → isVoid (true = Encyclopedia Esoterica).
        Map<String, Boolean> voidByIdentifier = enumerateVoidFlags();

        ResourceLocation arcanaBookId = ResourceLocation.fromNamespaceAndPath(MOD_ID, "encyclopedia_arcana");
        ResourceLocation esotericaBookId = ResourceLocation.fromNamespaceAndPath(MOD_ID, "encyclopedia_esoterica");

        for (String identifier : identifiers) {
            String title = lang.getOrDefault(ENTRY_PREFIX + identifier, identifier);
            String description = lang.getOrDefault(ENTRY_PREFIX + identifier + DESCRIPTION_SUFFIX, "");
            String summary = description.isBlank() ? title : title + "\n" + description;
            ResourceLocation documentId = ResourceLocation.fromNamespaceAndPath(
                    "ami",
                    "guide/malum/" + identifier.replace('.', '/').replace(' ', '_')
            );
            boolean isVoid = voidByIdentifier.getOrDefault(identifier,
                    identifier.contains("void") || identifier.contains("umbral") || identifier.contains("esoterica"));
            ResourceLocation bookId = isVoid ? esotericaBookId : arcanaBookId;
            documents.accept(AmiGuideDocument.builder(documentId, "malum_codex", MOD_ID, title)
                    .bookId(bookId)
                    .iconItemId(bookId)
                    .pageId(identifier)
                    .chapter(chapterFor(identifier, lang))
                    .tag("malum")
                    .tag("guide")
                    .tag("codex")
                    .summaryText(summary)
                    .openAction(AmiGuideOpeners.malumCodexEntry(identifier))
                    .build());
        }
    }

    /**
     * Iterates both progression screen entry lists and returns a map of identifier → isVoid.
     * Returns an empty map if the screens are not yet loaded.
     */
    private static Map<String, Boolean> enumerateVoidFlags() {
        Map<String, Boolean> out = new LinkedHashMap<>();
        for (String[] screenAndVoid : new String[][]{
                {"com.sammy.malum.client.screen.codex.screens.progression.ArcanaProgressionScreen", "false"},
                {"com.sammy.malum.client.screen.codex.screens.progression.VoidProgressionScreen", "true"}
        }) {
            String screenClassName = screenAndVoid[0];
            boolean isVoid = Boolean.parseBoolean(screenAndVoid[1]);
            try {
                Class<?> screenClass = Class.forName(screenClassName);
                Field screenField = screenClass.getField("SCREEN");
                Object holder = screenField.get(null);
                if (holder == null) continue;
                Method getScreen = holder.getClass().getMethod("getScreen");
                Object screen = getScreen.invoke(holder);
                if (screen == null) continue;
                Field entriesField = screen.getClass().getSuperclass().getDeclaredField("entries");
                entriesField.setAccessible(true);
                List<?> entries = (List<?>) entriesField.get(screen);
                if (entries == null || entries.isEmpty()) {
                    try {
                        screen.getClass().getMethod("setupEntries").invoke(screen);
                        entries = (List<?>) entriesField.get(screen);
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
                if (entries == null) continue;
                for (Object entry : entries) {
                    Field idField = entry.getClass().getSuperclass().getDeclaredField("identifier");
                    idField.setAccessible(true);
                    Object id = idField.get(entry);
                    if (id instanceof String s) {
                        out.put(s, isVoid);
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                LOGGER.log(Level.FINE, "AMI: Malum codex isVoid enumeration failed for " + screenClassName, e);
            }
        }
        return out;
    }

    private static String chapterFor(String identifier, Map<String, String> lang) {
        // Best-effort: check whether the identifier appears in a known chapter prefix.
        // Malum lang keys don't encode chapter membership, so this is a heuristic.
        if (identifier.contains("spirit") || identifier.contains("soul")) return "Spirits";
        if (identifier.contains("geas")) return "Geasa";
        if (identifier.contains("rune") || identifier.contains("runework")) return "Rune Working";
        if (identifier.contains("augment")) return "Augmentation";
        if (identifier.contains("totem")) return "Totem Magic";
        if (identifier.contains("void") || identifier.contains("umbral")) return "Void Codex";
        return "Malum";
    }

    public static Optional<Object> findCodexEntry(String identifier) {
        // Try ArcanaProgressionScreen.SCREEN first, then VoidProgressionScreen.SCREEN.
        for (String screenClassName : List.of(
                "com.sammy.malum.client.screen.codex.screens.progression.ArcanaProgressionScreen",
                "com.sammy.malum.client.screen.codex.screens.progression.VoidProgressionScreen")) {
            Optional<Object> entry = findEntryInScreen(screenClassName, identifier);
            if (entry.isPresent()) {
                return entry;
            }
        }
        return Optional.empty();
    }

    private static Optional<Object> findEntryInScreen(String screenClassName, String identifier) {
        try {
            Class<?> screenClass = Class.forName(screenClassName);
            Field screenField = screenClass.getField("SCREEN");
            Object holder = screenField.get(null);
            if (holder == null) {
                return Optional.empty();
            }
            // ProgressionScreenHolder.getScreen() — lazily creates the screen.
            Method getScreen = holder.getClass().getMethod("getScreen");
            Object screen = getScreen.invoke(holder);
            if (screen == null) {
                return Optional.empty();
            }
            // Ensure entries are populated. Call setupEntries() if the entries list is empty.
            Field entriesField = screen.getClass().getSuperclass().getDeclaredField("entries");
            entriesField.setAccessible(true);
            List<?> entries = (List<?>) entriesField.get(screen);
            if (entries == null || entries.isEmpty()) {
                try {
                    Method setupEntries = screen.getClass().getMethod("setupEntries");
                    setupEntries.invoke(screen);
                    entries = (List<?>) entriesField.get(screen);
                } catch (ReflectiveOperationException ignored) {
                }
            }
            if (entries == null) {
                return Optional.empty();
            }
            for (Object entry : entries) {
                Field idField = entry.getClass().getSuperclass().getDeclaredField("identifier");
                idField.setAccessible(true);
                if (identifier.equals(idField.get(entry))) {
                    return Optional.of(entry);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Malum codex entry lookup failed for " + identifier, e);
        }
        return Optional.empty();
    }

    private static Map<String, String> readMalumLang() {
        try {
            ResourceManager rm = Minecraft.getInstance().getResourceManager();
            Optional<Resource> resource = rm.getResource(
                    ResourceLocation.fromNamespaceAndPath(MOD_ID, "lang/en_us.json"));
            if (resource.isEmpty()) {
                return Map.of();
            }
            try (BufferedReader reader = resource.get().openAsReader()) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                JsonElement parsed = JsonParser.parseString(sb.toString());
                if (!parsed.isJsonObject()) {
                    return Map.of();
                }
                java.util.LinkedHashMap<String, String> out = new java.util.LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> entry : parsed.getAsJsonObject().entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        out.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
                return out;
            }
        } catch (IOException | RuntimeException | LinkageError e) {
            AmiCore.LOGGER.warn("AMI: Failed to read Malum lang file for codex indexing", e);
            return Map.of();
        }
    }
}
