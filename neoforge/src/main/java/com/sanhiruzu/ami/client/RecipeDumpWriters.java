package com.sanhiruzu.ami.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanhiruzu.ami.compat.JeiRuntimeAccessor;
import com.sanhiruzu.ami.index.providers.CreativeStackVariantExpander;
import com.sanhiruzu.ami.platform.Services;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import mezz.jei.api.ingredients.IIngredientSupplier;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

final class RecipeDumpWriters {
    private static final Gson GSON = new Gson();
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String RUNTIME_RECIPES_FILE = "recipes_runtime.jsonl";
    private static final String RUNTIME_RECIPES_CSV_FILE = "recipes_runtime.csv";
    private static final String RUNTIME_RECIPES_MD_FILE = "recipes_runtime.md";
    private static final String RUNTIME_META_FILE = "recipes_runtime.meta.json";
    private static final String VIEWER_META_FILE = "recipe_viewer_recipes.meta.json";
    private static final String LOOT_TABLES_FILE = "loot_tables_runtime.jsonl";
    private static final String LOOT_TABLES_CSV_FILE = "loot_tables_runtime.csv";
    private static final String LOOT_TABLES_MD_FILE = "loot_tables_runtime.md";
    private static final String LOOT_TABLES_META_FILE = "loot_tables_runtime.meta.json";

    private RecipeDumpWriters() {
    }

    static RuntimeRecipeDumpOutputs writeRuntimeRecipes(Path dumpDir, Level level) throws IOException {
        if (level == null) {
            throw new IllegalStateException("No client level is loaded");
        }
        Files.createDirectories(dumpDir);

        List<RuntimeRecipeSnapshot> snapshots = collectRuntimeRecipes(level);
        Path out = dumpDir.resolve(RUNTIME_RECIPES_FILE);
        writeJsonl(out, snapshots);

        Path csv = dumpDir.resolve(RUNTIME_RECIPES_CSV_FILE);
        Files.writeString(csv, renderRuntimeRecipeCsv(snapshots), StandardCharsets.UTF_8);

        Path markdown = dumpDir.resolve(RUNTIME_RECIPES_MD_FILE);
        Files.writeString(markdown, renderRuntimeRecipeMarkdown(snapshots), StandardCharsets.UTF_8);

        Path meta = dumpDir.resolve(RUNTIME_META_FILE);
        Files.writeString(meta, PRETTY_GSON.toJson(new RuntimeRecipeDumpMeta(
                Instant.now().toString(),
                AmiDebugSettings.versionLabel(),
                AmiDebugSettings.debugBuild(),
                snapshots.size(),
                countDistinctRuntimeTypes(snapshots),
                RUNTIME_RECIPES_FILE,
                RUNTIME_RECIPES_CSV_FILE,
                RUNTIME_RECIPES_MD_FILE
        )), StandardCharsets.UTF_8);

        return new RuntimeRecipeDumpOutputs(out, csv, markdown, meta, snapshots.size());
    }

    static ViewerRecipeDumpOutputs writeViewerRecipes(Path dumpDir, Level level) throws IOException {
        if (level == null) {
            throw new IllegalStateException("No client level is loaded");
        }
        Files.createDirectories(dumpDir);

        List<ViewerDatasetOutput> outputs = new ArrayList<>();
        outputs.addAll(writeEmiViewerRecipes(dumpDir, level));
        outputs.addAll(writeJeiViewerRecipes(dumpDir, level));

        int total = outputs.stream().mapToInt(ViewerDatasetOutput::recipeCount).sum();
        Path meta = dumpDir.resolve(VIEWER_META_FILE);
        Files.writeString(meta, PRETTY_GSON.toJson(new ViewerRecipeDumpMeta(
                Instant.now().toString(),
                AmiDebugSettings.versionLabel(),
                AmiDebugSettings.debugBuild(),
                total,
                outputs
        )), StandardCharsets.UTF_8);

        return new ViewerRecipeDumpOutputs(meta, outputs, total);
    }

