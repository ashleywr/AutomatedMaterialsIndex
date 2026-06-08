package com.sanhiruzu.ami.network;

import com.sanhiruzu.ami.compat.CobblemonServerCheats;
import com.sanhiruzu.ami.neoforge.AMI;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AmiCheatPokemonPacket(ResourceLocation speciesId, Action action) implements CustomPacketPayload {
    public static final Type<AmiCheatPokemonPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AMI.MODID, "cheat_pokemon"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AmiCheatPokemonPacket> STREAM_CODEC =
            StreamCodec.of(AmiCheatPokemonPacket::encode, AmiCheatPokemonPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, AmiCheatPokemonPacket packet) {
        buf.writeResourceLocation(packet.speciesId);
        buf.writeEnum(packet.action);
    }

    private static AmiCheatPokemonPacket decode(RegistryFriendlyByteBuf buf) {
        return new AmiCheatPokemonPacket(buf.readResourceLocation(), buf.readEnum(Action.class));
    }

    public static void handle(AmiCheatPokemonPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
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
