import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import detect
import evidence
import schema

FIX = os.path.join(os.path.dirname(__file__), "fixtures")
NODES = os.path.join(FIX, "search_nodes.jsonl")
RECIPES = os.path.join(FIX, "recipes_runtime.jsonl")


class EvidenceTest(unittest.TestCase):
    def test_recipe_index(self):
        idx = evidence.build_recipe_index(RECIPES)
        self.assertEqual(idx["swem:english_saddle"]["inputs"], ["minecraft:leather"])
        self.assertEqual(idx["swem:english_saddle"]["output"], "swem:saddle")

    def test_node_index(self):
        info, edges, meta = evidence.build_node_index(NODES)
        self.assertEqual(info["minecraft:leather"]["category"], "ingredients")
        self.assertEqual(edges["swem:saddle"]["OUTPUT_OF"], ["swem:english_saddle"])

    def test_evidence_for_saddle(self):
        info, edges, meta = evidence.build_node_index(NODES)
        ridx = evidence.build_recipe_index(RECIPES)
        cand = detect.candidate_record(
            {"id": "swem:saddle", "type": "ITEM",
             "metadata": {"facets": "wearable", "ontologyCategory": "swem",
                          "classificationRoutePhase": "compat_fallback",
                          "creativeTabLabel": "SWEM", "creativeTabId": "swem:tab"}},
            ["mod_pseudo_category", "weak_route"])
        rec = evidence.evidence_record(cand, info, edges, ridx)
        self.assertEqual(rec["craftedFrom"], [{"id": "minecraft:leather", "name": "Leather", "category": "ingredients"}])
        self.assertEqual(rec["usedToMake"], [])
        self.assertEqual(rec["tab"], {"label": "SWEM", "id": "swem:tab"})

    def test_run_round_trip(self):
        with tempfile.TemporaryDirectory() as d:
            cands = os.path.join(d, "candidates.jsonl")
            detect.run(NODES, cands, None)
            out = os.path.join(d, "evidence_batch.jsonl")
            n = evidence.run(cands, NODES, RECIPES, out)
            rows = schema.read_jsonl(out)
            self.assertEqual(n, len(rows))
            saddle = next(r for r in rows if r["id"] == "swem:saddle")
            self.assertEqual(saddle["craftedFrom"][0]["id"], "minecraft:leather")


if __name__ == "__main__":
    unittest.main()
