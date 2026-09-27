#!/usr/bin/env python3
"""Validate a training corpus before neural pretraining."""
from __future__ import annotations
import argparse
import json
from pathlib import Path

REQUIRED = {"id", "input", "target", "provenance"}

def load(path: str):
    rows=[]
    for line_no, line in enumerate(Path(path).read_text(encoding="utf-8").splitlines(), 1):
        if not line.strip():
            continue
        row=json.loads(line)
        rows.append((line_no,row))
    return rows

def validate(paths: list[str]) -> dict:
    errors=[]
    ids={}
    pairs={}
    records_by_split={}
    total=0
    for path in paths:
        split=Path(path).stem
        for line_no,row in load(path):
            total += 1
            missing=REQUIRED-set(row)
            if missing: errors.append(f"{path}:{line_no}: missing {sorted(missing)}")
            rid=row.get("id")
            if rid in ids: errors.append(f"duplicate id {rid}: {ids[rid]} and {path}:{line_no}")
            else: ids[rid]=f"{path}:{line_no}"
            if not str(row.get("input","")).strip() or not str(row.get("target","")).strip():
                errors.append(f"{path}:{line_no}: empty input or target")
            prov=row.get("provenance")
            if not isinstance(prov,dict) or not prov.get("type"):
                errors.append(f"{path}:{line_no}: provenance.type is required")
            pair=(str(row.get("input","")).strip(),str(row.get("target","")).strip())
            if pair in pairs: errors.append(f"duplicate input/target pair: {pairs[pair]} and {path}:{line_no}")
            else: pairs[pair]=f"{path}:{line_no}"
            record_id=row.get("record_id")
            if record_id is not None:
                records_by_split.setdefault(record_id,set()).add(split)
    for record_id,splits in records_by_split.items():
        if len(splits)>1:
            errors.append(f"record leakage across splits: {record_id} -> {sorted(splits)}")
    return {"files":paths,"example_count":total,"unique_ids":len(ids),"unique_pairs":len(pairs),
            "record_count":len(records_by_split),"errors":errors,"valid":not errors}

def main():
    p=argparse.ArgumentParser()
    p.add_argument("jsonl",nargs="+")
    a=p.parse_args()
    result=validate(a.jsonl)
    print(json.dumps(result,ensure_ascii=False,indent=2))
    if not result["valid"]: raise SystemExit(1)

if __name__=="__main__":
    main()
