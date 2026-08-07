package com.sanhiruzu.ami.index;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Writes a pack-author friendly coverage table from AMI's live Cobblemon species nodes.
 */
public final class PokemonCoverageDumpWriter {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PokemonCoverageDumpWriter() {
    }

    public static Outputs writeDump(Path dumpDir) throws IOException {
        Files.createDirectories(dumpDir);
        List<Row> rows = collectFromRuntime();

        Path json = dumpDir.resolve("pokemon_coverage.json");
        Path csv = dumpDir.resolve("pokemon_coverage.csv");
        Path html = dumpDir.resolve("pokemon_coverage.html");

        writeJson(json, rows);
        writeCsv(csv, rows);
        writeHtml(html, rows);

        return new Outputs(json, csv, html, rows.size());
    }

    public static List<Row> collectFromRuntime() {
        List<Row> rows = new ArrayList<>();
        for (SearchNode node : GlobalIndex.getInstance().getNodes(NodeType.ENTITY)) {
            if (!isPokemonSpecies(node)) {
                continue;
            }
            rows.add(Row.from(node));
        }
        rows.sort(Comparator
                .comparingInt(Row::dexSort)
                .thenComparing(Row::speciesId)
                .thenComparing(Row::nodeId));
        return rows;
    }

    private static boolean isPokemonSpecies(SearchNode node) {
        return "pokemon_species".equals(node.meta(SearchNodeKeys.ENTITY_CATEGORY, ""))
                || !node.meta(SearchNodeKeys.POKEMON_SPECIES, "").isBlank();
    }

    private static void writeJson(Path path, List<Row> rows) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", 1);
        root.addProperty("generatedAtUtc", Instant.now().toString());
        root.addProperty("pokemonCount", rows.size());

        JsonArray pokemon = new JsonArray();
        for (Row row : rows) {
            JsonObject object = new JsonObject();
            object.addProperty("dex", row.dex());
            object.addProperty("speciesId", row.speciesId());
            object.addProperty("nodeId", row.nodeId());
            object.addProperty("name", row.name());
            object.addProperty("implemented", row.implemented());
            object.add("types", strings(row.types()));
            object.addProperty("generation", row.generation());
            object.add("abilities", strings(row.abilities()));
            object.add("eggGroups", strings(row.eggGroups()));
            object.add("drops", strings(row.drops()));
            object.add("dropChances", strings(row.dropChances()));
            object.add("dropMinimums", strings(row.dropMinimums()));
            object.add("dropMaximums", strings(row.dropMaximums()));
            object.add("spawnBiomes", strings(row.spawnBiomes()));
            object.addProperty("hasKnownWildSpawnBiomes", !row.spawnBiomes().isEmpty());
            pokemon.add(object);
        }
        root.add("pokemon", pokemon);
        Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private static JsonArray strings(Collection<String> values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    private static void writeCsv(Path path, List<Row> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("dex,speciesId,name,implemented,types,generation,hasKnownWildSpawnBiomes,spawnBiomes,drops,abilities,eggGroups,nodeId");
            writer.newLine();
            for (Row row : rows) {
                writer.write(csv(Integer.toString(row.dex())));
                writer.write(',');
                writer.write(csv(row.speciesId()));
                writer.write(',');
                writer.write(csv(row.name()));
                writer.write(',');
                writer.write(csv(Boolean.toString(row.implemented())));
                writer.write(',');
                writer.write(csv(join(row.types())));
                writer.write(',');
                writer.write(csv(row.generation()));
                writer.write(',');
                writer.write(csv(Boolean.toString(!row.spawnBiomes().isEmpty())));
                writer.write(',');
                writer.write(csv(join(row.spawnBiomes())));
                writer.write(',');
                writer.write(csv(join(row.drops())));
                writer.write(',');
                writer.write(csv(join(row.abilities())));
                writer.write(',');
                writer.write(csv(join(row.eggGroups())));
                writer.write(',');
                writer.write(csv(row.nodeId()));
                writer.newLine();
            }
        }
    }

