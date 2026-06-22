package com.sanhiruzu.ami.index;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrimaryCategoryModFamiliesTest {
    @Test
    void classifiesCreateAndCreateAddons() {
        assertEquals(PrimaryCategoryModFamily.CREATE, PrimaryCategoryModFamilies.classify("create"));
        assertEquals(PrimaryCategoryModFamily.CREATE, PrimaryCategoryModFamilies.classify("create_new_age"));
        assertEquals(PrimaryCategoryModFamily.CREATE, PrimaryCategoryModFamilies.classify("railways"));
    }

    @Test
    void classifiesStorageAndPortableStorageFamiliesBeforeGenericStorageWords() {
        assertEquals(PrimaryCategoryModFamily.PORTABLE_STORAGE, PrimaryCategoryModFamilies.classify("sophisticatedbackpacks"));
        assertEquals(PrimaryCategoryModFamily.STORAGE, PrimaryCategoryModFamilies.classify("ae2"));
        assertEquals(PrimaryCategoryModFamily.STORAGE, PrimaryCategoryModFamilies.classify("refinedstorage"));
    }

    @Test
    void classifiesFoodDecorAndAutomationFamilies() {
        assertEquals(PrimaryCategoryModFamily.FOOD, PrimaryCategoryModFamilies.classify("farmersdelight"));
        assertEquals(PrimaryCategoryModFamily.DECOR, PrimaryCategoryModFamilies.classify("mcwdoors"));
        assertEquals(PrimaryCategoryModFamily.AUTOMATION, PrimaryCategoryModFamilies.classify("pneumaticcraft"));
    }

    @Test
    void blankAndUnknownModsAreGeneric() {
        assertEquals(PrimaryCategoryModFamily.GENERIC, PrimaryCategoryModFamilies.classify(null));
        assertEquals(PrimaryCategoryModFamily.GENERIC, PrimaryCategoryModFamilies.classify(""));
        assertEquals(PrimaryCategoryModFamily.GENERIC, PrimaryCategoryModFamilies.classify("example"));
    }
}
