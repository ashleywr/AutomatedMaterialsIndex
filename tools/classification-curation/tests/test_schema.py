import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import schema


class SchemaTest(unittest.TestCase):
    def test_jsonl_round_trip(self):
        rows = [{"id": "a:b", "n": 1}, {"id": "c:d", "n": 2}]
        with tempfile.TemporaryDirectory() as d:
            p = os.path.join(d, "x.jsonl")
            schema.write_jsonl(p, rows)
            self.assertEqual(schema.read_jsonl(p), rows)

    def test_read_jsonl_skips_blank_lines(self):
        with tempfile.TemporaryDirectory() as d:
            p = os.path.join(d, "x.jsonl")
            with open(p, "w", encoding="utf-8") as f:
                f.write('{"id":"a:b"}\n\n{"id":"c:d"}\n')
            self.assertEqual(schema.read_jsonl(p), [{"id": "a:b"}, {"id": "c:d"}])

    def test_split_id(self):
        self.assertEqual(schema.split_id("botania:mana_spreader"), ("botania", "mana_spreader"))
        self.assertEqual(schema.split_id("bareword"), ("minecraft", "bareword"))

    def test_csv_list_handles_empty_forms(self):
        self.assertEqual(schema.csv_list("placeable,stone_block"), ["placeable", "stone_block"])
        self.assertEqual(schema.csv_list(""), [])
        self.assertEqual(schema.csv_list(None), [])
        self.assertEqual(schema.csv_list("None"), [])

    def test_constants(self):
        self.assertIn("evidence_fallback", schema.WEAK_PHASES)
        self.assertEqual(schema.TAB_EXPECTED_CATEGORIES["Combat"], {"armor", "tools"})


if __name__ == "__main__":
    unittest.main()
