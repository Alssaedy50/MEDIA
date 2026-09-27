# MEDIA Tokenizer Foundation

The v0.2 tokenizer is a deterministic, dependency-free BPE implementation for medical English + Arabic text.

Goals:
- Preserve Unicode medical terminology.
- Use NFKC normalization and deterministic pre-tokenization.
- Learn merges from the canonical knowledge-derived corpus.
- Keep special tokens <pad>, <unk>, <bos>, and <eos> stable.
- Use character fallback so uncommon medical terms remain representable.
- Save a portable JSON artifact for the tiny Transformer and later offline runtime.

This is a foundation, not a claim that 4096 vocabulary is optimal. Vocabulary-size and sequence-length experiments will be used before freezing a production tokenizer.

Generate:
    python training/build_dataset.py
    python training/tokenizer.py datasets/hematology/train.jsonl datasets/hematology/validation.jsonl datasets/hematology/test.jsonl --vocab-size 4096 --output datasets/hematology/tokenizer.json

The tokenizer artifact is generated during CI and is not source-of-truth until the vocabulary experiments are reviewed.
