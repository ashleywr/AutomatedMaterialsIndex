package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.index.GroupingEngine;
import com.sanhiruzu.ami.index.ItemFilter;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns representative creative-tab stacks into AMI subtype nodes when a single
 * registered item advertises multiple component-backed variants.
 */
public final class CreativeStackVariantExpander {
    private static final Pattern RESOURCE_AMOUNT_PATTERN = Pattern.compile(
            "([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*(?:[kmg]?\\s*)?(?:fe|rf|mb|buckets?|liters?|l|j|eu)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NUMBER_PATTERN = Pattern.compile("([0-9][0-9,]*(?:\\.[0-9]+)?)");
    private static final List<String> WOOD_TOKENS = List.of(
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "bamboo", "crimson", "warped"
    );

    private CreativeStackVariantExpander() {
    }

    public static List<SubtypeExpander.SubtypeEntry> expand(
            ResourceLocation baseId,
            List<ItemFilter.CreativeStackInfo> creativeStacks,
            @Nullable Level level
    ) {
        if (baseId == null || creativeStacks == null || creativeStacks.isEmpty()) {
            return List.of();
        }

        List<ItemStack> distinct = distinctStacks(creativeStacks);
        if (distinct.size() < 2) {
            return List.of();
        }

        List<VisibleStack> visibleStacks = distinct.stream()
                .map(stack -> new VisibleStack(stack, level))
                .toList();
        Map<String, Integer> displayNameCounts = displayNameCounts(visibleStacks);
        Map<String, Integer> emittedByDisplayName = new HashMap<>();
        Map<String, List<VisibleStack>> emittedByVisibleName = new HashMap<>();
        boolean includeDiagnosticVariants = ItemFilter.shouldShowAccessLevel(ItemFilter.ACCESS_CHEAT);

        List<SubtypeExpander.SubtypeEntry> result = new ArrayList<>();
        Set<ResourceLocation> emittedIds = new HashSet<>();
        int ordinal = 0;
        for (VisibleStack visibleStack : visibleStacks) {
            if (result.size() >= SubtypeExpander.HARD_CAP) {
                AmiCore.LOGGER.debug("CreativeStackVariantExpander: hit HARD_CAP for {}; truncating expansion to {} entries.",
                        baseId, SubtypeExpander.HARD_CAP);
                break;
            }

            ItemStack stack = visibleStack.stack;
            String displayName = visibleStack.displayName;
            ResourceLocation syntheticId = syntheticId(baseId, displayName, ordinal++);
            while (!emittedIds.add(syntheticId)) {
                syntheticId = Services.PLATFORM.rl(baseId.getNamespace(), syntheticId.getPath() + "_" + ordinal++);
            }

            Map<String, String> extra = new LinkedHashMap<>();
            extra.put(SearchNodeKeys.VARIANT_SOURCE, "creative_tab");
            extra.put(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "auto");
            List<VisibleStack> sameNameStacks = emittedByVisibleName.getOrDefault(visibleStack.displayKey, List.of());
            boolean hiddenDuplicate = visiblyEquivalentToAny(sameNameStacks, visibleStack);
            String displayKey = visibleStack.displayKey;
            int displayOrdinal = emittedByDisplayName.merge(displayKey, 1, Integer::sum) - 1;
            if (hiddenDuplicate) {
                if (!includeDiagnosticVariants) {
                    continue;
                }
                extra.put(SearchNodeKeys.ACCESS_LEVEL, ItemFilter.ACCESS_CHEAT);
                extra.put("variantAccessReason", "hidden_component_duplicate");
            } else if (displayNameCounts.getOrDefault(displayKey, 0) > 1
                    && displayOrdinal > 0
                    && visibleStack.hasPositiveStoredResource()) {
                if (!includeDiagnosticVariants) {
                    continue;
                }
                extra.put(SearchNodeKeys.ACCESS_LEVEL, ItemFilter.ACCESS_CHEAT);
                extra.put("variantAccessReason", "prefilled_creative_stack");
            }
            String axes = variantAxes(baseId, displayName);
            if (!axes.isBlank()) {
                extra.put(SearchNodeKeys.VARIANT_AXES, axes);
            }
            String color = colorBucket(baseId, displayName);
            if (!color.isBlank()) {
                extra.put(SearchNodeKeys.COLOR_BUCKET, color);
            }

            result.add(new SubtypeExpander.SubtypeEntry(syntheticId, stack, displayName, extra));
            emittedByVisibleName.computeIfAbsent(visibleStack.displayKey, ignored -> new ArrayList<>())
                    .add(visibleStack);
        }
        return result;
    }

    private static Map<String, Integer> displayNameCounts(List<VisibleStack> stacks) {
        Map<String, Integer> counts = new HashMap<>();
        for (VisibleStack stack : stacks) {
            counts.merge(stack.displayKey, 1, Integer::sum);
        }
        return counts;
    }

    private static List<ItemStack> distinctStacks(List<ItemFilter.CreativeStackInfo> creativeStacks) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemFilter.CreativeStackInfo info : creativeStacks) {
            if (info == null || info.stack() == null || info.stack().isEmpty()) {
                continue;
            }
            ItemStack stack = info.stack().copy();
            stack.setCount(1);
            boolean seen = false;
            for (ItemStack existing : result) {
                if (Services.PLATFORM.sameItemSameComponents(existing, stack)) {
                    seen = true;
                    break;
                }
            }
            if (!seen) {
                result.add(stack);
            }
        }
        return result;
    }

    private static boolean visiblyEquivalentToAny(List<VisibleStack> existingStacks, VisibleStack candidate) {
        for (VisibleStack existing : existingStacks) {
            if (visiblyEquivalentCreativeStack(existing, candidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean visiblyEquivalentCreativeStack(VisibleStack first, VisibleStack second) {
        if (first == null || second == null || first.stack.isEmpty() || second.stack.isEmpty()) {
            return false;
        }
        if (first.stack.getItem() != second.stack.getItem()) {
            return false;
        }
        if (!first.displayKey.equals(second.displayKey)) {
            return false;
        }
        if (first.hasPositiveStoredResource() != second.hasPositiveStoredResource()) {
            return false;
        }
        return first.tooltipSignature().equals(second.tooltipSignature());
    }

    private static List<String> tooltipSignature(ItemStack stack, @Nullable Level level) {
        try {
            return Services.PLATFORM.getTooltipLines(stack, level)
                    .stream()
                    .map(Component::getString)
                    .map(CreativeStackVariantExpander::normalizeTooltipLine)
                    .filter(line -> !line.isBlank())
                    .toList();
        } catch (RuntimeException | LinkageError ignored) {
            return List.of();
        }
    }

    private static ResourceLocation syntheticId(ResourceLocation baseId, String displayName, int ordinal) {
        String slug = slug(displayName);
        if (slug.isBlank()) {
            slug = "variant_" + ordinal;
        }
        return Services.PLATFORM.rl(baseId.getNamespace(), baseId.getPath() + "/variant/" + slug + "_" + ordinal);
    }

    private static String variantAxes(ResourceLocation baseId, String displayName) {
        LinkedHashSet<String> axes = new LinkedHashSet<>();
        if (!colorBucket(baseId, displayName).isBlank()) {
            axes.add("color");
        }
        if (hasWoodToken(displayName) || hasWoodToken(baseId.getPath())) {
            axes.add("wood");
        }
        return String.join(",", axes);
    }

    private static boolean hasPositiveStoredResource(ItemStack stack, @Nullable Level level) {
        if (safeItemEnergyStored(stack) > 0) {
            return true;
        }
        if (safeItemFluidAmount(stack) > 0L) {
            return true;
        }

        try {
            return tooltipIndicatesPositiveStoredResource(Services.PLATFORM.getTooltipLines(stack, level)
                    .stream()
                    .map(Component::getString)
                    .toList());
        } catch (RuntimeException | LinkageError ignored) {
        }
        return false;
    }

    private static int safeItemEnergyStored(ItemStack stack) {
        try {
            return Services.PLATFORM.getItemEnergyStored(stack).orElse(0);
        } catch (RuntimeException | LinkageError ignored) {
            return 0;
        }
    }

    private static long safeItemFluidAmount(ItemStack stack) {
        try {
            return Services.PLATFORM.getItemFluidAmount(stack).orElse(0L);
        } catch (RuntimeException | LinkageError ignored) {
            return 0L;
        }
    }

    static boolean tooltipIndicatesPositiveStoredResource(Collection<String> tooltipLines) {
        if (tooltipLines == null || tooltipLines.isEmpty()) {
            return false;
        }
        for (String tooltipLine : tooltipLines) {
            String line = normalizeTooltipLine(tooltipLine);
            if (line.isBlank() || isCapacityOnlyLine(line)) {
                continue;
            }
            if (line.contains("/") && firstAmountBeforeSlash(line) > 0.0D) {
                return true;
            }
            if (line.contains("/")) {
                continue;
            }
            if (isStoredResourceLine(line) && firstResourceAmount(line) > 0.0D) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeTooltipLine(String line) {
        return line == null ? "" : line.toLowerCase(Locale.ROOT)
                .replace(",", "")
                .replace('_', ' ')
                .trim();
    }

    private static boolean isCapacityOnlyLine(String line) {
        return line.contains("capacity") && !line.contains("/");
    }

    private static boolean isStoredResourceLine(String line) {
        return (line.contains("stored")
                || line.contains("contents")
                || line.contains("energy:")
                || line.contains("fluid:")
                || line.contains("gas:")
                || line.contains("chemical:"))
                && !line.contains("capacity");
    }

    private static double firstResourceAmount(String line) {
        Matcher matcher = RESOURCE_AMOUNT_PATTERN.matcher(line);
        if (!matcher.find()) {
            return 0.0D;
        }
        try {
            return Double.parseDouble(matcher.group(1).replace(",", ""));
        } catch (NumberFormatException e) {
            return 0.0D;
        }
    }

    private static double firstAmountBeforeSlash(String line) {
        int slash = line.indexOf('/');
        String beforeSlash = slash >= 0 ? line.substring(0, slash) : line;
        Matcher matcher = NUMBER_PATTERN.matcher(beforeSlash);
        if (!matcher.find()) {
            return 0.0D;
        }
        try {
            return Double.parseDouble(matcher.group(1).replace(",", ""));
        } catch (NumberFormatException e) {
            return 0.0D;
        }
    }

    private static String colorBucket(ResourceLocation baseId, String displayName) {
        String fromDisplay = GroupingEngine.classifyColorFromPath(slug(displayName));
        if (!fromDisplay.isBlank()) {
            return fromDisplay;
        }
        return GroupingEngine.classifyColorFromPath(baseId.getPath());
    }

    private static String normalizedDisplayName(String displayName) {
        return slug(displayName);
    }

    private static boolean hasWoodToken(String value) {
        String normalized = slug(value);
        for (String token : WOOD_TOKENS) {
            if (hasToken(normalized, token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasToken(String value, String token) {
        for (String part : value.split("_+")) {
            if (part.equals(token)) {
                return true;
            }
        }
        return value.contains("_" + token + "_")
                || value.startsWith(token + "_")
                || value.endsWith("_" + token);
    }

    private static String slug(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_");
        while (normalized.startsWith("_")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("_")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static final class VisibleStack {
        final ItemStack stack;
        final Level level;
        final String displayName;
        final String displayKey;
        private Boolean positiveStoredResource;
        private List<String> tooltipSignature;

        VisibleStack(ItemStack stack, @Nullable Level level) {
            this.stack = stack;
            this.level = level;
            this.displayName = stack.getHoverName().getString();
            this.displayKey = normalizedDisplayName(displayName);
        }

        boolean hasPositiveStoredResource() {
            if (positiveStoredResource == null) {
                positiveStoredResource = CreativeStackVariantExpander.hasPositiveStoredResource(stack, level);
            }
            return positiveStoredResource;
        }

        List<String> tooltipSignature() {
            if (tooltipSignature == null) {
                tooltipSignature = CreativeStackVariantExpander.tooltipSignature(stack, level);
            }
            return tooltipSignature;
        }
    }
}
