# MEDIA Alpha — Medical Education AI

The first student-facing application layer for MEDIA wraps the evidence-aware retrieval/answer engine and exposes the frozen model-runtime boundary.

## Architecture

```
Android/Desktop Browser
        |
        v
Installable MEDIA Web App
        |
        v
FastAPI
   |          \
   v           v
Evidence      Local Transformer Runtime
Engine        (only when compatible weights exist)
   |
   v
Registered Hematology Knowledge
```

The API does not fabricate medical facts. Evidence retrieval remains independently traceable, and safe abstention is preserved.

## Current Alpha capabilities

- Hematology knowledge retrieval.
- Quick, explain, compare, exam, and sources modes.
- Registered source display.
- Explicit safe abstention for unsupported queries.
- Frozen 100M foundation metadata.
- Local model runtime boundary with explicit `weights_unavailable` state.
- Installable PWA shell for Android testing.
- Offline caching of the application shell; medical answering still requires the API/runtime until a fully local Android inference build is added.

## Run locally

From the repository root:

```bash
python -m pip install -r app/requirements.txt
uvicorn app.api.main:app --host 0.0.0.0 --port 8000
```

Open the displayed host address from an Android browser on the same network. The browser can offer **Install** when the PWA criteria are met.

Endpoints:

- `GET /health`
- `GET /api/scope`
- `GET /api/runtime`
- `POST /api/answer`
- `GET /docs`

## Model boundary

`model/foundation_spec.json` is frozen at 100,824,192 parameters. It is an architecture/tokenizer contract only. It is an architecture/tokenizer contract only.

No pretrained checkpoint is shipped yet. When a compatible checkpoint becomes available, `inference/model_runtime.py` can load it and expose local generation without changing the application contract.

## Safety

MEDIA is an educational research prototype and is not a clinical decision-support system. When the registered evidence is insufficient, the system abstains instead of inventing a medical answer.

Current knowledge scope: Hematology only.
