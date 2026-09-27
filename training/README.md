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
