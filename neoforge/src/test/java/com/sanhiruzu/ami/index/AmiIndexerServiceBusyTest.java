package com.sanhiruzu.ami.index;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class AmiIndexerServiceBusyTest {
    private static final String[] BUSY_FLAGS = {
            "isRebuilding",
            "isDeferredIndexing",
            "isDeferredGuideIndexing",
            "pendingRecipeIndexRebuild",
            "isRecipeIndexRebuilding"
    };

    @AfterEach
    void resetBusyFlags() throws Exception {
        for (String flag : BUSY_FLAGS) {
            setFlag(flag, false);
        }
    }

    @Test
    void reportsIdleWhenNoIndexingWorkIsActive() {
        assertFalse(AmiIndexerService.getInstance().isBusy());
    }

    @Test
    void reportsBusyForEveryIndexingPhase() throws Exception {
        for (String flag : BUSY_FLAGS) {
            setFlag(flag, true);
            assertTrue(AmiIndexerService.getInstance().isBusy(), flag);
            setFlag(flag, false);
            assertFalse(AmiIndexerService.getInstance().isBusy(), flag);
        }
    }

    private static void setFlag(String name, boolean value) throws Exception {
        Field field = AmiIndexerService.class.getDeclaredField(name);
        field.setAccessible(true);
        ((AtomicBoolean) field.get(AmiIndexerService.getInstance())).set(value);
    }
}
