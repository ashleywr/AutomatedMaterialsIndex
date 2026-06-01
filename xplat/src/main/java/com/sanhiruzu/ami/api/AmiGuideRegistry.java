package com.sanhiruzu.ami.api;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Public registry for guide documents contributed by AMI compat adapters or
 * third-party mods.
 */
public final class AmiGuideRegistry {
    private static final Logger LOGGER = Logger.getLogger(AmiGuideRegistry.class.getName());
    private static final Map<ResourceLocation, AmiGuideDocument> DOCUMENTS = new LinkedHashMap<>();

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
}
