package com.sanhiruzu.ami.compat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class JeiRuntimeAccessorTest {

    private static final String CLASS_NAME = "com.sanhiruzu.ami.compat.JeiRuntimeAccessor";

    private Object mockRuntime;

    @BeforeEach
    void createMockRuntime() throws Exception {
        Class<?> iJeiRuntime = Class.forName("mezz.jei.api.runtime.IJeiRuntime");
        mockRuntime = Proxy.newProxyInstance(
                iJeiRuntime.getClassLoader(),
                new Class<?>[]{iJeiRuntime},
                (proxy, method, args) -> null);
    }

    @AfterEach
    void resetRuntime() throws Exception {
        invokeSetRuntime(null);
    }

    private static void invokeSetRuntime(Object runtime) throws Exception {
        Class<?> clazz = Class.forName(CLASS_NAME);
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals("setRuntime")) {
                m.invoke(null, runtime);
                return;
            }
        }
    }

    private static Method findWithRuntimeConsumer() throws Exception {
        Class<?> clazz = Class.forName(CLASS_NAME);
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals("withRuntime") && m.getParameterCount() == 1) {
                return m;
            }
        }
        return null;
    }

    private static Method findWithRuntimeFunction() throws Exception {
        Class<?> clazz = Class.forName(CLASS_NAME);
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals("withRuntime") && m.getParameterCount() == 2) {
                return m;
            }
        }
        return null;
    }

    @Test
    void setRuntimeAcceptsNull() {
        assertDoesNotThrow(() -> invokeSetRuntime(null));
    }

    @Test
    void withRuntimeConsumerDoesNothingWhenRuntimeNotSet() throws Exception {
        Method withRuntime = findWithRuntimeConsumer();
        assertNotNull(withRuntime);

        AtomicReference<String> called = new AtomicReference<>("no");
        withRuntime.invoke(null, (java.util.function.Consumer<Object>) r -> called.set("yes"));
        assertEquals("no", called.get());
    }

    @Test
    void withRuntimeFunctionReturnsDefaultWhenRuntimeNotSet() throws Exception {
        Method withRuntime = findWithRuntimeFunction();
        assertNotNull(withRuntime);

        Object result = withRuntime.invoke(null,
                (java.util.function.Function<Object, String>) r -> "found", "default");
        assertEquals("default", result);
    }

    @Test
    void withRuntimeConsumerCallsActionWhenRuntimeSet() throws Exception {
        invokeSetRuntime(mockRuntime);

        Method withRuntime = findWithRuntimeConsumer();
        assertNotNull(withRuntime);

        AtomicReference<String> called = new AtomicReference<>("no");
        withRuntime.invoke(null, (java.util.function.Consumer<Object>) r -> called.set("yes"));
        assertEquals("yes", called.get());
    }

    @Test
    void withRuntimeFunctionReturnsActionResultWhenRuntimeSet() throws Exception {
        invokeSetRuntime(mockRuntime);

        Method withRuntime = findWithRuntimeFunction();
        assertNotNull(withRuntime);

        Object result = withRuntime.invoke(null,
                (java.util.function.Function<Object, String>) r -> "found", "default");
        assertEquals("found", result);
    }

    @Test
    void withRuntimeConsumerSwallowsExceptions() throws Exception {
        invokeSetRuntime(mockRuntime);

        Method withRuntime = findWithRuntimeConsumer();
        assertNotNull(withRuntime);

        assertDoesNotThrow(() ->
                withRuntime.invoke(null, (java.util.function.Consumer<Object>) r -> {
                    throw new RuntimeException("boom");
                }));
    }

    @Test
    void withRuntimeFunctionSwallowsExceptionsAndReturnsDefault() throws Exception {
        invokeSetRuntime(mockRuntime);

        Method withRuntime = findWithRuntimeFunction();
        assertNotNull(withRuntime);

        Object result = withRuntime.invoke(null,
                (java.util.function.Function<Object, String>) r -> {
                    throw new RuntimeException("boom");
                }, "fallback");
        assertEquals("fallback", result);
    }
}
