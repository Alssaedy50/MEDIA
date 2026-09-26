# MEDAI v0.1 — Anatomy Retrieval Prototype

This is the first executable MEDAI prototype.

## What it proves

MEDAI can take a medical question and search only the registered Hematology Anatomy knowledge base, returning:

- ranked concepts
- matched terms
- structured evidence
- terminology
- relationships
- source metadata
- explicit abstention when no registered record matches

The prototype intentionally **does not invent an answer** when evidence is absent.

## Current knowledge scope

The prototype is connected to the six reviewed Hematology Anatomy records:

1. Bone Marrow
2. Thymus
3. Lymph Nodes
4. Spleen
5. Lymphatic Vessels
6. Embryology of Hemopoietic and Lymphatic Systems

## Run

From the repository root:

```bash
python3 inference/retrieval.py "What is the function of the red pulp?"
```

Machine-readable evidence packet:

```bash
python3 inference/retrieval.py "What is the role of PROX1 in lymphatic development?" --json
```

Run the smoke tests:

```bash
python3 -m unittest discover -s inference -p 'test_*.py' -v
```

## Architecture

```text
Medical Question
      |
      v
Query Tokenization
      |
      v
Deterministic Retriever
      |
      v
Registered Knowledge Records
      |
      +---- Evidence
      +---- Terminology
      +---- Relations
      +---- Sources
      |
      v
Evidence Packet
      |
      +----> Future Answer Generator
      |
      +----> Future Verification Layer
```

## Important boundary

This component is a **retrieval engine**, not the final MEDAI model.

The next layers can be added without rewriting the knowledge base:

1. Answer generation
2. Claim-level verification
3. Evaluation harness
4. Arabic/English medical response policy
5. Local/offline model integration
6. Android inference runtime

This separation is intentional: medical knowledge remains independently updateable and source-traceable.
