package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.platform.Services;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.decoration.PaintingVariant;
import java.lang.reflect.Method;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AmiRegistryDocumentIndex {
    public static final AmiRegistryDocumentIndex EMPTY = new AmiRegistryDocumentIndex(List.of());


    private final List<RegistryDocument> documents;

    public AmiRegistryDocumentIndex(List<RegistryDocument> documents) {
        this.documents = List.copyOf(documents);
    }

    public List<RegistryDocument> allDocuments() {
        return documents;
    }

    /**
     * Returns documents matching the query text, filtered to the given kinds.
     * Handles $kind prefix (strips $, matches kind token), # prefix (strips #, matches tag id),
     * ~ prefix (strips ~, matches game rule id), and plain text.
     */
    public List<RegistryDocument> query(String rawQuery, Set<RegistryDocumentKind> enabledKinds) {
        if (rawQuery == null || rawQuery.isBlank() || enabledKinds.isEmpty()) {
            return List.of();
        }

        String query = rawQuery.strip();
        // Strip $ prefix — matches kind token baked into each document
        if (query.startsWith("$")) {
            query = query.substring(1);
        }
        // Strip # prefix — tag id search
        else if (query.startsWith("#")) {
            query = query.substring(1);
        }
        // Strip ~ prefix — game rule search
        else if (query.startsWith("~")) {
            query = query.substring(1);
        }

        if (query.isBlank()) {
            return List.of();
        }

        List<String> terms = tokenize(query);
        List<RegistryDocument> results = new ArrayList<>();
        for (RegistryDocument doc : documents) {
            if (!enabledKinds.contains(doc.kind())) continue;
            if (matches(doc, terms)) {
                results.add(doc);
            }
        }
        return List.copyOf(results);
    }

    // -------------------------------------------------------------------------
    // Registry document builders
    // -------------------------------------------------------------------------

    public static List<RegistryDocument> buildEnchantmentDocuments(RegistryAccess registryAccess) {
        List<RegistryDocument> docs = new ArrayList<>();
        var registry = registryAccess.registry(Registries.ENCHANTMENT).orElse(null);
        if (registry == null) return docs;
        // EnchantmentTags.CURSE / TREASURE constants (net.minecraft.tags.EnchantmentTags) exist in
        // MC 1.21+, but that class is absent in MC 1.20.1. Building the TagKey manually gives
        // identical keys on 1.21+ and silently returns false on 1.20.1 where enchantments are not
        // data-driven (Holder.is returns false rather than throwing).
        TagKey<Enchantment> curseTag = TagKey.create(Registries.ENCHANTMENT,
                Services.PLATFORM.rl("minecraft", "curse"));
        TagKey<Enchantment> treasureTag = TagKey.create(Registries.ENCHANTMENT,
                Services.PLATFORM.rl("minecraft", "treasure"));
        for (Holder.Reference<Enchantment> holder : registry.holders().toList()) {
            Enchantment enchantment = holder.value();
            ResourceLocation id = holder.key().location();
            String name = Component.translatable(
                    "enchantment." + id.getNamespace() + "." + id.getPath()).getString();
            int maxLevel = enchantment.getMaxLevel();
            boolean isCurse = holder.is(curseTag);
            boolean isTreasure = holder.is(treasureTag);

            List<String> tokens = new ArrayList<>();
            tokens.add(RegistryDocumentKind.ENCHANTMENT.categoryToken());
            if (isCurse) tokens.add("curse");
            if (isTreasure) tokens.add("treasure");
            tokens.add(id.getNamespace());
            tokens.add("level " + maxLevel);

            String description = buildEnchantmentDescription(maxLevel, isCurse, isTreasure);
            docs.add(new RegistryDocument(
                    RegistryDocumentKind.ENCHANTMENT,
                    id,
                    name,
                    description,
                    id.getNamespace(),
                    tokens
            ));
        }
        return docs;
    }

    private static String buildEnchantmentDescription(int maxLevel, boolean isCurse, boolean isTreasure) {
        StringBuilder sb = new StringBuilder();
        sb.append("Max level: ").append(maxLevel);
        if (isCurse) sb.append(" • Curse");
        if (isTreasure) sb.append(" • Treasure only");
        return sb.toString();
    }

    public static List<RegistryDocument> buildMobEffectDocuments() {
        List<RegistryDocument> docs = new ArrayList<>();
        for (var entry : BuiltInRegistries.MOB_EFFECT.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            MobEffect effect = entry.getValue();
            String name = Component.translatable(effect.getDescriptionId()).getString();
            MobEffectCategory category = effect.getCategory();
            boolean isInstant = effect.isInstantenous();

            List<String> tokens = new ArrayList<>();
            tokens.add(RegistryDocumentKind.MOB_EFFECT.categoryToken());
            tokens.add(category.name().toLowerCase(Locale.ROOT));
            if (isInstant) tokens.add("instant");
            tokens.add(id.getNamespace());

            String categoryName = category.name();
            String description = categoryName.charAt(0) + categoryName.substring(1).toLowerCase(Locale.ROOT)
                    + (isInstant ? " • Instant" : "");
            docs.add(new RegistryDocument(
                    RegistryDocumentKind.MOB_EFFECT,
                    id,
                    name,
                    description,
                    id.getNamespace(),
                    tokens
            ));
        }
        return docs;
    }

    public static List<RegistryDocument> buildPaintingDocuments(RegistryAccess registryAccess) {
        List<RegistryDocument> docs = new ArrayList<>();
        var registry = registryAccess.registry(Registries.PAINTING_VARIANT).orElse(null);
        if (registry == null) return docs;
        registry.holders().forEach(holder -> {
            ResourceLocation id = holder.key().location();
            PaintingVariant variant = holder.value();
            String rawName = id.getPath().replace('_', ' ');
            String name = Character.toUpperCase(rawName.charAt(0)) + rawName.substring(1);
            int w = paintingDimension(variant, "width", "m_219980_");
            int h = paintingDimension(variant, "height", "m_219985_");

            List<String> tokens = new ArrayList<>();
            tokens.add(RegistryDocumentKind.PAINTING.categoryToken());
            tokens.add(w + "x" + h);
            tokens.add(id.getNamespace());

            String description = w + "×" + h + " tiles";
            docs.add(new RegistryDocument(
                    RegistryDocumentKind.PAINTING,
                    id,
                    name,
                    description,
                    id.getNamespace(),
                    tokens
            ));
        });
        return docs;
    }

    public static List<RegistryDocument> buildGameRuleDocuments() {
        List<RegistryDocument> docs = new ArrayList<>();
        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                String ruleName = key.getId();
                ResourceLocation id = Services.PLATFORM.rl("minecraft", ruleName);
                T rule = type.createRule();
                String defaultVal = rule.serialize();
                String typeName;
                if (defaultVal.equals("true") || defaultVal.equals("false")) {
                    typeName = "boolean";
                } else {
                    try {
                        Integer.parseInt(defaultVal);
                        typeName = "integer";
                    } catch (NumberFormatException e) {
                        typeName = "value";
                    }
                }

                List<String> tokens = new ArrayList<>();
                tokens.add(RegistryDocumentKind.GAME_RULE.categoryToken());
                tokens.add("gamerule");
                tokens.add(typeName);
                tokens.add(ruleName.toLowerCase(Locale.ROOT));

                String description = typeName + " • Default: " + defaultVal;
                docs.add(new RegistryDocument(
                        RegistryDocumentKind.GAME_RULE,
                        id,
                        ruleName,
                        description,
                        "minecraft",
                        tokens
                ));
            }
        });
        return docs;
    }

    public static List<RegistryDocument> buildTagDocuments(RegistryAccess registryAccess) {
        List<RegistryDocument> docs = new ArrayList<>();
        buildTagsForRegistry(docs, registryAccess, Registries.ITEM, "item tag");
        buildTagsForRegistry(docs, registryAccess, Registries.BLOCK, "block tag");
        buildTagsForRegistry(docs, registryAccess, Registries.ENTITY_TYPE, "entity tag");
        buildTagsForRegistry(docs, registryAccess, Registries.BIOME, "biome tag");
        return docs;
    }

    private static <T> void buildTagsForRegistry(List<RegistryDocument> out,
                                                  RegistryAccess registryAccess,
                                                  ResourceKey<? extends Registry<T>> key,
                                                  String typeLabel) {
        var lookup = registryAccess.lookup(key).orElse(null);
        if (lookup == null) return;
        lookup.listTags().forEach(named -> {
            var tagKey = named.key();
            ResourceLocation tagId = tagKey.location();
            int memberCount = (int) named.stream().count();

            List<String> tokens = new ArrayList<>();
            tokens.add(RegistryDocumentKind.TAG.categoryToken());
            tokens.add(typeLabel);
            tokens.add(tagId.getNamespace());
            tokens.add(tagId.getPath().replace('/', ' '));
            tokens.add("#" + tagId.getNamespace() + ":" + tagId.getPath());

            String displayName = "#" + tagId.getNamespace() + ":" + tagId.getPath();
            String description = typeLabel + " • " + memberCount + " member" + (memberCount == 1 ? "" : "s");
            out.add(new RegistryDocument(
                    RegistryDocumentKind.TAG,
                    tagId,
                    displayName,
                    description,
                    tagId.getNamespace(),
                    tokens
            ));
        });
    }

    /** Reflectively reads a dimension int from PaintingVariant, tolerating SRG name differences. */
    private static int paintingDimension(PaintingVariant variant, String... methodNames) {
        for (String name : methodNames) {
            try {
                Method m = PaintingVariant.class.getMethod(name);
                Object result = m.invoke(variant);
                if (result instanceof Number n) return n.intValue();
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return 1;
    }

    // -------------------------------------------------------------------------
    // Query helpers
    // -------------------------------------------------------------------------

    private static boolean matches(RegistryDocument doc, List<String> terms) {
        String haystack = normalize(doc.displayName() + " " + doc.description() + " " + doc.id()
                + " " + String.join(" ", doc.searchTokens()));
        for (String term : terms) {
            if (!haystack.contains(term)) {
                return false;
            }
        }
        return true;
    }

    private static List<String> tokenize(String query) {
        List<String> tokens = new ArrayList<>();
        for (String part : normalize(query).split("\\s+")) {
            if (!part.isBlank()) tokens.add(part);
        }
        return tokens;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) return "";
        return value.toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replace('/', ' ')
                .trim()
                .replaceAll("\\s+", " ");
    }
}
