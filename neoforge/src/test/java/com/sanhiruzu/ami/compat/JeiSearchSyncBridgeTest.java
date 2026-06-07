package com.sanhiruzu.ami.compat;

import mezz.jei.api.runtime.IIngredientFilter;
import mezz.jei.api.runtime.IJeiRuntime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class JeiSearchSyncBridgeTest {

    @AfterEach
    void resetRuntime() {
        JeiRuntimeAccessor.clearRuntime();
    }

    @Test
    void sanitizeSearchTextClampsLongTextBeforeJeiSync() {
        String text = "a".repeat(JeiSearchSyncBridge.MAX_SYNC_SEARCH_TEXT_LENGTH + 1);

        String safeText = JeiSearchSyncBridge.sanitizeSearchText(text);

        assertEquals(JeiSearchSyncBridge.MAX_SYNC_SEARCH_TEXT_LENGTH, safeText.length());
        assertEquals("a".repeat(JeiSearchSyncBridge.MAX_SYNC_SEARCH_TEXT_LENGTH), safeText);
    }

    @Test
    void sanitizeSearchTextKeepsShortText() {
        assertEquals("minecraft:stone", JeiSearchSyncBridge.sanitizeSearchText("minecraft:stone"));
    }

    @Test
    void sanitizeSearchTextTreatsNullAsEmpty() {
        assertEquals("", JeiSearchSyncBridge.sanitizeSearchText(null));
    }

    @Test
    void setSearchTextSendsClampedTextToJei() {
        AtomicReference<String> syncedText = new AtomicReference<>();
        IIngredientFilter filter = ingredientFilterProxy((proxy, method, args) -> {
            if (method.getName().equals("setFilterText")) {
                syncedText.set((String) args[0]);
            }
            return null;
        });
        JeiRuntimeAccessor.setRuntime(runtimeProxy(filter));

        JeiSearchSyncBridge.setSearchText("b".repeat(JeiSearchSyncBridge.MAX_SYNC_SEARCH_TEXT_LENGTH + 10));

        assertEquals("b".repeat(JeiSearchSyncBridge.MAX_SYNC_SEARCH_TEXT_LENGTH), syncedText.get());
    }

    @Test
    void setSearchTextDoesNotPropagateJeiStackOverflow() {
        IIngredientFilter filter = ingredientFilterProxy((proxy, method, args) -> {
            if (method.getName().equals("setFilterText")) {
                throw new StackOverflowError("simulated jei search recursion");
            }
            return null;
        });
        JeiRuntimeAccessor.setRuntime(runtimeProxy(filter));

        assertDoesNotThrow(() -> JeiSearchSyncBridge.setSearchText("tooltip dump"));
    }

    private static IJeiRuntime runtimeProxy(IIngredientFilter filter) {
        return (IJeiRuntime) Proxy.newProxyInstance(
                IJeiRuntime.class.getClassLoader(),
                new Class<?>[]{IJeiRuntime.class},
                (proxy, method, args) -> method.getName().equals("getIngredientFilter") ? filter : null);
    }

    private static IIngredientFilter ingredientFilterProxy(java.lang.reflect.InvocationHandler handler) {
        return (IIngredientFilter) Proxy.newProxyInstance(
                IIngredientFilter.class.getClassLoader(),
                new Class<?>[]{IIngredientFilter.class},
                handler);
    }
}
