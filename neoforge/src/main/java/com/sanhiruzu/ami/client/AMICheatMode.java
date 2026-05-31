package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.config.AmiConfig.CheatGiveMode;
import com.sanhiruzu.ami.neoforge.AMI;
import com.sanhiruzu.ami.network.AmiCheatGivePacket;
import com.sanhiruzu.ami.network.AmiNetworkState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AMICheatMode {
    private AMICheatMode() {
    }

    /**
     * Config toggle is on AND the player is allowed to perform elevated actions.
     * Always true in singleplayer / as LAN host. On a dedicated server requires OP (level 2).
     */
    public static boolean isEnabled() {
        return AmiConfig.cheatMode && isAllowed();
    }

    /**
     * Returns whether the current player has the authority to run cheat actions,
     * regardless of whether the config toggle is on.
     */
    public static boolean isAllowed() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        if (mc.hasSingleplayerServer()) return true;           // singleplayer or LAN host
        return mc.player.hasPermissions(2);                    // OP on dedicated server
    }

    /**
     * True if the player currently has an item held on their cursor.
     */
    public static boolean hasCarriedItem() {
        var mc = Minecraft.getInstance();
        return mc.player != null && !mc.player.containerMenu.getCarried().isEmpty();
    }

    /**
     * Give one of an item to the local player.
     * When AMI is on the server: uses a custom packet so the item appears directly on the cursor.
     * In creative mode without AMI server: sets the cursor client-side.
     * Otherwise: falls back to /give (item goes to inventory).
     */
    public static void giveItem(ResourceLocation itemId) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == Items.AIR) return;

        giveItem(new ItemStack(item), itemId);
    }

    public static void giveItem(ItemStack source) {
        if (source == null || source.isEmpty()) return;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(source.getItem());
        if (itemId == null) return;
        ItemStack stack = source.copy();
        stack.setCount(1);
        giveItem(stack, itemId);
    }

    private static void giveItem(ItemStack stack, ResourceLocation itemId) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || stack.isEmpty()) return;

        if (mc.player.getAbilities().instabuild) {
            // Creative: always set cursor client-side regardless of give-mode setting
            // (server trusts creative clients; same approach as EMI).
            mc.player.containerMenu.setCarried(stack.copy());
        } else if (AmiNetworkState.onServer) {
            // Survival OP with AMI on server: use the give-mode preference.
            if (AmiConfig.cheatGiveMode == CheatGiveMode.CURSOR) {
                PacketDistributor.sendToServer(new AmiCheatGivePacket(stack.copy()));
            } else {
                sendCommand("give @s " + itemId);
            }
        } else {
            // Survival OP without AMI server: /give is the only option.
            sendCommand("give @s " + itemId);
        }
    }

    /**
     * Give a full stack of an item to the local player.
     * Creative: cursor client-side. Survival OP + AMI server: respects give-mode setting.
     */
    public static void giveStack(ResourceLocation itemId) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == Items.AIR) return;

        ItemStack stack = new ItemStack(item);
        stack.setCount(stack.getMaxStackSize());
        giveStack(stack, itemId);
    }

    public static void giveStack(ItemStack source) {
        if (source == null || source.isEmpty()) return;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(source.getItem());
        if (itemId == null) return;
        ItemStack stack = source.copy();
        stack.setCount(stack.getMaxStackSize());
        giveStack(stack, itemId);
    }

    private static void giveStack(ItemStack stack, ResourceLocation itemId) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || stack.isEmpty()) return;

        if (mc.player.getAbilities().instabuild) {
            mc.player.containerMenu.setCarried(stack.copy());
        } else if (AmiNetworkState.onServer) {
            if (AmiConfig.cheatGiveMode == CheatGiveMode.CURSOR) {
                PacketDistributor.sendToServer(new AmiCheatGivePacket(stack.copy()));
            } else {
                sendCommand("give @s " + itemId + " 64");
            }
        } else {
            sendCommand("give @s " + itemId + " 64");
        }
    }

    /**
     * Delete the item currently held on the player's cursor.
     * Creative: clear client-side. Survival OP + AMI server: clear via packet.
     */
    public static void deleteCarried() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (mc.player.getAbilities().instabuild) {
            mc.player.containerMenu.setCarried(ItemStack.EMPTY);
        } else if (AmiNetworkState.onServer) {
            PacketDistributor.sendToServer(new AmiCheatGivePacket(ItemStack.EMPTY));
        }
    }

    /**
     * Locate the nearest biome via /locate biome.
     */
    public static void locateBiome(ResourceLocation biomeId) {
        sendCommand("locate biome " + biomeId);
    }

    /**
     * Locate the nearest structure via /locate structure.
     */
    public static void locateStructure(ResourceLocation structureId) {
        sendCommand("locate structure " + structureId);
    }

    /**
     * Give one spawn egg for the specified entity type to the local player.
     * If the entity has a spawn egg, gives the spawn egg; otherwise does nothing.
     */
    public static void giveEntityAsSpawnEgg(ResourceLocation entityId) {
        ResourceLocation spawnEggId = entityToSpawnEggId(entityId);
        if (spawnEggId != null) {
            giveItem(spawnEggId);
        }
    }

    /**
     * Give a full stack of spawn eggs for the specified entity type to the local player.
     * If the entity has a spawn egg, gives a full stack of spawn eggs; otherwise does nothing.
     */
    public static void giveEntityStackAsSpawnEgg(ResourceLocation entityId) {
        ResourceLocation spawnEggId = entityToSpawnEggId(entityId);
        if (spawnEggId != null) {
            giveStack(spawnEggId);
        }
    }

    /**
     * Convert an entity ID to its corresponding spawn egg item ID.
     * Returns null if the spawn egg does not exist.
     * Example: minecraft:cow -> minecraft:cow_spawn_egg
     */
    private static ResourceLocation entityToSpawnEggId(ResourceLocation entityId) {
        ResourceLocation spawnEggId = ResourceLocation.fromNamespaceAndPath(
                entityId.getNamespace(),
                entityId.getPath() + "_spawn_egg"
        );
        if (BuiltInRegistries.ITEM.get(spawnEggId) != Items.AIR) {
            return spawnEggId;
        }
        return null;
    }

    /**
     * Spawns a Cobblemon Pokémon species in the world in front of the player.
     * Entity ID is expected in the form cobblemon:species/pancham.
     */
    public static void spawnPokemon(ResourceLocation entityId) {
        String species = extractSpeciesName(entityId);
        sendCommand("spawnpokemon " + species);
    }

    /**
     * Adds a Cobblemon Pokémon species directly to the player's party.
     */
    public static void pokemonToParty(ResourceLocation entityId) {
        String species = extractSpeciesName(entityId);
        sendCommand("pokegive @s " + species);
    }

    private static String extractSpeciesName(ResourceLocation entityId) {
        String path = entityId.getPath();
        return path.startsWith("species/") ? path.substring("species/".length()) : path;
    }

    private static void sendCommand(String command) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        AMI.LOGGER.debug("AMI cheat: /{}", command);
        mc.player.connection.sendCommand(command);
    }
}
