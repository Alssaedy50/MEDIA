import json
import tempfile
import unittest
from pathlib import Path

from training.validate_corpus import validate


class CorpusValidationTests(unittest.TestCase):
    def write(self, root, name, rows):
        p=Path(root)/name
        p.write_text("\n".join(json.dumps(r,ensure_ascii=False) for r in rows)+"\n",encoding="utf-8")
        return str(p)

    def row(self, rid, record_id=None):
        r={"id":rid,"input":"What is anemia?","target":"A reduction in red cell mass.",
           "provenance":{"type":"knowledge_derived","record_id":record_id}}
        if record_id is not None: r["record_id"]=record_id
        return r

    def test_valid_corpus(self):
        with tempfile.TemporaryDirectory() as d:
            result=validate([self.write(d,"train.jsonl",[self.row("a","r1")])])
            self.assertTrue(result["valid"])
            self.assertEqual(result["example_count"],1)

    def test_detects_cross_split_record_leakage(self):
        with tempfile.TemporaryDirectory() as d:
            a=self.write(d,"train.jsonl",[self.row("a","r1")])
            b=self.write(d,"test.jsonl",[self.row("b","r1")])
            result=validate([a,b])
            self.assertFalse(result["valid"])
            self.assertTrue(any("record leakage" in e for e in result["errors"]))

if __name__=="__main__":
    unittest.main()
