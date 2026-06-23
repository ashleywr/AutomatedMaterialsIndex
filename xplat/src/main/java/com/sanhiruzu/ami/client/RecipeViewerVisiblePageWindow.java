package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.RecipeLayout;
import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.SlotPosition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

final class RecipeViewerVisiblePageWindow {
    private RecipeViewerVisiblePageWindow() {
    }

    static Window compute(int requestedPageIndex, int recipesPerPage, int visibleCount) {
        int clampedRecipesPerPage = Math.max(1, recipesPerPage);
        int totalPages = Math.max(1, (int) Math.ceil((double) Math.max(0, visibleCount) / clampedRecipesPerPage));
        int pageIndex = Math.max(0, Math.min(requestedPageIndex, totalPages - 1));
        int startIndex = Math.min(pageIndex * clampedRecipesPerPage, Math.max(0, visibleCount));
        int endIndexExclusive = Math.min(startIndex + clampedRecipesPerPage, Math.max(0, visibleCount));
        return new Window(pageIndex, startIndex, endIndexExclusive, totalPages);
    }

    record Window(int pageIndex, int startIndex, int endIndexExclusive, int totalPages) {
    }

    static List<Integer> resolveVisibleCandidateIndexes(
            List<RecipeViewerDisplayEntryPolicy.Candidate> candidates,
            List<RecipeViewerDisplayEntryPolicy.DisplayEntry> visibleEntries
    ) {
        if (candidates == null || candidates.isEmpty() || visibleEntries == null || visibleEntries.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> firstIndexByKey = new LinkedHashMap<>();
        for (int i = 0; i < candidates.size(); i++) {
            RecipeViewerDisplayEntryPolicy.Candidate candidate = candidates.get(i);
            if (candidate == null) {
                continue;
            }
            firstIndexByKey.putIfAbsent(visibleEntryKey(
                    candidate.typeId(),
                    candidate.displayFamily(),
                    candidate.layout(),
                    RecipeViewerDisplayEntryPolicy.canonicalWorkstations(candidate.typeId(), candidate.workstations())), i);
        }

        List<Integer> resolvedIndexes = new ArrayList<>(visibleEntries.size());
        for (RecipeViewerDisplayEntryPolicy.DisplayEntry visibleEntry : visibleEntries) {
            Integer candidateIndex = firstIndexByKey.remove(visibleEntryKey(
                    visibleEntry.typeId(),
                    visibleEntry.displayFamily(),
                    visibleEntry.layout(),
                    visibleEntry.workstations()));
            if (candidateIndex != null) {
                resolvedIndexes.add(candidateIndex);
            }
        }
        return List.copyOf(resolvedIndexes);
    }

    private static String visibleEntryKey(
            ResourceLocation typeId,
            ResourceLocation displayFamily,
            RecipeLayout layout,
            List<ItemStack> workstations
    ) {
        StringBuilder signature = new StringBuilder();
        signature.append(Objects.toString(displayFamily, typeId == null ? "" : typeId.toString()));
        signature.append('|').append(outputId(layout == null ? ItemStack.EMPTY : layout.output()));
        signature.append('|');
        TreeSet<String> slotSignatures = new TreeSet<>();
        if (layout != null) {
            for (SlotPosition slot : layout.inputs()) {
                slotSignatures.add(slot.x() + "," + slot.y() + ":" + String.join(",", visibleAlternativeIds(slot.alternatives())));
            }
        }
        signature.append(String.join(";", slotSignatures));
        signature.append('|');
        signature.append(String.join(",", visibleAlternativeIds(workstations)));
        return signature.toString();
    }

    private static List<String> visibleAlternativeIds(List<ItemStack> stacks) {
        TreeSet<String> ids = new TreeSet<>();
        for (ItemStack stack : stacks == null ? List.<ItemStack>of() : stacks) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ids.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        }
        return List.copyOf(ids);
    }

    private static String outputId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
