# MEDAI Foundation

MEDAI is an educational medical AI for medical students. The first implementation domain is the official Hematology Block, with an architecture designed to expand across medicine.

## Core architecture

- Model: language understanding, reasoning patterns, and answer behavior.
- Medical Knowledge Base: structured, source-traceable medical knowledge.
- Retrieval: finds relevant knowledge and evidence.
- Verification: checks generated claims against evidence.
- Application: eventual offline Android runtime.

When evidence is insufficient, MEDAI should prefer explicit uncertainty or abstention rather than invent information.

## First domain

Hematology integrates Anatomy, Histology, Physiology, Pathology, Microbiology, Pharmacology, Biochemistry, Medicine, Pediatrics, and Community Medicine.

First content target:

**Hematology → Anatomy → Bone Marrow**

Then: Thymus → Lymph Nodes → Spleen → Lymphatic Vessels → Embryology.

## Knowledge architecture

Concepts should support definition, explanation, mechanism, structure/function, causes/effects, clinical relevance, diagnosis/treatment where applicable, high-yield points, terminology, relationships, source metadata, evidence level, review status, and Yemen-specific context when reliable evidence exists.

## Cross-disciplinary design

MEDAI should connect concepts across subjects rather than isolate them. Example: Bone Marrow → Hematopoiesis → Erythropoiesis → Globin Synthesis → Hemoglobinopathies.

## Source policy

Preferred sources include WHO/WHO EMRO, CDC, NIH/NCBI, PubMed/PMC, official clinical guidelines, recognized medical references, peer-reviewed research, and reliable Yemen-focused health publications. AI-generated statements are not automatically evidence.

## Copyright

Complete copyrighted textbooks or protected works must not be placed into the repository or training corpus without appropriate rights. MEDAI should create original educational synthesis and retain source metadata, links, licenses, and attribution where appropriate.

## Yemen context

Yemen-specific epidemiology, prevalence, endemic patterns, health-system information, treatment availability, or other local claims require traceable evidence and must not be invented.

## Progressive development

1. Architecture
2. Data schemas
3. Source registry
4. Structured medical knowledge
5. Terminology
6. Mechanisms
7. Questions and clinical cases
8. Retrieval
9. Verification
10. Model experiments
11. Evaluation
12. Offline Android integration

Large-scale model training should wait until data representation and evaluation are stable.

## Training loop

Knowledge → Training → Model → Evaluation → Errors → Improved Dataset → Training

Teacher-model outputs, if used later, are candidate data and require evidence-based evaluation before becoming training targets.

## Retrieval and verification

User Question → Intent/Topic Detection → Knowledge Retrieval → Evidence Assembly → Draft Answer → Claim Verification → Final Answer

## Evaluation

Measure factual accuracy, terminology, reasoning, retrieval, source correctness, hallucination, abstention, clinical cases, cross-disciplinary reasoning, Arabic/English medical language, Yemen-context accuracy, and offline performance.

## Offline goal

The final target is an Android application capable of operating offline with a local model, local medical knowledge, local retrieval, and local verification where feasible.

## First milestone

The first technical milestone is the foundation data schema. The first content milestone is **Hematology → Anatomy → Bone Marrow**.

## Non-goals for v0.1

MEDAI will not replace physicians, provide autonomous medical decisions, ingest entire copyrighted textbooks without rights, depend permanently on an external AI provider, or treat generated text as evidence without verification.
