# Hugging Face Dataset Publishing

This document describes how MEDIA aggregates open medical text and publishes it to
the Hugging Face Hub as the `media-100m-foundation-v1` corpus.

## Credential handling

The publisher reads the token from the `HF_TOKEN` environment variable **only**.
It is never written, echoed, or logged. Set it one of two ways:

- **Platform secret:** register a secret named `HF_TOKEN` (the run injects it into
  the environment automatically).
- **Local `.env`:** create `.env` with `HF_TOKEN=hf_...`. `.env` is git-ignored.

Never commit a token. A token pasted into chat must be treated as compromised and
rotated at https://huggingface.co/settings/tokens. The token needs **write** scope
for the target dataset repo.

## Target

- Repo: `DOCTOSHI/media-medical-corpus` (public dataset)
- Version: `media-100m-foundation-v1`
- Scope: anatomy, physiology, biochemistry, pathology, pharmacology, microbiology,
  hematology
- Uploaded schema: `{"text", "source"}` where `source` is the originating HF dataset id
- Language: English only for this version (Arabic is a planned future layer)

## Sources

Configured in the `SOURCES` mapping at the top of `scripts/build_medical_corpus.py`:

| key | dataset | config |
| --- | --- | --- |
| `medqa_usmle` | `GBaker/MedQA-USMLE-4-options` | - |
| `pubmedqa_labeled` | `qiaojin/PubMedQA` | `pqa_labeled` |
| `pubmedqa_artificial` | `qiaojin/PubMedQA` | `pqa_artificial` |

WikiProject Medicine is omitted: no unambiguous, licence-clear Hub extract was
found, so it is recorded as omitted in the dataset card rather than guessed.

## Normalization

HTML entity decode, mojibake repair, Unicode NFKC, HTML tag stripping,
citation-bracket removal (`[1]`, `[12]`), control-character removal, whitespace and
line cleanup, minimum length filter (>= 100 chars), and exact duplicate removal.
Rows are capped deterministically by content hash, so re-runs are reproducible.

## Commands

Install dependencies:

```bash
pip install -r training/requirements.txt
```

Build locally without publishing (safe, no token required):

```bash
python scripts/build_medical_corpus.py --skip-upload --max-rows 200000
```

Build and publish (requires `HF_TOKEN`):

```bash
export HF_TOKEN=...   # or rely on the injected platform secret
python scripts/build_medical_corpus.py --max-rows 200000
```

Outputs under `datasets/medical_corpus/` (git-ignored): `train.jsonl.gz`,
`manifest.json`, `README.md` (dataset card).

If `HF_TOKEN` is missing or invalid, the script still writes the local corpus and
prints an actionable message; it exits 0 because ingestion succeeded.

## Verification

```python
from datasets import load_dataset
ds = load_dataset("DOCTOSHI/media-medical-corpus", split="train", streaming=True)
```

The run also streams the published dataset and reports total samples, estimated
tokens, and the public URL.

## Licensing

Each upstream dataset keeps its own license and attribution terms; these are
recorded in the generated dataset card. Only subsets whose license permits
redistribution may be published. Raw third-party text is stored on the Hub with
license metadata and is **not** committed to git.
