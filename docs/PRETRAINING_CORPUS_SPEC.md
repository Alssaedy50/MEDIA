# MEDIA — Pretraining Corpus Expansion Specification

## Objective

Expand beyond the 51-record Hematology seed into a large, provenance-preserving medical corpus suitable for causal-language-model pretraining.

The corpus must remain medically grounded. Quantity is not a substitute for source quality.

## Source layers

### Layer A — Registered medical knowledge
The reviewed/verified MEDIA knowledge records remain the gold layer for retrieval, evaluation, terminology and abstention.

### Layer B — High-quality public medical text
Candidate sources may include authoritative public health guidance, biomedical reference material, open-access scientific literature, and other sources whose licensing permits the intended use.

Every imported item must retain:
- source organization or publisher;
- title;
- canonical source identifier or URL when permitted;
- publication/update date when available;
- license/provenance information;
- retrieval/import timestamp;
- domain and topic labels;
- processing version.

### Layer C — Student-oriented instructional material
Lecture-derived or educational material can be added only when its provenance and usage rights are clear. It should be tagged separately from authoritative medical evidence.

## Required normalization

Before training:
1. UTF-8 normalization.
2. Unicode normalization.
3. Whitespace and line cleanup.
4. Boilerplate/navigation removal.
5. Duplicate and near-duplicate detection.
6. Language detection.
7. Medical terminology preservation.
8. Source metadata preservation.
9. Record-level train/validation/test assignment.
10. Leakage validation.

## Split policy

The split unit should be a source document or source record, never an individual sentence.

A document and all derived chunks must remain in one split.

The validator must reject any record identifier appearing across multiple splits.

## Quality gates

A corpus release is not training-ready until:
- every item has provenance;
- no duplicate IDs exist;
- no duplicate input/target pairs exist;
- no source record crosses splits;
- empty or malformed examples are rejected;
- source metadata is retained;
- language distribution is measured;
- Arabic and English coverage is reported;
- medical terminology coverage is reported;
- sequence-length statistics are generated;
- tokenizer benchmark is rerun after major corpus expansion.

## Training readiness

The first 100M pretraining run should use a substantially larger corpus than the current 1,186-example seed. The Hematology seed remains part of the evaluation and grounding suite even after the pretraining corpus grows.

The final tokenizer must be trained only from the intended training split. Validation and test sets may be used for tokenizer auditing and benchmark analysis, but not for the final tokenizer artifact used by the training run.

## Release discipline

Every corpus release receives:
- a manifest;
- source/provenance metadata;
- deterministic split information;
- validation report;
- tokenizer benchmark;
- corpus statistics;
- a version identifier.

A new corpus release invalidates the previous tokenizer/model foundation decision if it materially changes token statistics.
