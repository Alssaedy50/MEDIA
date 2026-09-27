# MEDIA v0.1 Web/API Prototype

The first student-facing web layer for MEDIA wraps the existing evidence-aware retrieval and answer engine without replacing it.

## Architecture

Browser -> FastAPI -> inference.answer -> inference.retrieval -> knowledge/hematology

The API does not train or run the future Transformer yet. It exposes the deterministic evidence pipeline so the application contract can be validated before model training.

## Run locally

From the repository root, install `app/requirements.txt`, then run `uvicorn app.api.main:app --reload` and open `http://127.0.0.1:8000`.

Endpoints: `GET /health`, `GET /api/scope`, `POST /api/answer`, and `GET /docs`.

## Safety behavior

The application preserves safe abstention. When the registered knowledge base does not contain sufficient evidence, the API returns `abstain: true` and does not invent a medical answer.

Current knowledge scope: Hematology only.

MEDIA is an educational research prototype and is not a clinical decision-support system.
