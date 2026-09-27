from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from training.tokenizer_benchmark import benchmark


class TokenizerBenchmarkTests(unittest.TestCase):
    def test_benchmark_reports_requested_size_and_architecture(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            rows = [
                {"input": "What is anemia?", "target": "Anemia is a reduction in red blood cell mass."},
                {"input": "ما هو فقر الدم؟", "target": "فقر الدم هو انخفاض في كتلة كريات الدم الحمراء."},
            ]
            corpus = root / "tiny.jsonl"
            corpus.write_text(
                "
".join(json.dumps(r, ensure_ascii=False) for r in rows) + "
",
                encoding="utf-8",
            )
            output = root / "benchmark.json"
            payload = benchmark([str(corpus)], [64], str(output))
            result = payload["results"][0]
            self.assertEqual(result["requested_vocab_size"], 64)
            self.assertGreater(result["actual_vocab_size"], 0)
            self.assertEqual(result["unknown_token_count"], 0)
            self.assertIn("architecture_near_100m", result)
            self.assertTrue(output.exists())


if __name__ == "__main__":
    unittest.main()