    static LootTableDumpOutputs writeLootTables(Path dumpDir) throws IOException {
        Files.createDirectories(dumpDir);

        List<LootTableSnapshot> snapshots = collectLootTables();
        Path out = dumpDir.resolve(LOOT_TABLES_FILE);
        writeJsonl(out, snapshots);

        Path csv = dumpDir.resolve(LOOT_TABLES_CSV_FILE);
        Files.writeString(csv, renderLootTableCsv(snapshots), StandardCharsets.UTF_8);

        Path markdown = dumpDir.resolve(LOOT_TABLES_MD_FILE);
        Files.writeString(markdown, renderLootTableMarkdown(snapshots), StandardCharsets.UTF_8);

        Path meta = dumpDir.resolve(LOOT_TABLES_META_FILE);
        Files.writeString(meta, PRETTY_GSON.toJson(new LootTableDumpMeta(
                Instant.now().toString(),
                AmiDebugSettings.versionLabel(),
                AmiDebugSettings.debugBuild(),
                snapshots.size(),
                LOOT_TABLES_FILE,
                LOOT_TABLES_CSV_FILE,
                LOOT_TABLES_MD_FILE
        )), StandardCharsets.UTF_8);

        return new LootTableDumpOutputs(out, csv, markdown, meta, snapshots.size());
    }

    private static List<RuntimeRecipeSnapshot> collectRuntimeRecipes(Level level) {
        List<RuntimeRecipeSnapshot> snapshots = new ArrayList<>();
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return snapshots;
        for (RecipeHolder<?> holder : serverLevel.recipeAccess().getRecipes()) {
            Recipe<?> recipe = holder.value();
            List<List<StackSnapshot>> inputs = new ArrayList<>();
            try {
                for (Ingredient ingredient : recipe.placementInfo().ingredients()) {
                    inputs.add(stackSnapshots(ingredient.items().map(ItemStack::new).toArray(ItemStack[]::new), level));
                }
            } catch (RuntimeException | LinkageError ignored) {
            }

            StackSnapshot result = StackSnapshot.empty();
            try {
                var displays = recipe.display();
                if (!displays.isEmpty()) {
                    var resultStacks = displays.get(0).result().resolveForStacks(SlotDisplayContext.fromLevel(level));
                    if (!resultStacks.isEmpty()) {
                        result = stackSnapshot(resultStacks.get(0), level);
                    }
                }
            } catch (RuntimeException | LinkageError ignored) {
            }

            String recipeId = holder.id().identifier().toString();
            snapshots.add(new RuntimeRecipeSnapshot(
                    recipeId,
                    holder.id().identifier().getNamespace(),
                    recipeTypeId(recipe),
                    recipeSerializerId(recipe),
                    recipe.getClass().getName(),
                    inputs,
                    result,
                    cookingTime(recipe),
                    cookingExperience(recipe),
                    runtimeKubeJsHints(recipeId, recipeTypeId(recipe), result.itemId())
            ));
        }
        snapshots.sort(Comparator.comparing(RuntimeRecipeSnapshot::recipeType)
                .thenComparing(RuntimeRecipeSnapshot::id));
        return snapshots;
    }

    private static List<ViewerDatasetOutput> writeEmiViewerRecipes(Path dumpDir, Level level) throws IOException {
        if (!Services.PLATFORM.isModLoaded("emi")) {
            return List.of();
        }

        List<ViewerRecipeSnapshot> snapshots = new ArrayList<>();
        for (EmiRecipe recipe : EmiApi.getRecipeManager().getRecipes()) {
            snapshots.add(emiRecipeSnapshot(recipe, level));
        }
        snapshots.sort(Comparator.comparing(ViewerRecipeSnapshot::categoryId)
                .thenComparing(ViewerRecipeSnapshot::recipeId)
                .thenComparing(ViewerRecipeSnapshot::recipeClass));

        Path out = dumpDir.resolve(viewerDumpFileName("emi", "all"));
        writeJsonl(out, snapshots);
        return List.of(new ViewerDatasetOutput("emi", "all", out.getFileName().toString(), snapshots.size()));
    }

