import json
import tempfile
import unittest
from pathlib import Path

from model.config import TransformerConfig


class TransformerConfigTests(unittest.TestCase):
    def test_loads_foundation_spec(self):
        spec = {
            "status": "candidate_pending_freeze",
            "transformer": {
                "vocab_size": 8192, "max_seq_len": 256, "d_model": 768,
                "n_heads": 8, "n_layers": 8, "d_ff": 2304
            }
        }
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/"spec.json"
            p.write_text(json.dumps(spec), encoding="utf-8")
            cfg=TransformerConfig.from_foundation_spec(p)
            self.assertEqual(cfg.vocab_size,8192)
            self.assertEqual(cfg.head_dim,96)
            self.assertEqual(cfg.d_ff,2304)

    def test_rejects_unusable_spec(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/"spec.json"
            p.write_text(json.dumps({"status":"draft","transformer":{}}),encoding="utf-8")
            with self.assertRaises(ValueError):
                TransformerConfig.from_foundation_spec(p)

if __name__=="__main__":
    unittest.main()
