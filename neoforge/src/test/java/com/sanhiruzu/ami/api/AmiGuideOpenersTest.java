package com.sanhiruzu.ami.api;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmiGuideOpenersTest {
    @Test
    void patchouliEntryIdUsesBookNamespaceForPlainPaths() {
        ResourceLocation bookId = ResourceLocation.fromNamespaceAndPath("silentgear", "guide_book");

        assertEquals(ResourceLocation.fromNamespaceAndPath("silentgear", "trait/floatstoner"),
                AmiGuideOpeners.patchouliEntryId(bookId, "trait/floatstoner"));
        assertEquals(ResourceLocation.fromNamespaceAndPath("apotheosis", "affix/socketed"),
                AmiGuideOpeners.patchouliEntryId(bookId, "apotheosis:affix/socketed"));
    }

    @Test
    void patchouliEntryCandidateScoringHandlesClosePaths() {
        assertEquals(1000, AmiGuideOpeners.scorePatchouliEntryCandidate(
                "enchanting/table/stats",
                "enchanting/table/stats"));
        assertTrue(AmiGuideOpeners.scorePatchouliEntryCandidate(
                "trait/floatstoner",
                "gear_traits/floatstoner") > 0);
        assertTrue(AmiGuideOpeners.scorePatchouliEntryCandidate(
                "enchanting_stats/amethyst_cluster",
                "enchanting/table/stats") > 0);
    }

    @Test
    void tryInvokePatchouliSupportsPrimitiveIntParameters() throws Exception {
        class DummyPatchouliApi {
            boolean intOverloadCalled;
            boolean stringOverloadCalled;

            public void openBookEntry(String bookId, String entryId, int page) {
                intOverloadCalled = true;
            }

            public void openBookEntry(String bookId, String entryId, String page) {
                stringOverloadCalled = true;
            }
        }

        DummyPatchouliApi api = new DummyPatchouliApi();
        Method method = AmiGuideOpeners.class.getDeclaredMethod("tryInvokePatchouli", Object.class, String.class, Object[].class);
        method.setAccessible(true);
        boolean result = (Boolean) method.invoke(null, api, "openBookEntry", new Object[]{"book", "entry", 0});

        assertTrue(result);
        assertTrue(api.intOverloadCalled);
        assertFalse(api.stringOverloadCalled);
    }

    @Test
    void patchouliOpenInvocationMustActuallyOpenExpectedBookWhenQueryable() throws Exception {
        class DummyPatchouliApi {
            ResourceLocation openBook;

            public void openBookGUI(ResourceLocation bookId) {
                if ("valid".equals(bookId.getPath())) {
                    openBook = bookId;
                }
            }

            public ResourceLocation getOpenBookGui() {
                return openBook;
            }
        }

        DummyPatchouliApi api = new DummyPatchouliApi();
        Method method = AmiGuideOpeners.class.getDeclaredMethod(
                "tryInvokePatchouliOpen",
                Object.class,
                String.class,
                ResourceLocation.class,
                Object[].class
        );
        method.setAccessible(true);

        ResourceLocation invalidBook = ResourceLocation.fromNamespaceAndPath("example", "missing");
        ResourceLocation validBook = ResourceLocation.fromNamespaceAndPath("example", "valid");

        assertFalse((Boolean) method.invoke(null, api, "openBookGUI", invalidBook, new Object[]{invalidBook}));
        assertTrue((Boolean) method.invoke(null, api, "openBookGUI", validBook, new Object[]{validBook}));
    }
}