    private static List<LootTableSnapshot> collectLootTables() {
        ResourceManager resourceManager = resourceManager();
        if (resourceManager == null) {
            return List.of();
        }

        List<LootTableSnapshot> snapshots = new ArrayList<>();
        snapshots.addAll(collectLootTables(resourceManager, "loot_table"));
        snapshots.addAll(collectLootTables(resourceManager, "loot_tables"));
        snapshots.sort(Comparator.comparing(LootTableSnapshot::tableId));
        return snapshots;
    }

    private static List<LootTableSnapshot> collectLootTables(ResourceManager resourceManager, String root) {
        List<LootTableSnapshot> snapshots = new ArrayList<>();
        try {
            resourceManager.listResources(root, id -> id.getPath().endsWith(".json"))
                    .entrySet()
                    .stream()
                    .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                    .forEach(entry -> readLootTable(root, entry.getKey(), entry.getValue(), snapshots));
        } catch (RuntimeException ignored) {
        }
        return snapshots;
    }

    private static void readLootTable(String root, Identifier resourceId, Resource resource,
                                      List<LootTableSnapshot> snapshots) {
        try (BufferedReader reader = resource.openAsReader()) {
            JsonElement json = JsonParser.parseReader(reader);
            String tableId = normalizeLootTableId(root, resourceId);
            String path = tableId.contains(":") ? tableId.substring(tableId.indexOf(':') + 1) : tableId;
            String kind = path.contains("/") ? path.substring(0, path.indexOf('/')) : "";
            Set<String> itemRefs = new LinkedHashSet<>();
            collectJsonStringValues(json, "name", itemRefs);
            snapshots.add(new LootTableSnapshot(
                    tableId,
                    tableId.contains(":") ? tableId.substring(0, tableId.indexOf(':')) : "",
                    kind,
                    resourceId.toString(),
                    resource.sourcePackId(),
                    itemRefs.stream().sorted().toList(),
                    PRETTY_GSON.toJson(json),
                    lootKubeJsHint(tableId, kind)
            ));
        } catch (IOException | RuntimeException ignored) {
        }
    }

