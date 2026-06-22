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

    def test_evidence_record_carries_tags_and_runner_up(self):
        info, edges, _ = evidence.build_node_index(NODES)
        ridx = evidence.build_recipe_index(RECIPES)
        cand = detect.candidate_record(
            {"id": "swem:saddle", "type": "ITEM",
             "metadata": {"facets": "wearable", "ontologyCategory": "swem",
                          "classificationRoutePhase": "compat_fallback",
                          "creativeTabLabel": "SWEM", "creativeTabId": "swem:tab"}},
            ["mod_pseudo_category", "weak_route"])
        cand["tagsCsv"] = "minecraft:wearable,forge:saddles"
        cand["runnerUp"] = "armor:horse; tools:misc"
        rec = evidence.evidence_record(cand, info, edges, ridx)
        self.assertEqual(rec["tags"], ["minecraft:wearable", "forge:saddles"])
        self.assertEqual(rec["runnerUp"], "armor:horse; tools:misc")

    def test_record_without_meta_defaults_tags_and_runner_up_empty(self):
        info, edges, _ = evidence.build_node_index(NODES)
        ridx = evidence.build_recipe_index(RECIPES)
        cand = detect.candidate_record(
            {"id": "swem:saddle", "type": "ITEM",
             "metadata": {"facets": "wearable", "ontologyCategory": "swem",
                          "classificationRoutePhase": "compat_fallback"}},
            ["weak_route"])
        rec = evidence.evidence_record(cand, info, edges, ridx)
        self.assertEqual(rec["tags"], [])
        self.assertEqual(rec["runnerUp"], "")

    def test_used_to_make_missing_neighbor_resolves_to_blank(self):
        info, edges, _ = evidence.build_node_index(NODES)
        ridx = evidence.build_recipe_index(RECIPES)
        cand = detect.candidate_record(
            {"id": "farmersdelight:cabbage", "type": "ITEM",
             "metadata": {"facets": "edible", "ontologyCategory": "nature",
                          "ontologySubcategory": "crop",
                          "classificationRoutePhase": "hard_identity",
                          "creativeTabLabel": "Food & Drinks",
                          "creativeTabId": "minecraft:food_and_drinks"}},
            [])
        rec = evidence.evidence_record(cand, info, edges, ridx)
        # cabbage_rolls is the recipe output but not an ITEM node -> blank resolve
        self.assertEqual(
            rec["usedToMake"],
            [{"id": "farmersdelight:cabbage_rolls", "name": "", "category": ""}])

    def test_run_augments_tags_and_runner_up_from_metadata(self):
        with tempfile.TemporaryDirectory() as d:
            nodes = os.path.join(d, "nodes.jsonl")
            recipes = os.path.join(d, "recipes.jsonl")
            cands = os.path.join(d, "candidates.jsonl")
            out = os.path.join(d, "evidence_batch.jsonl")
            schema.write_jsonl(nodes, [
                {"id": "alexscaves:ferromental", "type": "ITEM",
                 "displayName": "Ferromental",
                 "metadata": {"facets": "", "ontologyCategory": "",
                              "classificationRoutePhase": "unknown",
                              "tags": "c:ingots,c:metals",
                              "classificationCandidates": "tech:misc,combat:tools"},
                 "unresolvedEdges": {}},
            ])
            schema.write_jsonl(recipes, [])
            detect.run(nodes, cands, None)
            evidence.run(cands, nodes, recipes, out)
            row = next(r for r in schema.read_jsonl(out)
                       if r["id"] == "alexscaves:ferromental")
            self.assertEqual(row["tags"], ["c:ingots", "c:metals"])
            self.assertEqual(row["runnerUp"], "tech:misc; combat:tools")


if __name__ == "__main__":
    unittest.main()
