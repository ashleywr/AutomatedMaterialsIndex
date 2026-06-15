package com.sanhiruzu.ami.network;

import com.sanhiruzu.ami.compat.CobblemonServerCheats;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record AmiCheatPokemonPacket(ResourceLocation speciesId, Action action) implements CustomPacketPayload {
    private static final Logger LOGGER = LoggerFactory.getLogger("AMI");

    public static final Type<AmiCheatPokemonPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("ami", "cheat_pokemon"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AmiCheatPokemonPacket> STREAM_CODEC =
            StreamCodec.of(AmiCheatPokemonPacket::encode, AmiCheatPokemonPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, AmiCheatPokemonPacket packet) {
        buf.writeResourceLocation(packet.speciesId);
        buf.writeEnum(packet.action);
    }

    private static AmiCheatPokemonPacket decode(RegistryFriendlyByteBuf buf) {
        return new AmiCheatPokemonPacket(buf.readResourceLocation(), buf.readEnum(Action.class));
    }

    /**
     * Server-side handler.
     */
    public void handleOnServer(ServerPlayer serverPlayer) {
        if (!AmiCheatPermissions.canUseCheats(serverPlayer)) {
            LOGGER.warn("AMI cheat: {} attempted Pokemon {} without permission",
                    serverPlayer.getName().getString(), action);
            return;
        }
        String species = extractSpeciesName(speciesId);
        boolean ok = switch (action) {
            case SPAWN -> CobblemonServerCheats.spawnPokemon(serverPlayer, species);
            case PARTY -> CobblemonServerCheats.givePokemon(serverPlayer, species);
        };
        if (!ok) {
            LOGGER.warn("AMI cheat: failed Pokemon {} for {}", action, speciesId);
        }
    }

    private static String extractSpeciesName(ResourceLocation entityId) {
        String path = entityId.getPath();
        return path.startsWith("species/") ? path.substring("species/".length()) : path;
    }

    @Override
    public Type<AmiCheatPokemonPacket> type() {
        return TYPE;
    }

    public enum Action {
        SPAWN,
        PARTY
    }
}
