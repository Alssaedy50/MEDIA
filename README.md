# MEDAI — Medical Education AI

**MEDAI** is an experimental medical-education AI platform designed for medical students.

The project is being built around a source-traceable medical knowledge layer, retrieval, verification, evaluation, and an eventual offline Android runtime.

> **Current milestone: MEDAI v0.1 — Hematology Anatomy retrieval prototype**

## Vision

MEDAI is not intended to be a generic medical chatbot.

Its core principle is:

```text
Trusted Medical Knowledge
        ↓
Retrieval
        ↓
Evidence
        ↓
Reasoning
        ↓
Verification
        ↓
Student-facing Answer
```

The knowledge layer is kept independent from the model so that medical content can be reviewed, corrected, expanded, and updated without retraining the entire system.

## Current milestone

The first executable prototype works on the six reviewed Hematology Anatomy records:

- Bone Marrow
- Thymus
- Lymph Nodes
- Spleen
- Lymphatic Vessels
- Embryology of Hemopoietic and Lymphatic Systems

The retrieval engine returns ranked concepts, structured evidence, terminology, relationships, and source metadata. If the registered knowledge base does not contain a sufficient match, it explicitly abstains instead of inventing medical content.

See [inference/README.md](inference/README.md) for the prototype and commands.

## Knowledge architecture

Each medical concept is stored as structured, source-traceable data.

```text
Hematology
└── Anatomy
    ├── Bone Marrow
    ├── Thymus
    ├── Lymph Nodes
    ├── Spleen
    ├── Lymphatic Vessels
    └── Embryology
```

The schema supports:

- definitions and explanations
- mechanisms
- structure and function
- causes/effects
- clinical relevance
- diagnosis and treatment
- terminology and Arabic equivalents
- concept relationships
- Yemen-specific context with evidence
- source metadata and evidence level
- review/verification status

## Development roadmap

### Phase 1 — Foundation
- [x] Knowledge schema
- [x] Hematology Anatomy knowledge snapshot
- [x] Deterministic retrieval prototype
- [x] Retrieval smoke tests
- [x] Continuous integration test workflow

### Phase 2 — Medical reasoning
- [ ] Evidence-aware answer generation
- [ ] Claim-level verification
- [ ] Explicit uncertainty and abstention policy
- [ ] Arabic/English medical response policy
- [ ] Clinical-case reasoning

### Phase 3 — Evaluation
- [ ] Curated medical question set
- [ ] Factual accuracy evaluation
- [ ] Terminology evaluation
- [ ] Retrieval evaluation
- [ ] Hallucination/abstention evaluation
- [ ] Source correctness evaluation

### Phase 4 — Model and offline runtime
- [ ] Model selection based on evaluation data
- [ ] Efficient local inference
- [ ] Android runtime
- [ ] Offline knowledge packaging
- [ ] Incremental knowledge updates

## Source policy

MEDAI uses authoritative or peer-reviewed sources where appropriate, including WHO, CDC, NIH/NCBI, PubMed/PMC, recognized guidelines, and established medical references.

The repository stores **original educational synthesis plus source metadata**, rather than copying copyrighted textbook content.

## Repository structure

```text
schemas/        Knowledge contracts
knowledge/      Source-traceable medical concepts
inference/      Retrieval and inference components
evaluation/     Evaluation datasets and tests
datasets/       Training/evaluation data
model/          Model artifacts and configuration
training/       Training pipeline
app/            Future Android application
docs/           Architecture and project documentation
releases/       Versioned knowledge snapshots
```

## Status

This is an active research/prototype project. The current Anatomy engine should be treated as a demonstrator and educational prototype, not as a clinical decision-support system.
