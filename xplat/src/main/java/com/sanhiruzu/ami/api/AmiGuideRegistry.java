package com.sanhiruzu.ami.api;

import com.sanhiruzu.searchableguides.api.SearchableGuideDocument;
import com.sanhiruzu.searchableguides.api.SearchableGuideProvider;
import com.sanhiruzu.searchableguides.api.SearchableGuideProviders;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Public registry for guide documents contributed by AMI compat adapters or
 * third-party mods.
 */
public final class AmiGuideRegistry {
    private static final Logger LOGGER = Logger.getLogger(AmiGuideRegistry.class.getName());
    private static final Map<Identifier, AmiGuideDocument> DOCUMENTS = new LinkedHashMap<>();

    private AmiGuideRegistry() {
    }

    public static synchronized void register(AmiGuideDocument document) {
        if (document == null) {
            return;
        }
        DOCUMENTS.put(document.id(), document);
    }

    public static synchronized void registerSource(AmiGuideSource source) {
        if (source == null) {
            return;
        }
        try {
            source.registerGuideDocuments(AmiGuideRegistry::register);
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.WARNING, "AMI: Guide source failed: " + sourceId(source), e);
        }
    }

    public static synchronized void registerPluginGuides() {
        for (IAmiPlugin plugin : AmiPluginRegistry.getPlugins()) {
            try {
                plugin.addGuideDocuments(AmiGuideRegistry::register);
            } catch (RuntimeException | LinkageError e) {
                LOGGER.log(Level.WARNING, "AMI: Plugin guide registration failed: " + plugin.getClass().getName(), e);
            }
        }
    }

    public static synchronized void registerSearchableGuideProviders() {
        for (SearchableGuideProvider provider : SearchableGuideProviders.getProviders()) {
            try {
                provider.addGuideDocuments(document -> register(fromSearchableGuideDocument(document)));
            } catch (RuntimeException | LinkageError e) {
                LOGGER.log(Level.WARNING, "AMI: Shared guide provider failed: " + providerId(provider), e);
            }
        }
    }

    public static synchronized List<AmiGuideDocument> getDocuments() {
        List<AmiGuideDocument> documents = new ArrayList<>(DOCUMENTS.values());
        documents.sort(Comparator.comparing(document -> document.id().toString()));
        return List.copyOf(documents);
    }

    public static synchronized void clear() {
        DOCUMENTS.clear();
    }

    public static synchronized int size() {
        return DOCUMENTS.size();
    }

    private static String sourceId(AmiGuideSource source) {
        try {
            String id = source.id();
            return id == null || id.isBlank() ? source.getClass().getName() : id;
        } catch (RuntimeException | LinkageError ignored) {
            return source.getClass().getName();
        }
    }

    private static String providerId(SearchableGuideProvider provider) {
        try {
            String id = provider.id();
            return id == null || id.isBlank() ? provider.getClass().getName() : id;
        } catch (RuntimeException | LinkageError ignored) {
            return provider.getClass().getName();
        }
    }

    private static AmiGuideDocument fromSearchableGuideDocument(SearchableGuideDocument document) {
        if (document == null) {
            return null;
        }
        return AmiGuideDocument.builder(document.id(), document.sourceType(), document.modId(), document.title())
                .bookId(document.bookId())
                .iconItemId(document.iconItemId())
                .pageId(document.pageId())
                .chapter(document.chapter())
                .referencedItems(document.referencedItems())
                .tags(document.tags())
                .summaryText(document.summaryText())
                .visibility(document.visibility())
                .openAction(document.openAction())
                .build();
    }
}
