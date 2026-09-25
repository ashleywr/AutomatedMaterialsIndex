package com.sanhiruzu.ami.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Writes the live NeoForge villager and wandering-trader offer factories as resolved offers. */
public final class TradeDumpWriter {

    public static final int SCHEMA_VERSION = 1;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private TradeDumpWriter() {
    }

    public record StackView(String item, int count, String snbt) {
    }

    public record TradeRow(
            String source,
            String profession,
            int level,
            int listingIndex,
            String listingClass,
            StackView costA,
            StackView costB,
            StackView result,
            int maxUses,
            int villagerXp,
            float priceMultiplier,
            boolean rewardsExperience,
            String status,
            String error) {
    }

    public record Dump(int schemaVersion, int tradeCount, int unresolvedCount, List<TradeRow> trades) {
    }

    public record Counts(int trades, int unresolved) {
    }

    public static Counts writeJson(Path out, ServerLevel level) throws IOException {
        List<TradeRow> rows = collect(level);
        int unresolved = (int) rows.stream().filter(row -> !"resolved".equals(row.status())).count();
        Files.createDirectories(out.getParent());
        try (Writer writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            GSON.toJson(new Dump(SCHEMA_VERSION, rows.size(), unresolved, rows), writer);
        }
        return new Counts(rows.size(), unresolved);
    }

    public static List<TradeRow> collect(ServerLevel level) {
        List<TradeRow> rows = new ArrayList<>();

        List<Map.Entry<VillagerProfession, Int2ObjectMap<VillagerTrades.ItemListing[]>>> professions =
                new ArrayList<>(VillagerTrades.TRADES.entrySet());
        professions.sort(Comparator.comparing(entry -> professionId(entry.getKey())));

        for (Map.Entry<VillagerProfession, Int2ObjectMap<VillagerTrades.ItemListing[]>> professionEntry : professions) {
            VillagerProfession profession = professionEntry.getKey();
            int[] levels = professionEntry.getValue().keySet().toIntArray();
            java.util.Arrays.sort(levels);
            for (int villagerLevel : levels) {
                Villager villager = new Villager(EntityType.VILLAGER, level);
                villager.setVillagerData(villager.getVillagerData()
                        .setProfession(profession)
                        .setLevel(villagerLevel));
                appendListings(rows, "villager", professionId(profession), villagerLevel,
                        professionEntry.getValue().get(villagerLevel), villager, level);
                villager.discard();
            }
        }

        int[] wanderingLevels = VillagerTrades.WANDERING_TRADER_TRADES.keySet().toIntArray();
        java.util.Arrays.sort(wanderingLevels);
        for (int tradeLevel : wanderingLevels) {
            WanderingTrader trader = new WanderingTrader(EntityType.WANDERING_TRADER, level);
            appendListings(rows, "wandering_trader", null, tradeLevel,
                    VillagerTrades.WANDERING_TRADER_TRADES.get(tradeLevel), trader, level);
            trader.discard();
        }

        return rows;
    }

    private static void appendListings(
            List<TradeRow> rows,
            String source,
            String profession,
            int tradeLevel,
            VillagerTrades.ItemListing[] listings,
            Entity entity,
            ServerLevel level) {
        if (listings == null) {
            return;
        }
        for (int index = 0; index < listings.length; index++) {
            VillagerTrades.ItemListing listing = listings[index];
            String listingClass = listing == null ? "null" : listing.getClass().getName();
            if (listing == null) {
                rows.add(unresolved(source, profession, tradeLevel, index, listingClass, "null listing"));
                continue;
            }
            try {
                long seed = 0x414D490000000000L ^ ((long) source.hashCode() << 32)
                        ^ ((long) tradeLevel << 24) ^ index;
                MerchantOffer offer = listing.getOffer(entity, RandomSource.create(seed));
                if (offer == null) {
                    rows.add(unresolved(source, profession, tradeLevel, index, listingClass,
                            "listing returned no offer for the deterministic sample"));
                    continue;
                }
                rows.add(new TradeRow(
                        source,
                        profession,
                        tradeLevel,
                        index,
                        listingClass,
                        stack(offer.getBaseCostA(), level),
                        stack(offer.getCostB(), level),
                        stack(offer.getResult(), level),
                        offer.getMaxUses(),
                        offer.getXp(),
                        offer.getPriceMultiplier(),
                        offer.shouldRewardExp(),
                        "resolved",
                        null));
            } catch (Exception error) {
                rows.add(unresolved(source, profession, tradeLevel, index, listingClass,
                        error.getClass().getName() + ": " + String.valueOf(error.getMessage())));
            }
        }
    }

    private static TradeRow unresolved(
            String source, String profession, int level, int index, String listingClass, String error) {
        return new TradeRow(source, profession, level, index, listingClass,
                null, null, null, 0, 0, 0.0F, false, "unresolved", error);
    }

    private static StackView stack(ItemStack stack, ServerLevel level) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return new StackView(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                stack.getCount(),
                stack.save(level.registryAccess()).toString());
    }

    private static String professionId(VillagerProfession profession) {
        var id = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
        return id == null ? profession.toString() : id.toString();
    }
}
