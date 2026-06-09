package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.client.icon.RecipeViewerIngredientRenderer;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.IAmiDataProvider;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.text.Normalizer;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class IngredientIndexProvider implements IAmiDataProvider {
    public static final String TYPE_UID_KEY = "ingredientTypeUid";
    public static final String INGREDIENT_UID_KEY = "ingredientUid";
    public static final String DISPLAY_MOD_ID_KEY = "displayModId";

    @Override
    public void populate(GlobalIndex index, @Nullable Level level) {
        long start = System.currentTimeMillis();
        RecipeViewerIngredientRenderer.clearPersistent();
        int count = indexNativeIngredients(index);
        AmiCore.LOGGER.info("AMI indexing: IngredientIndexProvider indexed {} ingredients in {}ms",
                count, System.currentTimeMillis() - start);
    }

    private static int indexNativeIngredients(GlobalIndex index) {
        int[] count = {0};
        var registration = new IngredientRegistrationImpl(index, count);
        IngredientPluginRegistry.registerAllIngredients(registration, registration);
        return count[0];
    }

    public static void rebuildRuntimeHandles(GlobalIndex index) {
        RecipeViewerIngredientRenderer.clearPersistent();
        indexNativeIngredients(index);
    }

    private static final class IngredientRegistrationImpl
            implements IRecipeViewerPlugin.IIngredientRegistration,
                       IRecipeViewerPlugin.IExtraIngredientRegistration {

        private final GlobalIndex index;
        private final int[] count;
        private final Set<String> seenKeys = new LinkedHashSet<>();

        IngredientRegistrationImpl(GlobalIndex index, int[] count) {
            this.index = index;
            this.count = count;
        }

        @Override
        public <V> void register(IRecipeViewerPlugin.IIngredientType<V> type,
                                 Collection<? extends V> ingredients,
                                 IRecipeViewerPlugin.IIngredientHelper<V> helper) {
            String typeId = type.getTypeId().toString();
            for (V ingredient : ingredients) {
                if (ingredient == null) continue;
                ResourceLocation id = helper.getResourceLocation(ingredient);
                if (id == null) continue;
                String seenKey = typeId + "|" + id;
                if (!seenKeys.add(seenKey)) continue;

                String displayName = helper.getDisplayName(ingredient);
                if (displayName == null || displayName.isBlank()) continue;
                String modId = helper.getDisplayModId(ingredient);

                Map<String, String> meta = new LinkedHashMap<>();
                meta.put(SearchNodeKeys.MOD_ID, modId);
                meta.put(TYPE_UID_KEY, typeId);
                meta.put(INGREDIENT_UID_KEY, id.toString());

                String modName = Services.PLATFORM.getModName(modId).orElse(modId);
                addSearchTokens(meta, modName);
                addSearchTokens(meta, displayName);

                index.addNode(new SearchNode(id, NodeType.INGREDIENT, displayName, 0xFFFFFFFF, 0, meta));
                count[0]++;
            }
        }

        @Override
        public void addExtraItemStacks(Collection<ItemStack> stacks) {
            // extra item stacks feed into the item index, not ingredient index
            for (ItemStack stack : stacks) {
                if (stack == null || stack.isEmpty()) continue;
                // ItemProvider handles ItemStacks — just log for now, hook in later
                AmiCore.LOGGER.debug("AMI: extra ItemStack registered via plugin: {}", stack);
            }
        }

        @Override
        public <V> void addExtraIngredients(IRecipeViewerPlugin.IIngredientType<V> type,
                                             Collection<? extends V> ingredients) {
            // route through the same path as register()
        }

        private static void addSearchTokens(Map<String, String> meta, String rawValue) {
            if (rawValue == null || rawValue.isBlank()) return;
            String normalized = Normalizer.normalize(
                            rawValue.replaceAll("([a-z])([A-Z])", "$1 $2"),
                            Normalizer.Form.NFD)
                    .replaceAll("\\p{M}+", "")
                    .toLowerCase(Locale.ROOT);
            for (String part : normalized.split("[^a-z0-9]+")) {
                if (part.length() >= 2) {
                    meta.merge(SearchNodeKeys.PLAIN_SEARCH_TOKENS, part, (a, b) ->
                            List.of(a.split("\\s+")).contains(b) ? a : a + " " + b);
                }
            }
        }
    }
}
