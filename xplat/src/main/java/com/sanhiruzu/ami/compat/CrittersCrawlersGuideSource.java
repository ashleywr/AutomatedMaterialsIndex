package com.sanhiruzu.ami.compat;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.api.AmiGuideDocument;
import com.sanhiruzu.ami.api.AmiGuideOpeners;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.IOException;
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
 * Guide source for the Critters n' Crawlers Field Guide.
 * <p>
 * CnC stores all guide text in compiled Java (MCreator-generated per-creature
 * screens). This source reads entry text from the CnC lang file at guide-index
 * time and opens the appropriate creature screen via the integrated server at
 * click time.
 */
public final class CrittersCrawlersGuideSource {
    private static final Logger LOGGER = Logger.getLogger(CrittersCrawlersGuideSource.class.getName());
    private static final String MOD_ID = "cnc";
    private static final String SCREEN_PACKAGE = "net.imasillylittleguy.cnc.client.gui";
    private static final String PROCEDURE_PACKAGE = "net.imasillylittleguy.cnc.procedures";
    private static final String MENU_PACKAGE = "net.imasillylittleguy.cnc.world.inventory";

    // Snake-case IDs that are navigation/meta screens, not creature entries.
    private static final Set<String> SKIP_IDS = Set.of(
            "contents", "contents_2", "contents_written", "cover", "credits"
    );

    private CrittersCrawlersGuideSource() {
    }

    public static void registerGuideDocuments(Consumer<AmiGuideDocument> documents) {
        if (documents == null) {
            return;
        }
        Map<String, String> lang = readCncLang();
        if (lang.isEmpty()) {
            return;
        }

        // Collect distinct creature IDs from label_line_1 keys:
        // gui.cnc.field_guide_{id}.label_line_1
        Set<String> creatureIds = new LinkedHashSet<>();
        for (String key : lang.keySet()) {
            if (!key.startsWith("gui.cnc.field_guide_") || !key.endsWith(".label_line_1")) {
                continue;
            }
            String id = key.substring("gui.cnc.field_guide_".length(),
                    key.length() - ".label_line_1".length());
            if (!skipCreature(id)) {
                creatureIds.add(id);
            }
        }

        Identifier bookItemId = Identifier.fromNamespaceAndPath(MOD_ID, "field_guide");
        for (String creatureId : creatureIds) {
            String classSuffix = toClassSuffix(creatureId);
            if (!screenClassExists(classSuffix)) {
                continue;
            }
            String title = resolveTitle(creatureId, classSuffix, lang);
            String description = resolveDescription(creatureId, lang);
            String summary = description.isBlank() ? title : title + "\n" + description;
            Identifier docId = Identifier.fromNamespaceAndPath(
                    "ami", "guide/cnc/" + creatureId);
            documents.accept(AmiGuideDocument.builder(docId, "cnc_field_guide", MOD_ID, title)
                    .bookId(bookItemId)
                    .iconItemId(bookItemId)
                    .pageId(creatureId)
                    .tag("cnc")
                    .tag("guide")
                    .summaryText(summary)
                    .openAction(AmiGuideOpeners.cncFieldGuideCreature(classSuffix))
                    .build());
        }
    }

    private static boolean skipCreature(String id) {
        for (String skip : SKIP_IDS) {
            if (id.equals(skip) || id.endsWith("_" + skip)) {
                return true;
            }
        }
        // Skip duplicate/page-2 entries (e.g. whitetail_2, white_tailed_deer).
        if (id.endsWith("_2")) {
            return true;
        }
        // Skip white_tailed_deer — covered by whitetail.
        if (id.equals("white_tailed_deer")) {
            return true;
        }
        // Skip timber_wolf — same content as greywolf.
        if (id.equals("timber_wolf")) {
            return true;
        }
        return false;
    }

    /**
     * Converts a snake_case creature ID (without the "field_guide_" prefix)
     * to the PascalCase suffix used in class names.
     * Examples: "black_bear" → "BlackBear", "pit_viper" → "PitViper".
     */
    static String toClassSuffix(String snakeId) {
        StringBuilder sb = new StringBuilder();
        for (String part : snakeId.split("_")) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1));
                }
            }
        }
        return sb.toString();
    }

    private static boolean screenClassExists(String classSuffix) {
        try {
            Class.forName(SCREEN_PACKAGE + ".FieldGuide" + classSuffix + "Screen");
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /**
     * Returns the open procedure class name for a given class suffix, or null if not found.
     */
    public static String procedureClassName(String classSuffix) {
        String name = PROCEDURE_PACKAGE + ".FieldGuide" + classSuffix + "OpenProcedure";
        try {
            Class.forName(name);
            return name;
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    private static String resolveTitle(String creatureId, String classSuffix, Map<String, String> lang) {
        // Try tooltip keys in the contents screens (compact form, no underscores).
        String compact = creatureId.replace("_", "");
        for (String prefix : List.of("gui.cnc.field_guide_contents.tooltip_",
                "gui.cnc.field_guide_contents_2.tooltip_",
                "gui.cnc.field_guide_contents_written.tooltip_")) {
            String v = lang.get(prefix + compact);
            if (v != null && !v.isBlank()) {
                return v;
            }
            v = lang.get(prefix + creatureId);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        // Fall back to prettified class suffix.
        return classSuffix.replaceAll("([a-z])([A-Z])", "$1 $2");
    }

    private static String resolveDescription(String creatureId, Map<String, String> lang) {
        List<String> lines = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            String key = "gui.cnc.field_guide_" + creatureId + ".label_line_" + i;
            String v = lang.get(key);
            if (v != null && !v.isBlank()) {
                lines.add(v.strip());
            }
        }
        return String.join(" ", lines);
    }

    private static Map<String, String> readCncLang() {
        try {
            ResourceManager rm = Minecraft.getInstance().getResourceManager();
            Optional<Resource> resource = rm.getResource(
                    Identifier.fromNamespaceAndPath(MOD_ID, "lang/en_us.json"));
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
                LinkedHashMap<String, String> out = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> entry : parsed.getAsJsonObject().entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        out.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
                return out;
            }
        } catch (IOException | RuntimeException | LinkageError e) {
            AmiCore.LOGGER.warn("AMI: Failed to read CnC lang file for field guide indexing", e);
            return Map.of();
        }
    }
}
