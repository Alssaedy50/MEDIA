import json
import tempfile
import unittest
from pathlib import Path

from training.tokenizer import BPETokenizer, train_vocab
from training.tokenizer_audit import audit

class TokenizerAuditTests(unittest.TestCase):
    def test_audit_counts_exact_tokens_and_unknowns(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            corpus = root / "data.jsonl"
            tok_path = root / "tokenizer.json"
            rows = [
                {"input": "Hemoglobin", "target": "الهيموغلوبين"},
                {"input": "RBC", "target": "red blood cell"},
            ]
            corpus.write_text("\n".join(json.dumps(x, ensure_ascii=False) for x in rows) + "\n", encoding="utf-8")
            config = train_vocab(["Hemoglobin", "الهيموغلوبين", "RBC", "red blood cell"], vocab_size=64, min_frequency=1)
            BPETokenizer(config).save(tok_path)
            result = audit([corpus], tok_path)
            self.assertEqual(result["examples"], 2)
            self.assertEqual(result["texts"], 4)
            self.assertEqual(result["unknown_token_count"], 0)
            self.assertGreater(result["token_count_including_specials"], 0)
            self.assertIn("languages", result)

if __name__ == "__main__":
    unittest.main()
