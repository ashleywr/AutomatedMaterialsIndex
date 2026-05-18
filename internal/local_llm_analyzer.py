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
I will provide you with a list of items from a specific Minecraft mod, along with their assigned AMI categories and tags.
Your job is to identify any anomalies or likely miscategorizations based on the item's name and its assigned category.

Examples of anomalies:
- A food item (e.g., "Cheese") placed in the 'Tech' category.
- A sword placed in the 'Materials' category instead of 'Combat'.
- An ingot placed in 'Nature' instead of 'Materials'.
- A decorative or display block placed in 'Geology' or 'Masonry'.
- A sign, jar, trophy, head, skull, candle, carpet, or placeable food placed in 'Terrain' or 'Geology'.
- A block that clearly opens a menu or behaves like a workstation placed in 'Building' instead of 'Tech'.

Important review rules:
- Do not treat "placeable" or "no_recipe" as evidence for geology/terrain by itself.
- If the item is a jar, sign, display container, food block, decorative block, interactive block, or any obvious furnishing object, it usually belongs in Decoration, Tech, Nature, or Food, not Geology.
- Only call something Terrain/Geology when it is clearly an inert natural block or stone-like world material.

Return a concise list of just the anomalous items and a very brief reason why they seem wrong, including the better-fitting category family when obvious.
If everything looks generally correct, just say "No obvious anomalies found."
Do not rewrite the list. Only point out the mistakes.
"""
MAX_ITEMS_PER_REQUEST = 80

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

def analyze_items_with_retry(mod_id, items, chunk_size):
    responses = []
    for chunk in chunk_list(items, chunk_size):
        items_text = "\n".join(chunk)
        try:
            response = query_local_llm(mod_id, items_text)
            if "No obvious anomalies found" not in response and len(response) > 20:
                responses.append(response)
        except RuntimeError as e:
            if "HTTP 400" not in str(e) or len(chunk) <= 20:
                raise
            responses.extend(analyze_items_with_retry(mod_id, chunk, max(20, chunk_size // 2)))
    return responses

def main():
    if len(sys.argv) < 2:
        print("Usage: python local_llm_analyzer.py [--include-vanilla] <path_to_ontology_dump.csv> [fallback_paths...]")
        sys.exit(1)

    include_vanilla = '--include-vanilla' in sys.argv
    args = [arg for arg in sys.argv[1:] if arg != '--include-vanilla']

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
            # Limit the fields so we don't waste tokens
            item_summary = f"- {row['DisplayName']} ({row['ItemID']}) -> {row['AMI_Category']}"
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
            
            if mod_id == 'minecraft':
                continue
                
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
