from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

import torch

from model.config import TransformerConfig
from model.transformer import MediaTransformerLM
from training.tokenizer import BPETokenizer, train_vocab
from training.train_lm import checkpoint_payload, load_checkpoint, save_checkpoint


def make_tokenizer():
    return BPETokenizer(train_vocab(["blood hemoglobin", "red blood cell", "خلايا الدم"], vocab_size=64, min_frequency=1))


class TrainPipelineTests(unittest.TestCase):
    def test_checkpoint_round_trip_restores_training_state(self):
        tokenizer = make_tokenizer()
        config = TransformerConfig(vocab_size=tokenizer.vocab_size, max_seq_len=32, d_model=32, n_heads=4, n_layers=2, d_ff=64, dropout=0.0)
        model = MediaTransformerLM(config)
        optimizer = torch.optim.AdamW(model.parameters(), lr=1e-3)
        scheduler = torch.optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=10)
        inputs = torch.tensor([[1, 4, 5, 2]], dtype=torch.long)
        targets = torch.tensor([[4, 5, 2, 2]], dtype=torch.long)
        _, loss = model(inputs, targets)
        loss.backward()
        optimizer.step()
        scheduler.step()

        with tempfile.TemporaryDirectory() as td:
            path = Path(td) / "checkpoint.pt"
            save_checkpoint(path, checkpoint_payload(model, optimizer, scheduler, None, 3, 7, 1.25, {"seed": 42, "test": True}))
            restored = MediaTransformerLM(config)
            restored_optimizer = torch.optim.AdamW(restored.parameters(), lr=1e-3)
            restored_scheduler = torch.optim.lr_scheduler.CosineAnnealingLR(restored_optimizer, T_max=10)
            epoch, step, best, saved_config = load_checkpoint(path, restored, restored_optimizer, restored_scheduler)
            self.assertEqual((epoch, step, best), (3, 7, 1.25))
            self.assertEqual(saved_config["seed"], 42)
            for left, right in zip(model.parameters(), restored.parameters()):
                self.assertTrue(torch.equal(left, right))

    def test_checkpoint_has_explicit_media_format(self):
        tokenizer = make_tokenizer()
        config = TransformerConfig(vocab_size=tokenizer.vocab_size, max_seq_len=16, d_model=32, n_heads=4, n_layers=1, d_ff=64, dropout=0.0)
        model = MediaTransformerLM(config)
        optimizer = torch.optim.AdamW(model.parameters(), lr=1e-3)
        payload = checkpoint_payload(model, optimizer, None, None, 0, 0, float("inf"), {})
        self.assertEqual(payload["format_version"], "media-training-checkpoint-v1")
        self.assertEqual(payload["architecture"]["vocab_size"], tokenizer.vocab_size)


if __name__ == "__main__":
    unittest.main()
