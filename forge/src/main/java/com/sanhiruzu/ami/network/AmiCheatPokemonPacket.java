package com.sanhiruzu.ami.network;

import com.sanhiruzu.ami.compat.CobblemonServerCheats;
import com.sanhiruzu.ami.forge.AMI;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AmiCheatPokemonPacket {
    public enum Action {
        SPAWN,
        PARTY
    }

    private final ResourceLocation speciesId;
    private final Action action;

    public AmiCheatPokemonPacket(ResourceLocation speciesId, Action action) {
        this.speciesId = speciesId;
        this.action = action;
    }

    public static void encode(AmiCheatPokemonPacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.speciesId);
        buf.writeEnum(packet.action);
    }

    public static AmiCheatPokemonPacket decode(FriendlyByteBuf buf) {
        return new AmiCheatPokemonPacket(buf.readResourceLocation(), buf.readEnum(Action.class));
    }

    public static void handle(AmiCheatPokemonPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Player player = context.getSender();
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            if (!AmiCheatPermissions.canUseCheats(serverPlayer)) {
                AMI.LOGGER.warn("AMI cheat: {} attempted Pokemon {} without permission",
                        player.getName().getString(), packet.action);
                return;
            }

            String species = extractSpeciesName(packet.speciesId);
            boolean ok = switch (packet.action) {
                case SPAWN -> CobblemonServerCheats.spawnPokemon(serverPlayer, species);
                case PARTY -> CobblemonServerCheats.givePokemon(serverPlayer, species);
            };
            if (!ok) {
                AMI.LOGGER.warn("AMI cheat: failed Pokemon {} for {}", packet.action, packet.speciesId);
            }
        });
        context.setPacketHandled(true);
    }

    private static String extractSpeciesName(ResourceLocation entityId) {
        String path = entityId.getPath();
        return path.startsWith("species/") ? path.substring("species/".length()) : path;
    }
}
