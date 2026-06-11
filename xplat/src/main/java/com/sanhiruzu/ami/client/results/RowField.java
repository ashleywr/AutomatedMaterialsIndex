package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.compat.CompatDisplayNames;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.util.StorageDisplayFormatter;
import net.minecraft.network.chat.Component;

/**
 * Fields that can appear on the subtitle line of a list-view row.
 * Ordinal order is the display order when multiple fields are enabled.
 */
public enum RowField {

    MOD_NAME(Component.translatable("ami.row_field.mod")) {
        @Override
        public String extract(SearchNode node) {
            return displayModName(node);
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.id().getNamespace().isEmpty();
        }
    },

    ID(Component.translatable("ami.row_field.id")) {
        @Override
        public String extract(SearchNode node) {
            return node.id().toString();
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return true;
        }
    },

    TYPE(Component.translatable("ami.row_field.type")) {
        @Override
        public String extract(SearchNode node) {
            return node.type().displayName().getString();
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return true;
        }
    },

    DISCOVERY(Component.translatable("ami.row_field.discovery")) {
        @Override
        public String extract(SearchNode node) {
            String state = node.meta(SearchNodeKeys.DISCOVERY_STATE, "");
            if (state.isBlank()) {
                return "";
            }
            if (node.type() == com.sanhiruzu.ami.index.NodeType.ITEM
                    && !node.meta(SearchNodeKeys.FOOD_NUTRITION, "").isBlank()) {
                return Component.translatable("ami.discovery.food." + state).getString();
            }
            return Component.translatable("ami.discovery." + state).getString();
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.DISCOVERY_STATE, "").isBlank();
        }
    },

    AMMO_TYPE(Component.translatable("ami.row_field.ammo")) {
        @Override
        public String extract(SearchNode node) {
            String ammo = node.meta(SearchNodeKeys.AMMO_TYPE, "");
            return ammo.isBlank() ? "" : Component.translatable("ami.row_field.ammo_value", ammo).getString();
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.AMMO_TYPE, "").isBlank();
        }
    },

    STORAGE_CAPACITY(Component.translatable("ami.row_field.storage")) {
        @Override
        public String extract(SearchNode node) {
            String cap = node.meta(SearchNodeKeys.ESM_CAPACITY, "");
            return cap.isEmpty() ? "" : Component.translatable("ami.row_field.storage_capacity",
                    StorageDisplayFormatter.formatChestEquivalent(cap)).getString();
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.ESM_CAPACITY, "").isEmpty();
        }
    },

    ENERGY_GENERATION(Component.translatable("ami.row_field.energy_generation")) {
        @Override
        public String extract(SearchNode node) {
            String generation = node.meta(SearchNodeKeys.ENERGY_GENERATION, "");
            return generation.isEmpty() ? "" : Component.translatable("ami.row_field.energy_generation_value", generation).getString();
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.ENERGY_GENERATION, "").isEmpty();
        }
    },

    ENERGY_CAPACITY(Component.translatable("ami.row_field.energy")) {
        @Override
        public String extract(SearchNode node) {
            String energy = node.meta(SearchNodeKeys.ENERGY_CAPACITY, "");
            return energy.isEmpty() ? "" : Component.translatable("ami.row_field.energy_value", energy).getString();
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.ENERGY_CAPACITY, "").isEmpty();
        }
    },

    GREGTECH_EU_GENERATION(Component.translatable("ami.row_field.gregtech_eu_generation")) {
        @Override
        public String extract(SearchNode node) {
            String eu = node.meta(SearchNodeKeys.GREGTECH_EU_GENERATION, "");
            return eu.isEmpty() ? "" : eu + " EU/t out";
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.GREGTECH_EU_GENERATION, "").isEmpty();
        }
    },

    GREGTECH_EU_CONSUMPTION(Component.translatable("ami.row_field.gregtech_eu_consumption")) {
        @Override
        public String extract(SearchNode node) {
            String eu = node.meta(SearchNodeKeys.GREGTECH_EU_CONSUMPTION, "");
            return eu.isEmpty() ? "" : eu + " EU/t use";
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.GREGTECH_EU_CONSUMPTION, "").isEmpty();
        }
    },

    GREGTECH_EU_INPUT(Component.translatable("ami.row_field.gregtech_eu_input")) {
        @Override
        public String extract(SearchNode node) {
            String eu = node.meta(SearchNodeKeys.GREGTECH_EU_INPUT, "");
            return eu.isEmpty() ? "" : eu + " EU/t in";
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.GREGTECH_EU_INPUT, "").isEmpty();
        }
    },

    GREGTECH_EU_OUTPUT(Component.translatable("ami.row_field.gregtech_eu_output")) {
        @Override
        public String extract(SearchNode node) {
            String eu = node.meta(SearchNodeKeys.GREGTECH_EU_OUTPUT, "");
            return eu.isEmpty() ? "" : eu + " EU/t out";
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.GREGTECH_EU_OUTPUT, "").isEmpty();
        }
    },

    GREGTECH_AMPERAGE(Component.translatable("ami.row_field.gregtech_amps")) {
        @Override
        public String extract(SearchNode node) {
            String amps = node.meta(SearchNodeKeys.GREGTECH_AMPERAGE, "");
            return amps.isEmpty() ? "" : amps + "A";
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.GREGTECH_AMPERAGE, "").isEmpty();
        }
    },

    FLUID_CAPACITY(Component.translatable("ami.row_field.fluid_capacity")) {
        @Override
        public String extract(SearchNode node) {
            String capacity = node.meta(SearchNodeKeys.FLUID_CAPACITY, "");
            return capacity.isEmpty() ? "" : Component.translatable("ami.row_field.fluid_capacity_value", capacity).getString();
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.FLUID_CAPACITY, "").isEmpty();
        }
    },

    TOOL_SPEED(Component.translatable("ami.row_field.tool_speed")) {
        @Override
        public String extract(SearchNode node) {
            String speed = node.meta(SearchNodeKeys.TOOL_SPEED, "");
            return speed.isEmpty() ? "" : Component.translatable("ami.row_field.tool_speed_value", speed).getString();
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.TOOL_SPEED, "").isEmpty();
        }
    },

    TOOL_USES(Component.translatable("ami.row_field.tool_uses")) {
        @Override
        public String extract(SearchNode node) {
            String uses = node.meta(SearchNodeKeys.TOOL_USES, "");
            return uses.isEmpty() ? "" : Component.translatable("ami.row_field.tool_uses_value", uses).getString();
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.TOOL_USES, "").isEmpty();
        }
    },

    ARMOR_DEFENSE(Component.translatable("ami.row_field.armor_defense")) {
        @Override
        public String extract(SearchNode node) {
            String defense = node.meta(SearchNodeKeys.ARMOR_DEFENSE, "");
            return defense.isEmpty() ? "" : Component.translatable("ami.row_field.armor_defense_value", defense).getString();
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.ARMOR_DEFENSE, "").isEmpty();
        }
    },

    ARMOR_TOUGHNESS(Component.translatable("ami.row_field.armor_toughness")) {
        @Override
        public String extract(SearchNode node) {
            String toughness = node.meta(SearchNodeKeys.ARMOR_TOUGHNESS, "");
            return toughness.isEmpty() ? "" : Component.translatable("ami.row_field.armor_toughness_value", toughness).getString();
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.ARMOR_TOUGHNESS, "").isEmpty();
        }
    },

    FOOD_NUTRITION(Component.translatable("ami.row_field.food_nutrition")) {
        @Override
        public String extract(SearchNode node) {
            String nutrition = node.meta(SearchNodeKeys.FOOD_NUTRITION, "");
            return nutrition.isEmpty() ? "" : Component.translatable("ami.row_field.food_nutrition_value", nutrition).getString();
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.FOOD_NUTRITION, "").isEmpty();
        }
    },

    FOOD_SATURATION(Component.translatable("ami.row_field.food_saturation")) {
        @Override
        public String extract(SearchNode node) {
            String saturation = node.meta(SearchNodeKeys.FOOD_SATURATION, "");
            return saturation.isEmpty() ? "" : Component.translatable("ami.row_field.food_saturation_value", saturation).getString();
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.FOOD_SATURATION, "").isEmpty();
        }
    },

    REQUIRED_TOOL(Component.translatable("ami.row_field.required_tool")) {
        @Override
        public String extract(SearchNode node) {
            String tool = node.meta(SearchNodeKeys.REQUIRED_TOOL, "");
            if (tool.isBlank()) {
                return "";
            }
            String path = tool.contains(":") ? tool.substring(tool.indexOf(':') + 1) : tool;
            return Component.translatable("ami.row_field.required_tool_value",
                    path.replace('_', ' ')).getString();
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.REQUIRED_TOOL, "").isBlank();
        }
    },

    HEALTH(Component.translatable("ami.row_field.health")) {
        @Override
        public String extract(SearchNode node) {
            String health = node.meta(SearchNodeKeys.ENTITY_HEALTH, "");
            return health.isEmpty() ? "" : Component.translatable("ami.row_field.health_value", health).getString();
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.ENTITY_HEALTH, "").isEmpty();
        }
    },

    DAMAGE(Component.translatable("ami.row_field.damage")) {
        @Override
        public String extract(SearchNode node) {
            String damage = damageValue(node);
            return damage.isEmpty() ? "" : Component.translatable("ami.row_field.damage_value", damage).getString();
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !damageValue(node).isEmpty();
        }

        private String damageValue(SearchNode node) {
            String itemDamage = node.meta(SearchNodeKeys.ATTACK_DAMAGE, "");
            return itemDamage.isEmpty() ? node.meta(SearchNodeKeys.ENTITY_ATTACK_DAMAGE, "") : itemDamage;
        }
    },

    DPS(Component.translatable("ami.row_field.dps")) {
        @Override
        public String extract(SearchNode node) {
            String dps = node.meta(SearchNodeKeys.DPS, "");
            return dps.isEmpty() ? "" : Component.translatable("ami.row_field.dps_value", dps).getString();
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.DPS, "").isEmpty();
        }
    },

    MODULAR_GEAR_KIND(Component.translatable("ami.row_field.modular_gear_kind")) {
        @Override
        public String extract(SearchNode node) {
            return formatTokenList(node.meta(SearchNodeKeys.MODULAR_GEAR_ITEM_KIND, ""));
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.MODULAR_GEAR_ITEM_KIND, "").isBlank();
        }
    },

    MODULAR_GEAR_MATERIAL(Component.translatable("ami.row_field.modular_gear_material")) {
        @Override
        public String extract(SearchNode node) {
            return formatToken(node.meta(SearchNodeKeys.MODULAR_GEAR_MATERIAL, ""));
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.MODULAR_GEAR_MATERIAL, "").isBlank();
        }
    },

    MODULAR_GEAR_PART(Component.translatable("ami.row_field.modular_gear_part")) {
        @Override
        public String extract(SearchNode node) {
            return formatToken(node.meta(SearchNodeKeys.MODULAR_GEAR_PART, ""));
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.MODULAR_GEAR_PART, "").isBlank();
        }
    },

    MODULAR_GEAR_TRAITS(Component.translatable("ami.row_field.modular_gear_traits")) {
        @Override
        public String extract(SearchNode node) {
            String runtimeTraits = formatTokenList(node.meta(SearchNodeKeys.MODULAR_GEAR_RUNTIME_TRAITS, ""));
            if (!runtimeTraits.isBlank()) {
                return runtimeTraits;
            }
            return formatMaterialTraitDetails(
                    node.meta(SearchNodeKeys.MODULAR_GEAR_MATERIAL_TRAIT_DETAILS, ""),
                    node.meta(SearchNodeKeys.MODULAR_GEAR_MATERIAL_TRAITS, ""));
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.MODULAR_GEAR_RUNTIME_TRAITS, "").isBlank()
                    || !node.meta(SearchNodeKeys.MODULAR_GEAR_MATERIAL_TRAITS, "").isBlank();
        }
    },

    POKEMON_TYPE(Component.translatable("ami.row_field.pokemon_type")) {
        @Override
        public String extract(SearchNode node) {
            String type = formatTokenList(node.meta(SearchNodeKeys.POKEMON_TYPE, ""));
            return type.isEmpty() ? "" : type;
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.POKEMON_TYPE, "").isEmpty();
        }
    },

    POKEMON_HEALING(Component.translatable("ami.row_field.pokemon_healing")) {
        @Override
        public String extract(SearchNode node) {
            String healing = node.meta(SearchNodeKeys.POKEMON_HEALING, "");
            return healing.isEmpty() ? "" : healing + " HP";
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.POKEMON_HEALING, "").isEmpty();
        }
    },

    POKEMON_DEX(Component.translatable("ami.row_field.pokemon_dex")) {
        @Override
        public String extract(SearchNode node) {
            String dex = node.meta(SearchNodeKeys.POKEMON_DEX_NUMBER, "");
            return dex.isEmpty() ? "" : "#" + dex;
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.POKEMON_DEX_NUMBER, "").isEmpty();
        }
    },

    POKEMON_SPEED(Component.translatable("ami.row_field.pokemon_speed")) {
        @Override
        public String extract(SearchNode node) {
            String speed = node.meta(SearchNodeKeys.POKEMON_BASE_SPEED, "");
            return speed.isEmpty() ? "" : speed + " Spe";
        }

        @Override
        public boolean hasValue(SearchNode node) {
            return !node.meta(SearchNodeKeys.POKEMON_BASE_SPEED, "").isEmpty();
        }
    };

    public final Component displayName;

    RowField(Component displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the display string for this field from the given node, or "" if not applicable.
     */
    public abstract String extract(SearchNode node);

    public boolean hasValue(SearchNode node) {
        return !extract(node).isEmpty();
    }

    private static String displayModName(SearchNode node) {
        return CompatDisplayNames.displayModName(node);
    }

    private static String formatTokenList(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (String token : raw.split("[,\\s]+")) {
            if (token.isBlank()) continue;
            if (out.length() > 0) out.append(", ");
            out.append(formatToken(token));
        }
        return out.toString();
    }

    private static String formatMaterialTraitDetails(String details, String rawTraits) {
        java.util.LinkedHashSet<String> values = new java.util.LinkedHashSet<>();
        if (details != null && !details.isBlank()) {
            for (String detail : details.split(",")) {
                String token = detail.trim();
                int separator = token.lastIndexOf(':');
                if (separator >= 0 && separator < token.length() - 1) {
                    token = token.substring(separator + 1);
                }
                if (!token.isBlank()) {
                    values.add(token);
                }
            }
        }
        if (values.isEmpty() && rawTraits != null && !rawTraits.isBlank()) {
            java.util.List<String> tokens = java.util.Arrays.stream(rawTraits.split("[,\\s]+"))
                    .filter(token -> !token.isBlank())
                    .toList();
            for (String token : tokens) {
                boolean hasLevelToken = tokens.stream().anyMatch(other -> other.startsWith(token + "_"));
                if (!hasLevelToken) {
                    values.add(token);
                }
            }
        }
        return formatTokenList(String.join(",", values));
    }

    private static String formatToken(String raw) {
        String normalized = raw.replace('_', ' ').replace('-', ' ').trim();
        if (normalized.isBlank()) return "";
        StringBuilder out = new StringBuilder();
        for (String word : normalized.split("\\s+")) {
            if (word.isBlank()) continue;
            if (out.length() > 0) out.append(' ');
            if (word.matches("(?i)[ivxlcdm]+")) {
                out.append(word.toUpperCase(java.util.Locale.ROOT));
                continue;
            }
            out.append(word.substring(0, 1).toUpperCase(java.util.Locale.ROOT));
            if (word.length() > 1) {
                out.append(word.substring(1).toLowerCase(java.util.Locale.ROOT));
            }
        }
        return out.toString();
    }
}
