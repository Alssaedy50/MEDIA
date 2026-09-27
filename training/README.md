# MEDIA Training Pipeline — Hematology v1

The first training-data stage for MEDIA is deliberately knowledge-derived, not LLM-invented.

Registered Hematology records are transformed into supervised examples while retaining the source record_id. Every example therefore has traceable medical provenance.

Example families:
- definition
- explanation
- mechanism
- structure
- function
- clinical relevance
- diagnosis
- treatment
- high-yield facts
- medical terminology
- Arabic terminology
- pronunciation
- explicit abstention cases

Split policy:
- 80% train
- 10% validation
- 10% test
- record-level SHA-256 bucket, not example-level random splitting

Generate from repository root:
    python training/build_dataset.py

Outputs:
    datasets/hematology/train.jsonl
    datasets/hematology/validation.jsonl
    datasets/hematology/test.jsonl
    datasets/hematology/manifest.json

The generated JSONL is a canonical intermediate format. A later tokenizer stage will convert it into model-ready token sequences.

Important limitation: 51 medical records form the first corpus, not yet enough to justify a final 100M-parameter training run. We will measure token count and diversity first, then run a tiny-model overfit test before an expensive 100M run.


## Training Pipeline

Frozen Foundation + Train/Validation JSONL + Frozen Tokenizer -> training/train_lm.py -> AdamW + cosine LR schedule -> optional CUDA AMP + gradient clipping -> validation loss/perplexity -> last checkpoint + best checkpoint.

The trainer validates the tokenizer/model vocabulary match before training and supports --resume from a MEDIA training checkpoint.

A training checkpoint contains model state, optimizer state, scheduler state when present, training position, best validation loss, architecture metadata, and AMP scaler state when enabled. This follows the standard PyTorch pattern for resumable training rather than treating an inference export as a training checkpoint. See the PyTorch saving/loading and AMP documentation.

The current hematology corpus is a pipeline-validation corpus, not a claim of sufficient data for high-quality 100M-parameter pretraining. Large-corpus expansion remains a prerequisite for serious pretraining.
