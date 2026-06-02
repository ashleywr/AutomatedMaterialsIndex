package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class CobblemonPokedexOpener {
    private enum GuiCtorVariant { THREE_ARG, TWO_ARG, FIVE_ARG }

    private static PokedexApi api;
    private static boolean unavailable;
    private static CuriosBridge curiosBridge;
    private static boolean curiosUnavailable;

    private CobblemonPokedexOpener() {
    }

    public static void invalidateCaches() {
        api = null;
        unavailable = false;
        curiosBridge = null;
        curiosUnavailable = false;
    }

    public static boolean hasPokedex() {
        PokedexApi api = api();
        if (api == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        return findPokedexType(api, mc.player) != null;
    }

    public static boolean isPokemonSpecies(SearchNode node) {
        return node != null && "pokemon_species".equals(node.meta(SearchNodeKeys.ENTITY_CATEGORY, ""));
    }

    public static boolean handlePrimaryClick(SearchNode node) {
        if (!isPokemonSpecies(node)) {
            return false;
        }

        PokedexApi api = api();
        if (api == null) {
            return true;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return true;
        }

        Object pokedexType = findPokedexType(api, mc.player);
        if (pokedexType == null) {
            return true;
        }

        ResourceLocation speciesId = speciesId(node);
        if (speciesId == null) {
            return true;
        }

        try {
            Object screen = api.createGui(pokedexType, speciesId);
            mc.setScreen((Screen) screen);
        } catch (Throwable ignored) {
            // Keep AMI clicks non-fatal when Cobblemon changes the screen ABI.
        }
        return true;
    }

    private static Object findPokedexType(PokedexApi api, LivingEntity player) {
        if (!(player instanceof net.minecraft.world.entity.player.Player realPlayer)) {
            return null;
        }

        Inventory inventory = realPlayer.getInventory();
        Object type = findPokedexType(api, inventory.items);
        if (type != null) return type;
        type = findPokedexType(api, inventory.offhand);
        if (type != null) return type;
        type = findPokedexType(api, inventory.armor);
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

    private static ResourceLocation speciesId(SearchNode node) {
        String species = node.meta(SearchNodeKeys.POKEMON_SPECIES, "");
        if (!species.isBlank()) {
            ResourceLocation parsed = ResourceLocation.tryParse(species);
            if (parsed != null) return parsed;
        }

        if ("cobblemon".equals(node.id().getNamespace()) && node.id().getPath().startsWith("species/")) {
            return ResourceLocation.fromNamespaceAndPath("cobblemon", node.id().getPath().substring("species/".length()));
        }

        return null;
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

            // Try multiple constructor shapes across Cobblemon builds. Cobblemon uses @JvmOverloads
            // on PokedexGUI, so the preferred form is the 3-arg @JvmOverloads constructor.
            Constructor<?> guiCtor = null;
            GuiCtorVariant ctorVariant = null;

            // Shape 1: @JvmOverloads primary — PokedexGUI(PokedexType, ResourceLocation, BlockPos)
            var c3 = ReflectiveCompat.findConstructor(pokedexGui, pokedexType, ResourceLocation.class, BlockPos.class);
            if (c3.isPresent()) {
                guiCtor = c3.get();
                ctorVariant = GuiCtorVariant.THREE_ARG;
            }

            // Shape 2: @JvmOverloads secondary — PokedexGUI(PokedexType, ResourceLocation)
            if (guiCtor == null) {
                var c2 = ReflectiveCompat.findConstructor(pokedexGui, pokedexType, ResourceLocation.class);
                if (c2.isPresent()) {
                    guiCtor = c2.get();
                    ctorVariant = GuiCtorVariant.TWO_ARG;
                }
            }

            // Shape 3: Kotlin default-args synthetic — PokedexGUI(PokedexType, ResourceLocation, BlockPos, int, DefaultConstructorMarker)
            if (guiCtor == null) {
                try {
                    Class<?> dcm = Class.forName("kotlin.jvm.internal.DefaultConstructorMarker");
                    Constructor<?> c5 = pokedexGui.getDeclaredConstructor(
                            pokedexType, ResourceLocation.class, BlockPos.class, int.class, dcm);
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

            api = new PokedexApi(pokedexItem, getType, guiCtor, ctorVariant);
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
            GuiCtorVariant ctorVariant
    ) {
        Object createGui(Object type, ResourceLocation speciesId) throws ReflectiveOperationException {
            return switch (ctorVariant) {
                case THREE_ARG -> pokedexGuiCtor.newInstance(type, speciesId, null);
                case TWO_ARG -> pokedexGuiCtor.newInstance(type, speciesId);
                // bitmask 4 = bit 2 set: BlockPos parameter uses its default (null)
                case FIVE_ARG -> pokedexGuiCtor.newInstance(type, speciesId, null, 4, null);
            };
        }
    }

    private record CuriosBridge(Method getCuriosInventory, Method findFirstCurio, Method slotResultStack) {
    }
}
