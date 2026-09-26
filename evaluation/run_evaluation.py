#!/usr/bin/env python3
"""Run the checked-in MEDAI evaluation set against the current answer engine."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "inference"))

from answer import answer

DEFAULT_CASES = ROOT / "evaluation" / "anatomy_eval.json"


def evaluate_case(case: dict[str, Any]) -> dict[str, Any]:
    result = answer(case["query"], mode=case.get("mode", "quick"), top_k=3)

    expected_ids = set(case.get("expected_concepts", []))
    actual_ids = set(result.get("retrieved_ids", []))
    expected_state = case["expected_evidence_state"]

    state_ok = result.get("evidence_state") == expected_state

    if expected_ids:
        if case.get("mode") == "compare":
            concept_ok = expected_ids.issubset(actual_ids)
        else:
            concept_ok = bool(result.get("answer")) and not result.get("abstain") and (
                bool(expected_ids & actual_ids)
            )
    else:
        concept_ok = result.get("abstain") is True and not actual_ids

    source_count = len(result.get("sources", []))
    source_ok = (source_count > 0) if expected_ids else (source_count == 0)

    passed = state_ok and concept_ok and source_ok
    return {
        "id": case["id"],
        "passed": passed,
        "state_ok": state_ok,
        "concept_ok": concept_ok,
        "source_ok": source_ok,
        "source_count": source_count,
        "expected_evidence_state": expected_state,
        "actual_evidence_state": result.get("evidence_state"),
        "expected_ids": sorted(expected_ids),
        "actual_ids": sorted(actual_ids),
    }


def run(path: Path = DEFAULT_CASES) -> dict[str, Any]:
    data = json.loads(path.read_text(encoding="utf-8"))
    cases = data["cases"]
    results = [evaluate_case(case) for case in cases]
    passed = sum(item["passed"] for item in results)

    supported = [item for item in results if item["expected_evidence_state"] != "insufficient"]
    abstention = [item for item in results if item["expected_evidence_state"] == "insufficient"]

    return {
        "evaluation": data["name"],
        "version": data["version"],
        "total_cases": len(results),
        "passed_cases": passed,
        "failed_cases": len(results) - passed,
        "pass_rate": round(passed / len(results), 3) if results else 0.0,
        "supported_cases": len(supported),
        "supported_passed": sum(item["passed"] for item in supported),
        "abstention_cases": len(abstention),
        "abstention_passed": sum(item["passed"] for item in abstention),
        "source_traceability_passed": sum(item["source_ok"] for item in results),
        "results": results,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--cases", type=Path, default=DEFAULT_CASES)
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()

    report = run(args.cases)
    if args.json:
        print(json.dumps(report, ensure_ascii=False, indent=2))
    else:
        print(f"{report['evaluation']} — {report['version']}")
        print(
            f"Passed: {report['passed_cases']}/{report['total_cases']} "
            f"({report['pass_rate']:.0%})"
        )
        print(
            f"Supported: {report['supported_passed']}/{report['supported_cases']} | "
            f"Abstention: {report['abstention_passed']}/{report['abstention_cases']} | "
            f"Source traceability: {report['source_traceability_passed']}/{report['total_cases']}"
        )
        for item in report["results"]:
            mark = "PASS" if item["passed"] else "FAIL"
            print(f"[{mark}] {item['id']}")


if __name__ == "__main__":
    main()
