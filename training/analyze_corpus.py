#!/usr/bin/env python3
"""Deterministic corpus analysis for the generated Hematology dataset."""
from __future__ import annotations

import argparse
import collections
import hashlib
import json
import math
import re
from pathlib import Path
from typing import Iterable

TOKEN_RE = re.compile(r"[A-Za-z]+(?:[0-9]+)?|[\u0600-\u06FF]+|[^\s]")
ARABIC_RE = re.compile(r"[\u0600-\u06FF]")
LATIN_RE = re.compile(r"[A-Za-z]")

def baseline_tokens(text: str) -> list[str]:
    return TOKEN_RE.findall(text or "")

def language_of(text: str) -> str:
    ar = len(ARABIC_RE.findall(text or ""))
    en = len(LATIN_RE.findall(text or ""))
    if ar and en:
        return "mixed"
    if ar:
        return "ar"
    if en:
        return "en"
    return "other"

def percentile(values: list[int], q: float) -> int:
    if not values:
        return 0
    values = sorted(values)
    pos = (len(values) - 1) * q
    lo, hi = math.floor(pos), math.ceil(pos)
    if lo == hi:
        return values[lo]
    return round(values[lo] + (values[hi] - values[lo]) * (pos - lo))

def load_examples(root: Path) -> list[dict]:
    rows = []
    for split in ("train", "validation", "test"):
        path = root / f"{split}.jsonl"
        if not path.exists():
            raise FileNotFoundError(path)
        with path.open(encoding="utf-8") as f:
            for line_no, line in enumerate(f, 1):
                row = json.loads(line)
                row["_file_split"] = split
                row["_line"] = line_no
                rows.append(row)
    return rows

def duplicate_counts(rows: list[dict]) -> dict:
    def count(field_tuple):
        c = collections.Counter(field_tuple)
        return sum(v - 1 for v in c.values() if v > 1)
    pairs = [(r["input"].strip(), r["target"].strip()) for r in rows]
    inputs = [p[0] for p in pairs]
    targets = [p[1] for p in pairs]
    normalized_inputs = [" ".join(x.lower().split()) for x in inputs]
    return {
        "duplicate_input_rows": count(inputs),
        "duplicate_target_rows": count(targets),
        "duplicate_input_target_pairs": count(pairs),
        "normalized_input_duplicates": count(normalized_inputs),
    }

def leakage(rows: list[dict]) -> dict:
    by_split = collections.defaultdict(set)
    for r in rows:
        by_split[r["_file_split"]].add(" ".join(r["input"].lower().split()))
    pairs = {}
    for a, b in (("train", "validation"), ("train", "test"), ("validation", "test")):
        overlap = by_split[a] & by_split[b]
        pairs[f"{a}_vs_{b}"] = len(overlap)
    return pairs

def top_vocab(rows: list[dict], limit: int) -> dict:
    counter = collections.Counter()
    for r in rows:
        counter.update(baseline_tokens(r["input"]))
        counter.update(baseline_tokens(r["target"]))
    total = sum(counter.values())
    covered = {}
    for k in (16_000, 32_000, 50_000):
        covered[k] = {
            "unique_tokens": min(k, len(counter)),
            "token_coverage": round(
                sum(v for _, v in counter.most_common(k)) / total, 6
            ) if total else 0.0,
        }
    return {
        "unique_baseline_tokens": len(counter),
        "total_baseline_tokens": total,
        "top_tokens": counter.most_common(limit),
        "vocabulary_capacity_estimates": covered,
    }

def analyze(root: Path) -> dict:
    rows = load_examples(root)
    split_counts = collections.Counter(r["_file_split"] for r in rows)
    task_counts = collections.Counter(r["task"] for r in rows)
    language_counts = collections.Counter(r["language"] for r in rows)
    detected_language_counts = collections.Counter(language_of(r["input"] + " " + r["target"]) for r in rows)
    record_counts = collections.Counter(r.get("record_id") for r in rows if r.get("record_id"))
    source_counts = collections.Counter(
        (r.get("provenance") or {}).get("source_title") or "none"
        for r in rows
    )
    lengths = [len(baseline_tokens(r["input"] + " " + r["target"])) for r in rows]
    input_lengths = [len(baseline_tokens(r["input"])) for r in rows]
    target_lengths = [len(baseline_tokens(r["target"])) for r in rows]
    abstention = [r for r in rows if r["task"] == "abstention"]
    provenance_types = collections.Counter((r.get("provenance") or {}).get("type", "missing") for r in rows)

    report = {
        "version": "corpus-analysis-v1",
        "corpus": root.name,
        "record_count": len(record_counts),
        "example_count": len(rows),
        "split_counts": dict(sorted(split_counts.items())),
        "task_distribution": dict(sorted(task_counts.items())),
        "language_distribution_declared": dict(sorted(language_counts.items())),
        "language_distribution_detected": dict(sorted(detected_language_counts.items())),
        "record_example_distribution": {
            "records_with_examples": len(record_counts),
            "min_examples_per_record": min(record_counts.values()) if record_counts else 0,
            "max_examples_per_record": max(record_counts.values()) if record_counts else 0,
            "mean_examples_per_record": round(sum(record_counts.values()) / len(record_counts), 3) if record_counts else 0.0,
        },
        "sequence_lengths_baseline_tokens": {
            "all": {
                "min": min(lengths) if lengths else 0,
                "p50": percentile(lengths, 0.50),
                "p90": percentile(lengths, 0.90),
                "p95": percentile(lengths, 0.95),
                "p99": percentile(lengths, 0.99),
                "max": max(lengths) if lengths else 0,
                "mean": round(sum(lengths) / len(lengths), 3) if lengths else 0.0,
            },
            "input": {"mean": round(sum(input_lengths) / len(input_lengths), 3) if input_lengths else 0.0},
            "target": {"mean": round(sum(target_lengths) / len(target_lengths), 3) if target_lengths else 0.0},
        },
        "duplicate_analysis": duplicate_counts(rows),
        "cross_split_leakage": leakage(rows),
        "vocabulary": top_vocab(rows, 50),
        "arabic_english_balance": {
            "declared_arabic_examples": language_counts.get("ar", 0),
            "declared_english_examples": language_counts.get("en", 0),
            "declared_mixed_examples": language_counts.get("mixed", 0),
        },
        "source_distribution_top": source_counts.most_common(20),
        "provenance_type_distribution": dict(sorted(provenance_types.items())),
        "abstention": {
            "count": len(abstention),
            "ids": [r["id"] for r in abstention],
            "splits": dict(collections.Counter(r["_file_split"] for r in abstention)),
        },
    }
    return report

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dataset", default="datasets/hematology")
    parser.add_argument("--output", default=None)
    args = parser.parse_args()
    root = Path(args.dataset)
    report = analyze(root)
    text = json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    if args.output:
        Path(args.output).write_text(text, encoding="utf-8")
    print(text, end="")

if __name__ == "__main__":
    main()
