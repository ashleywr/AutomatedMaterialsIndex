#!/usr/bin/env python3
"""
Fetch Minecraft blocks and items semantic taxonomy from the official Minecraft Wiki.
Uses the MediaWiki Cargo API to extract structured category/subcategory data.
"""

import requests
import json
import csv
from typing import List, Dict, Any
from pathlib import Path

# API endpoint and custom user agent
WIKI_API_URL = "https://minecraft.wiki/api.php"
USER_AGENT = "AMI-SemanticIndexer/1.0 (Automated Materials Index - offline semantic tree project)"

def fetch_cargo_data(table: str, fields: str, limit: int = 500) -> List[Dict[str, Any]]:
    """
    Fetch data from a MediaWiki Cargo table using pagination.

    Args:
        table: Name of the cargo table (e.g., 'Blocks', 'Items')
        fields: Comma-separated field names
        limit: Rows per request (max 500)

    Returns:
        List of all rows across all pages
    """
    all_rows = []
    offset = 0

    while True:
        params = {
            'action': 'cargoquery',
            'format': 'json',
            'tables': table,
            'fields': fields,
            'limit': limit,
            'offset': offset,
        }

        headers = {'User-Agent': USER_AGENT}

        print(f"Fetching {table} (offset={offset})...", end=" ", flush=True)
        response = requests.get(WIKI_API_URL, params=params, headers=headers, timeout=30)
        response.raise_for_status()

        data = response.json()
        rows = data.get('cargoquery', [])

        if not rows:
            print("done")
            break

        print(f"got {len(rows)} rows")

        # Flatten the cargo response: cargoquery returns [{"title": {"field1": val1, ...}}, ...]
        for row in rows:
            if 'title' in row:
                all_rows.append(row['title'])

        offset += limit

    return all_rows

def save_to_csv(data: List[Dict[str, Any]], filename: str, fieldnames: List[str]) -> None:
    """Save data to CSV file."""
    with open(filename, 'w', newline='', encoding='utf-8') as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(data)
    print(f"[OK] Saved to {filename} ({len(data)} rows)")

def main():
    output_dir = Path(__file__).parent.parent / "wiki_taxonomy"
    output_dir.mkdir(exist_ok=True)

    print("=" * 70)
    print("Minecraft Wiki Semantic Taxonomy Extractor")
    print("=" * 70)
    print()

    # Fetch blocks
    print("Fetching BLOCKS...")
    blocks_fields = "Name,Category,Subcategory,Type"
    blocks = fetch_cargo_data("Blocks", blocks_fields)
    blocks_file = output_dir / "minecraft_blocks_semantic.csv"
    save_to_csv(blocks, str(blocks_file), blocks_fields.split(','))

    print()

    # Fetch items
    print("Fetching ITEMS...")
    items_fields = "Name,Category,Subcategory,Type"
    items = fetch_cargo_data("Items", items_fields)
    items_file = output_dir / "minecraft_items_semantic.csv"
    save_to_csv(items, str(items_file), items_fields.split(','))

    print()
    print("=" * 70)
    print(f"Total blocks: {len(blocks)}")
    print(f"Total items: {len(items)}")
    print(f"Output directory: {output_dir}")
    print("=" * 70)

if __name__ == "__main__":
    main()
