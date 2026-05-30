package com.sanhiruzu.ami.index;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Readable catalog view over AMI's current taxonomy definitions.
 * <p>
 * This is intentionally a thin facade while the resolver is still Java-backed:
 * it gives tests, reports, and future tooling one place to inspect the category
 * tree without chasing AmiOntology, AmiOntologyKinds, and lang keys separately.
 */
public final class AmiTaxonomyCatalog {
    private AmiTaxonomyCatalog() {
    }

    public static List<CategoryEntry> categories() {
        List<CategoryEntry> result = new ArrayList<>();
        for (AmiOntology.Category category : AmiOntology.CATEGORIES) {
            List<SubcategoryEntry> subcategories = new ArrayList<>();
            for (AmiOntology.SubCategory subcategory : category.subCategories) {
                subcategories.add(new SubcategoryEntry(
                        subcategory.id(),
                        label(subcategory.translationKey()),
                        kinds(category.id, subcategory.id())
                ));
            }
            result.add(new CategoryEntry(category.id, label(category.translationKey), List.copyOf(subcategories)));
        }
        return List.copyOf(result);
    }

    public static List<KindEntry> kinds(String categoryId, String subcategoryId) {
        return AmiOntologyKinds.kindsFor(categoryId, subcategoryId).stream()
                .map(kind -> new KindEntry(kind.id(), kind.label()))
                .toList();
    }

    public static String toMarkdown() {
        StringBuilder out = new StringBuilder();
        out.append("# AMI Taxonomy\n\n");
        for (CategoryEntry category : categories()) {
            out.append("## ").append(category.label()).append(" (`").append(category.id()).append("`)\n\n");
            for (SubcategoryEntry subcategory : category.subcategories()) {
                out.append("- ").append(subcategory.label()).append(" (`")
                        .append(category.id()).append('/').append(subcategory.id()).append("`)\n");
                for (KindEntry kind : subcategory.kinds()) {
                    out.append("  - ").append(kind.label()).append(" (`").append(kind.id()).append("`)\n");
                }
            }
            out.append('\n');
        }
        return out.toString();
    }

    private static String label(String translationKey) {
        return Labels.EN_US.getOrDefault(translationKey, translationKey);
    }

    public record CategoryEntry(String id, String label, List<SubcategoryEntry> subcategories) {
    }

    public record SubcategoryEntry(String id, String label, List<KindEntry> kinds) {
    }

    public record KindEntry(String id, String label) {
    }

    private static final class Labels {
        private static final Map<String, String> EN_US = loadEnglishLabels();

        private static Map<String, String> loadEnglishLabels() {
            Map<String, String> labels = new ConcurrentHashMap<>();
            try (var stream = AmiTaxonomyCatalog.class.getClassLoader()
                    .getResourceAsStream("assets/ami/lang/en_us.json")) {
                if (stream == null) {
                    return labels;
                }
                JsonObject object = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                for (var entry : object.entrySet()) {
                    if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                        labels.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            } catch (RuntimeException | java.io.IOException ignored) {
                labels.clear();
            }
            return labels;
        }
    }
}
