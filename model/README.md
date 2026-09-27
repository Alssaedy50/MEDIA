# MEDIA Model Foundation

This directory contains the first from-scratch Transformer implementation for MEDIA.

## Design

- Decoder-only causal language model.
- Pre-norm Transformer blocks with multi-head causal self-attention.
- GELU feed-forward layers and tied token embedding / LM head.
- CPU-compatible training loop with gradient clipping and checkpoint resume.
- The default configuration is a validation-scale foundation, not the final 100M model.

## Validation

Run: python -m unittest model/test_transformer.py -v

The tests cover tensor shapes, finite loss, gradient flow, causal masking, checkpoint round-trip, generation, and tiny-corpus overfitting.

## Training

Generate the canonical dataset and tokenizer, then run:

python training/build_dataset.py
python training/tokenizer.py datasets/hematology/train.jsonl datasets/hematology/validation.jsonl datasets/hematology/test.jsonl --vocab-size 4096 --output datasets/hematology/tokenizer.json
python -m model.train datasets/hematology/train.jsonl datasets/hematology/tokenizer.json --steps 100 --checkpoint checkpoints/media-tiny.pt

A final ~100M architecture will be calculated only after tokenizer and corpus measurements are validated.
