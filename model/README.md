# MEDIA Model Foundation

This directory contains the from-scratch Transformer implementation for MEDIA.

## Frozen foundation

The committed `model/foundation_spec.json` is the current model contract:

- Decoder-only causal Transformer.
- Vocabulary: 4709.
- `d_model=896`.
- 10 layers.
- 8 attention heads.
- `d_ff=3584`.
- Context length: 256.
- Pre-norm blocks.
- GELU feed-forward layers.
- Tied token embedding / LM head.
- 101,266,816 parameters.

This is a **frozen architecture/tokenizer baseline**, not pretrained weights.

## Validation

Run:

```bash
python -m unittest model/test_transformer.py -v
python -m unittest model/test_config.py model/test_size.py -v
```

The tests cover tensor shapes, finite loss, gradient flow, causal masking, checkpoint round-trip, generation, tiny-corpus overfitting, configuration validation, and parameter sizing.

## Training boundary

The current Hematology corpus is too small for genuine 100M pretraining. It is retained as a gold seed for retrieval, evaluation, tokenizer validation, and future supervised/domain adaptation.

A large, licensed/provenance-tracked medical corpus must pass the pretraining gates before full 100M training begins.

## Local runtime

`inference/model_runtime.py` consumes the frozen foundation specification and can load a compatible checkpoint when one exists. Until then, it explicitly reports `weights_unavailable` rather than pretending that a trained model is available.

## Future Android path

The application contract is already separated from the model weights. This allows the Android Alpha experience to be validated now, while the 100M checkpoint is trained later and plugged into the same runtime boundary.
