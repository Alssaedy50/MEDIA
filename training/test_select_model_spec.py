import json
import tempfile
import unittest
from pathlib import Path

from training.select_model_spec import select


class ModelSpecSelectionTests(unittest.TestCase):
    def test_selects_smallest_vocab_within_two_percent_of_best_compression(self):
        data = {
            "version": "test",
            "corpus_paths": ["a.jsonl"],
            "results": [
                {"requested_vocab_size": 4096, "actual_vocab_size": 4096, "unknown_token_count": 0,
                 "token_count_including_specials": 1000, "merge_count": 4092, "mean_tokens": 10,
                 "p95_tokens": 20, "max_tokens": 30, "compression_ratio_tokens_per_pretoken": 1.1,
                 "parameter_count_near_100m": 99000000,
                 "architecture_near_100m": {"d_model": 768, "n_heads": 8, "n_layers": 8, "d_ff": 2304}},
                {"requested_vocab_size": 8192, "actual_vocab_size": 8192, "unknown_token_count": 0,
                 "token_count_including_specials": 1010, "merge_count": 8188, "mean_tokens": 10,
                 "p95_tokens": 20, "max_tokens": 30, "compression_ratio_tokens_per_pretoken": 1.11,
                 "parameter_count_near_100m": 99500000,
                 "architecture_near_100m": {"d_model": 768, "n_heads": 8, "n_layers": 8, "d_ff": 2304}},
            ],
        }
        spec = select(data)
        self.assertEqual(spec["selected_tokenizer"]["actual_vocab_size"], 4096)
        self.assertEqual(spec["transformer"]["parameter_count"], 99000000)

    def test_rejects_unknown_token_candidates(self):
        data = {"results": [{
            "requested_vocab_size": 4096, "actual_vocab_size": 4096,
            "unknown_token_count": 2, "token_count_including_specials": 100,
        }]}
        with self.assertRaises(ValueError):
            select(data)


if __name__ == "__main__":
    unittest.main()
