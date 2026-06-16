package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.AmiCore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public final class CobblemonServerCheats {
    private static final String POKEMON_PROPERTIES = "com.cobblemon.mod.common.api.pokemon.PokemonProperties";
    private static final String COBBLEMON = "com.cobblemon.mod.common.Cobblemon";

    private CobblemonServerCheats() {
    }

    public static boolean spawnPokemon(ServerPlayer player, String species) {
        if (player == null) return false;
        return parseProperties(species).flatMap(properties -> createPokemonEntity(properties, player.level()))
                .map(entity -> {
                    Vec3 pos = player.position().add(player.getLookAngle().normalize().scale(2.0));
                    entity.setPos(pos.x, pos.y, pos.z);
                    entity.setYRot(player.getYRot());
                    entity.setXRot(player.getXRot());
                    boolean spawned = player.level().addFreshEntity(entity);
                    if (spawned) {
                        AmiCore.LOGGER.debug("AMI cheat spawned Cobblemon Pokemon {} for {}",
                                species, player.getName().getString());
                    }
                    return spawned;
                })
                .orElse(false);
    }

    public static boolean givePokemon(ServerPlayer player, String species) {
        if (player == null) return false;
        Optional<Object> pokemon = parseProperties(species).flatMap(CobblemonServerCheats::createPokemon);
        if (pokemon.isEmpty()) return false;

        try {
            Class<?> cobblemon = Class.forName(COBBLEMON);
            Object instance = cobblemon.getField("INSTANCE").get(null);
            Object storage = cobblemon.getMethod("getStorage").invoke(instance);
            Object party = storage.getClass().getMethod("getParty", ServerPlayer.class).invoke(storage, player);
            Method add = Arrays.stream(party.getClass().getMethods())
                    .filter(method -> "add".equals(method.getName()))
                    .filter(method -> method.getParameterCount() == 1)
                    .filter(method -> method.getParameterTypes()[0].isInstance(pokemon.get()))
                    .findFirst()
                    .orElseThrow();
            add.invoke(party, pokemon.get());
            AmiCore.LOGGER.debug("AMI cheat gave Cobblemon Pokemon {} to {}",
                    species, player.getName().getString());
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            AmiCore.LOGGER.warn("AMI cheat failed to give Cobblemon Pokemon {}", species, e);
            return false;
        }
    }

    private static Optional<Object> parseProperties(String species) {
        String normalized = normalizeSpecies(species);
        if (normalized.isEmpty()) return Optional.empty();
        try {
            Class<?> properties = Class.forName(POKEMON_PROPERTIES);
            Object companion = properties.getField("Companion").get(null);
            Method parse = companion.getClass().getMethod("parse", String.class, String.class, String.class);
            return Optional.ofNullable(parse.invoke(companion, normalized, " ", "="));
        } catch (ReflectiveOperationException | RuntimeException e) {
            AmiCore.LOGGER.warn("AMI cheat failed to parse Cobblemon Pokemon {}", normalized, e);
            return Optional.empty();
        }
    }

    private static Optional<Entity> createPokemonEntity(Object properties, Level level) {
        try {
            Object entity = properties.getClass().getMethod("createEntity", Level.class).invoke(properties, level);
            return entity instanceof Entity typed ? Optional.of(typed) : Optional.empty();
        } catch (ReflectiveOperationException | RuntimeException e) {
            AmiCore.LOGGER.warn("AMI cheat failed to create Cobblemon Pokemon entity", e);
            return Optional.empty();
        }
    }

    private static Optional<Object> createPokemon(Object properties) {
        try {
            return Optional.ofNullable(properties.getClass().getMethod("create").invoke(properties));
        } catch (ReflectiveOperationException | RuntimeException e) {
            AmiCore.LOGGER.warn("AMI cheat failed to create Cobblemon Pokemon", e);
            return Optional.empty();
        }
    }

    private static String normalizeSpecies(String species) {
        if (species == null) return "";
        String normalized = species.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("cobblemon:species/")) {
            normalized = normalized.substring("cobblemon:species/".length());
        } else if (normalized.startsWith("species/")) {
            normalized = normalized.substring("species/".length());
        } else if (normalized.startsWith("cobblemon:")) {
            normalized = normalized.substring("cobblemon:".length());
        }
        return normalized.matches("[a-z0-9_\\-]+") ? normalized : "";
    }
}
