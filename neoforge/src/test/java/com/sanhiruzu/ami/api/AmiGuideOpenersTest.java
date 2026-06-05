package com.sanhiruzu.ami.api;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmiGuideOpenersTest {
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
}
