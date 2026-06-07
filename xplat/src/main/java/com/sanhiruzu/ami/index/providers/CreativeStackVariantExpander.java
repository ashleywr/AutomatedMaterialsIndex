package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.GroupingEngine;
import com.sanhiruzu.ami.index.IndexingHotItemPolicy;
import com.sanhiruzu.ami.index.ItemFilter;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
        if (creativeStacks.size() < 2) {
            return List.of();
        }
        if (isSuppressedComponentBackedFamily(baseId)) {
            return List.of();
        }

        List<CreativeStackCandidate> distinct = distinctStacks(creativeStacks);
        if (distinct.size() < 2) {
            return List.of();
        }

        List<VisibleStack> visibleStacks = distinct.stream()
                .map(candidate -> new VisibleStack(candidate.stack(), level, candidate.tabId()))
                .toList();
        Map<String, Integer> displayNameCounts = displayNameCounts(visibleStacks);
        Map<String, Integer> emittedByDisplayName = new HashMap<>();
        Map<String, List<VisibleStack>> emittedByVisibleName = new HashMap<>();
        boolean includeDiagnosticVariants = AmiConfig.devMode;
        int hiddenDuplicateSkips = 0;

        List<SubtypeExpander.SubtypeEntry> result = new ArrayList<>();
        Set<ResourceLocation> emittedIds = new HashSet<>();
        for (VisibleStack visibleStack : visibleStacks) {
            if (result.size() >= SubtypeExpander.HARD_CAP) {
                AmiCore.LOGGER.debug("CreativeStackVariantExpander: hit HARD_CAP for {}; truncating expansion to {} entries.",
                        baseId, SubtypeExpander.HARD_CAP);
                break;
            }

            ItemStack stack = visibleStack.stack;
            String displayName = visibleStack.displayName;

            Map<String, String> extra = new LinkedHashMap<>();
            extra.put(SearchNodeKeys.VARIANT_SOURCE, "creative_tab");
            extra.put(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "auto");
            List<VisibleStack> sameNameStacks = emittedByVisibleName.getOrDefault(visibleStack.displayKey, List.of());
            boolean hiddenDuplicate = visiblyEquivalentToAny(sameNameStacks, visibleStack);
            String displayKey = visibleStack.displayKey;
            int displayOrdinal = emittedByDisplayName.merge(displayKey, 1, Integer::sum) - 1;
            if (hiddenDuplicate) {
                hiddenDuplicateSkips++;
                if (isHiddenDuplicateDiagnosticsEnabled()) {
                    logHiddenDuplicateSkip(baseId, sameNameStacks, visibleStack);
                }
                continue;
            } else if (displayNameCounts.getOrDefault(displayKey, 0) > 1
                    && displayOrdinal > 0
                    && visibleStack.hasPositiveStoredResource()) {
                if (!includeDiagnosticVariants) {
                    continue;
                }
                extra.put(SearchNodeKeys.ACCESS_LEVEL, ItemFilter.ACCESS_CHEAT);
                extra.put("variantAccessReason", "prefilled_creative_stack");
            }

            ResourceLocation syntheticId = syntheticId(baseId, displayName, visibleStack.identityHash(baseId));
            int collisionOrdinal = 1;
            while (!emittedIds.add(syntheticId)) {
                syntheticId = Services.PLATFORM.rl(baseId.getNamespace(), syntheticId.getPath() + "_" + collisionOrdinal++);
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

    private static boolean isSuppressedComponentBackedFamily(ResourceLocation baseId) {
        return IndexingHotItemPolicy.shouldHideComponentBackedVariantsByDefault(baseId);
    }

    private static Map<String, Integer> displayNameCounts(List<VisibleStack> stacks) {
        Map<String, Integer> counts = new HashMap<>();
        for (VisibleStack stack : stacks) {
            counts.merge(stack.displayKey, 1, Integer::sum);
        }
        return counts;
    }

    private static List<CreativeStackCandidate> distinctStacks(List<ItemFilter.CreativeStackInfo> creativeStacks) {
        List<CreativeStackCandidate> result = new ArrayList<>();
        Map<String, List<CreativeStackCandidate>> seenByIdentity = new HashMap<>();
        for (ItemFilter.CreativeStackInfo info : creativeStacks) {
            if (info == null || info.stack() == null || info.stack().isEmpty()) {
                continue;
            }
            ItemStack stack = info.stack().copy();
            stack.setCount(1);
            boolean seen = false;
            String identity = distinctStackIdentity(stack);
            List<CreativeStackCandidate> candidates = seenByIdentity.computeIfAbsent(identity, ignored -> new ArrayList<>());
            for (CreativeStackCandidate existing : candidates) {
                if (Services.PLATFORM.sameItemSameComponents(existing.stack(), stack)) {
                    seen = true;
                    break;
                }
            }
            if (!seen) {
                ItemFilter.CreativeTabInfo tab = info.tab();
                CreativeStackCandidate candidate = new CreativeStackCandidate(stack, tab == null ? "" : tab.id());
                candidates.add(candidate);
                result.add(candidate);
            }
        }
        return result;
    }

    private static String distinctStackIdentity(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String componentKey = reflectedStackDetail(stack, "getComponentsPatch");
        if (componentKey.isBlank()) {
            componentKey = reflectedStackDetail(stack, "getTag");
        }
        if (componentKey.isBlank()) {
            componentKey = reflectedStackDetail(stack, "getShareTag");
        }
        return (itemId == null ? stack.getItem().getClass().getName() : itemId.toString()) + "|" + componentKey;
    }

    private record CreativeStackCandidate(ItemStack stack, String tabId) {
    }

    private static boolean visiblyEquivalentToAny(List<VisibleStack> existingStacks, VisibleStack candidate) {
        for (VisibleStack existing : existingStacks) {
            if (visiblyEquivalentCreativeStack(existing, candidate)) {
                return true;
            }
        }
        return false;
    }

    private static void logHiddenDuplicateSkip(ResourceLocation baseId, List<VisibleStack> existingStacks, VisibleStack candidate) {
        if (!AmiConfig.devMode) {
            return;
        }
        VisibleStack matching = null;
        for (VisibleStack existing : existingStacks) {
            if (visiblyEquivalentCreativeStack(existing, candidate)) {
                matching = existing;
                break;
            }
        }
        AmiCore.LOGGER.trace(
                "CreativeStackVariantExpander: hidden duplicate skipped for {} display='{}' candidateTab={} candidateHash={} existingTab={} existingHash={} tooltipHash={}",
                baseId,
                candidate.displayName,
                candidate.tabId,
                candidate.identityHash(baseId),
                matching != null ? matching.tabId : "?",
                matching != null ? matching.identityHash(baseId) : "?",
                shortHash(String.join("\n", candidate.tooltipSignature()))
        );
    }

    private static boolean isHiddenDuplicateDiagnosticsEnabled() {
        if (!AmiConfig.devMode) {
            return false;
        }
        return Boolean.getBoolean("sanhiruzu.ami.debugHiddenDuplicateVariants");
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

    static String stackIdentityHash(ResourceLocation baseId, ItemStack stack, @Nullable Level level) {
        if (stack == null || stack.isEmpty()) {
            return shortHash(String.valueOf(baseId));
        }
        return stackIdentityHash(
                baseId,
                stack,
                normalizedDisplayName(stack.getHoverName().getString()),
                hasPositiveStoredResource(stack, level),
                tooltipSignature(stack, level)
        );
    }

    private static String stackIdentityHash(ResourceLocation baseId, ItemStack stack, String displayKey,
                                            boolean positiveStoredResource, List<String> tooltipSignature) {
        StringBuilder key = new StringBuilder();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        key.append(baseId).append('\n');
        key.append(itemId == null ? "unknown" : itemId).append('\n');
        key.append(displayKey).append('\n');
        key.append(positiveStoredResource).append('\n');
        for (String line : tooltipSignature) {
            key.append(line).append('\n');
        }

        // Include the platform's component/tag string where available. This keeps
        // diagnostic hidden-component variants stable without depending on loader APIs.
        String componentKey = reflectedStackDetail(stack, "getComponentsPatch");
        if (componentKey.isBlank()) {
            componentKey = reflectedStackDetail(stack, "getTag");
        }
        if (componentKey.isBlank()) {
            componentKey = reflectedStackDetail(stack, "getShareTag");
        }
        if (!componentKey.isBlank()) {
            key.append(componentKey);
        }
        return shortHash(key.toString());
    }

    private static String reflectedStackDetail(ItemStack stack, String methodName) {
        try {
            Object detail = stack.getClass().getMethod(methodName).invoke(stack);
            return detail == null ? "" : detail.toString();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return "";
        }
    }

    private static String shortHash(String input) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] digest = sha1.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 6 && i < digest.length; i++) {
                hex.append(String.format("%02x", digest[i]));
            }
            return hex.toString();
        } catch (RuntimeException | java.security.NoSuchAlgorithmException e) {
            return Integer.toHexString(Objects.hashCode(input));
        }
    }

    private static ResourceLocation syntheticId(ResourceLocation baseId, String displayName, String identityHash) {
        String slug = slug(displayName);
        if (slug.isBlank()) {
            slug = "variant";
        }
        return Services.PLATFORM.rl(baseId.getNamespace(), baseId.getPath() + "/variant/" + slug + "_" + identityHash);
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
        final String tabId;
        private Boolean positiveStoredResource;
        private List<String> tooltipSignature;
        private String identityHash;

        VisibleStack(ItemStack stack, @Nullable Level level) {
            this(stack, level, "");
        }

        VisibleStack(ItemStack stack, @Nullable Level level, String tabId) {
            this.stack = stack;
            this.level = level;
            this.displayName = stack.getHoverName().getString();
            this.displayKey = normalizedDisplayName(displayName);
            this.tabId = tabId == null ? "" : tabId;
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

        String identityHash(ResourceLocation baseId) {
            if (identityHash == null) {
                identityHash = CreativeStackVariantExpander.stackIdentityHash(
                        baseId,
                        stack,
                        displayKey,
                        hasPositiveStoredResource(),
                        tooltipSignature()
                );
            }
            return identityHash;
        }
    }
}
