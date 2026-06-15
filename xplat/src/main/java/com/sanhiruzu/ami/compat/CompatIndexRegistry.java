package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.api.AmiGuideDocument;
import com.sanhiruzu.ami.index.GlobalIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class CompatIndexRegistry {
    private static final List<CompatIndexPlugin> BUILT_IN = List.of(
            new SilentGearPlugin(),
            new PatchouliPlugin(),
            new GuideMePlugin(),
            new ModonomiconPlugin(),
            new ResourceBookPlugin(),
            new ApotheosisPlugin(),
            new MalumCodexPlugin(),
            new CrittersCrawlersPlugin()
    );

    private static final List<CompatIndexPlugin> EXTERNAL = new CopyOnWriteArrayList<>();

    private CompatIndexRegistry() {}

    public static void register(CompatIndexPlugin plugin) {
        if (plugin != null) {
            EXTERNAL.add(plugin);
        }
    }

    public static void applyAll(GlobalIndex index) {
        for (CompatIndexPlugin plugin : plugins()) {
            runSafely(plugin.modId(), "applyToIndex", () -> plugin.applyToIndex(index));
        }
    }

    public static void registerAllGuideDocuments(Consumer<AmiGuideDocument> consumer) {
        for (CompatIndexPlugin plugin : plugins()) {
            runSafely(plugin.modId(), "registerGuideDocuments", () -> plugin.registerGuideDocuments(consumer));
        }
    }

    private static List<CompatIndexPlugin> plugins() {
        if (EXTERNAL.isEmpty()) {
            return BUILT_IN;
        }
        List<CompatIndexPlugin> all = new ArrayList<>(BUILT_IN);
        all.addAll(EXTERNAL);
        return List.copyOf(all);
    }

    private static void runSafely(String modId, String phase, Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            AmiCore.LOGGER.warn("AMI compat '{}' failed during {}.", modId, phase, t);
        }
    }

    private static final class SilentGearPlugin implements CompatIndexPlugin {
        @Override
        public String modId() { return "silentgear"; }

        @Override
        public void applyToIndex(GlobalIndex index) {
            SilentGearMaterialTraitIndex.applyToIndex(index);
        }

        @Override
        public void registerGuideDocuments(Consumer<AmiGuideDocument> registry) {
            SilentGearMaterialBookGuideSource.registerGuideDocuments(registry);
            SilentGearTraitGuideSource.registerGuideDocuments(registry);
        }
    }

    private static final class PatchouliPlugin implements CompatIndexPlugin {
        @Override
        public String modId() { return "patchouli"; }

        @Override
        public void registerGuideDocuments(Consumer<AmiGuideDocument> registry) {
            PatchouliRuntimeGuideSource.registerGuideDocuments(registry);
        }
    }

    private static final class GuideMePlugin implements CompatIndexPlugin {
        @Override
        public String modId() { return "guideme"; }

        @Override
        public void registerGuideDocuments(Consumer<AmiGuideDocument> registry) {
            GuideMeRuntimeGuideSource.registerGuideDocuments(registry);
        }
    }

    private static final class ModonomiconPlugin implements CompatIndexPlugin {
        @Override
        public String modId() { return "modonomicon"; }

        @Override
        public void registerGuideDocuments(Consumer<AmiGuideDocument> registry) {
            ModonomiconRuntimeGuideSource.registerGuideDocuments(registry);
        }
    }

    private static final class ResourceBookPlugin implements CompatIndexPlugin {
        @Override
        public String modId() { return "resource_book"; }

        @Override
        public void registerGuideDocuments(Consumer<AmiGuideDocument> registry) {
            ResourceBookRuntimeGuideSource.registerGuideDocuments(registry);
        }
    }

    private static final class ApotheosisPlugin implements CompatIndexPlugin {
        @Override
        public String modId() { return "apotheosis"; }

        @Override
        public void registerGuideDocuments(Consumer<AmiGuideDocument> registry) {
            ApotheosisGuideSource.registerGuideDocuments(registry);
        }
    }

    private static final class MalumCodexPlugin implements CompatIndexPlugin {
        @Override
        public String modId() { return "malum"; }

        @Override
        public void registerGuideDocuments(Consumer<AmiGuideDocument> registry) {
            MalumCodexGuideSource.registerGuideDocuments(registry);
        }
    }

    private static final class CrittersCrawlersPlugin implements CompatIndexPlugin {
        @Override
        public String modId() { return "cnc"; }

        @Override
        public void registerGuideDocuments(Consumer<AmiGuideDocument> registry) {
            CrittersCrawlersGuideSource.registerGuideDocuments(registry);
        }
    }
}
