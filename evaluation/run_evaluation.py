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

    # Support both the canonical evaluation schema and legacy checked-in cases.
    # Canonical keys: expected_concepts / expected_evidence_state.
    # Legacy keys: expected_ids / expected_state.
    expected_ids = set(case.get("expected_concepts", case.get("expected_ids", [])))
    actual_ids = set(result.get("retrieved_ids", []))
    expected_state = case.get("expected_evidence_state", case.get("expected_state", "insufficient"))
    # Legacy cases called the safe no-evidence state "abstain"; the answer
    # engine uses the more precise "insufficient" evidence-state label.
    if expected_state == "abstain":
        expected_state = "insufficient"

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

    passed = state_ok and concept_ok
    return {
        "id": case["id"],
        "passed": passed,
        "state_ok": state_ok,
        "concept_ok": concept_ok,
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

    return {
        "evaluation": data.get("name", data.get("scope", path.stem)),
        "version": data.get("version", "0.1.0"),
        "total_cases": len(results),
        "passed_cases": passed,
        "failed_cases": len(results) - passed,
        "pass_rate": round(passed / len(results), 3) if results else 0.0,
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
        for item in report["results"]:
            mark = "PASS" if item["passed"] else "FAIL"
            print(f"[{mark}] {item['id']}")
            if not item["passed"]:
                print(
                    f"    expected_state={item['expected_evidence_state']} "
                    f"actual_state={item['actual_evidence_state']} "
                    f"expected_ids={item['expected_ids']} actual_ids={item['actual_ids']}"
                )


if __name__ == "__main__":
    main()
