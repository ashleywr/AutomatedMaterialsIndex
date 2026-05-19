import csv
import json
import urllib.request
import urllib.error
import sys
import os
import shutil
import datetime
from collections import defaultdict

# ==========================================
# Configuration for Local LM Studio
# ==========================================
API_URL = "http://127.0.0.1:1234/v1/chat/completions"
MODEL_ID = "mistralai/devstral-small-2-2512"
SYSTEM_PROMPT = """You are an expert Minecraft modding analyst.
I will provide you with a list of items from a specific Minecraft mod, along with their assigned AMI categories and supporting metadata.
Your job is to identify any anomalies or likely miscategorizations based on the item's name, assigned category, subcategory, facets, tags, and other metadata.

AMI category guide:
- utility: navigation items, portable utility items, buckets, leads, maps, fireworks, discs.
- social: player identity, teams, claims, social/player-oriented artifacts.
- bestiary: spawn eggs, mob/entity-related items.
- magic: potion/enchanting/reagent/artifact items with clear magical function.
- armor: wearable protection and curios-like equipment.
- tools: weapons, harvest tools, combat tools, fishing rods, shears, similar hand tools.
- tech: machines, workstations, redstone-like function, transport, engineered parts, ingots, dusts, circuits.
- nature: plants, fungi, seeds, crops, wood, organics, edible foods, nylium/mycelium-like living growth blocks.
- ingredients: loose crafting parts and raw components that are not primarily tech parts or food.
- decoration: furniture, lighting, display objects, textiles, cosmetic furnishing blocks.
- environment: biome/dimension/structure world concepts rather than ordinary placeable item families.
- geology: inert natural terrain, soil, sand, gravel, clay, stone-like world material.
- masonry: player-facing building materials and construction shapes such as bricks, slabs, stairs, walls, panes, and similar building blocks.
- misc: uncertain fallback only when no clearer family fits.

Evidence weighting:
- Trust these most: facets, AMI_Subcategory, BlocksMaterial, RequiredTool, CreativeTabLabel.
- Trust these next: DisplayName, ItemID, VariantGroup.
- Treat Tags as supporting hints only.
- Recipe/compatibility/search tags from other mods are weak evidence and should not override stronger metadata.
- If metadata is sparse or conflicting, be conservative and avoid weak guesses.

Examples of anomalies:
- A food item (e.g., "Cheese") placed in the 'Tech' category.
- A sword placed in the 'ingredients' category instead of 'tools'.
- An ingot placed in 'nature' instead of 'tech'.
- A decorative or display block placed in 'Geology' or 'Masonry'.
- A sign, jar, trophy, head, skull, candle, carpet, or placeable food placed in 'Terrain' or 'Geology'.
- A block that clearly opens a menu or behaves like a workstation placed in 'masonry' instead of 'tech'.

Important review rules:
- Do not treat "placeable" or "no_recipe" as evidence for geology/terrain by itself.
- If the item is a jar, sign, display container, food block, decorative block, interactive block, or any obvious furnishing object, it usually belongs in decoration, tech, nature, or ingredients, not geology.
- Only call something Terrain/Geology when it is clearly an inert natural block or stone-like world material.
- Nylium, mycelium, moss, crops, saplings, leaves, fungus, and similar living or spreadable organic blocks usually fit Nature, not Masonry.
- Masonry is for construction materials or building shapes, not for biome-spreading or living ground blocks.
- Geology is for inert world material, not furnishings, machines, organic growth blocks, or decorative containers.
- Treat category mismatches as lower confidence when the metadata is sparse.
- Use facets, blocksMaterial, variantGroup, requiredTool, and creativeTabLabel as stronger evidence than the display name alone.
- Do not flag hidden/dev/subtype rows unless the categorization itself still looks clearly wrong.

Return a concise list of only clear anomalies and a very brief reason why they seem wrong, including the better-fitting AMI category when obvious.
If everything looks generally correct, just say "No obvious anomalies found."
Do not rewrite the list. Only point out the mistakes.
"""
MAX_ITEMS_PER_REQUEST = 80
FIELD_CHAR_LIMITS = {
    'AMI_Subcategory': 40,
    'Facets': 140,
    'Tags': 180,
    'BlocksMaterial': 24,
    'VariantGroup': 24,
    'RequiredTool': 40,
    'CreativeTabLabel': 48,
    'Visibility': 16,
    'AccessLevel': 16,
    'Obtainability': 24,
    'SubtypeOf': 80,
}

SUMMARY_FIELDS = [
    ('AMI_Subcategory', 'sub'),
    ('Facets', 'facets'),
    ('Tags', 'tags'),
    ('BlocksMaterial', 'material'),
    ('VariantGroup', 'shape'),
    ('RequiredTool', 'tool'),
    ('CreativeTabLabel', 'tab'),
    ('Visibility', 'visibility'),
    ('AccessLevel', 'access'),
    ('Obtainability', 'obtain'),
    ('SubtypeOf', 'subtype_of'),
]

def query_local_llm(mod_id, items_text):
    prompt = f"Mod ID: {mod_id}\nHere are the items and their current categories:\n\n{items_text}\n\nList any anomalies."
    
    data = {
        "model": MODEL_ID,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": prompt}
        ],
        "temperature": 0.3, # Low temp for analytical task
        "max_tokens": 1500
    }
    
    req = urllib.request.Request(API_URL, data=json.dumps(data).encode('utf-8'), headers={'Content-Type': 'application/json'})
    
    try:
        with urllib.request.urlopen(req) as response:
            result = json.loads(response.read().decode('utf-8'))
            return result['choices'][0]['message']['content'].strip()
    except urllib.error.HTTPError as e:
        raise RuntimeError(f"HTTP {e.code}")
    except urllib.error.URLError as e:
        print(f"\n[!] Failed to connect to LM Studio at {API_URL}.")
        print(f"Error: {e.reason}")
        print("Make sure LM Studio is running and the local server is started on port 1234.")
        sys.exit(1)

