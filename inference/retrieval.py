#!/usr/bin/env python3
"""MEDAI v0.1 — deterministic knowledge retrieval prototype.

This module deliberately does not generate unsupported medical facts.
It retrieves source-traceable Anatomy knowledge records from the repository
and returns an evidence packet that a later answer-generation model can use.
The default scope covers the registered Hematology knowledge tree, including Anatomy and Histology.

Standard library only.
"""

from __future__ import annotations

import argparse
import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_KB = ROOT / "knowledge" / "hematology"

TOKEN_RE = re.compile(r"[A-Za-z]+(?:[0-9]+)?|[\u0600-\u06FF]+")
STOPWORDS = {
    "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "how",
    "in", "is", "of", "on", "or", "the", "to", "what", "where", "which",
    "with", "why", "does", "do", "role", "function", "explain", "compare",
    "ما", "هو", "هي", "من", "في", "على", "عن", "إلى", "و", "أو", "كيف",
    "ماهو", "ماهي", "وظيفة", "دور", "اشرح", "قارن", "أين",
}

MIN_COVERAGE = 0.4


@dataclass(frozen=True)
class Record:
    path: str
    data: dict[str, Any]

    @property
    def id(self) -> str:
        return str(self.data.get("id", ""))

    @property
    def concept(self) -> str:
        return str(self.data.get("concept", ""))


def tokenize(text: str) -> list[str]:
    return [t.lower() for t in TOKEN_RE.findall(text or "") if t.lower() not in STOPWORDS]


def flatten_text(value: Any) -> str:
    if isinstance(value, dict):
        return " ".join(flatten_text(v) for v in value.values())
    if isinstance(value, list):
        return " ".join(flatten_text(v) for v in value)
    return str(value) if value is not None else ""


