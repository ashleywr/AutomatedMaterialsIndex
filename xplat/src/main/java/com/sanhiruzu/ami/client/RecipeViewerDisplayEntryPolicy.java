package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.RecipeLayout;
import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.SlotPosition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

final class RecipeViewerDisplayEntryPolicy {
    private static final ResourceLocation ANVIL_REPAIRING = ResourceLocation.parse("ami:anvil_repairing");
    private static final ResourceLocation ENCHANTING = ResourceLocation.parse("ami:enchanting");
    private static final ResourceLocation ANVIL = ResourceLocation.parse("minecraft:anvil");

    private RecipeViewerDisplayEntryPolicy() {
    }

    record Candidate(
            ResourceLocation typeId,
            ResourceLocation displayFamily,
            RecipeLayout layout,
            List<ItemStack> workstations
    ) {
        Candidate {
            workstations = copyStacks(workstations);
        }
    }

    record DisplayEntry(
            ResourceLocation typeId,
            ResourceLocation displayFamily,
            RecipeLayout layout,
            List<ItemStack> workstations
    ) {
        DisplayEntry {
            workstations = copyStacks(workstations);
        }
    }

    static List<ItemStack> canonicalWorkstations(ResourceLocation typeId, List<ItemStack> workstations) {
        if (ANVIL_REPAIRING.equals(typeId) || ENCHANTING.equals(typeId)) {
            for (ItemStack workstation : workstations == null ? List.<ItemStack>of() : workstations) {
                if (workstation != null && !workstation.isEmpty()
                        && ANVIL.equals(BuiltInRegistries.ITEM.getKey(workstation.getItem()))) {
                    return List.of(workstation.copy());
                }
            }
            return List.of();
        }

        LinkedHashSet<ResourceLocation> seen = new LinkedHashSet<>();
        List<ItemStack> canonical = new ArrayList<>();
        for (ItemStack workstation : workstations == null ? List.<ItemStack>of() : workstations) {
            if (workstation == null || workstation.isEmpty()) {
                continue;
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(workstation.getItem());
            if (seen.add(itemId)) {
                canonical.add(workstation.copy());
            }
        }
        return List.copyOf(canonical);
    }

    static List<DisplayEntry> visibleEntries(List<Candidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> seenSignatures = new LinkedHashSet<>();
        List<DisplayEntry> visible = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (candidate == null || !isMeaningfullyRenderable(candidate.layout())) {
                continue;
            }

            DisplayEntry entry = new DisplayEntry(
                    candidate.typeId(),
                    candidate.displayFamily(),
                    candidate.layout(),
                    canonicalWorkstations(candidate.typeId(), candidate.workstations()));
            if (seenSignatures.add(visibleSignature(entry))) {
                visible.add(entry);
            }
        }
        return List.copyOf(visible);
    }

    private static boolean isMeaningfullyRenderable(RecipeLayout layout) {
        if (layout == null) {
            return false;
        }
        if (layout.output() != null && !layout.output().isEmpty()) {
            return true;
        }
        for (SlotPosition slot : layout.inputs()) {
            for (ItemStack alternative : slot.alternatives()) {
                if (alternative != null && !alternative.isEmpty()) {
                    return true;
                }
            }
        }
        return layout.backgroundTexture() != null && layout.bgW() > 0 && layout.bgH() > 0;
    }

    private static String visibleSignature(DisplayEntry entry) {
        StringBuilder signature = new StringBuilder();
        signature.append(Objects.toString(entry.displayFamily(), entry.typeId() == null ? "" : entry.typeId().toString()));
        signature.append('|').append(outputId(entry.layout().output()));
        signature.append('|');
        for (SlotPosition slot : entry.layout().inputs()) {
            signature.append(slot.x()).append(',').append(slot.y()).append(':');
            signature.append(String.join(",", visibleAlternativeIds(slot.alternatives())));
            signature.append(';');
        }
        signature.append('|');
        signature.append(String.join(",", visibleAlternativeIds(entry.workstations())));
        return signature.toString();
    }

    private static List<String> visibleAlternativeIds(List<ItemStack> stacks) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
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

    private static List<ItemStack> copyStacks(List<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            return List.of();
        }
        List<ItemStack> copies = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            copies.add(stack == null ? ItemStack.EMPTY : stack.copy());
        }
        return List.copyOf(copies);
    }
}
