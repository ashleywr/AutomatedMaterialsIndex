package com.sanhiruzu.ami.api;

import com.sanhiruzu.searchableguides.api.SearchableGuideDocument;
import com.sanhiruzu.searchableguides.api.SearchableGuideProvider;
import com.sanhiruzu.searchableguides.api.SearchableGuideProviders;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmiGuideRegistryTest {
    @AfterEach
    void cleanup() {
        AmiGuideRegistry.clear();
        AmiPluginRegistry.clearForTests();
        SearchableGuideProviders.clearForTests();
    }

    @Test
    void duplicateIdsReplacePreviousDocumentDeterministically() {
        ResourceLocation id = new ResourceLocation("ami", "guides/test");

        AmiGuideRegistry.register(AmiGuideDocument.builder(id, "plugin", "example", "Old Title").build());
        AmiGuideRegistry.register(AmiGuideDocument.builder(id, "plugin", "example", "New Title").build());

        assertEquals(1, AmiGuideRegistry.size());
        assertEquals("New Title", AmiGuideRegistry.getDocuments().getFirst().title());
    }

    @Test
    void sourceFailureDoesNotRemovePreviouslyRegisteredDocuments() {
        AmiGuideRegistry.registerSource(source("good", documents -> documents.accept(
                AmiGuideDocument.builder(new ResourceLocation("ami", "guides/good"), "plugin", "example", "Good").build()
        )));

        AmiGuideRegistry.registerSource(new AmiGuideSource() {
            @Override
            public String id() {
                return "bad";
            }

            @Override
            public void registerGuideDocuments(java.util.function.Consumer<AmiGuideDocument> documents) {
                throw new IllegalStateException("boom");
            }
        });

        AmiGuideRegistry.registerSource(source("also_good", documents -> documents.accept(
                AmiGuideDocument.builder(new ResourceLocation("ami", "guides/also_good"), "plugin", "example", "Also Good").build()
        )));

        assertEquals(2, AmiGuideRegistry.size());
    }

    @Test
    void openCallbacksAreOptionalAndNotInvokedDuringRegistration() {
        AtomicBoolean opened = new AtomicBoolean(false);

        AmiGuideRegistry.register(AmiGuideDocument.builder(
                        new ResourceLocation("ami", "guides/openable"),
                        "plugin",
                        "example",
                        "Openable"
                )
                .openAction(() -> opened.set(true))
                .build());

        assertFalse(opened.get());
        AmiGuideRegistry.getDocuments().getFirst().open();
        assertTrue(opened.get());
    }

    @Test
    void pluginGuideDocumentsAreCollectedThroughPluginRegistry() {
        AmiPluginRegistry.register(new IAmiPlugin() {
            @Override
            public void addGuideDocuments(Consumer<AmiGuideDocument> documents) {
                documents.accept(AmiGuideDocument.builder(
                        new ResourceLocation("ami", "guides/plugin_doc"),
                        "plugin",
                        "example",
                        "Plugin Guide"
                ).build());
            }
        });

        AmiGuideRegistry.registerPluginGuides();

        assertEquals(1, AmiGuideRegistry.size());
        assertEquals("Plugin Guide", AmiGuideRegistry.getDocuments().getFirst().title());
    }

    @Test
    void pluginGuideFailureDoesNotBlockOtherPlugins() {
        AmiPluginRegistry.register(new IAmiPlugin() {
            @Override
            public void addGuideDocuments(Consumer<AmiGuideDocument> documents) {
                throw new IllegalStateException("bad plugin");
            }
        });
        AmiPluginRegistry.register(new IAmiPlugin() {
            @Override
            public void addGuideDocuments(Consumer<AmiGuideDocument> documents) {
                documents.accept(AmiGuideDocument.builder(
                        new ResourceLocation("ami", "guides/good_plugin_doc"),
                        "plugin",
                        "example",
                        "Good Plugin Guide"
                ).build());
            }
        });

        AmiGuideRegistry.registerPluginGuides();

        assertEquals(1, AmiGuideRegistry.size());
        assertEquals("Good Plugin Guide", AmiGuideRegistry.getDocuments().getFirst().title());
    }

    @Test
    void amiApiRegistersGuideDocumentsAndSources() {
        AmiApi.registerGuideDocument(AmiGuideDocument.builder(
                new ResourceLocation("ami", "guides/api_doc"),
                "plugin",
                "example",
                "API Guide"
        ).build());
        AmiApi.registerGuideSource(source("api_source", documents -> documents.accept(
                AmiGuideDocument.builder(
                        new ResourceLocation("ami", "guides/api_source_doc"),
                        "plugin",
                        "example",
                        "API Source Guide"
                ).build()
        )));

        assertEquals(2, AmiGuideRegistry.size());
    }

    @Test
    void sharedSearchableGuideProvidersAreAdaptedIntoAmiDocuments() {
        SearchableGuideProviders.register(searchableProvider("shared", documents -> documents.accept(
                SearchableGuideDocument.builder(
                                new ResourceLocation("example", "guides/shared_doc"),
                                "example_manual",
                                "example",
                                "Shared Guide")
                        .bookId(new ResourceLocation("example", "manual"))
                        .iconItemId(new ResourceLocation("example", "manual"))
                        .pageId("machines/press")
                        .chapter("Machines")
                        .referencedItem(new ResourceLocation("example", "press"))
                        .tag("machine")
                        .summaryText("A viewer-neutral guide document.")
                        .build()
        )));

        AmiGuideRegistry.registerSearchableGuideProviders();

        assertEquals(1, AmiGuideRegistry.size());
        AmiGuideDocument document = AmiGuideRegistry.getDocuments().getFirst();
        assertEquals("Shared Guide", document.title());
        assertEquals("example_manual", document.sourceType());
        assertEquals(new ResourceLocation("example", "manual"), document.iconItemId());
        assertEquals(List.of(new ResourceLocation("example", "press")), document.referencedItems());
        assertTrue(document.summaryText().contains("viewer-neutral"));
    }

    @Test
    void sharedSearchableGuideProviderFailureDoesNotBlockOtherProviders() {
        SearchableGuideProviders.register(new SearchableGuideProvider() {
            @Override
            public String id() {
                return "bad_shared";
            }

            @Override
            public void addGuideDocuments(Consumer<SearchableGuideDocument> documents) {
                throw new IllegalStateException("bad shared provider");
            }
        });
        SearchableGuideProviders.register(searchableProvider("good_shared", documents -> documents.accept(
                SearchableGuideDocument.builder(
                        new ResourceLocation("example", "guides/good_shared_doc"),
                        "example_manual",
                        "example",
                        "Good Shared Guide"
                ).build()
        )));

        AmiGuideRegistry.registerSearchableGuideProviders();

        assertEquals(1, AmiGuideRegistry.size());
        assertEquals("Good Shared Guide", AmiGuideRegistry.getDocuments().getFirst().title());
    }

    @Test
    void amiApiRegistersSharedSearchableGuideProviders() {
        AmiApi.registerSearchableGuideProvider(searchableProvider("api_shared", documents -> documents.accept(
                SearchableGuideDocument.builder(
                        new ResourceLocation("example", "guides/api_shared_doc"),
                        "example_manual",
                        "example",
                        "API Shared Guide"
                ).build()
        )));

        AmiGuideRegistry.registerSearchableGuideProviders();

        assertEquals(1, AmiGuideRegistry.size());
        assertEquals("API Shared Guide", AmiGuideRegistry.getDocuments().getFirst().title());
    }

    private static AmiGuideSource source(String id, Consumer<Consumer<AmiGuideDocument>> registrar) {
        return new AmiGuideSource() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public void registerGuideDocuments(Consumer<AmiGuideDocument> documents) {
                registrar.accept(documents);
            }
        };
    }

    private static SearchableGuideProvider searchableProvider(String id,
                                                             Consumer<Consumer<SearchableGuideDocument>> registrar) {
        return new SearchableGuideProvider() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public void addGuideDocuments(Consumer<SearchableGuideDocument> documents) {
                registrar.accept(documents);
            }
        };
    }
}
