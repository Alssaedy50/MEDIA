#!/usr/bin/env python3
"""MEDAI v0.1 — evidence-aware student answer generator.

This layer sits on top of deterministic retrieval. It does not call an LLM
and does not invent medical facts: every generated statement is selected
directly from registered knowledge evidence.
"""

from __future__ import annotations

import argparse
import json
from typing import Any

from retrieval import retrieve


def _first_nonempty(*values: Any) -> str | None:
    for value in values:
        if isinstance(value, str) and value.strip():
            return value.strip()
    return None


def _terms(hit: dict[str, Any], limit: int = 6) -> list[str]:
    result = []
    for item in hit.get("terminology", []) or []:
        if isinstance(item, dict) and item.get("term"):
            result.append(str(item["term"]))
    return result[:limit]


def _sources(hits: list[dict[str, Any]], limit: int = 6) -> list[dict[str, Any]]:
    seen: set[tuple[str, str | None]] = set()
    result = []
    for hit in hits:
        for source in hit.get("sources", []) or []:
            title = source.get("title")
            url = source.get("url")
            key = (str(title), url)
            if title and key not in seen:
                seen.add(key)
                result.append(source)
            if len(result) >= limit:
                return result
    return result


def _hit_summary(hit: dict[str, Any], mode: str) -> tuple[str | None, list[str]]:
    evidence = hit.get("evidence", {}) or {}
    concept = str(hit.get("concept") or "the retrieved concept")
    definition = _first_nonempty(evidence.get("definition"))
    explanation = _first_nonempty(evidence.get("explanation"))
    mechanism = _first_nonempty(evidence.get("mechanism"))
    structure = _first_nonempty(evidence.get("structure"))
    function = _first_nonempty(evidence.get("function"))
    clinical = _first_nonempty(evidence.get("clinical_relevance"))
    high_yield = [str(x) for x in evidence.get("high_yield", []) if str(x).strip()]

    if mode == "quick":
        answer = definition or explanation or function or mechanism
        return answer, high_yield[:3]

    if mode == "explain":
        answer = definition or explanation or function or mechanism
        points = []
        for value in (explanation, mechanism, structure, function):
            if value and value != answer:
                points.append(value)
        return answer, points[:4] + high_yield[:2]

    if mode == "exam":
        answer = definition or explanation or function
        return answer, high_yield[:6] or [x for x in (function, mechanism, clinical) if x][:6]

    # "compare" is handled at the multi-hit level.
    answer = definition or explanation or function or mechanism
    return answer, high_yield[:3]


def answer(
    query: str,
    *,
    mode: str = "quick",
    top_k: int = 3,
) -> dict[str, Any]:
    result = retrieve(query, top_k=top_k)
    hits = result["hits"]

    if result["abstain"]:
        return {
            "engine": "MEDAI Answer v0.1",
            "query": query,
            "mode": mode,
            "evidence_state": "insufficient",
            "abstain": True,
            "answer": (
                "I don't have enough registered evidence in MEDAI's current "
                "knowledge base to answer this safely."
            ),
            "key_points": [],
            "terms": [],
            "sources": [],
        }

    if mode == "sources":
        return {
            "engine": "MEDAI Answer v0.1",
            "query": query,
            "mode": mode,
            "evidence_state": "supported",
            "abstain": False,
            "answer": "Registered sources for the retrieved evidence:",
            "key_points": [],
            "terms": [],
            "sources": _sources(hits),
        }

    if mode == "compare":
        if len(hits) < 2:
            text, points = _hit_summary(hits[0], "explain")
            return {
                "engine": "MEDAI Answer v0.1",
                "query": query,
                "mode": mode,
                "evidence_state": "single_concept",
                "abstain": False,
                "answer": text,
                "key_points": points,
                "terms": _terms(hits[0]),
                "sources": _sources(hits),
            }

        comparison = []
        for hit in hits[:2]:
            evidence = hit.get("evidence", {}) or {}
            comparison.append({
                "concept": hit.get("concept"),
                "definition": _first_nonempty(evidence.get("definition")),
                "structure": _first_nonempty(evidence.get("structure")),
                "function": _first_nonempty(evidence.get("function")),
                "clinical_relevance": _first_nonempty(evidence.get("clinical_relevance")),
            })
        return {
            "engine": "MEDAI Answer v0.1",
            "query": query,
            "mode": mode,
            "evidence_state": "multi_concept",
            "abstain": False,
            "answer": "Comparison assembled only from registered knowledge evidence.",
            "comparison": comparison,
            "key_points": [],
            "terms": sorted(set(_terms(hits[0]) + _terms(hits[1])))[:8],
            "sources": _sources(hits),
        }

    answer_text, points = _hit_summary(hits[0], mode)
    return {
        "engine": "MEDAI Answer v0.1",
        "query": query,
        "mode": mode,
        "evidence_state": "supported",
        "abstain": False,
        "answer": answer_text,
        "key_points": points,
        "terms": _terms(hits[0]),
        "sources": _sources(hits),
        "retrieved_concepts": [hit["concept"] for hit in hits],
    }


def format_human(result: dict[str, Any]) -> str:
    lines = [
        "MEDAI Answer v0.1",
        f"Mode: {result['mode']}",
        f"Query: {result['query']}",
        "",
    ]

    if result["abstain"]:
        lines.extend(["ABSTAIN", result["answer"]])
        return "
".join(lines)

    lines.append(result["answer"])

    if result.get("comparison"):
        lines.append("")
        for item in result["comparison"]:
            lines.append(f"{item['concept']}:")
            for key in ("definition", "structure", "function", "clinical_relevance"):
                if item.get(key):
                    lines.append(f"  {key}: {item[key]}")

    if result.get("key_points"):
        lines.append("")
        lines.append("Key points:")
        lines.extend(f"- {point}" for point in result["key_points"])

    if result.get("terms"):
        lines.append("")
        lines.append("Terms: " + ", ".join(result["terms"]))

    if result.get("sources"):
        lines.append("")
        lines.append("Sources:")
        for source in result["sources"]:
            lines.append(f"- {source.get('title')} ({source.get('year')})")
            if source.get("url"):
                lines.append(f"  {source['url']}")

    return "
".join(lines)


def main() -> None:
    parser = argparse.ArgumentParser(description="MEDAI v0.1 student answer demo")
    parser.add_argument("query")
    parser.add_argument(
        "--mode",
        choices=("quick", "explain", "compare", "exam", "sources"),
        default="quick",
    )
    parser.add_argument("--top-k", type=int, default=3)
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()

    result = answer(args.query, mode=args.mode, top_k=args.top_k)
    print(json.dumps(result, ensure_ascii=False, indent=2) if args.json else format_human(result))


if __name__ == "__main__":
    main()
