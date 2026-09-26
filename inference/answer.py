#!/usr/bin/env python3
"""MEDAI v0.1 — evidence-aware student answer generator."""

from __future__ import annotations

import argparse
import json
from typing import Any

from retrieval import retrieve, tokenize


def _first_nonempty(*values: Any) -> str | None:
    for value in values:
        if isinstance(value, str) and value.strip():
            return value.strip()
    return None


def _terms(hit: dict[str, Any], limit: int = 10) -> list[str]:
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
    definition = _first_nonempty(evidence.get("definition"))
    explanation = _first_nonempty(evidence.get("explanation"))
    mechanism = _first_nonempty(evidence.get("mechanism"))
    structure = _first_nonempty(evidence.get("structure"))
    function = _first_nonempty(evidence.get("function"))
    clinical = _first_nonempty(evidence.get("clinical_relevance"))
    high_yield = [str(x) for x in evidence.get("high_yield", []) if str(x).strip()]

    if mode == "quick":
        return definition or explanation or function or mechanism, high_yield[:3]

    if mode == "explain":
        answer = definition or explanation or function or mechanism
        points = [
            value for value in (explanation, mechanism, structure, function)
            if value and value != answer
        ]
        return answer, points[:4] + high_yield[:2]

    if mode == "exam":
        return (
            definition or explanation or function,
            high_yield[:6] or [x for x in (function, mechanism, clinical) if x][:6],
        )

    return definition or explanation or function or mechanism, high_yield[:3]


def _base(result: dict[str, Any], mode: str) -> dict[str, Any]:
    hits = result["hits"]
    return {
        "engine": "MEDAI Answer v0.1",
        "query": result["query"],
        "mode": mode,
        "knowledge_scope": result["knowledge_scope"],
        "evidence_state": "supported",
        "abstain": False,
        "retrieved_ids": [hit["id"] for hit in hits],
        "retrieved_concepts": [hit["concept"] for hit in hits],
    }


def answer(query: str, *, mode: str = "quick", top_k: int = 3) -> dict[str, Any]:
    result = retrieve(query, top_k=top_k)
    hits = result["hits"]

    if result["abstain"]:
        return {
            "engine": "MEDAI Answer v0.1",
            "query": query,
            "mode": mode,
            "knowledge_scope": result["knowledge_scope"],
            "evidence_state": "insufficient",
            "abstain": True,
            "retrieved_ids": [],
            "retrieved_concepts": [],
            "answer": (
                "I don't have enough registered evidence in MEDAI's current "
                "knowledge base to answer this safely."
            ),
            "key_points": [],
            "terms": [],
            "sources": [],
        }

    if mode == "sources":
        response = _base(result, mode)
        response.update({
            "answer": "Registered sources for the retrieved evidence:",
            "key_points": [],
            "terms": [],
            "sources": _sources(hits),
        })
        return response

    if mode == "compare":
        # Retrieve explicit concepts from contiguous query phrases, then combine
        # them without relying on the global top-k ranking.
        terms = result.get("query_terms", [])
        candidates = []
        for size in (3, 2, 1):
            for i in range(len(terms) - size + 1):
                candidates.append(" ".join(terms[i:i + size]))

        explicit_hits: dict[str, dict[str, Any]] = {}
        for candidate in candidates:
            extra = retrieve(candidate, top_k=1)
            for hit in extra.get("hits", []):
                explicit_hits.setdefault(hit["id"], hit)

        comparison_hits = list(explicit_hits.values())
        if len(comparison_hits) < 2:
            comparison_hits = result["hits"][:2]

        comparison = []
        for hit in comparison_hits[:2]:
            evidence = hit.get("evidence", {}) or {}
            comparison.append({
                "id": hit.get("id"),
                "concept": hit.get("topic") or hit.get("concept"),
                "definition": _first_nonempty(evidence.get("definition")),
                "structure": _first_nonempty(evidence.get("structure")),
                "function": _first_nonempty(evidence.get("function")),
                "clinical_relevance": _first_nonempty(evidence.get("clinical_relevance")),
            })

        result["hits"] = comparison_hits
        response = _base(result, mode)
        response.update({
            "evidence_state": "multi_concept" if len(comparison_hits) >= 2 else "single_concept",
            "answer": "Comparison assembled only from registered knowledge evidence.",
            "comparison": comparison,
            "key_points": [],
            "terms": sorted(set(sum((_terms(hit) for hit in comparison_hits[:2]), [])))[:10],
            "sources": _sources(comparison_hits),
        })
        return response

    answer_text, points = _hit_summary(hits[0], mode)
    response = _base(result, mode)
    response.update({
        "answer": answer_text,
        "key_points": points,
        "terms": _terms(hits[0]),
        "sources": _sources(hits),
    })
    return response


def format_human(result: dict[str, Any]) -> str:
    lines = [
        "MEDAI Answer v0.1",
        f"Mode: {result['mode']}",
        f"Query: {result['query']}",
        "",
    ]

    if result["abstain"]:
        lines.extend(["ABSTAIN", result["answer"]])
        return "\n".join(lines)

    lines.append(result["answer"])

    if result.get("comparison"):
        lines.append("")
        for item in result["comparison"]:
            lines.append(f"{item['concept']} [{item['id']}]:")
            for key in ("definition", "structure", "function", "clinical_relevance"):
                if item.get(key):
                    lines.append(f"  {key}: {item[key]}")

    if result.get("key_points"):
        lines.extend(["", "Key points:"])
        lines.extend(f"- {point}" for point in result["key_points"])

    if result.get("terms"):
        lines.extend(["", "Terms: " + ", ".join(result["terms"])])

    if result.get("sources"):
        lines.extend(["", "Sources:"])
        for source in result["sources"]:
            lines.append(f"- {source.get('title')} ({source.get('year')})")
            if source.get("url"):
                lines.append(f"  {source['url']}")

    return "\n".join(lines)


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
