# MEDIA — Medical Education AI

**MEDIA** is an experimental medical-education AI platform designed for medical students.

The project is being built around a source-traceable medical knowledge layer, retrieval, verification, evaluation, and an eventual offline Android runtime.

> **Current milestone: MEDIA — Hematology training-data foundation**

## Vision

MEDIA is not intended to be a generic medical chatbot.

Its core principle is:

```
Trusted Medical Knowledge
        ↓
Training Data + Retrieval
        ↓
Model
        ↓
Evidence / Reasoning
        ↓
Verification
        ↓
Student-facing Answer
```

The knowledge layer remains independent from the model so medical content can be reviewed, corrected, expanded, and updated without requiring every knowledge change to be embedded into model weights.

## Current milestone

The Hematology block is the first complete academic block in the repository.

It currently contains **51 reviewed/verified knowledge records** covering:

- Anatomy
- Histology
- Physiology
- Pathology
- Microbiology
- Pharmacology
- Medicine
- Pediatrics
- Community Medicine

The next stage is no longer adding another block. MEDIA now has a Web/API prototype over the existing evidence pipeline while model-training engineering continues.

The first training corpus is generated reproducibly from these registered records. Examples retain their originating record ID and use a record-level train/validation/test split to reduce semantic leakage.

## Training roadmap

### Stage 1 — Knowledge-to-dataset
- [x] Training dataset generator
- [x] Provenance-preserving examples
- [x] Record-level 80/10/10 split
- [x] Abstention test examples
- [x] Training pipeline unit tests
- [x] CI generation and corpus-count verification

### Stage 2 — Tokenizer and corpus analysis
- [x] Medical English + Arabic tokenizer
- [ ] Vocabulary analysis
- [ ] Exact token counts
- [ ] Sequence-length analysis
- [ ] Dataset quality audit

### Stage 3 — Model foundation
- [ ] Tiny Transformer overfit test
- [ ] ~100M-parameter architecture
- [ ] From-scratch initialization
- [ ] Checkpointing and resume
- [ ] GPU training configuration

### Stage 4 — Medical AI
- [ ] Retrieval integration
- [ ] Evidence-grounded generation
- [ ] Claim verification
- [ ] Clinical reasoning evaluation
- [ ] Hallucination and abstention evaluation

### Stage 5 — Offline Android
- [ ] Quantized model
- [ ] Local inference
- [ ] Offline knowledge package
- [ ] Android application
- [ ] Performance/RAM testing

## Source policy

MEDIA uses authoritative or peer-reviewed sources where appropriate, including WHO, CDC, NIH/NCBI, PubMed/PMC, recognized guidelines, and established medical references.

The repository stores original educational synthesis plus source metadata rather than copying copyrighted textbook content.

## Repository structure

```
schemas/        Knowledge contracts
knowledge/      Source-traceable medical concepts
inference/      Retrieval and inference components
evaluation/     Evaluation datasets and tests
datasets/       Training/evaluation data
model/          Model artifacts and configuration
training/       Training pipeline
app/            Web/API prototype and future Android application
docs/           Architecture and project documentation
releases/       Versioned knowledge snapshots
```

## Status

This is an active research/prototype project. MEDIA is an educational system under development and is not a clinical decision-support system.
