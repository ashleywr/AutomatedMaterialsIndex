#!/usr/bin/env python3
"""Debug script to explore available Cargo tables on minecraft.wiki"""

import requests
import json

WIKI_API_URL = "https://minecraft.wiki/api.php"
USER_AGENT = "AMI-SemanticIndexer/1.0"

def test_query(table: str, fields: str, limit: int = 5) -> None:
    """Test a cargo query and print results."""
    params = {
        'action': 'cargoquery',
        'format': 'json',
        'tables': table,
        'fields': fields,
        'limit': limit,
    }
    headers = {'User-Agent': USER_AGENT}

    print(f"\nTesting: table='{table}', fields='{fields}'")
    print("-" * 60)

    try:
        response = requests.get(WIKI_API_URL, params=params, headers=headers, timeout=10)
        data = response.json()

        rows = data.get('cargoquery', [])
        print(f"Result: {len(rows)} rows")

        if rows:
            for i, row in enumerate(rows[:3], 1):
                print(f"  Row {i}: {row}")

        # Print any query warnings
        if 'query-continue-offset' in data:
            print(f"  Note: More rows available (offset: {data['query-continue-offset']})")
    except Exception as e:
        print(f"Error: {e}")

# Try different table and field combinations
test_query("Items", "Name")
test_query("Items", "_pageName,Name")
test_query("Items", "*")  # All fields
test_query("Blocks", "*")
test_query("Item", "Name")
test_query("Block", "Name")