def chunk_list(items, chunk_size):
    for i in range(0, len(items), chunk_size):
        yield items[i:i + chunk_size]

def row_value(row, key):
    return (row.get(key) or '').strip()

def compact_value(key, value):
    limit = FIELD_CHAR_LIMITS.get(key)
    if not limit or len(value) <= limit:
        return value
    if key in ('Facets', 'Tags'):
        parts = [part.strip() for part in value.split(',') if part.strip()]
        kept = []
        total = 0
        for part in parts:
            projected = total + (2 if kept else 0) + len(part)
            if projected > max(0, limit - 12):
                break
            kept.append(part)
            total = projected
        remainder = len(parts) - len(kept)
        if kept:
            suffix = f", +{remainder} more" if remainder > 0 else ""
            return ", ".join(kept) + suffix
    return value[: max(0, limit - 3)] + "..."

def format_item_summary(row):
    base = f"- {row_value(row, 'DisplayName')} ({row_value(row, 'ItemID')}) -> {row_value(row, 'AMI_Category') or 'uncategorized'}"
    details = []
    for csv_key, label in SUMMARY_FIELDS:
        value = compact_value(csv_key, row_value(row, csv_key))
        if value:
            details.append(f"{label}={value}")
    if details:
        return f"{base} [{'; '.join(details)}]"
    return base

def analyze_items_with_retry(mod_id, items, chunk_size):
    responses = []
    for chunk in chunk_list(items, chunk_size):
        items_text = "\n".join(chunk)
        try:
            response = query_local_llm(mod_id, items_text)
            if "No obvious anomalies found" not in response and len(response) > 20:
                responses.append(response)
        except RuntimeError as e:
            if "HTTP 400" not in str(e) or len(chunk) <= 1:
                raise
            responses.extend(analyze_items_with_retry(mod_id, chunk, max(1, chunk_size // 2)))
    return responses

def main():
    if len(sys.argv) < 2:
        print("Usage: python local_llm_analyzer.py [--include-vanilla] [--include-hidden-dev] <path_to_ontology_dump.csv> [fallback_paths...]")
        sys.exit(1)

    include_vanilla = '--include-vanilla' in sys.argv
    include_hidden_dev = '--include-hidden-dev' in sys.argv
    args = [arg for arg in sys.argv[1:] if arg not in ('--include-vanilla', '--include-hidden-dev')]

    csv_path = None
    for arg in args:
        if os.path.exists(arg):
            csv_path = arg
            break

    if not csv_path:
        print(f"File not found. Attempted paths: {args}")
        sys.exit(1)

    print(f"Reading {csv_path}...")
    
    # Group items by ModID
    mods_data = defaultdict(list)
    
    with open(csv_path, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            mod_id = row['ModID']
            if mod_id == 'minecraft' and not include_vanilla:
                continue
            if not include_hidden_dev:
                visibility = row_value(row, 'Visibility')
                access_level = row_value(row, 'AccessLevel')
                subtype_of = row_value(row, 'SubtypeOf')
                if visibility == 'hidden' or access_level == 'dev' or subtype_of:
                    continue
            item_summary = format_item_summary(row)
            mods_data[mod_id].append(item_summary)
            
    print(f"Found {len(mods_data)} mods. Starting analysis with {MODEL_ID}...\n")
    
    output_file = "anomaly_report.md"
    if os.path.exists(output_file):
        reports_dir = os.path.join("internal", "reports")
        os.makedirs(reports_dir, exist_ok=True)
        stamp = datetime.datetime.now().strftime("%Y-%m-%d_%H%M%S")
        archived = os.path.join(reports_dir, f"anomaly_report_{stamp}.md")
        shutil.move(output_file, archived)
        print(f"Archived previous report to {archived}")

    anomalous_mods = []
    total_anomalies_guessed = 0

    with open(output_file, 'w', encoding='utf-8') as out:
        out.write("# AMI Ontology Anomaly Report\n\n")
        
        for mod_id in sorted(mods_data.keys()):
            items = mods_data[mod_id]
            
            print(f"Analyzing {mod_id} ({len(items)} items)...", end=" ")

            responses = analyze_items_with_retry(mod_id, items, MAX_ITEMS_PER_REQUEST)

            response = "\n".join(responses).strip()

            if response:
                print(f"Anomalies detected!")
                out.write(f"## {mod_id}\n")
                out.write(response + "\n\n")
                out.flush()
                
                # Simple heuristic to guess anomaly count (count lines starting with '-')
                anomaly_count = sum(1 for line in response.split('\n') if line.strip().startswith('-'))
                anomalous_mods.append((mod_id, anomaly_count))
                total_anomalies_guessed += anomaly_count
            else:
                print(f"Looks clean.")
                
    print(f"\n==========================================")
    print(f"            ANALYSIS REDUCTION            ")
    print(f"==========================================")
    print(f"Analysis complete! Detailed results saved to {output_file}")
    
    if not anomalous_mods:
        print("\nSUCCESS: The LLM found zero categorization anomalies across all evaluated mods!")
    else:
        print(f"\nWARNING: The LLM flagged roughly {total_anomalies_guessed} total potential anomalies across {len(anomalous_mods)} mods.")
        print("\nTop mods with potential miscategorizations:")
        anomalous_mods.sort(key=lambda x: x[1], reverse=True)
        for mod, count in anomalous_mods[:10]:
            print(f"  - {mod}: ~{count} issues flagged")
        print("\nPlease review 'anomaly_report.md' for the full specific item details.")

if __name__ == "__main__":
    main()
