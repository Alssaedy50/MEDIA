import json
import tempfile
import unittest
from pathlib import Path

from training.analyze_corpus import analyze, baseline_tokens

class CorpusAnalysisTests(unittest.TestCase):
    def test_baseline_tokenizer_handles_arabic_english_and_punctuation(self):
        tokens = baseline_tokens("HbA1c تحليل الدم.")
        self.assertIn("HbA1c", tokens)
        self.assertIn("تحليل", tokens)
        self.assertIn(".", tokens)

    def test_analysis_reports_splits_tasks_languages_and_leakage(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            rows = {
                "train": [
                    {"id":"r1:a:0","record_id":"r1","split":"train","task":"definition","language":"en","input":"What is anemia?","target":"A condition.","provenance":{"type":"knowledge_derived","source_title":"WHO"}},
                ],
                "validation": [
                    {"id":"r2:a:1","record_id":"r2","split":"validation","task":"terminology_arabic","language":"ar","input":"ما معنى anemia؟","target":"فقر الدم.","provenance":{"type":"knowledge_derived","source_title":"WHO"}},
                ],
                "test": [
                    {"id":"abstain:x:0","record_id":None,"split":"test","task":"abstention","language":"en","input":"What is Xylomab?","target":"Insufficient evidence.","provenance":{"type":"abstention_test"}},
                ],
            }
            for split, data in rows.items():
                (root / f"{split}.jsonl").write_text(
                    "".join(json.dumps(x, ensure_ascii=False)+"\n" for x in data),
                    encoding="utf-8",
                )
            report = analyze(root)
            self.assertEqual(report["example_count"], 3)
            self.assertEqual(report["split_counts"]["test"], 1)
            self.assertEqual(report["task_distribution"]["abstention"], 1)
            self.assertEqual(report["abstention"]["count"], 1)
            self.assertEqual(report["cross_split_leakage"]["train_vs_validation"], 0)
            self.assertGreater(report["vocabulary"]["unique_baseline_tokens"], 0)

if __name__ == "__main__":
    unittest.main()
