# MEDIA — 100M Model Foundation

## Purpose

This document defines the reproducible gate between tokenizer benchmarking and neural-model training.

The 100M model is **not trained from the current Hematology corpus**. The current corpus is a verified seed/evaluation domain. A substantially larger medical corpus is required before genuine pretraining.

## Frozen foundation

The first foundation has now been frozen in `model/foundation_spec.json`:

- Requested tokenizer vocabulary: **8192**
- Actual tokenizer vocabulary: **4709**
- BPE merges: **4591**
- Training unknown tokens: **0**
- `d_model`: **896**
- Layers: **10**
- Attention heads: **8**
- FFN dimension: **3584**
- Context: **256**
- Weight tying: **enabled**
- Parameter count: **100,824,192**
- Status: **frozen**

The freeze is an engineering baseline, not a claim about final model quality. No pretrained weights are included.

## Foundation pipeline

`Registered Medical Knowledge → Dataset Generation → Tokenizer Benchmark → Foundation Selection → Frozen Foundation → Large-Corpus Pretraining → Evaluation → Retrieval + Verification → Offline Android`

## Tokenizer selection gate

Candidate vocabularies are benchmarked on the generated **training split only**, because the final tokenizer must be trained from training data without using validation/test text. A candidate is valid only when its training-split unknown-token count is zero. Validation/test are held out for downstream evaluation and tokenizer coverage reporting.

The deterministic selector in `training/select_model_spec.py`:

1. Finds the minimum total token count among valid candidates.
2. Keeps candidates within 2% of that minimum.
3. Chooses the smallest actual vocabulary inside that efficiency band.
4. Takes the nearest-100M Transformer architecture calculated for that vocabulary.
5. Records the decision inputs.

CI now recomputes the candidate and compares its critical fields against the committed frozen specification. A corpus change that alters the frozen foundation therefore fails CI instead of silently changing the model contract.

## Pretraining gate

Do **not** start 100M pretraining until all of these are true:

- frozen foundation remains compatible with the current corpus release;
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

## Android Alpha boundary

The application layer can now consume the frozen foundation contract without pretending that pretrained weights exist.

Current Alpha behavior:

- deterministic registered-knowledge retrieval;
- evidence-aware answer modes;
- explicit safe abstention;
- source display;
- local Transformer runtime boundary;
- explicit `weights_unavailable` state until a compatible checkpoint exists;
- installable web application shell for Android testing.

The Android shell is therefore useful for validating the **student experience and application contract** before the expensive 100M training stage.

## Reproducibility

The benchmark JSON records candidate tokenizer metrics. The committed foundation specification records the frozen decision. Any future corpus expansion must trigger a new benchmark and an explicit foundation review before changing the frozen production tokenizer/model contract.
