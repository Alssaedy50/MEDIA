#!/usr/bin/env python3
"""Build the canonical Hematology training dataset from registered knowledge."""
from __future__ import annotations
import argparse, hashlib, json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
KB = ROOT / "knowledge" / "hematology"
OUT = ROOT / "datasets" / "hematology"
VERSION = "hematology-knowledge-derived-v1"

def load_records():
    records = []
    for path in sorted(KB.rglob("*.json")):
        if path.name.lower() == "readme.json":
            continue
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue
        if isinstance(data, dict) and data.get("id") and data.get("status") in {"reviewed", "verified"}:
            records.append((path, data))
    return records

def split_for(record_id: str) -> str:
    bucket = int(hashlib.sha256(record_id.encode("utf-8")).hexdigest()[:8], 16) % 100
    return "train" if bucket < 80 else "validation" if bucket < 90 else "test"

def add(rows, record_id, split, task, language, prompt, answer, source_title=None):
    prompt, answer = str(prompt).strip(), str(answer).strip()
    if not prompt or not answer:
        return
    rows.append({
        "id": f"{record_id}:{task}:{len(rows)}",
        "record_id": record_id, "split": split, "task": task,
        "language": language, "input": prompt, "target": answer,
        "provenance": {"type": "knowledge_derived", "record_id": record_id,
                       "source_title": source_title, "generator": VERSION}
    })

def build_record_rows(record):
    record_id = str(record["id"])
    split = split_for(record_id)
    concept = str(record.get("concept", "")).strip()
    content = record.get("content") or {}
    rows = []
    sources = record.get("sources") or []
    source_title = sources[0].get("title") if sources and isinstance(sources[0], dict) else None

    fields = [
        ("definition", "definition", f"What is {concept}?"),
        ("explanation", "explanation", f"Explain {concept}."),
        ("mechanism", "mechanism", f"What is the mechanism of {concept}?"),
        ("structure", "structure", f"Describe the structure of {concept}."),
        ("function", "function", f"What is the function of {concept}?"),
        ("clinical_relevance", "clinical_relevance", f"What is the clinical relevance of {concept}?"),
        ("diagnosis", "diagnosis", f"How is {concept} diagnosed?"),
        ("treatment", "treatment", f"What are the treatment principles for {concept}?"),
    ]
    for field, task, prompt in fields:
        value = content.get(field)
        if value:
            add(rows, record_id, split, task, "en", prompt, value, source_title)

    for item in content.get("high_yield") or []:
        if item:
            add(rows, record_id, split, "high_yield", "en",
                f"What is a high-yield point about {concept}?", item, source_title)

    for term in record.get("terminology") or []:
        if not isinstance(term, dict) or not term.get("term"):
            continue
        word = str(term["term"])
        notes = str(term.get("notes") or "").strip()
        arabic = str(term.get("arabic") or "").strip()
        pronunciation = str(term.get("pronunciation") or "").strip()
        if notes:
            add(rows, record_id, split, "terminology", "en",
                f"What does the medical term {word} mean in {record.get('topic', concept)}?", notes, source_title)
        if arabic:
            add(rows, record_id, split, "terminology_arabic", "ar",
                f"ما معنى المصطلح الطبي {word} في موضوع {record.get('topic', concept)}؟", arabic, source_title)
        if pronunciation:
            add(rows, record_id, split, "pronunciation", "en",
                f"How is the medical term {word} pronounced in the context of {record.get('topic', concept)}?", pronunciation, source_title)
    return rows

ABSTENTION = [
    {"id":"abstain:xylomab","record_id":None,"split":"test","task":"abstention",
     "language":"en","input":"What is the molecular mechanism and treatment of the fictional drug Xylomab?",
     "target":"I do not have sufficient registered evidence to answer this reliably.",
     "provenance":{"type":"abstention_test","generator":VERSION}},
    {"id":"abstain:novemia","record_id":None,"split":"test","task":"abstention",
     "language":"en","input":"Give the definitive treatment protocol for a fictional hematologic disease called Novemia.",
     "target":"I do not have sufficient registered evidence to answer this reliably.",
     "provenance":{"type":"abstention_test","generator":VERSION}},
]

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default=str(OUT))
    args = parser.parse_args()
    output = Path(args.output)
    output.mkdir(parents=True, exist_ok=True)
    records = load_records()
    rows = []
    for _, record in records:
        rows.extend(build_record_rows(record))
    rows.extend(ABSTENTION)
    by_split = {"train": [], "validation": [], "test": []}
    for row in rows:
        by_split[row["split"]].append(row)
    for split, split_rows in by_split.items():
        with (output / f"{split}.jsonl").open("w", encoding="utf-8") as handle:
            for row in split_rows:
                handle.write(json.dumps(row, ensure_ascii=False, sort_keys=True) + "\n")
    manifest = {
        "version": VERSION, "domain": "Hematology", "record_count": len(records),
        "example_count": len(rows),
        "split_counts": {k: len(v) for k, v in by_split.items()},
        "split_policy": "stable record-level SHA-256 bucket: 80/10/10",
        "source_policy": "knowledge-derived; no LLM-generated medical claims",
        "abstention_cases": len(ABSTENTION)
    }
    (output / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    print(json.dumps(manifest, ensure_ascii=False, indent=2))

if __name__ == "__main__":
    main()