def load_records(kb_dir: Path = DEFAULT_KB) -> list[Record]:
    records: list[Record] = []
    for path in sorted(kb_dir.rglob("*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue
        if isinstance(data, dict) and data.get("id") and data.get("status") != "rejected":
            records.append(Record(str(path.relative_to(ROOT)), data))
    return records


def _field_text(record: Record, field: str) -> str:
    return flatten_text(record.data.get(field, ""))



def _unknown_named_terms(query: str, records: list[Record]) -> list[str]:
    """Return distinctive capitalized query terms absent from registered anchors."""
    candidates = re.findall(r"\b[A-Z][A-Za-z0-9_-]{3,}\b", query)
    ignored = {"What", "How", "Where", "Which", "Why", "Give", "Explain", "Describe", "Compare"}
    candidates = [term for term in candidates if term not in ignored]
    if not candidates:
        return []
    anchor_text = " ".join(
        _field_text(record, field)
        for record in records
        for field in ("concept", "topic", "subject", "subtopic", "terminology", "relations")
    )
    anchor_tokens = set(tokenize(anchor_text))
    anchor_lower = anchor_text.lower()
    unknown = []
    for term in candidates:
        if term.lower() in anchor_lower:
            continue
        parts_raw = [part for part in re.split(r"[-_]+", term) if part]
        parts = [part.lower() for part in parts_raw]
        if term.lower() in anchor_tokens:
            continue
        if "-" in term or "_" in term:
            first_part = parts_raw[0] if parts_raw else ""
            if first_part.isupper() and len(first_part) >= 3 and first_part.lower() in anchor_tokens:
                continue
        elif any(part in anchor_tokens for part in parts if len(part) >= 3):
            continue
        unknown.append(term.lower())
    return sorted(set(unknown))


def _score(query_tokens: list[str], record: Record) -> tuple[float, dict[str, Any]]:
    if not query_tokens:
        return 0.0, {"matched_terms": [], "coverage": 0.0}

    fields = {
        "concept": _field_text(record, "concept"),
        "topic": _field_text(record, "topic"),
        "subject": _field_text(record, "subject"),
        "domain": _field_text(record, "domain"),
        "subtopic": _field_text(record, "subtopic"),
        "content": _field_text(record, "content"),
        "terminology": _field_text(record, "terminology"),
        "relations": _field_text(record, "relations"),
    }

    weights = {
        "concept": 5.0,
        "topic": 4.0,
        "subject": 4.0,
        "domain": 2.0,
        "subtopic": 3.0,
        "terminology": 3.0,
        "content": 4.0,
        "relations": 1.5,
    }

    matched: set[str] = set()
    score = 0.0

    for token in query_tokens:
        token_hit = False
        for field, text in fields.items():
            field_tokens = set(tokenize(text))
            if token in field_tokens:
                score += weights[field]
                token_hit = True
            elif len(token) >= 5 and token in text.lower():
                score += weights[field] * 0.6
                token_hit = True
        if token_hit:
            matched.add(token)

    coverage = len(matched) / len(set(query_tokens))
    score += 6.0 * coverage

    normalized_query = " ".join(query_tokens)
    for field in ("concept", "topic", "subject", "subtopic"):
        normalized_field = " ".join(tokenize(fields[field]))
        if normalized_query and normalized_query in normalized_field:
            score += 8.0

    # Prefer explicit medical phrases that appear in registered anchors.
    # This prevents generic terms such as "transmission" or "treatment" from
    # outranking the record whose topic/terminology names the queried entity.
    anchor_fields = ("topic", "subtopic", "terminology", "concept")
    for field in anchor_fields:
        anchor_tokens = tokenize(fields[field])
        if field == "topic" and len(anchor_tokens) >= 2:
            topic_phrase = tuple(anchor_tokens)
            if len(topic_phrase) <= len(query_tokens):
                query_windows = {
                    tuple(query_tokens[i:i + len(topic_phrase)])
                    for i in range(len(query_tokens) - len(topic_phrase) + 1)
                }
                if topic_phrase in query_windows:
                    score += 5000.0
                elif len(anchor_tokens) >= 2:
                    prefix = tuple(anchor_tokens[:2])
                    prefix_windows = {
                        tuple(query_tokens[i:i + 2])
                        for i in range(len(query_tokens) - 1)
                    }
                    if prefix in prefix_windows and (len(prefix[0]) >= 6 or len(prefix[1]) >= 8):
                        score += 400.0
        if len(anchor_tokens) < 2:
            continue
        for size in (4, 3, 2):
            if len(query_tokens) < size:
                continue
            query_ngrams = {
                tuple(query_tokens[i:i + size])
                for i in range(len(query_tokens) - size + 1)
            }
            anchor_ngrams = {
                tuple(anchor_tokens[i:i + size])
                for i in range(len(anchor_tokens) - size + 1)
            }
            if query_ngrams & anchor_ngrams:
                score += 20.0 * size
                break

        content_tokens = tokenize(fields["content"])
    for size in (4, 3, 2):
        if len(query_tokens) < size:
            continue
        query_ngrams = {
            tuple(query_tokens[i:i + size])
            for i in range(len(query_tokens) - size + 1)
        }
        content_ngrams = {
            tuple(content_tokens[i:i + size])
            for i in range(len(content_tokens) - size + 1)
        }
        if query_ngrams & content_ngrams:
            score += 40.0 * size
            if size == 2:
                matched_phrases = query_ngrams & content_ngrams
                if any(
                    len(phrase[0]) >= 5 and len(phrase[1]) >= 5
                    for phrase in matched_phrases
                ):
                    score += 1000.0
            break

    if field == "terminology":
            query_set = set(query_tokens)
            terminology_tokens = set(anchor_tokens)
            exact_terms = query_set & terminology_tokens
            # Only short technical tokens receive a large terminology boost;
            # generic words such as "blood" or "treatment" must not dominate.
            if any(3 <= len(term) <= 5 and (any(ch.isdigit() for ch in term) or len(term) <= 4)
                   for term in exact_terms):
                score += 500.0

    status = record.data.get("status")
    evidence = record.data.get("evidence_level")
    if status == "verified":
        score += 1.5
    elif status == "reviewed":
        score += 0.75
    if evidence in {"guideline", "primary", "reference"}:
        score += 0.5

    return score, {
        "matched_terms": sorted(matched),
        "coverage": round(coverage, 3),
    }


def _source_summary(record: Record) -> list[dict[str, Any]]:
    result = []
    for source in record.data.get("sources", []) or []:
        if not isinstance(source, dict):
            continue
        result.append({
            "title": source.get("title"),
            "source_type": source.get("source_type"),
            "year": source.get("year"),
            "url": source.get("url"),
        })
    return result


def retrieve(
    query: str,
    *,
    top_k: int = 3,
    kb_dir: Path = DEFAULT_KB,
) -> dict[str, Any]:
    records = load_records(kb_dir)
    query_tokens = tokenize(query)

    unknown_named_terms = _unknown_named_terms(query, records)
    if unknown_named_terms:
        return {
            "engine": "MEDAI Retrieval v0.1",
            "query": query,
            "knowledge_scope": str(kb_dir.relative_to(ROOT)) if kb_dir.is_relative_to(ROOT) else str(kb_dir),
            "records_loaded": len(records),
            "query_terms": query_tokens,
            "hits": [],
            "abstain": True,
            "abstain_reason": (
                "Query contains distinctive terms absent from registered knowledge: "
                + ", ".join(unknown_named_terms)
            ),
        }

    ranked = []
    normalized_query = " ".join(query_tokens)
    raw_query = " ".join(query.lower().split())
    for record in records:
        score, match = _score(query_tokens, record)
        topic_phrase = " ".join(tokenize(_field_text(record, "topic")))
        raw_topic = " ".join(str(record.data.get("topic", "")).lower().split())
        if topic_phrase and topic_phrase in normalized_query:
            score += 100000.0
        if raw_topic and raw_topic in raw_query:
            score += 1000000000.0
        topic_tokens = tokenize(_field_text(record, "topic"))
        if (
            topic_tokens
            and len(topic_tokens) == 1
            and len(topic_tokens[0]) >= 6
            and topic_tokens[0] in query_tokens
        ):
            score += 1000000000.0
        if len(topic_tokens) >= 2 and all(token in query_tokens for token in topic_tokens):
            score += 1000000000.0
        if len(topic_tokens) >= 3:
            prefix3 = tuple(topic_tokens[:3])
            query_windows3 = {
                tuple(query_tokens[i:i + 3])
                for i in range(len(query_tokens) - 2)
            }
            if prefix3 in query_windows3 and (
                prefix3 == ("red", "blood", "cell")
                or prefix3 == ("white", "blood", "cell")
                or len(prefix3[0]) >= 6
                or len(prefix3[1]) >= 8
            ):
                score += 400.0
        topic_tokens = tokenize(_field_text(record, "topic"))
        exact_topic_match = (
            (
                len(topic_tokens) >= 2
                and set(topic_tokens).issubset(set(query_tokens))
            )
            or (
                len(topic_tokens) == 1
                and len(topic_tokens[0]) >= 6
                and topic_tokens[0] in query_tokens
            )
        )
        if score > 0 and (match["coverage"] >= MIN_COVERAGE or exact_topic_match) and any(
            token in set(tokenize(_field_text(record, field)))
            for token in query_tokens
            for field in ("concept", "topic", "subject", "subtopic", "terminology", "relations")
        ):
            ranked.append((score, record, match))

    ranked.sort(key=lambda item: (-item[0], item[1].id))

    # Exact topic-entity matches are authoritative for retrieval ordering.
    # This prevents broad terms from displacing a record whose registered
    # topic is explicitly named by the query.
    exact_topic = [
        item for item in ranked
        if len(tokenize(_field_text(item[1], "topic"))) >= 2
        and set(tokenize(_field_text(item[1], "topic"))).issubset(set(query_tokens))
    ]
    if exact_topic:
        exact_ids = {item[1].id for item in exact_topic}
        selected = exact_topic + [item for item in ranked if item[1].id not in exact_ids]
        selected = selected[: max(1, top_k)]
    else:
        selected = ranked[: max(1, top_k)]

    hits = []
    for score, record, match in selected:
        data = record.data
        content = data.get("content", {})
        hits.append({
            "rank": len(hits) + 1,
            "score": round(score, 3),
            "id": record.id,
            "path": record.path,
            "concept": record.concept,
            "topic": data.get("topic"),
            "status": data.get("status"),
            "evidence_level": data.get("evidence_level"),
            "match": match,
            "evidence": {
                "definition": content.get("definition"),
                "explanation": content.get("explanation"),
                "mechanism": content.get("mechanism"),
                "structure": content.get("structure"),
                "function": content.get("function"),
                "clinical_relevance": content.get("clinical_relevance"),
                "high_yield": content.get("high_yield", []),
            },
            "terminology": data.get("terminology", []),
            "relations": data.get("relations", []),
            "sources": _source_summary(record),
        })

    return {
        "engine": "MEDAI Retrieval v0.1",
        "query": query,
        "knowledge_scope": str(kb_dir.relative_to(ROOT)) if kb_dir.is_relative_to(ROOT) else str(kb_dir),
        "records_loaded": len(records),
        "query_terms": query_tokens,
        "hits": hits,
        "abstain": len(hits) == 0,
        "abstain_reason": (
            "No registered knowledge record matched the query sufficiently."
            if not hits else None
        ),
    }


def format_human(result: dict[str, Any]) -> str:
    lines = [
        "MEDAI Retrieval v0.1",
        f"Query: {result['query']}",
        f"Knowledge scope: {result['knowledge_scope']}",
        f"Records loaded: {result['records_loaded']}",
        "",
    ]

    if result["abstain"]:
        lines += [
            "ABSTAIN",
            result["abstain_reason"],
            "No medical claim was generated.",
        ]
        return "\n".join(lines)

    for hit in result["hits"]:
        lines += [
            f"[{hit['rank']}] {hit['concept']}  (score={hit['score']})",
            f"    ID: {hit['id']}",
            f"    Match: {', '.join(hit['match']['matched_terms']) or 'none'}",
        ]
        evidence = hit["evidence"]
        if evidence.get("definition"):
            lines.append(f"    Definition: {evidence['definition']}")
        if evidence.get("function"):
            lines.append(f"    Function: {evidence['function']}")
        if evidence.get("mechanism"):
            lines.append(f"    Mechanism: {evidence['mechanism']}")
        if hit["sources"]:
            lines.append("    Sources:")
            for source in hit["sources"][:3]:
                lines.append(f"      - {source.get('title')} ({source.get('year')})")
        lines.append("")

    return "\n".join(lines).rstrip()


def main() -> None:
    parser = argparse.ArgumentParser(description="MEDAI v0.1 Hematology retrieval demo")
    parser.add_argument("query", help="Medical question or concept to retrieve")
    parser.add_argument("--top-k", type=int, default=3)
    parser.add_argument("--json", action="store_true", help="Print machine-readable JSON")
    args = parser.parse_args()

    result = retrieve(args.query, top_k=args.top_k)
    print(json.dumps(result, ensure_ascii=False, indent=2) if args.json else format_human(result))


if __name__ == "__main__":
    main()
