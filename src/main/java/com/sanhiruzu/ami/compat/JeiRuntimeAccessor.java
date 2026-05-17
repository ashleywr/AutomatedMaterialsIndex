package com.sanhiruzu.ami.compat;

import mezz.jei.api.runtime.IJeiRuntime;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import java.util.function.Consumer;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
class JeiRuntimeAccessor {
    private static IJeiRuntime runtime;

    static void setRuntime(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    static <T> T withRuntime(Function<IJeiRuntime, T> action, T defaultValue) {
        if (runtime != null) {
            try {
                return action.apply(runtime);
            } catch (Exception e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    static void withRuntime(Consumer<IJeiRuntime> action) {
        if (runtime != null) {
            try {
                action.accept(runtime);
            } catch (Exception e) {
                // Silently continue
            }
        }
    }
}
