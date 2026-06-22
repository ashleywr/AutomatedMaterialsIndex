import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import detect
import schema

FIXTURE = os.path.join(os.path.dirname(__file__), "fixtures", "search_nodes.jsonl")


class DetectTest(unittest.TestCase):
    def setUp(self):
        self.by_id = {n["id"]: n for n in schema.read_jsonl(FIXTURE)}

    def detectors(self, rid, reject=None):
        return detect.detectors_for(self.by_id[rid], reject or set())

    def test_clean_item_has_no_detectors(self):
        self.assertEqual(self.detectors("minecraft:stone"), [])
        self.assertEqual(self.detectors("botania:mana_spreader"), [])
        self.assertEqual(self.detectors("farmersdelight:cabbage"), [])

    def test_weak_route_and_tab_contradiction(self):
        self.assertEqual(self.detectors("minecraft:fire_charge"), ["tab_contradiction", "weak_route"])

    def test_empty_facets(self):
        self.assertEqual(self.detectors("minecraft:goat_horn"), ["empty_facets", "weak_route"])
        self.assertEqual(self.detectors("alexscaves:ferromental"), ["empty_facets", "weak_route"])

    def test_mod_pseudo_category(self):
        self.assertEqual(self.detectors("swem:saddle"), ["mod_pseudo_category", "weak_route"])
        self.assertEqual(self.detectors("create:cogwheel"), ["mod_pseudo_category"])

    def test_reject_ledger_suppresses(self):
        self.assertEqual(self.detectors("swem:saddle", {"swem:saddle"}), [])

    def test_run_writes_candidates(self):
        with tempfile.TemporaryDirectory() as d:
            out = os.path.join(d, "candidates.jsonl")
            count = detect.run(FIXTURE, out, None)
            rows = schema.read_jsonl(out)
            ids = {r["id"] for r in rows}
            self.assertEqual(count, len(rows))
            self.assertIn("swem:saddle", ids)
            self.assertNotIn("minecraft:stone", ids)
            row = next(r for r in rows if r["id"] == "minecraft:fire_charge")
            self.assertEqual(row["facets"], ["projectile"])
            self.assertEqual(row["mod"], "minecraft")
            self.assertEqual(row["path"], "fire_charge")


if __name__ == "__main__":
    unittest.main()
