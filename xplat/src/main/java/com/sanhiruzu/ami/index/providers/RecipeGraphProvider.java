package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.index.EdgeType;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.IAmiDataProvider;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.recipe.AmiRecipeCategoryRegistry;
import com.sanhiruzu.ami.recipe.AmiRecipeIndex;
import com.sanhiruzu.ami.util.AmiRecipeHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Emits NodeType.RECIPE nodes and wires bidirectional edges to NodeType.ITEM nodes.
 *
 * Must run after RecipeProvider (which rebuilds AmiRecipeIndex) and after ItemProvider
 * (which creates the ITEM nodes that edges attach to).
 *
 * RECIPE nodes are not serialized to the index cache — they are rebuilt every session
 * because recipe data is world/datapack-specific.
 *
 * After this provider runs, any ITEM node supports:
 *   node.getEdges(EdgeType.OUTPUT_OF)  → RECIPE nodes that produce this item
 *   node.getEdges(EdgeType.USED_IN)    → RECIPE nodes that consume this item
 *
 * And any RECIPE node supports:
 *   node.getEdges(EdgeType.PRODUCES)   → the ITEM node(s) this recipe outputs
 *   node.getEdges(EdgeType.REQUIRES)   → ITEM nodes consumed as ingredients
 */
public class RecipeGraphProvider implements IAmiDataProvider {

    @Override
    public void populate(GlobalIndex index, @Nullable Level level) {
        long start = System.currentTimeMillis();
        AmiRecipeIndex recipeIndex = AmiRecipeIndex.getInstance();
        if (!recipeIndex.isBuilt()) {
            AmiCore.LOGGER.warn("AMI: RecipeGraphProvider ran before AmiRecipeIndex was built — skipping");
            return;
        }

        Set<ResourceLocation> emitted = new HashSet<>();
        int nodeCount = 0;
        int edgeCount = 0;

        // Phase 1: recipes with a known output item
        for (Item outputItem : recipeIndex.getAllOutputItems()) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(outputItem);
            if (itemId == null) continue;

            for (AmiRecipeHolder<?> recipe : recipeIndex.getRecipesFor(new ItemStack(outputItem))) {
                ResourceLocation recipeId = recipe.id();
                if (!emitted.add(recipeId)) continue;

                RecipeType<?> recipeType = recipe.value().getType();
                String typeId = recipeTypeId(recipeType);
                String displayName = index.getNode(itemId, NodeType.ITEM)
                        .map(SearchNode::displayName)
                        .orElse(itemId.getPath());

                Map<String, String> meta = new LinkedHashMap<>();
                meta.put(SearchNodeKeys.RECIPE_TYPE_ID, typeId);
                meta.put(SearchNodeKeys.MOD_ID, recipeId.getNamespace());
                putRecipeMethodMetadata(meta, recipeType);

                SearchNode recipeNode = new SearchNode(recipeId, NodeType.RECIPE,
                        displayName, 0xFFFFFFFF, 0, meta);

                // RECIPE → output ITEM
                recipeNode.addUnresolvedEdge(EdgeType.PRODUCES, itemId);
                edgeCount++;

                // RECIPE → ingredient ITEMs (deduplicated per recipe)
                Set<ResourceLocation> seenIngredients = new LinkedHashSet<>();
                try {
                    for (var ingredient : recipe.value().getIngredients()) {
                        for (ItemStack stack : ingredient.getItems()) {
                            if (stack.isEmpty()) continue;
                            ResourceLocation ingId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                            if (ingId != null && seenIngredients.add(ingId)) {
                                recipeNode.addUnresolvedEdge(EdgeType.REQUIRES, ingId);
                                edgeCount++;
                            }
                        }
                    }
                } catch (Exception ignored) {}

                index.addNode(recipeNode);
                nodeCount++;

                // ITEM → RECIPE (output_of)
                index.getNode(itemId, NodeType.ITEM).ifPresent(itemNode -> {
                    itemNode.addUnresolvedEdge(EdgeType.OUTPUT_OF, recipeId);
                });

                // ingredient ITEMs → RECIPE (used_in), same deduplicated set
                for (ResourceLocation ingId : seenIngredients) {
                    index.getNode(ingId, NodeType.ITEM).ifPresent(ingNode -> {
                        ingNode.addUnresolvedEdge(EdgeType.USED_IN, recipeId);
                    });
                }
                edgeCount += seenIngredients.size();
            }
        }

        // Phase 2: input-only recipes not captured above (fuels, etc.)
        for (Item inputItem : recipeIndex.getAllInputItems()) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(inputItem);
            if (itemId == null) continue;

            for (AmiRecipeHolder<?> recipe : recipeIndex.getUsesFor(new ItemStack(inputItem))) {
                ResourceLocation recipeId = recipe.id();
                if (!emitted.add(recipeId)) continue;

                RecipeType<?> recipeType = recipe.value().getType();
                String typeId = recipeTypeId(recipeType);
                Map<String, String> meta = new LinkedHashMap<>();
                meta.put(SearchNodeKeys.RECIPE_TYPE_ID, typeId);
                meta.put(SearchNodeKeys.MOD_ID, recipeId.getNamespace());
                putRecipeMethodMetadata(meta, recipeType);

                SearchNode recipeNode = new SearchNode(recipeId, NodeType.RECIPE,
                        typeId, 0xFFFFFFFF, 0, meta);

                index.addNode(recipeNode);
                nodeCount++;

                index.getNode(itemId, NodeType.ITEM).ifPresent(ingNode -> {
                    ingNode.addUnresolvedEdge(EdgeType.USED_IN, recipeId);
                    recipeNode.addUnresolvedEdge(EdgeType.REQUIRES, itemId);
                });
                edgeCount += 2;
            }
        }

        AmiCore.LOGGER.info("AMI indexing: RecipeGraphProvider emitted {} recipe nodes, {} edges in {}ms",
                nodeCount, edgeCount, System.currentTimeMillis() - start);
    }

    private static String recipeTypeId(RecipeType<?> type) {
        ResourceLocation key = BuiltInRegistries.RECIPE_TYPE.getKey(type);
        if (key != null) return key.getPath();
        return type.toString().toLowerCase(java.util.Locale.ROOT);
    }

    private static void putRecipeMethodMetadata(Map<String, String> meta, RecipeType<?> type) {
        String name = AmiRecipeCategoryRegistry.getEmiCategoryName(type);
        if (name != null && !name.isBlank()) {
            meta.put(SearchNodeKeys.RECIPE_METHOD_LABEL, name);
        }

        ItemStack icon = AmiRecipeCategoryRegistry.getEmiCategoryIcon(type);
        if (!icon.isEmpty()) {
            ResourceLocation iconId = BuiltInRegistries.ITEM.getKey(icon.getItem());
            if (iconId != null) {
                meta.put(SearchNodeKeys.RECIPE_METHOD_ICON_ITEM_ID, iconId.toString());
            }
        }
    }
}
