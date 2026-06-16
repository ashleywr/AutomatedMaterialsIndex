package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class CobblemonPokedexOpener {
    private enum GuiCtorVariant { THREE_ARG, TWO_ARG, FOUR_ARG_MARKER, FIVE_ARG }

    private static PokedexApi api;
    private static boolean unavailable;
    private static CobbledexApi cobbledexApi;
    private static boolean cobbledexUnavailable;
    private static CuriosBridge curiosBridge;
    private static boolean curiosUnavailable;
    private static final int MAX_OPEN_FAILURE_LOGS = 8;
    private static final Set<Identifier> LOGGED_COBBLEDEX_OPEN_FAILURES = new HashSet<>();
    private static final Set<Identifier> LOGGED_POKEDEX_OPEN_FAILURES = new HashSet<>();
    private static int openFailureLogCount;

    private CobblemonPokedexOpener() {
    }

    public static void invalidateCaches() {
        api = null;
        unavailable = false;
        cobbledexApi = null;
        cobbledexUnavailable = false;
        curiosBridge = null;
        curiosUnavailable = false;
        LOGGED_COBBLEDEX_OPEN_FAILURES.clear();
        LOGGED_POKEDEX_OPEN_FAILURES.clear();
        openFailureLogCount = 0;
    }

    public static boolean hasPokedex() {
        if (cobbledexApi() != null) {
            return true;
        }

        PokedexApi api = api();
        if (api == null) return false;
        return true;
    }

    public static boolean isPokemonSpecies(SearchNode node) {
        return node != null && "pokemon_species".equals(node.meta(SearchNodeKeys.ENTITY_CATEGORY, ""));
    }

    public static boolean handlePrimaryClick(SearchNode node) {
        if (!isPokemonSpecies(node)) {
            return false;
        }

        Identifier speciesId = speciesId(node);
        if (speciesId == null) {
            return true;
        }

        Minecraft mc = Minecraft.getInstance();
        if (tryOpenCobbledex(mc, speciesId)) {
            return true;
        }

        PokedexApi api = api();
        if (api == null) {
            return true;
        }

        if (mc.player == null) {
            return true;
        }

        Object pokedexType = findPokedexType(api, mc.player);
        if (pokedexType == null) pokedexType = api.defaultPokedexType;

        try {
            Object screen = api.createGui(pokedexType, speciesId);
            mc.setScreen((Screen) screen);
        } catch (Throwable e) {
            // Keep AMI clicks non-fatal when Cobblemon changes the screen ABI.
            logOpenFailure("Cobblemon Pokedex", speciesId, e, LOGGED_POKEDEX_OPEN_FAILURES);
        }
        return true;
    }

    private static Object findPokedexType(PokedexApi api, LivingEntity player) {
        if (!(player instanceof net.minecraft.world.entity.player.Player realPlayer)) {
            return null;
        }

        Inventory inventory = realPlayer.getInventory();
        List<ItemStack> allSlots = new java.util.ArrayList<>(inventory.getContainerSize());
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            allSlots.add(inventory.getItem(i));
        }
        Object type = findPokedexType(api, allSlots);
        if (type != null) return type;
        return findCuriosPokedexType(api, player);
    }

    private static Object findPokedexType(PokedexApi api, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            Object item = stack.getItem();
            if (!api.pokedexItemClass.isInstance(item)) continue;
            try {
                Object type = api.getType.invoke(item);
                if (type != null) return type;
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Object findCuriosPokedexType(PokedexApi api, LivingEntity player) {
        CuriosBridge curios = curiosBridge();
        if (curios == null) {
            return null;
        }

        try {
            Object optionalInventory = curios.getCuriosInventory.invoke(null, player);
            if (!(optionalInventory instanceof Optional<?> optional) || optional.isEmpty()) {
                return null;
            }

            Object curiosHandler = optional.get();
            Predicate<ItemStack> isPokedex = stack -> !stack.isEmpty()
                    && api.pokedexItemClass.isInstance(stack.getItem());
            Object optionalSlot = curios.findFirstCurio.invoke(curiosHandler, isPokedex);
            if (!(optionalSlot instanceof Optional<?> slotOptional) || slotOptional.isEmpty()) {
                return null;
            }

            Object slotResult = slotOptional.get();
            Object stackObject = curios.slotResultStack.invoke(slotResult);
            if (stackObject instanceof ItemStack stack && !stack.isEmpty()
                    && api.pokedexItemClass.isInstance(stack.getItem())) {
                Object type = api.getType.invoke(stack.getItem());
                if (type != null) return type;
            }
        } catch (Throwable ignored) {
            // Curios is optional; a bridge failure should just behave like no equipped Pokédex.
        }

        return null;
    }

    private static boolean tryOpenCobbledex(Minecraft mc, Identifier speciesId) {
        CobbledexApi api = cobbledexApi();
        if (api == null) {
            return false;
        }

        try {
            Object species = api.getByIdentifier.invoke(api.pokemonSpecies, speciesId);
            if (species == null) {
                return false;
            }

            Object form = api.getStandardForm.invoke(species);
            if (form == null) {
                return false;
            }

            Object screen = api.cobbledexGuiCtor.newInstance(form, Set.of());
            mc.setScreen((Screen) screen);
            return true;
        } catch (Throwable e) {
            // Keep AMI clicks non-fatal when Cobbledex changes its screen ABI.
            logOpenFailure("Cobbledex", speciesId, e, LOGGED_COBBLEDEX_OPEN_FAILURES);
            return false;
        }
    }

    private static Identifier speciesId(SearchNode node) {
        String species = node.meta(SearchNodeKeys.POKEMON_SPECIES, "");
        if (!species.isBlank()) {
            Identifier parsed = Identifier.tryParse(species);
            if (parsed != null) return parsed;
        }

        if ("cobblemon".equals(node.id().getNamespace()) && node.id().getPath().startsWith("species/")) {
            return Identifier.fromNamespaceAndPath("cobblemon", node.id().getPath().substring("species/".length()));
        }

        return null;
    }

    private static CobbledexApi cobbledexApi() {
        if (cobbledexUnavailable) {
            return null;
        }
        if (cobbledexApi != null) {
            return cobbledexApi;
        }

        try {
            Class<?> pokemonSpecies = ReflectiveCompat.findClass("com.cobblemon.mod.common.api.pokemon.PokemonSpecies").orElseThrow();
            Class<?> species = ReflectiveCompat.findClass("com.cobblemon.mod.common.pokemon.Species").orElseThrow();
            Class<?> formData = ReflectiveCompat.findClass("com.cobblemon.mod.common.pokemon.FormData").orElseThrow();
            Class<?> cobbledexGui = ReflectiveCompat.findClass("com.rafacasari.mod.cobbledex.client.gui.CobbledexGUI").orElseThrow();

            Field instance = pokemonSpecies.getField("INSTANCE");
            Object pokemonSpeciesInstance = instance.get(null);
            Method getByIdentifier = ReflectiveCompat.findMethod(pokemonSpecies, "getByIdentifier", Identifier.class).orElseThrow();
            Method getStandardForm = ReflectiveCompat.findMethod(species, "getStandardForm").orElseThrow();
            Constructor<?> guiCtor = ReflectiveCompat.findConstructor(cobbledexGui, formData, Set.class).orElseThrow();

            cobbledexApi = new CobbledexApi(pokemonSpeciesInstance, getByIdentifier, getStandardForm, guiCtor);
            return cobbledexApi;
        } catch (Throwable e) {
            cobbledexUnavailable = true;
            return null;
        }
    }

    private static PokedexApi api() {
        if (unavailable) {
            return null;
        }
        if (api != null) {
            return api;
        }

        try {
            Class<?> pokedexItem = ReflectiveCompat.findClass("com.cobblemon.mod.common.item.PokedexItem").orElseThrow();
            Class<?> pokedexType = ReflectiveCompat.findClass("com.cobblemon.mod.common.client.pokedex.PokedexType").orElseThrow();
            Class<?> pokedexGui = ReflectiveCompat.findClass("com.cobblemon.mod.common.client.gui.pokedex.PokedexGUI").orElseThrow();

            Method getType = ReflectiveCompat.findMethod(pokedexItem, "getType").orElseThrow();

            Object defaultPokedexType = Enum.valueOf((Class<Enum>) pokedexType.asSubclass(Enum.class), "RED");

            // Try multiple constructor shapes across Cobblemon builds. Some Cobblemon builds expose
            // PokedexGUI constructors only as private Kotlin primaries or synthetic bridges.
            Constructor<?> guiCtor = null;
            GuiCtorVariant ctorVariant = null;

            // Shape 1: primary — PokedexGUI(PokedexType, Identifier, BlockPos)
            try {
                guiCtor = pokedexGui.getDeclaredConstructor(pokedexType, Identifier.class, BlockPos.class);
                guiCtor.setAccessible(true);
                ctorVariant = GuiCtorVariant.THREE_ARG;
            } catch (Throwable ignored) {
            }

            // Shape 2: @JvmOverloads secondary — PokedexGUI(PokedexType, Identifier)
            if (guiCtor == null) {
                var c2 = ReflectiveCompat.findConstructor(pokedexGui, pokedexType, Identifier.class);
                if (c2.isPresent()) {
                    guiCtor = c2.get();
                    ctorVariant = GuiCtorVariant.TWO_ARG;
                }
            }

            // Shape 3: Kotlin synthetic bridge — PokedexGUI(PokedexType, Identifier, BlockPos, DefaultConstructorMarker)
            if (guiCtor == null) {
                try {
                    Class<?> dcm = Class.forName("kotlin.jvm.internal.DefaultConstructorMarker");
                    Constructor<?> c4 = pokedexGui.getDeclaredConstructor(
                            pokedexType, Identifier.class, BlockPos.class, dcm);
                    c4.setAccessible(true);
                    guiCtor = c4;
                    ctorVariant = GuiCtorVariant.FOUR_ARG_MARKER;
                } catch (Throwable ignored) {
                }
            }

            // Shape 4: Kotlin default-args synthetic — PokedexGUI(PokedexType, Identifier, BlockPos, int, DefaultConstructorMarker)
            if (guiCtor == null) {
                try {
                    Class<?> dcm = Class.forName("kotlin.jvm.internal.DefaultConstructorMarker");
                    Constructor<?> c5 = pokedexGui.getDeclaredConstructor(
                            pokedexType, Identifier.class, BlockPos.class, int.class, dcm);
                    c5.setAccessible(true);
                    guiCtor = c5;
                    ctorVariant = GuiCtorVariant.FIVE_ARG;
                } catch (Throwable ignored) {
                }
            }

            if (guiCtor == null) {
                unavailable = true;
                return null;
            }

            api = new PokedexApi(pokedexItem, getType, guiCtor, ctorVariant, defaultPokedexType);
            return api;
        } catch (Throwable e) {
            unavailable = true;
            return null;
        }
    }

    private static CuriosBridge curiosBridge() {
        if (curiosUnavailable) {
            return null;
        }
        if (curiosBridge != null) {
            return curiosBridge;
        }

        try {
            Class<?> curiosApi = ReflectiveCompat.findClass("top.theillusivec4.curios.api.CuriosApi").orElseThrow();
            Class<?> curiosHandler = ReflectiveCompat.findClass("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler").orElseThrow();
            Class<?> slotResult = ReflectiveCompat.findClass("top.theillusivec4.curios.api.SlotResult").orElseThrow();

            Method getCuriosInventory = ReflectiveCompat.findMethod(curiosApi, "getCuriosInventory", LivingEntity.class).orElseThrow();
            Method findFirstCurio = ReflectiveCompat.findMethod(curiosHandler, "findFirstCurio", Predicate.class).orElseThrow();
            Method slotResultStack = ReflectiveCompat.findMethod(slotResult, "stack").orElseThrow();

            curiosBridge = new CuriosBridge(getCuriosInventory, findFirstCurio, slotResultStack);
            return curiosBridge;
        } catch (Throwable e) {
            curiosUnavailable = true;
            return null;
        }
    }

    private record PokedexApi(
            Class<?> pokedexItemClass,
            Method getType,
            Constructor<?> pokedexGuiCtor,
            GuiCtorVariant ctorVariant,
            Object defaultPokedexType
    ) {
        Object createGui(Object type, Identifier speciesId) throws ReflectiveOperationException {
            return switch (ctorVariant) {
                case THREE_ARG -> pokedexGuiCtor.newInstance(type, speciesId, null);
                case TWO_ARG -> pokedexGuiCtor.newInstance(type, speciesId);
                case FOUR_ARG_MARKER -> pokedexGuiCtor.newInstance(type, speciesId, null, null);
                // bitmask 4 = bit 2 set: BlockPos parameter uses its default (null)
                case FIVE_ARG -> pokedexGuiCtor.newInstance(type, speciesId, null, 4, null);
            };
        }
    }

    private record CobbledexApi(
            Object pokemonSpecies,
            Method getByIdentifier,
            Method getStandardForm,
            Constructor<?> cobbledexGuiCtor
    ) {
    }

    private record CuriosBridge(Method getCuriosInventory, Method findFirstCurio, Method slotResultStack) {
    }

    private static void logOpenFailure(String apiName, Identifier speciesId, Throwable e, Set<Identifier> loggedSpecies) {
        if (!loggedSpecies.add(speciesId)) {
            return;
        }
        if (openFailureLogCount < MAX_OPEN_FAILURE_LOGS) {
            AmiCore.LOGGER.warn("AMI Cobblemon: failed to open {} for {}", apiName, speciesId, e);
        } else if (openFailureLogCount == MAX_OPEN_FAILURE_LOGS) {
            AmiCore.LOGGER.warn("AMI Cobblemon: suppressing further Pokedex open failure logs");
        }
        openFailureLogCount++;
    }
}
