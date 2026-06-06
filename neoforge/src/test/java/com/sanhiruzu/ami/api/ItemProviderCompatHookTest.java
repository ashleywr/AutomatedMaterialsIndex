package com.sanhiruzu.ami.api;

import com.sanhiruzu.searchableitems.api.SearchableItemProvider;
import com.sanhiruzu.searchableitems.api.SearchableItemProviders;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemProviderCompatHookTest {
    @BeforeEach
    @AfterEach
    void resetState() {
        AmiPluginRegistry.clearForTests();
        SearchableItemProviders.clearForTests();
        ItemProviderCompatHooks.clearDisabledCompatHooks();
    }

    @Test
    void runCompatSafelyDisablesCompatForRemainingItemsInPopulatePass() {
        AtomicInteger compatCalls = new AtomicInteger();

        ItemProviderCompatHooks.runCompatSafely("CompatFail", () -> {
            compatCalls.incrementAndGet();
            throw new IllegalStateException("boom");
        });
        ItemProviderCompatHooks.runCompatSafely("CompatFail", compatCalls::incrementAndGet);
        ItemProviderCompatHooks.runCompatSafely("CompatPass", compatCalls::incrementAndGet);

        assertEquals(2, compatCalls.get());
        assertTrue(ItemProviderCompatHooks.getDisabledCompatHooks().contains("CompatFail"));
        assertFalse(ItemProviderCompatHooks.getDisabledCompatHooks().contains("CompatPass"));
    }

    @Test
    void runCompatSafelyDisablesKnownBuiltinCompatOnFailure() {
        AtomicBoolean executedSecond = new AtomicBoolean();

        ItemProviderCompatHooks.runCompatSafely("AE2Compat", () -> {
            throw new IllegalStateException("compat failed");
        });
        ItemProviderCompatHooks.runCompatSafely("AE2Compat", () -> executedSecond.set(true));

        assertFalse(executedSecond.get());
        assertTrue(ItemProviderCompatHooks.getDisabledCompatHooks().contains("AE2Compat"));
    }

    @Test
    void pluginCompatErrorsDoNotAbortItemIndexing() {
        AtomicInteger failingPluginCalls = new AtomicInteger();
        AtomicInteger safePluginCalls = new AtomicInteger();

        AmiPluginRegistry.register(new IAmiPlugin() {
            @Override
            public void enrichItemMeta(ResourceLocation id, ItemStack stack, Level level, Map<String, String> metadata) {
                failingPluginCalls.incrementAndGet();
                throw new IllegalStateException("compat exploded");
            }
        });

        AmiPluginRegistry.register(new IAmiPlugin() {
            @Override
            public void enrichItemMeta(ResourceLocation id, ItemStack stack, Level level, Map<String, String> metadata) {
                safePluginCalls.incrementAndGet();
                if (id.getPath().equals("stone")) {
                    metadata.put("ami", "safe_plugin");
                }
            }
        });

        Map<String, String> metadata = new HashMap<>();
        ItemProviderCompatHooks.runPluginItemCompatHooks(new ResourceLocation("minecraft", "stone"),
                ItemStack.EMPTY, null, metadata);

        assertEquals(1, failingPluginCalls.get());
        assertEquals(1, safePluginCalls.get());
        assertEquals("safe_plugin", metadata.get("ami"));
    }

    @Test
    void failingPluginMetadataCompatIsDisabledAfterFirstFailure() {
        AtomicInteger failingPluginCalls = new AtomicInteger();

        AmiPluginRegistry.register(new IAmiPlugin() {
            @Override
            public void enrichItemMeta(ResourceLocation id, ItemStack stack, Level level, Map<String, String> metadata) {
                failingPluginCalls.incrementAndGet();
                throw new RuntimeException("expected failure");
            }
        });

        ItemProviderCompatHooks.runPluginItemCompatHooks(new ResourceLocation("minecraft", "stone"),
                ItemStack.EMPTY, null, new HashMap<>());
        ItemProviderCompatHooks.runPluginItemCompatHooks(new ResourceLocation("minecraft", "stone"),
                ItemStack.EMPTY, null, new HashMap<>());

        assertEquals(1, failingPluginCalls.get(),
                "plugin enrichItemMeta should be disabled after first failure in the same index pass");
    }

    @Test
    void pluginMetadataHooksRunForBothBaseItemsAndExpandedSubtypes() {
        AtomicInteger baseItemCalls = new AtomicInteger();
        AtomicInteger subtypeCalls = new AtomicInteger();

        AmiPluginRegistry.register(new IAmiPlugin() {
            @Override
            public void enrichItemMeta(ResourceLocation id, ItemStack stack, Level level, Map<String, String> metadata) {
                if ("minecraft".equals(id.getNamespace()) && "stone".equals(id.getPath())) {
                    baseItemCalls.incrementAndGet();
                    metadata.put("ami", "base");
                }
                if (id.getPath().startsWith("potion/")) {
                    subtypeCalls.incrementAndGet();
                    metadata.put("ami", "subtype");
                }
            }
        });

        Map<String, String> baseMetadata = new HashMap<>();
        Map<String, String> subtypeMetadata = new HashMap<>();

        ItemProviderCompatHooks.runPluginItemCompatHooks(new ResourceLocation("minecraft", "stone"),
                ItemStack.EMPTY, null, baseMetadata);
        ItemProviderCompatHooks.runPluginItemCompatHooks(new ResourceLocation("minecraft", "potion/strength"),
                ItemStack.EMPTY, null, subtypeMetadata);

        assertEquals(1, baseItemCalls.get(),
                "expected plugin hook to run for base-item path");
        assertEquals(1, subtypeCalls.get(),
                "expected plugin hook to run for subtype paths");
        assertEquals("base", baseMetadata.get("ami"));
        assertEquals("subtype", subtypeMetadata.get("ami"));
    }

    @Test
    void sharedItemProviderMetadataHooksAreApplied() {
        SearchableItemProviders.register(new SearchableItemProvider() {
            @Override
            public String id() {
                return "example:items";
            }

            @Override
            public void enrichItemMetadata(ResourceLocation id, ItemStack stack, Level level, Map<String, String> metadata) {
                if ("stone".equals(id.getPath())) {
                    metadata.put("shared", "provider");
                }
            }
        });

        Map<String, String> metadata = new HashMap<>();
        ItemProviderCompatHooks.runPluginItemCompatHooks(new ResourceLocation("minecraft", "stone"),
                ItemStack.EMPTY, null, metadata);

        assertEquals("provider", metadata.get("shared"));
    }

    @Test
    void failingSharedItemProviderIsDisabledAfterFirstFailure() {
        AtomicInteger failingProviderCalls = new AtomicInteger();
        SearchableItemProviders.register(new SearchableItemProvider() {
            @Override
            public String id() {
                return "example:failing";
            }

            @Override
            public void enrichItemMetadata(ResourceLocation id, ItemStack stack, Level level, Map<String, String> metadata) {
                failingProviderCalls.incrementAndGet();
                throw new RuntimeException("expected failure");
            }
        });

        ItemProviderCompatHooks.runPluginItemCompatHooks(new ResourceLocation("minecraft", "stone"),
                ItemStack.EMPTY, null, new HashMap<>());
        ItemProviderCompatHooks.runPluginItemCompatHooks(new ResourceLocation("minecraft", "stone"),
                ItemStack.EMPTY, null, new HashMap<>());

        assertEquals(1, failingProviderCalls.get());
        assertTrue(ItemProviderCompatHooks.getDisabledCompatHooks().contains("SearchableItemProvider.example:failing"));
    }
}