    private static void writeHtml(Path path, List<Row> rows) throws IOException {
        long wildRows = rows.stream().filter(row -> !row.spawnBiomes().isEmpty()).count();
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        html.append("<title>AMI Pokemon Coverage</title>");
        html.append("<style>");
        html.append(":root{color-scheme:light dark}body{margin:0;font-family:Segoe UI,Arial,sans-serif;background:#f5f7fa;color:#17202a}");
        html.append("header{background:#263238;color:white;padding:22px 28px}h1{margin:0 0 8px;font-size:24px}");
        html.append("main{padding:18px 28px}.summary{display:flex;flex-wrap:wrap;gap:10px;margin-bottom:14px}.pill{background:white;border:1px solid #d7dde5;border-radius:6px;padding:8px 10px}");
        html.append(".tools{margin-bottom:12px}input{font:inherit;padding:8px 10px;border:1px solid #b9c3cf;border-radius:4px;min-width:280px}");
        html.append("table{width:100%;border-collapse:collapse;background:white}th,td{border-bottom:1px solid #e0e5ec;padding:8px 10px;text-align:left;vertical-align:top;font-size:13px}");
        html.append("th{position:sticky;top:0;background:#eef2f6;z-index:1}.muted{color:#657182}.yes{color:#116329;font-weight:600}.no{color:#a33b2f;font-weight:600}");
        html.append("@media (prefers-color-scheme:dark){body{background:#12171c;color:#e7edf4}.pill,table{background:#1b2229}.pill,th,td,input{border-color:#34424f}th{background:#25313b}input{background:#151b21;color:#e7edf4}.muted{color:#9ca8b5}}");
        html.append("</style></head><body><header><h1>AMI Pokemon Coverage</h1><div>Generated from AMI's live Cobblemon species index.</div></header><main>");
        html.append("<div class=\"summary\"><div class=\"pill\">Pokemon rows: ").append(rows.size()).append("</div>");
        html.append("<div class=\"pill\">With known wild spawn biomes: ").append(wildRows).append("</div>");
        html.append("<div class=\"pill\">Without known wild spawn biomes: ").append(Math.max(0, rows.size() - wildRows)).append("</div></div>");
        html.append("<div class=\"tools\"><input id=\"filter\" type=\"search\" placeholder=\"Filter Pokemon, type, biome, drop, ability\"></div>");
        html.append("<table id=\"coverage\"><thead><tr><th>Dex</th><th>Pokemon</th><th>Types</th><th>Wild Biomes</th><th>Drops</th><th>Abilities</th><th>Egg Groups</th></tr></thead><tbody>");
        for (Row row : rows) {
            html.append("<tr><td>").append(row.dex() > 0 ? row.dex() : "").append("</td><td><strong>")
                    .append(escapeHtml(row.name())).append("</strong><br><span class=\"muted\">")
                    .append(escapeHtml(row.speciesId())).append("</span></td><td>")
                    .append(escapeHtml(join(row.types()))).append("</td><td class=\"")
                    .append(row.spawnBiomes().isEmpty() ? "no" : "yes").append("\">")
                    .append(row.spawnBiomes().isEmpty() ? "No indexed biomes" : escapeHtml(join(row.spawnBiomes())))
                    .append("</td><td>").append(escapeHtml(join(row.drops()))).append("</td><td>")
                    .append(escapeHtml(join(row.abilities()))).append("</td><td>")
                    .append(escapeHtml(join(row.eggGroups()))).append("</td></tr>");
        }
        html.append("</tbody></table>");
        html.append("<script>const f=document.getElementById('filter'),rows=[...document.querySelectorAll('#coverage tbody tr')];f.addEventListener('input',()=>{const q=f.value.toLowerCase();for(const r of rows)r.style.display=r.textContent.toLowerCase().includes(q)?'':'none';});</script>");
        html.append("</main></body></html>");
        Files.writeString(path, html.toString(), StandardCharsets.UTF_8);
    }

    private static List<String> splitCsvMetadata(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isBlank()) {
                values.add(trimmed);
            }
        }
        return List.copyOf(values);
    }

    private static String join(Collection<String> values) {
        return String.join("; ", values);
    }

    private static String csv(String value) {
        String escaped = Optional.ofNullable(value).orElse("").replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private static String escapeHtml(String value) {
        return Optional.ofNullable(value).orElse("")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public record Outputs(Path json, Path csv, Path html, int pokemonCount) {
    }

    public record Row(
            int dex,
            String speciesId,
            String nodeId,
            String name,
            boolean implemented,
            List<String> types,
            String generation,
            List<String> abilities,
            List<String> eggGroups,
            List<String> drops,
            List<String> dropChances,
            List<String> dropMinimums,
            List<String> dropMaximums,
            List<String> spawnBiomes
    ) {
        static Row from(SearchNode node) {
            String speciesId = node.meta(SearchNodeKeys.POKEMON_SPECIES, "");
            if (speciesId.isBlank() && "cobblemon".equals(node.id().getNamespace()) && node.id().getPath().startsWith("species/")) {
                speciesId = "cobblemon:" + node.id().getPath().substring("species/".length());
            }

            Set<String> biomes = new LinkedHashSet<>();
            for (SearchNode biome : node.getEdges(EdgeType.SPAWNS_IN)) {
                if (biome.type() == NodeType.BIOME) {
                    biomes.add(biome.displayName().isBlank() ? biome.id().toString() : biome.displayName());
                }
            }
            for (ResourceLocation biomeId : node.getUnresolvedEdgeIds(EdgeType.SPAWNS_IN)) {
                biomes.add(biomeId.toString());
            }

            return new Row(
                    parseInt(node.meta(SearchNodeKeys.POKEMON_DEX_NUMBER, "0")),
                    speciesId,
                    node.id().toString(),
                    node.displayName(),
                    Boolean.parseBoolean(node.meta(SearchNodeKeys.POKEMON_IMPLEMENTED, "true")),
                    splitCsvMetadata(node.meta(SearchNodeKeys.POKEMON_TYPE, "")),
                    node.meta(SearchNodeKeys.POKEMON_GENERATION, ""),
                    splitCsvMetadata(node.meta(SearchNodeKeys.POKEMON_ABILITIES, "")),
                    splitCsvMetadata(node.meta(SearchNodeKeys.POKEMON_EGG_GROUPS, "")),
                    splitCsvMetadata(node.meta(SearchNodeKeys.POKEMON_DROP_ITEM, "")),
                    splitCsvMetadata(node.meta(SearchNodeKeys.POKEMON_DROP_CHANCE, "")),
                    splitCsvMetadata(node.meta(SearchNodeKeys.POKEMON_DROP_MIN, "")),
                    splitCsvMetadata(node.meta(SearchNodeKeys.POKEMON_DROP_MAX, "")),
                    List.copyOf(biomes)
            );
        }

        int dexSort() {
            return dex > 0 ? dex : Integer.MAX_VALUE;
        }

        private static int parseInt(String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
    }
}