    private static List<ViewerDatasetOutput> writeJeiViewerRecipes(Path dumpDir, Level level) throws IOException {
        return JeiRuntimeAccessor.withRuntime(runtime -> {
            try {
                IRecipeManager recipeManager = runtime.getRecipeManager();
                List<ViewerDatasetOutput> outputs = new ArrayList<>();

                List<ViewerRecipeSnapshot> visible = collectJeiRecipes(recipeManager, level, false);
                Path visibleOut = dumpDir.resolve(viewerDumpFileName("jei", "visible"));
                writeJsonl(visibleOut, visible);
                outputs.add(new ViewerDatasetOutput("jei", "visible", visibleOut.getFileName().toString(), visible.size()));

                List<ViewerRecipeSnapshot> all = collectJeiRecipes(recipeManager, level, true);
                Path allOut = dumpDir.resolve(viewerDumpFileName("jei", "all"));
                writeJsonl(allOut, all);
                outputs.add(new ViewerDatasetOutput("jei", "all", allOut.getFileName().toString(), all.size()));

                return outputs;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, List.of());
    }

    private static List<ViewerRecipeSnapshot> collectJeiRecipes(IRecipeManager recipeManager, Level level,
                                                               boolean includeHidden) {
        var categoryLookup = recipeManager.createRecipeCategoryLookup();
        if (includeHidden) {
            categoryLookup.includeHidden();
        }

        List<ViewerRecipeSnapshot> snapshots = new ArrayList<>();
        for (IRecipeCategory<?> category : categoryLookup.get().toList()) {
            snapshots.addAll(collectJeiCategoryRecipes(recipeManager, category, level, includeHidden));
        }
        snapshots.sort(Comparator.comparing(ViewerRecipeSnapshot::categoryId)
                .thenComparing(ViewerRecipeSnapshot::recipeId)
                .thenComparing(ViewerRecipeSnapshot::recipeClass));
        return snapshots;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<ViewerRecipeSnapshot> collectJeiCategoryRecipes(IRecipeManager recipeManager,
                                                                        IRecipeCategory category,
                                                                        Level level,
                                                                        boolean includeHidden) {
        var lookup = recipeManager.createRecipeLookup(category.getRecipeType());
        if (includeHidden) {
            lookup.includeHidden();
        }

        List<ViewerRecipeSnapshot> snapshots = new ArrayList<>();
        for (Object recipe : lookup.get().toList()) {
            snapshots.add(jeiRecipeSnapshot(recipeManager, category, recipe, level, includeHidden));
        }
        return snapshots;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ViewerRecipeSnapshot jeiRecipeSnapshot(IRecipeManager recipeManager,
                                                          IRecipeCategory category,
                                                          Object recipe,
                                                          Level level,
                                                          boolean includeHidden) {
        String categoryId = category.getRecipeType().getUid().toString();
        String categoryTitle = safeComponentString(category.getTitle());
        String recipeId = resourceLocationString(category.getRegistryName(recipe));
        List<IngredientSnapshot> inputs = List.of();
        List<IngredientSnapshot> catalysts = List.of();
        List<IngredientSnapshot> outputs = List.of();
        String backingRecipeId = "";

        if (recipe instanceof RecipeHolder<?> holder) {
            backingRecipeId = holder.id().identifier().toString();
            if (recipeId.isBlank()) {
                recipeId = backingRecipeId;
            }
            try {
                inputs = holder.value().placementInfo().ingredients().stream()
                        .map(ingredient -> new IngredientSnapshot(
                                "minecraft_ingredient",
                                "",
                                stackSnapshots(ingredient.items().map(ItemStack::new).toArray(ItemStack[]::new), level)
                        ))
                        .toList();
                var displays = holder.value().display();
                if (!displays.isEmpty()) {
                    var resultStacks = displays.get(0).result().resolveForStacks(SlotDisplayContext.fromLevel(level));
                    if (!resultStacks.isEmpty()) {
                        ItemStack output = resultStacks.get(0);
                        outputs = output.isEmpty()
                                ? List.of()
                                : List.of(new IngredientSnapshot("item_stack", output.getHoverName().getString(), List.of(stackSnapshot(output, level))));
                    }
                }
            } catch (RuntimeException | LinkageError ignored) {
            }
        }

        return new ViewerRecipeSnapshot(
                "jei",
                includeHidden ? "all" : "visible",
                recipeId,
                categoryId,
                categoryTitle,
                recipe.getClass().getName(),
                backingRecipeId,
                inputs,
                catalysts,
                outputs,
                0,
                0
        );
    }

    private static ViewerRecipeSnapshot emiRecipeSnapshot(EmiRecipe recipe, Level level) {
        EmiRecipeCategory category = recipe.getCategory();
        String categoryId = "";
        if (category != null) {
            try {
                java.lang.reflect.Method getIdMethod = category.getClass().getMethod("getId");
                Object id = getIdMethod.invoke(category);
                categoryId = id == null ? "" : id.toString();
            } catch (ReflectiveOperationException | LinkageError ignored) {}
        }
        String categoryTitle = category == null ? "" : safeComponentString(category.getName());
        String recipeId = "";
        try {
            java.lang.reflect.Method getIdMethod = recipe.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(recipe);
            recipeId = id == null ? "" : id.toString();
        } catch (ReflectiveOperationException | LinkageError ignored) {}
        String backingRecipeId = "";
        try {
            RecipeHolder<?> backing = recipe.getBackingRecipe();
            if (backing != null) {
                backingRecipeId = backing.id().identifier().toString();
            }
        } catch (RuntimeException | LinkageError ignored) {
        }

        return new ViewerRecipeSnapshot(
                "emi",
                "all",
                recipeId,
                categoryId,
                categoryTitle,
                recipe.getClass().getName(),
                backingRecipeId,
                emiIngredients(recipe.getInputs(), level),
                emiIngredients(recipe.getCatalysts(), level),
                emiStacks(recipe.getOutputs(), level),
                recipe.getDisplayWidth(),
                recipe.getDisplayHeight()
        );
    }

    private static List<IngredientSnapshot> jeiIngredients(List<ITypedIngredient<?>> typedIngredients, Level level) {
        List<IngredientSnapshot> snapshots = new ArrayList<>();
        for (ITypedIngredient<?> typed : typedIngredients) {
            ItemStack stack = typed.getItemStack().orElse(ItemStack.EMPTY);
            snapshots.add(new IngredientSnapshot(
                    typed.getType().getUid(),
                    stack.isEmpty() ? String.valueOf(typed.getIngredient()) : stack.getHoverName().getString(),
                    stack.isEmpty() ? List.of() : List.of(stackSnapshot(stack, level))
            ));
        }
        return snapshots;
    }

    private static List<IngredientSnapshot> emiIngredients(List<EmiIngredient> ingredients, Level level) {
        List<IngredientSnapshot> snapshots = new ArrayList<>();
        for (EmiIngredient ingredient : ingredients) {
            snapshots.add(new IngredientSnapshot(
                    ingredient.getClass().getName(),
                    "",
                    emiStackSnapshots(ingredient.getEmiStacks(), level)
            ));
        }
        return snapshots;
    }

    private static List<IngredientSnapshot> emiStacks(List<EmiStack> stacks, Level level) {
        return stacks.stream()
                .map(stack -> stack.getItemStack())
                .filter(stack -> !stack.isEmpty())
                .map(stack -> new IngredientSnapshot("item_stack", stack.getHoverName().getString(), List.of(stackSnapshot(stack, level))))
                .toList();
    }

    private static List<StackSnapshot> emiStackSnapshots(List<EmiStack> stacks, Level level) {
        List<StackSnapshot> snapshots = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (EmiStack emiStack : stacks) {
            StackSnapshot snapshot = stackSnapshot(emiStack.getItemStack(), level);
            if (!snapshot.itemId().isBlank() && seen.add(snapshot.exactKey())) {
                snapshots.add(snapshot);
            }
        }
        return snapshots;
    }

    private static List<StackSnapshot> stackSnapshots(ItemStack[] stacks, Level level) {
        List<StackSnapshot> snapshots = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (ItemStack stack : stacks) {
            StackSnapshot snapshot = stackSnapshot(stack, level);
            if (!snapshot.itemId().isBlank() && seen.add(snapshot.exactKey())) {
                snapshots.add(snapshot);
            }
        }
        return snapshots;
    }

    private static StackSnapshot stackSnapshot(ItemStack stack, Level level) {
        if (stack == null || stack.isEmpty()) {
            return StackSnapshot.empty();
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String itemIdString = itemId == null ? "" : itemId.toString();
        String exactHash = itemId == null ? "" : CreativeStackVariantExpander.stackIdentityHash(itemId, stack, level);
        String exactKey = itemIdString.isBlank() || exactHash.isBlank() ? "" : itemIdString + "|" + exactHash;
        return new StackSnapshot(itemIdString, stack.getHoverName().getString(), stack.getCount(), exactHash, exactKey);
    }

    private static ResourceManager resourceManager() {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        var server = minecraft.getSingleplayerServer();
        if (server != null) {
            return server.getResourceManager();
        }
        var currentServer = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (currentServer != null) {
            return currentServer.getResourceManager();
        }
        return minecraft.getResourceManager();
    }

    private static String recipeTypeId(Recipe<?> recipe) {
        Identifier id = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
        return id == null ? String.valueOf(recipe.getType()) : id.toString();
    }

    private static String recipeSerializerId(Recipe<?> recipe) {
        Identifier id = BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer());
        return id == null ? String.valueOf(recipe.getSerializer()) : id.toString();
    }

    private static long countDistinctRuntimeTypes(List<RuntimeRecipeSnapshot> snapshots) {
        return snapshots.stream().map(RuntimeRecipeSnapshot::recipeType).distinct().count();
    }

    private static String renderRuntimeRecipeCsv(List<RuntimeRecipeSnapshot> snapshots) {
        StringBuilder out = new StringBuilder();
        out.append("id,modId,type,serializer,outputItem,outputName,outputCount,inputItems,cookingTime,experience,kubejsRemoveById,kubejsRemoveByOutput,kubejsRemoveByTypeAndOutput\n");
        for (RuntimeRecipeSnapshot snapshot : snapshots) {
            out.append(csv(snapshot.id())).append(',')
                    .append(csv(snapshot.modId())).append(',')
                    .append(csv(snapshot.recipeType())).append(',')
                    .append(csv(snapshot.serializer())).append(',')
                    .append(csv(snapshot.output().itemId())).append(',')
                    .append(csv(snapshot.output().displayName())).append(',')
                    .append(snapshot.output().count()).append(',')
                    .append(csv(String.join("; ", flatInputItemIds(snapshot)))).append(',')
                    .append(csv(snapshot.cookingTime())).append(',')
                    .append(csv(snapshot.experience())).append(',')
                    .append(csv(snapshot.kubeJsHints().removeById())).append(',')
                    .append(csv(snapshot.kubeJsHints().removeByOutput())).append(',')
                    .append(csv(snapshot.kubeJsHints().removeByTypeAndOutput())).append('\n');
        }
        return out.toString();
    }

    private static String renderRuntimeRecipeMarkdown(List<RuntimeRecipeSnapshot> snapshots) {
        StringBuilder out = new StringBuilder();
        out.append("# AMI Runtime Recipe Dump\n\n");
        out.append("- Generated: ").append(Instant.now()).append(" UTC\n");
        out.append("- Recipes: ").append(snapshots.size()).append('\n');
        out.append("- Types: ").append(countDistinctRuntimeTypes(snapshots)).append("\n\n");
        out.append("## KubeJS Patterns\n\n");
        out.append("```js\n");
        out.append("ServerEvents.recipes(event => {\n");
        out.append("  event.remove({ id: 'mod:recipe_id' })\n");
        out.append("  event.remove({ output: 'mod:item' })\n");
        out.append("  event.remove({ type: 'minecraft:smelting', output: 'mod:item' })\n");
        out.append("})\n");
        out.append("```\n\n");

        snapshots.stream()
                .collect(java.util.stream.Collectors.groupingBy(RuntimeRecipeSnapshot::recipeType,
                        java.util.TreeMap::new,
                        java.util.stream.Collectors.toList()))
                .forEach((type, recipes) -> {
                    out.append("## ").append(type).append(" (").append(recipes.size()).append(")\n\n");
                    out.append("| Output | Recipe ID | Inputs | Remove by ID |\n");
                    out.append("| --- | --- | --- | --- |\n");
                    recipes.stream().limit(250).forEach(snapshot -> out.append("| ")
                            .append(md(snapshot.output().itemId())).append(" | ")
                            .append(md(snapshot.id())).append(" | ")
                            .append(md(String.join(", ", flatInputItemIds(snapshot)))).append(" | `")
                            .append(snapshot.kubeJsHints().removeById()).append("` |\n"));
                    if (recipes.size() > 250) {
                        out.append("| ... | ").append(recipes.size() - 250).append(" more in CSV/JSONL | ... | ... |\n");
                    }
                    out.append('\n');
                });
        return out.toString();
    }

    private static String renderLootTableCsv(List<LootTableSnapshot> snapshots) {
        StringBuilder out = new StringBuilder();
        out.append("tableId,namespace,kind,sourcePack,resourceId,itemRefs,kubejsHint\n");
        for (LootTableSnapshot snapshot : snapshots) {
            out.append(csv(snapshot.tableId())).append(',')
                    .append(csv(snapshot.namespace())).append(',')
                    .append(csv(snapshot.kind())).append(',')
                    .append(csv(snapshot.sourcePack())).append(',')
                    .append(csv(snapshot.resourceId())).append(',')
                    .append(csv(String.join("; ", snapshot.itemRefs()))).append(',')
                    .append(csv(snapshot.kubeJsHint())).append('\n');
        }
        return out.toString();
    }

    private static String renderLootTableMarkdown(List<LootTableSnapshot> snapshots) {
        StringBuilder out = new StringBuilder();
        out.append("# AMI Runtime Loot Table Dump\n\n");
        out.append("- Generated: ").append(Instant.now()).append(" UTC\n");
        out.append("- Loot tables: ").append(snapshots.size()).append("\n\n");
        out.append("## KubeJS Notes\n\n");
        out.append("Loot table APIs vary by KubeJS/Minecraft version. Use the table IDs below as the stable targets; ");
        out.append("block tables usually map from `namespace:blocks/name` to block id `namespace:name`.\n\n");

        snapshots.stream()
                .collect(java.util.stream.Collectors.groupingBy(LootTableSnapshot::kind,
                        java.util.TreeMap::new,
                        java.util.stream.Collectors.toList()))
                .forEach((kind, tables) -> {
                    out.append("## ").append(kind.isBlank() ? "other" : kind).append(" (").append(tables.size()).append(")\n\n");
                    out.append("| Table | Source Pack | Item Refs | Hint |\n");
                    out.append("| --- | --- | --- | --- |\n");
                    tables.stream().limit(250).forEach(snapshot -> out.append("| ")
                            .append(md(snapshot.tableId())).append(" | ")
                            .append(md(snapshot.sourcePack())).append(" | ")
                            .append(md(String.join(", ", snapshot.itemRefs().stream().limit(12).toList()))).append(" | `")
                            .append(snapshot.kubeJsHint()).append("` |\n"));
                    if (tables.size() > 250) {
                        out.append("| ... | ").append(tables.size() - 250).append(" more in CSV/JSONL | ... | ... |\n");
                    }
                    out.append('\n');
                });
        return out.toString();
    }

    private static List<String> flatInputItemIds(RuntimeRecipeSnapshot snapshot) {
        return snapshot.inputs().stream()
                .flatMap(List::stream)
                .map(StackSnapshot::itemId)
                .filter(id -> !id.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    private static KubeJsRecipeHints runtimeKubeJsHints(String id, String type, String outputItem) {
        String removeById = "event.remove({ id: '" + id + "' })";
        String removeByOutput = outputItem.isBlank()
                ? ""
                : "event.remove({ output: '" + outputItem + "' })";
        String removeByTypeAndOutput = outputItem.isBlank()
                ? ""
                : "event.remove({ type: '" + type + "', output: '" + outputItem + "' })";
        return new KubeJsRecipeHints(removeById, removeByOutput, removeByTypeAndOutput);
    }

    private static String cookingTime(Recipe<?> recipe) {
        return recipe instanceof AbstractCookingRecipe cooking ? String.valueOf(cooking.cookingTime()) : "";
    }

    private static String cookingExperience(Recipe<?> recipe) {
        return recipe instanceof AbstractCookingRecipe cooking ? String.valueOf(cooking.experience()) : "";
    }

    private static String lootKubeJsHint(String tableId, String kind) {
        if ("blocks".equals(kind)) {
            String blockId = tableId.replace(":blocks/", ":");
            return "ServerEvents.blockLootTables(event => event.modifyBlock('" + blockId + "', table => { /* ... */ }))";
        }
        return "Modify loot table '" + tableId + "' in your loot-table script/API";
    }

    private static void collectJsonStringValues(JsonElement element, String key, Set<String> values) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (var entry : object.entrySet()) {
                if (key.equals(entry.getKey()) && entry.getValue().isJsonPrimitive()) {
                    String value = entry.getValue().getAsString();
                    if (value.contains(":")) {
                        values.add(value);
                    }
                }
                collectJsonStringValues(entry.getValue(), key, values);
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) {
                collectJsonStringValues(child, key, values);
            }
        }
    }

    private static String normalizeLootTableId(String root, Identifier resourceId) {
        String path = resourceId.getPath();
        String prefix = root + "/";
        if (path.startsWith(prefix)) {
            path = path.substring(prefix.length());
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - ".json".length());
        }
        return resourceId.getNamespace() + ":" + path;
    }

    private static String viewerDumpFileName(String source, String scope) {
        return "recipe_viewer_recipes_"
                + source.toLowerCase(Locale.ROOT)
                + "_"
                + scope.toLowerCase(Locale.ROOT)
                + ".jsonl";
    }

    private static String resourceLocationString(Identifier id) {
        return id == null ? "" : id.toString();
    }

    private static String safeComponentString(net.minecraft.network.chat.Component component) {
        return component == null ? "" : component.getString();
    }

    private static String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private static String md(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace("\n", " ");
    }

    private static void writeJsonl(Path path, List<?> rows) throws IOException {
        Files.createDirectories(Objects.requireNonNull(path.getParent()));
        List<String> lines = new ArrayList<>(rows.size());
        for (Object row : rows) {
            lines.add(GSON.toJson(row));
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    record RuntimeRecipeDumpOutputs(Path dump, Path csv, Path markdown, Path meta, int recipeCount) {
    }

    record ViewerRecipeDumpOutputs(Path meta, List<ViewerDatasetOutput> datasets, int totalRecipes) {
    }

    record LootTableDumpOutputs(Path dump, Path csv, Path markdown, Path meta, int tableCount) {
    }

    record RuntimeRecipeSnapshot(
            String id,
            String modId,
            String recipeType,
            String serializer,
            String recipeClass,
            List<List<StackSnapshot>> inputs,
            StackSnapshot output,
            String cookingTime,
            String experience,
            KubeJsRecipeHints kubeJsHints
    ) {
    }

    record ViewerRecipeSnapshot(
            String source,
            String scope,
            String recipeId,
            String categoryId,
            String categoryTitle,
            String recipeClass,
            String backingRecipeId,
            List<IngredientSnapshot> inputs,
            List<IngredientSnapshot> catalysts,
            List<IngredientSnapshot> outputs,
            int displayWidth,
            int displayHeight
    ) {
    }

    record IngredientSnapshot(String ingredientType, String displayName, List<StackSnapshot> stacks) {
    }

    record StackSnapshot(String itemId, String displayName, int count, String exactHash, String exactKey) {
        static StackSnapshot empty() {
            return new StackSnapshot("", "", 0, "", "");
        }
    }

    record KubeJsRecipeHints(String removeById, String removeByOutput, String removeByTypeAndOutput) {
    }

    record LootTableSnapshot(
            String tableId,
            String namespace,
            String kind,
            String resourceId,
            String sourcePack,
            List<String> itemRefs,
            String json,
            String kubeJsHint
    ) {
    }

    record RuntimeRecipeDumpMeta(
            String generatedAtUtc,
            String amiVersion,
            boolean debugBuild,
            int recipeCount,
            long recipeTypes,
            String dumpFile,
            String csvFile,
            String markdownFile
    ) {
    }

    record ViewerRecipeDumpMeta(
            String generatedAtUtc,
            String amiVersion,
            boolean debugBuild,
            int totalRecipes,
            List<ViewerDatasetOutput> datasets
    ) {
    }

    record ViewerDatasetOutput(String source, String scope, String dumpFile, int recipeCount) {
    }

    record LootTableDumpMeta(
            String generatedAtUtc,
            String amiVersion,
            boolean debugBuild,
            int tableCount,
            String dumpFile,
            String csvFile,
            String markdownFile
    ) {
    }
}
