package com.sanhiruzu.ami.compat;

import mezz.jei.api.runtime.IJeiRuntime;

import java.util.function.Consumer;
import java.util.function.Function;

public class JeiRuntimeAccessor {
    private static IJeiRuntime runtime;

    public static void setRuntime(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    public static void clearRuntime() {
        runtime = null;
    }

    public static <T> T withRuntime(Function<IJeiRuntime, T> action, T defaultValue) {
        if (runtime != null) {
            try {
                return action.apply(runtime);
            } catch (Exception e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public static void withRuntime(Consumer<IJeiRuntime> action) {
        if (runtime != null) {
            try {
                action.accept(runtime);
            } catch (Exception e) {
            }
        }
    }
}
