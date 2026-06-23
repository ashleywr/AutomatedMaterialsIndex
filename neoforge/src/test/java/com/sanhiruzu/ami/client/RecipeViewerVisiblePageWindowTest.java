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

class RecipeViewerVisiblePageWindowTest {

    @Test
    void computeUsesVisibleEntryCountForTheFirstPage() {
        assertEquals(
                new RecipeViewerVisiblePageWindow.Window(0, 0, 2, 2),
                RecipeViewerVisiblePageWindow.compute(0, 2, 3));
    }

    @Test
    void computeClampsRequestedPageIntoTheVisibleRange() {
        assertEquals(
                new RecipeViewerVisiblePageWindow.Window(1, 2, 3, 2),
                RecipeViewerVisiblePageWindow.compute(9, 2, 3));
    }

    @Test
    void computeReturnsASingleEmptyPageWhenNoVisibleEntriesRemain() {
        assertEquals(
                new RecipeViewerVisiblePageWindow.Window(0, 0, 0, 1),
                RecipeViewerVisiblePageWindow.compute(4, 2, 0));
    }

    @Test
    void resolveVisibleCandidateIndexesMatchesWorkstationBackedEntriesWithoutItemStackEquality() {
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

        List<Candidate> candidates = List.of(first, duplicate);
        List<DisplayEntry> visibleEntries = RecipeViewerDisplayEntryPolicy.visibleEntries(candidates);

        assertEquals(List.of(0),
                RecipeViewerVisiblePageWindow.resolveVisibleCandidateIndexes(candidates, visibleEntries));
    }

    @Test
    void resolveVisibleCandidateIndexesSkipsBlankAndNullLayoutCandidatesBeforeVisibleMatch() {
        Candidate blank = candidate(
                "ami:test",
                "ami:shared",
                layout(List.of(), ItemStack.EMPTY, null, 0, 0),
                List.of());
        Candidate nullLayout = candidate(
                "ami:test",
                "ami:shared",
                null,
                List.of());
        Candidate visible = candidate(
                "ami:test",
                "ami:shared",
                layout(
                        List.of(new SlotPosition(0, 0, List.of(stack("ami_test:input_a")))),
                        stack("ami_test:output"),
                        ResourceLocation.parse("ami:textures/gui/test.png"),
                        32,
                        18),
                List.of());

        List<Candidate> candidates = List.of(blank, nullLayout, visible);
        List<DisplayEntry> visibleEntries = RecipeViewerDisplayEntryPolicy.visibleEntries(candidates);

        assertEquals(List.of(2),
                RecipeViewerVisiblePageWindow.resolveVisibleCandidateIndexes(candidates, visibleEntries));
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

    private static ItemStack stack(String itemId) {
        ResourceLocation id = ResourceLocation.parse(itemId);
        registerIfMissing(id);
        return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(id.getNamespace(), id.getPath())));
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
