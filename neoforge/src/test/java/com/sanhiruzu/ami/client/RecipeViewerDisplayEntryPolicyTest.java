package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.RecipeViewerDisplayEntryPolicy.Candidate;
import com.sanhiruzu.ami.client.RecipeViewerDisplayEntryPolicy.DisplayEntry;
import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.RecipeLayout;
import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.SlotPosition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipeViewerDisplayEntryPolicyTest {
    @Test
    void canonicalWorkstationsCollapseSpecialAnvilFamiliesToRegularAnvil() {
        List<ItemStack> workstations = List.of(
                stack("ami_test:alpha"),
                stack("ami_test:beta"),
                stack("minecraft:anvil")
        );

        assertEquals(List.of(id("minecraft:anvil")),
                itemIds(RecipeViewerDisplayEntryPolicy.canonicalWorkstations(
                        ResourceLocation.parse("ami:anvil_repairing"),
                        workstations)));
        assertEquals(List.of(id("minecraft:anvil")),
                itemIds(RecipeViewerDisplayEntryPolicy.canonicalWorkstations(
                        ResourceLocation.parse("ami:enchanting"),
                        workstations)));
    }

    @Test
    void canonicalWorkstationsDeduplicateNonEmptyEntriesByItemId() {
        List<ItemStack> workstations = List.of(
                stack("ami_test:alpha"),
                ItemStack.EMPTY,
                stack("ami_test:alpha"),
                stack("ami_test:beta")
        );

        assertEquals(List.of(id("ami_test:alpha"), id("ami_test:beta")),
                itemIds(RecipeViewerDisplayEntryPolicy.canonicalWorkstations(
                        ResourceLocation.parse("minecraft:smelting"),
                        workstations)));
    }

    @Test
    void visibleEntriesFilterBlankLayoutsButKeepTexturedCards() {
        Candidate blank = candidate(
                "ami:test",
                "ami:test",
                layout(List.of(), ItemStack.EMPTY, null, 0, 0),
                List.of());
        Candidate textured = candidate(
                "ami:textured",
                "ami:textured",
                layout(List.of(), ItemStack.EMPTY, ResourceLocation.parse("ami:textures/gui/test.png"), 32, 18),
                List.of(stack("ami_test:output")));

        List<DisplayEntry> visible = RecipeViewerDisplayEntryPolicy.visibleEntries(List.of(blank, textured));

        assertEquals(1, visible.size());
        assertEquals(ResourceLocation.parse("ami:textured"), visible.getFirst().typeId());
    }

    @Test
    void visibleEntriesDeduplicateSameVisibleSignatureAfterWorkstationNormalization() {
        Candidate first = candidate(
                "ami:anvil_repairing",
                "ami:anvil_repairing",
                layout(
                        List.of(
                                new SlotPosition(0, 0, List.of(stack("ami_test:input_a"))),
                                new SlotPosition(49, 0, List.of(stack("ami_test:input_b")))
                        ),
                        stack("ami_test:output"),
                        ResourceLocation.parse("ami:textures/gui/anvil.png"),
                        126,
                        20),
                List.of(stack("minecraft:anvil"), stack("ami_test:alpha")));
        Candidate duplicate = candidate(
                "ami:anvil_repairing",
                "ami:anvil_repairing",
                layout(
                        List.of(
                                new SlotPosition(0, 0, List.of(stack("ami_test:input_a"))),
                                new SlotPosition(49, 0, List.of(stack("ami_test:input_b")))
                        ),
                        stack("ami_test:output"),
                        ResourceLocation.parse("ami:textures/gui/anvil.png"),
                        126,
                        20),
                List.of(stack("minecraft:anvil"), stack("ami_test:beta")));

        List<DisplayEntry> visible = RecipeViewerDisplayEntryPolicy.visibleEntries(List.of(first, duplicate));

        assertEquals(1, visible.size());
        assertEquals(List.of(id("minecraft:anvil")), itemIds(visible.getFirst().workstations()));
    }

    @Test
    void visibleEntriesKeepDistinctLayoutsWhenVisibleSlotsDiffer() {
        Candidate first = candidate(
                "ami:anvil_repairing",
                "ami:anvil_repairing",
                layout(
                        List.of(
                                new SlotPosition(0, 0, List.of(stack("ami_test:input_a"))),
                                new SlotPosition(49, 0, List.of(stack("ami_test:input_b")))
                        ),
                        stack("ami_test:output"),
                        ResourceLocation.parse("ami:textures/gui/anvil.png"),
                        126,
                        20),
                List.of(stack("minecraft:anvil")));
        Candidate second = candidate(
                "ami:anvil_repairing",
                "ami:anvil_repairing",
                layout(
                        List.of(
                                new SlotPosition(0, 0, List.of(stack("ami_test:input_a"))),
                                new SlotPosition(67, 0, List.of(stack("ami_test:input_b")))
                        ),
                        stack("ami_test:output"),
                        ResourceLocation.parse("ami:textures/gui/anvil.png"),
                        126,
                        20),
                List.of(stack("minecraft:anvil")));

        assertEquals(2, RecipeViewerDisplayEntryPolicy.visibleEntries(List.of(first, second)).size());
    }

    private static Candidate candidate(String typeId, String displayFamily, RecipeLayout layout, List<ItemStack> workstations) {
        return new Candidate(ResourceLocation.parse(typeId), ResourceLocation.parse(displayFamily), layout, workstations);
    }

    private static RecipeLayout layout(
            List<SlotPosition> inputs,
            ItemStack output,
            ResourceLocation backgroundTexture,
            int bgW,
            int bgH
    ) {
        return new RecipeLayout(
                ResourceLocation.parse("ami:test_recipe"),
                ItemStack.EMPTY,
                "test",
                inputs,
                output,
                0,
                0,
                false,
                40,
                0,
                20,
                0,
                backgroundTexture,
                0,
                0,
                bgW,
                bgH,
                0,
                0,
                false
        );
    }

    private static List<ResourceLocation> itemIds(List<ItemStack> stacks) {
        return stacks.stream().map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem())).toList();
    }

    private static ItemStack stack(String itemId) {
        ResourceLocation id = id(itemId);
        registerIfMissing(id);
        return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(id.getNamespace(), id.getPath())));
    }

    private static ResourceLocation id(String itemId) {
        return ResourceLocation.parse(itemId);
    }

    private static void registerIfMissing(ResourceLocation id) {
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            BuiltInRegistries.itemRegistry().register(id, new TestItemLike(id.toString()));
        }
    }

    private static final class TestItemLike extends Item implements net.minecraft.world.level.ItemLike {
        private TestItemLike(String name) {
            super(name);
        }

        @Override
        public Item asItem() {
            return this;
        }
    }
}
