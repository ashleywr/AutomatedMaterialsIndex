package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ClassificationOverrides {
    private static volatile Map<String, ClassificationOverride> itemOverrides = Map.of();
    private static volatile Map<String, List<ModPatternRule>> modPatternRules = Map.of();

    private ClassificationOverrides() {
    }

    public static void install(Map<String, ClassificationOverride> items,
                               Map<String, List<ModPatternRule>> patterns) {
        itemOverrides = Map.copyOf(items);
        modPatternRules = Map.copyOf(patterns);
    }

    public static void clear() {
        itemOverrides = Map.of();
        modPatternRules = Map.of();
    }

    public static Optional<ClassificationOverride> forItem(ResourceLocation id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(itemOverrides.get(id.toString().toLowerCase(Locale.ROOT)));
    }

    public static Optional<ModPatternRule> patternFor(String modId, String path) {
        if (modId == null || path == null) {
            return Optional.empty();
        }
        List<ModPatternRule> rules = modPatternRules.get(modId.toLowerCase(Locale.ROOT));
        if (rules == null) {
            return Optional.empty();
        }
        String[] tokens = path.toLowerCase(Locale.ROOT).split("[_/]");
        for (ModPatternRule rule : rules) {
            for (String token : tokens) {
                if (rule.pathTokens().contains(token)) {
                    return Optional.of(rule);
                }
            }
        }
        return Optional.empty();
    }
}
