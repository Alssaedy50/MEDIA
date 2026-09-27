# MEDIA — 100M Model Foundation

## Purpose

This document defines the reproducible gate between tokenizer benchmarking and neural-model training.

The 100M model is **not trained from the current Hematology corpus**. The current corpus is a verified seed/evaluation domain. A substantially larger medical corpus is required before genuine pretraining.

## Foundation pipeline

`Registered Medical Knowledge → Dataset Generation → Tokenizer Benchmark → Foundation Selection → Large-Corpus Pretraining → Evaluation → Retrieval + Verification → Offline Android`

## Tokenizer selection gate

Candidate vocabularies are benchmarked on the generated corpus. A candidate is valid only when its unknown-token count is zero.

The deterministic selector in `training/select_model_spec.py`:

1. Finds the minimum total token count among valid candidates.
2. Keeps candidates within 2% of that minimum.
3. Chooses the smallest actual vocabulary inside that efficiency band.
4. Takes the nearest-100M Transformer architecture calculated for that vocabulary.
5. Records every candidate and the selection rule in `model/foundation_spec.json`.

The selector deliberately does not claim that the current small corpus predicts final pretraining quality. Its purpose is to make the engineering decision reproducible.

## Model constraints

- Decoder-only causal Transformer
- Maximum sequence length: 256 for the initial foundation
- Tied token input/output embeddings
- Target parameter count: approximately 100M
- Multi-head causal self-attention
- Pre-norm Transformer blocks
- GELU MLP
- Deterministic parameter-count calculation in `model/size.py`

## Pretraining gate

Do **not** start 100M pretraining until all of these are true:

- tokenizer candidate benchmark completed;
- foundation specification generated and reviewed;
- tokenizer final artifact is trained from the intended training corpus only;
- large corpus has provenance and source metadata;
- train/validation/test leakage checks pass;
- sequence-length distribution is measured;
- Arabic/English/medical terminology coverage is measured;
- model forward/backward/checkpoint tests pass;
- baseline retrieval and safe-abstention evaluation remains green.

## Current corpus role

The 51 reviewed/verified Hematology knowledge records and their generated examples are retained as the **gold domain seed**. They support:

- retrieval evaluation;
- terminology evaluation;
- answer grounding;
- safe-abstention tests;
- tokenizer and model pipeline validation;
- later supervised/domain adaptation.

They are not sufficient as a 100M pretraining corpus.

## Reproducibility

The benchmark JSON records the candidate tokenizer metrics and the corresponding nearest-100M architecture. The generated foundation specification records the exact decision inputs. Any future corpus expansion must trigger a new tokenizer benchmark before freezing the final production tokenizer.
