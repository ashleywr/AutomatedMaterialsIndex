import json
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import apply
import schema


class ApplyTest(unittest.TestCase):
    def test_merge_item_keeps_only_present_fields(self):
        ov = {"items": {}, "modPatterns": []}
        updated, rejects = apply.merge(ov, [
            {"id": "minecraft:fire_charge", "scope": "item", "decision": "approve",
             "override": {"category": "tools", "subcategory": "throwable", "addFacets": [], "removeFacets": []},
             "rationale": "x"},
        ])
        self.assertEqual(updated["items"]["minecraft:fire_charge"],
                         {"category": "tools", "subcategory": "throwable"})
        self.assertEqual(rejects, [])

    def test_merge_mod_pattern_dedups(self):
        ov = {"items": {}, "modPatterns": []}
        prop = {"id": "botania:mana_spreader", "scope": "modPattern", "decision": "approve",
                "override": {"mod": "botania", "pathTokens": ["spreader"], "category": "tech", "subcategory": "mana"},
                "rationale": "x"}
        updated, _ = apply.merge(ov, [prop, dict(prop)])
        self.assertEqual(len(updated["modPatterns"]), 1)

    def test_reject_recorded(self):
        ov = {"items": {}, "modPatterns": []}
        updated, rejects = apply.merge(ov, [
            {"id": "a:b", "scope": "item", "decision": "reject", "override": {"category": "tools"}, "rationale": "x"},
        ])
        self.assertEqual(updated["items"], {})
        self.assertEqual(rejects, [{"id": "a:b", "scope": "item", "key": "a:b"}])

    def test_merge_rejects_invalid_approved(self):
        ov = {"items": {}, "modPatterns": []}
        with self.assertRaises(ValueError):
            apply.merge(ov, [{"id": "a:b", "scope": "item", "decision": "approve",
                              "override": {"subcategory": "x"}, "rationale": "x"}])

    def test_run_writes_files(self):
        with tempfile.TemporaryDirectory() as d:
            proposals = os.path.join(d, "proposals.jsonl")
            overrides = os.path.join(d, "classification_overrides.json")
            reject = os.path.join(d, "reject_ledger.jsonl")
            schema.write_jsonl(proposals, [
                {"id": "minecraft:fire_charge", "scope": "item", "decision": "approve",
                 "override": {"category": "tools"}, "rationale": "x"},
                {"id": "a:b", "scope": "item", "decision": "reject", "override": {"category": "tools"}, "rationale": "x"},
            ])
            approved, rejected = apply.run(proposals, overrides, reject)
            self.assertEqual((approved, rejected), (1, 1))
            with open(overrides, encoding="utf-8") as f:
                data = json.load(f)
            self.assertEqual(data["items"]["minecraft:fire_charge"], {"category": "tools"})
            self.assertEqual(schema.read_jsonl(reject), [{"id": "a:b", "scope": "item", "key": "a:b"}])


if __name__ == "__main__":
    unittest.main()
