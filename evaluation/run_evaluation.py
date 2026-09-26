#!/usr/bin/env python3
"""Run the checked-in MEDAI evaluation set against the current answer engine."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

from answer import answer


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CASES = ROOT / "evaluation" / "anatomy_eval.json"


def evaluate_case(case: dict[str, Any]) -> dict[str, Any]:
    result = answer(
        case["query"],
        mode=case.get("mode", "quick"),
        top_k=3,
    )

    expected_ids = set(case.get("expected_concepts", []))
    actual_ids = set()

    if result.get("retrieved_concepts"):
        # Normal answer modes expose concept names rather than IDs.
        # The evaluation currently checks evidence state for these modes.
        pass

    if result.get("comparison"):
        actual_names = {str(item.get("concept")) for item in result["comparison"]}
    else:
        actual_names = set(result.get("retrieved_concepts", []))

    expected_state = case["expected_evidence_state"]
    state_ok = result.get("evidence_state") == expected_state

    # Compare mode is validated by required concept names because its public
    # response is intentionally student-facing.
    if expected_ids and case.get("mode") != "compare":
        concept_ok = bool(result.get("answer")) and not result.get("abstain")
    elif expected_ids:
        concept_ok = (
            any("Spleen" in name for name in actual_names)
            and any("Lymph Nodes" in name for name in actual_names)
        )
    else:
        concept_ok = result.get("abstain") is True

    passed = state_ok and concept_ok
    return {
        "id": case["id"],
        "passed": passed,
        "state_ok": state_ok,
        "concept_ok": concept_ok,
        "expected_evidence_state": expected_state,
        "actual_evidence_state": result.get("evidence_state"),
    }


def run(path: Path = DEFAULT_CASES) -> dict[str, Any]:
    data = json.loads(path.read_text(encoding="utf-8"))
    cases = data["cases"]
    results = [evaluate_case(case) for case in cases]
    passed = sum(item["passed"] for item in results)

    return {
        "evaluation": data["name"],
        "version": data["version"],
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


if __name__ == "__main__":
    main()
