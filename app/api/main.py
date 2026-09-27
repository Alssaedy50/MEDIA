"""MEDIA v0.2 Web/API application layer.

Thin HTTP/UI layer over the evidence-aware answer engine plus the frozen
local-model runtime boundary. The API never invents medical facts.
"""

from __future__ import annotations

from pathlib import Path
from typing import Literal

from fastapi import FastAPI, HTTPException
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel, Field

from inference.answer import answer
from inference.model_runtime import runtime_status

ROOT = Path(__file__).resolve().parents[2]
WEB_DIR = ROOT / "app" / "web"

app = FastAPI(
    title="MEDIA — Medical Education AI",
    version="0.2.0",
    description="Evidence-aware medical education application for the registered Hematology knowledge base.",
)

Mode = Literal["quick", "explain", "compare", "exam", "sources"]


class AnswerRequest(BaseModel):
    query: str = Field(min_length=1, max_length=1000)
    mode: Mode = "quick"
    top_k: int = Field(default=3, ge=1, le=10)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "MEDIA Web/API", "version": "0.2.0"}


@app.get("/api/scope")
def scope() -> dict[str, object]:
    return {
        "name": "MEDIA",
        "version": "0.2.0",
        "knowledge_scope": "knowledge/hematology",
        "purpose": "Medical education",
        "evidence_grounded": True,
        "safe_abstention": True,
        "model": "deterministic retrieval + evidence-aware answer engine + local Transformer runtime boundary",
    }


@app.get("/api/runtime")
def runtime() -> dict:
    return runtime_status()


@app.post("/api/answer")
def api_answer(request: AnswerRequest) -> dict:
    try:
        return answer(request.query, mode=request.mode, top_k=request.top_k)
    except Exception as exc:
        raise HTTPException(status_code=500, detail="MEDIA answer engine failed.") from exc


@app.get("/", include_in_schema=False)
def index() -> FileResponse:
    return FileResponse(WEB_DIR / "index.html")


app.mount("/static", StaticFiles(directory=WEB_DIR), name="static")
