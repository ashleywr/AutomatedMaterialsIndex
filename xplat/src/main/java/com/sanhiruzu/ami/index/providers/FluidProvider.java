package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.index.*;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FluidProvider implements IAmiDataProvider {

    @Override
    public void populate(GlobalIndex index, @Nullable Level level) {
        int count = 0;
        Map<String, String> modNameCache = new HashMap<>();

        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (fluid == Fluids.EMPTY) continue;
            if (!fluid.isSource(fluid.defaultFluidState())) continue;

            ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
            if (id == null) continue;

            String displayName = resolveDisplayName(fluid);
            if (displayName.isBlank()) continue;

            String modId = id.getNamespace();

            registerBucketIcon(id, fluid);

            Map<String, String> meta = new HashMap<>(4);
            meta.put(SearchNodeKeys.MOD_ID, modId);

            String modName = modNameCache.computeIfAbsent(modId, ns ->
                    Services.PLATFORM.getModName(ns).orElse(ns));
            addModNameTokens(meta, modName);

            index.addNode(new SearchNode(id, NodeType.FLUID, displayName, 0xFFFFFF, 0, meta));
            count++;
        }

        AmiCore.LOGGER.info("AMI indexing: FluidProvider indexed {} source fluids", count);
    }

    public static void registerBucketIcon(ResourceLocation fluidId, Fluid fluid) {
        Item bucket = fluid.getBucket();
        if (bucket != Items.AIR && bucket != Items.BUCKET) {
            ItemIconRenderer.registerStack(fluidId, new ItemStack(bucket));
        }
    }

    private static String resolveDisplayName(Fluid fluid) {
        String platformName = Services.PLATFORM.getFluidDisplayName(fluid).getString();
        if (!platformName.isBlank()) return platformName;

        // Fallback: strip " Bucket" from the bucket item name
        Item bucket = fluid.getBucket();
        if (bucket != Items.AIR && bucket != Items.BUCKET) {
            String bucketName = new ItemStack(bucket).getHoverName().getString();
            if (bucketName.endsWith(" Bucket")) {
                return bucketName.substring(0, bucketName.length() - " Bucket".length());
            }
            return bucketName;
        }
        return "";
    }

    private static void addModNameTokens(Map<String, String> meta, String modName) {
        if (modName == null || modName.isBlank()) return;
        String normalized = Normalizer.normalize(
                modName.replaceAll("([a-z])([A-Z])", "$1 $2"), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        for (String part : normalized.split("[^a-z0-9]+")) {
            if (part.length() >= 3) {
                meta.merge(SearchNodeKeys.PLAIN_SEARCH_TOKENS, part, (existing, added) ->
                        existing.contains(added) ? existing : existing + " " + added);
            }
        }
    }
}
