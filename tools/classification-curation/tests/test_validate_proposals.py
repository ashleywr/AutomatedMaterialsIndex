import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import schema
import validate_proposals as vp


class ValidateTest(unittest.TestCase):
    def test_valid_item(self):
        row = {"id": "minecraft:fire_charge", "scope": "item", "decision": "pending",
               "override": {"category": "tools", "subcategory": "throwable"}, "rationale": "x"}
        self.assertEqual(vp.validate_proposal(row), [])

    def test_valid_mod_pattern(self):
        row = {"id": "botania:mana_spreader", "scope": "modPattern", "decision": "approve",
               "override": {"mod": "botania", "pathTokens": ["spreader"], "category": "tech", "subcategory": "mana"},
               "rationale": "x"}
        self.assertEqual(vp.validate_proposal(row), [])

    def test_bad_scope(self):
        self.assertTrue(vp.validate_proposal({"id": "a:b", "scope": "nope", "decision": "pending", "override": {}}))

    def test_item_requires_some_change(self):
        row = {"id": "a:b", "scope": "item", "decision": "pending", "override": {"subcategory": "x"}}
        self.assertTrue(vp.validate_proposal(row))

    def test_pattern_token_must_be_single(self):
        row = {"id": "a:b", "scope": "modPattern", "decision": "pending",
               "override": {"mod": "a", "pathTokens": ["mana_spreader"], "category": "tech"}}
        self.assertTrue(vp.validate_proposal(row))

    def test_bad_decision(self):
        row = {"id": "a:b", "scope": "item", "decision": "maybe",
               "override": {"category": "tools"}}
        self.assertTrue(vp.validate_proposal(row))

    def test_validate_file(self):
        with tempfile.TemporaryDirectory() as d:
            p = os.path.join(d, "proposals.jsonl")
            schema.write_jsonl(p, [
                {"id": "a:b", "scope": "item", "decision": "pending", "override": {"category": "tools"}, "rationale": "x"},
                {"id": "c:d", "scope": "nope", "decision": "pending", "override": {}},
            ])
            errors = vp.validate_file(p)
            self.assertEqual(len(errors), 1)
            self.assertIn("line 2", errors[0])


if __name__ == "__main__":
    unittest.main()
